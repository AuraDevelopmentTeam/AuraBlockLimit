package dev.aura.blocklimit.command;

import com.google.common.collect.ImmutableMap;
import dev.aura.blocklimit.AuraBlockLimit;
import dev.aura.blocklimit.message.PluginMessages;
import dev.aura.blocklimit.permission.PermissionRegistry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandLimit implements CommandExecutor {
  public static final String BASE_PERMISSION = PermissionRegistry.COMMAND + ".limit";
  public static final String SHOW_PERMISSION = BASE_PERMISSION + ".base";
  public static final String RELOAD_PERMISSION = BASE_PERMISSION + ".reload";

  private static final String PARAM_RELOAD = "reload";

  private final AuraBlockLimit plugin;

  public static void register(AuraBlockLimit plugin) {
    CommandSpec realTime =
        CommandSpec.builder()
            .description(Text.of("Enables or disables synchronizing the world time with realtime."))
            .executor(new CommandLimit(plugin))
            .arguments(
                GenericArguments.optional(
                    GenericArguments.literal(Text.of(PARAM_RELOAD), PARAM_RELOAD)))
            .build();

    Sponge.getCommandManager().register(plugin, realTime, "limit", "blocklimit", "bl");
  }

  @Override
  public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
    if (args.hasAny(PARAM_RELOAD)) {
      if (!src.hasPermission(RELOAD_PERMISSION)) {
        throw new CommandPermissionException();
      }

      Sponge.getScheduler()
          .createTaskBuilder()
          .async()
          .execute(
              () -> {
                try {
                  plugin.reload(null);

                  src.sendMessage(PluginMessages.ADMIN_REALOAD_SUCCESSFUL.getMessage());
                } catch (Exception e) {
                  AuraBlockLimit.getLogger().error("Error while reloading the plugin:", e);
                  src.sendMessage(
                      PluginMessages.ADMIN_REALOAD_NOT_SUCCESSFUL.getMessage(
                          ImmutableMap.of("error", e.getMessage())));
                }
              })
          .submit(plugin);
    } else {
      if (!src.hasPermission(SHOW_PERMISSION)) {
        throw new CommandPermissionException();
      } else if (!(src instanceof Player)) {
        throw new CommandException(PluginMessages.ERROR_NOT_A_PLAYER.getMessage());
      }

      final Player player = (Player) src;

      player.getItemInHand(HandTypes.MAIN_HAND);
      // TODO
    }

    return CommandResult.success();
  }
}