package dev.aura.blocklimit.command;

import dev.aura.blocklimit.AuraBlockLimit;
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
      if (!src.hasPermission(RELOAD_PERMISSION)) throw new CommandPermissionException();

      Sponge.getScheduler()
          .createTaskBuilder()
          .async()
          .execute(
              () -> {
                try {
                  plugin.reload(null);

                  // TODO: show message
                } catch (Exception e) {
                  AuraBlockLimit.getLogger().error("Error while reloading the plugin:", e);
                }
              })
          .submit(plugin);
    } else {
      if (!src.hasPermission(SHOW_PERMISSION)) throw new CommandPermissionException();
      // TODO
    }

    return CommandResult.success();
  }
}
