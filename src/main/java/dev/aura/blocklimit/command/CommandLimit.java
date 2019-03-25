package dev.aura.blocklimit.command;

import com.google.common.collect.ImmutableMap;
import dev.aura.blocklimit.AuraBlockLimit;
import dev.aura.blocklimit.counter.BlockCounter;
import dev.aura.blocklimit.counter.PlayerLimits;
import dev.aura.blocklimit.message.PluginMessages;
import dev.aura.blocklimit.permission.PermissionRegistry;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
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
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandLimit implements CommandExecutor {
  public static final String BASE_PERMISSION = PermissionRegistry.COMMAND + ".limit";
  public static final String SHOW_PERMISSION = BASE_PERMISSION + ".base";
  public static final String RELOAD_PERMISSION = BASE_PERMISSION + ".reload";

  private static final String PARAM_RELOAD = "reload";

  private final AuraBlockLimit plugin;
  private final Text padding;

  private CommandLimit(AuraBlockLimit plugin) {
    this(plugin, PluginMessages.LIMIT_BLOCK_STATS_PADDING.getMessage());
  }

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
      final Optional<BlockState> blockState =
          player
              .getItemInHand(HandTypes.MAIN_HAND)
              .filter(item -> (item.getItem() != ItemTypes.NONE) && (item.getQuantity() > 0))
              .map(ItemStack::getItem)
              .flatMap(ItemType::getBlock)
              .map(BlockType::getDefaultState);

      if (!blockState.isPresent()) {
        throw new CommandException(PluginMessages.ERROR_NNO_BLOCK_IN_HAND.getMessage());
      }

      final BlockState block = blockState.get();
      final String type = block.getType().getId();
      final String id = block.getId();

      final int typeCount = BlockCounter.getBlockCount(player, type);
      final int idCount = BlockCounter.getBlockCount(player, id);

      final int typeLimit = PlayerLimits.getLimit(player, type);
      final int idLimit = PlayerLimits.getLimit(player, id);

      Map<String, String> replacements = null;

      if (typeLimit > PlayerLimits.UNLIMITED) {
        replacements =
            ImmutableMap.of(
                "block",
                type,
                "limit",
                Integer.toString(typeLimit),
                "count",
                Integer.toString(typeCount),
                "remaining",
                Integer.toString(typeLimit - typeCount));
      } else if (idLimit > PlayerLimits.UNLIMITED) {
        replacements =
            ImmutableMap.of(
                "block",
                id,
                "limit",
                Integer.toString(idLimit),
                "count",
                Integer.toString(idCount),
                "remaining",
                Integer.toString(idLimit - idCount));
      }

      Text title;
      Text message;

      if (replacements != null) {
        title = PluginMessages.LIMIT_BLOCK_STATS_TITLE.getMessage(replacements);
        message = PluginMessages.LIMIT_BLOCK_STATS.getMessage(replacements);
      } else {
        title = Text.EMPTY;
        message = PluginMessages.LIMIT_NO_LIMIT.getMessage(ImmutableMap.of("block", id));
      }

      PaginationList.builder().padding(padding).title(title).contents(message).sendTo(player);
    }

    return CommandResult.success();
  }
}
