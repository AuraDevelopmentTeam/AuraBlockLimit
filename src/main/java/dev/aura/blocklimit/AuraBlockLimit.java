package dev.aura.blocklimit;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import dev.aura.blocklimit.command.CommandLimit;
import dev.aura.blocklimit.config.Config;
import dev.aura.blocklimit.counter.BlockCounter;
import dev.aura.blocklimit.event.PlayerEvents;
import dev.aura.blocklimit.permission.PermissionRegistry;
import dev.aura.blocklimit.util.database.DataSource;
import dev.aura.blocklimit.util.metrics.FeatureChart;
import dev.aura.lib.messagestranslator.MessagesTranslator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.GuiceObjectMapperFactory;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.bstats.sponge.Metrics2;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

@Plugin(
  id = AuraBlockLimit.ID,
  name = AuraBlockLimit.NAME,
  version = AuraBlockLimit.VERSION,
  description = AuraBlockLimit.DESCRIPTION,
  url = AuraBlockLimit.URL,
  authors = {AuraBlockLimit.AUTHOR}
)
public class AuraBlockLimit {
  public static final String ID = "@id@";
  public static final String NAME = "@name@";
  public static final String VERSION = "@version@";
  public static final String DESCRIPTION = "@description@";
  public static final String URL = "https://github.com/AuraDevelopmentTeam/AuraBlockLimit";
  public static final String AUTHOR = "The_BrainStone";

  @NonNull @Getter private static AuraBlockLimit instance = null;

  @Inject @NonNull private PluginContainer container;
  @Inject private Metrics2 metrics;
  @Inject @NonNull private Logger logger;

  @Inject private GuiceObjectMapperFactory factory;

  @Inject
  @DefaultConfig(sharedRoot = false)
  private ConfigurationLoader<CommentedConfigurationNode> loader;

  @Inject
  @ConfigDir(sharedRoot = false)
  @NonNull
  private Path configDir;

  @NonNull private Config config;
  @NonNull private MessagesTranslator translator;

  @NonNull private DataSource dataSource;
  private PermissionRegistry permissionRegistry;
  private List<AutoCloseable> eventListeners = new LinkedList<>();

  public AuraBlockLimit() {
    if (instance != null) throw new IllegalStateException("Instance already exists!");

    instance = this;
  }

  public static PluginContainer getContainer() {
    return instance.container;
  }

  public static Logger getLogger() {
    return instance.logger;
  }

  public static Path getConfigDir() {
    return instance.configDir;
  }

  public static Config getConfig() {
    return instance.config;
  }

  public static MessagesTranslator getTranslator() {
    return instance.translator;
  }

  public static DataSource getDataSource() {
    return instance.dataSource;
  }

  @Listener
  public void init(GameInitializationEvent event)
      throws SQLException, IOException, ObjectMappingException {
    logger.info("Initializing " + NAME + " Version " + VERSION);

    if (VERSION.contains("SNAPSHOT")) {
      logger.warn("WARNING! This is a snapshot version!");
      logger.warn("Use at your own risk!");
    }
    if (VERSION.contains("development")) {
      logger.info("This is a unreleased development version!");
      logger.info("Things might not work properly!");
    }

    loadConfig();

    if (permissionRegistry == null) {
      permissionRegistry = new PermissionRegistry(this);
      logger.debug("Registered permissions");
    }

    translator =
        new MessagesTranslator(
            new File(getConfigDir().toFile(), "lang"), config.getGeneral().getLanguage(), this);

    dataSource = new DataSource();
    BlockCounter.setDataSource(dataSource);
    BlockCounter.startTask();

    CommandLimit.register(this);

    addEventListener(new PlayerEvents());
    logger.debug("Registered events");

    logger.info("Loaded successfully!");
  }

  @Listener
  public void onServerStart(GameStartedServerEvent event) {
    metrics.addCustomChart(new FeatureChart("features"));
  }

  @Listener
  public void reload(GameReloadEvent event) throws Exception {
    Cause cause =
        Cause.builder()
            .append(this)
            .build(EventContext.builder().add(EventContextKeys.PLUGIN, container).build());

    // Unregistering everything
    GameStoppingEvent gameStoppingEvent = SpongeEventFactory.createGameStoppingEvent(cause);
    stop(gameStoppingEvent);

    // Starting over
    GameInitializationEvent gameInitializationEvent =
        SpongeEventFactory.createGameInitializationEvent(cause);
    init(gameInitializationEvent);

    logger.info("Reloaded successfully!");
  }

  @Listener
  public void stop(GameStoppingEvent event) throws Exception {
    logger.info("Shutting down " + NAME + " Version " + VERSION);

    removeCommands();
    logger.debug("Unregistered commands");

    removeEventListeners();
    logger.debug("Unregistered events");

    BlockCounter.stopTask();
    logger.debug("Stopped saving task");

    dataSource = null;
    logger.debug("Closed database connection");

    config = null;
    logger.debug("Unloaded config");

    logger.info("Unloaded successfully!");
  }

  private void loadConfig() throws IOException, ObjectMappingException {
    final TypeToken<Config> configToken = TypeToken.of(Config.class);

    logger.debug("Loading config...");

    CommentedConfigurationNode node =
        loader.load(ConfigurationOptions.defaults().setObjectMapperFactory(factory));

    final Object globalValue = node.getNode("global").getValue();

    if (globalValue != null) {
      node.getNode("general").setValue(globalValue);
      node.removeChild("global");
    }

    try {
      config = node.<Config>getValue(configToken, Config::new);
    } catch (ObjectMappingException e) {
      final String message = e.getMessage();

      if (!message.startsWith("Invalid enum constant provided for storageEngine:")) throw e;

      final String defaultStorageEngine = (new Config.Storage()).getStorageEngine().name();

      logger.error(message);
      logger.warn("Possible values are: " + Config.Storage.StorageEngine.allowedValues);
      logger.warn("To fix your config we changed the storage engine to " + defaultStorageEngine);

      node.getNode("storage", "storageEngine").setValue(defaultStorageEngine);

      config = node.<Config>getValue(configToken, Config::new);
    }

    logger.debug("Saving/Formatting config...");
    node.setValue(configToken, config);
    loader.save(node);
  }

  private void addEventListener(AutoCloseable listener) {
    eventListeners.add(listener);

    Sponge.getEventManager().registerListeners(this, listener);
  }

  private void removeCommands() {
    final CommandManager commandManager = Sponge.getCommandManager();

    commandManager.getOwnedBy(this).forEach(commandManager::removeMapping);
  }

  private void removeEventListeners() throws Exception {
    for (AutoCloseable listener : eventListeners) {
      Sponge.getEventManager().unregisterListeners(listener);

      listener.close();
    }

    eventListeners.clear();
  }
}
