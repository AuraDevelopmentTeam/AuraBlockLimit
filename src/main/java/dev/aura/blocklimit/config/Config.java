package dev.aura.blocklimit.config;

import dev.aura.blocklimit.AuraBlockLimit;
import dev.aura.blocklimit.util.database.DatabaseConnection;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class Config {
  @Setting @Getter private General general = new General();

  @Setting @Getter private Storage storage = new Storage();

  @ConfigSerializable
  public static class General {
    @Setting(comment = "Enable debug logging")
    @Getter
    private boolean debug = false;

    @Setting(
        comment =
            "Select which language from the lang dir to use.\n"
                + "You can add your own translations in there. If you name your file \"test.lang\", choose \"test\" here.")
    @Getter
    private String language = "en_US";
  }

  @ConfigSerializable
  public static class Storage {
    @Setting(comment = "The stoage engine that should be used\n" + "Allowed values: h2 mysql")
    @Getter
    private StorageEngine storageEngine = StorageEngine.h2;

    @Setting(
        comment =
            "If this is true blocks that do not have a limit set at all will not be stored in the database. You can still manually\n"
                + "make this plugin ignore blocks by setting the limit to -2 (which means unlimited and dont't store) instead of just -1\n"
                + "(which means unlimited)")
    @Getter
    private boolean ignoreUnset = true;

    @Setting(comment = "The interval in which the counts are saved to storage (in ms)")
    @Getter
    private int saveInterval = 10_000;

    @Setting(comment = "Settings for the h2 storage engine")
    @Getter
    private H2 h2 = new H2();

    @Setting(value = "MySQL", comment = "Settings for the MySQL storage engine")
    @Getter
    private MySQL mysql = new MySQL();

    public boolean isH2() {
      return getStorageEngine() == Config.Storage.StorageEngine.h2;
    }

    public boolean isMySQL() {
      return getStorageEngine() == Config.Storage.StorageEngine.mysql;
    }

    public static enum StorageEngine {
      h2,
      mysql;

      public static final String allowedValues =
          Arrays.stream(Config.Storage.StorageEngine.values())
              .map(Enum::name)
              .collect(Collectors.joining(", "));
    }

    @ConfigSerializable
    public static class H2 {
      @Setting(
          comment =
              "If this is a relative path, it will be relative to the AuraBlockLimit config dir (should be \"config/blocklimit\").\n"
                  + "Absolute paths work too of course")
      @Getter
      private String databaseFile = "blockCounts";

      public Path getAbsoluteDatabasePath() {
        return AuraBlockLimit.getConfigDir().resolve(getDatabaseFile()).toAbsolutePath();
      }
    }

    @ConfigSerializable
    public static class MySQL {
      @Setting @Getter private String host = "localhost";
      @Setting @Getter private int port = DatabaseConnection.DEFAULT_MYSQL_PORT;
      @Setting @Getter private String database = "blocklimit";
      @Setting @Getter private String user = "blocklimit";
      @Setting @Getter private String password = "sup3rS3cur3Pa55w0rd!";

      @Setting(comment = "Prefix for the plugin tables")
      @Getter
      private String tablePrefix = "blocklimit_";
    }
  }
}
