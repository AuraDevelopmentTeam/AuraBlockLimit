package dev.aura.blocklimit.counter;

import com.google.common.collect.ImmutableMap;
import dev.aura.blocklimit.AuraBlockLimit;
import dev.aura.blocklimit.message.PluginMessages;
import dev.aura.blocklimit.util.database.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.chat.ChatTypes;

@UtilityClass
public class BlockCounter {
  private static final Map<UUID, Map<String, Integer>> playerBlockCounts = new HashMap<>();
  private static final SortedSet<UUID> playersToSave = new TreeSet<>();

  @Setter private static DataSource dataSource;

  private static Task task;

  public static void startTask() {
    task =
        Sponge.getScheduler()
            .createTaskBuilder()
            .async()
            .interval(
                AuraBlockLimit.getConfig().getStorage().getSaveInterval(), TimeUnit.MILLISECONDS)
            .execute(BlockCounter::saveNow)
            .submit(AuraBlockLimit.getInstance());
  }

  public static void stopTask() {
    task.cancel();
  }

  public static void loadPlayer(Player player) {
    final UUID uuid = player.getUniqueId();

    playerBlockCounts.remove(uuid);

    // Load data async
    Sponge.getScheduler()
        .createTaskBuilder()
        .async()
        .execute(
            () ->
                dataSource
                    .getBlockCounts(player)
                    .ifPresent(blockCounts -> playerBlockCounts.put(uuid, blockCounts)))
        .submit(AuraBlockLimit.getInstance());
  }

  public static void savePlayer(Player player) {
    synchronized (playersToSave) {
      playersToSave.add(player.getUniqueId());
    }
  }

  public static void savePlayerNow(Player player) {
    savePlayerNow(player, true);
  }

  private static void savePlayerNow(Player player, boolean removeData) {
    final UUID uuid = player.getUniqueId();

    dataSource.saveBlockCounts(player, playerBlockCounts.get(uuid));

    if (removeData) {
      playerBlockCounts.remove(uuid);
    }
  }

  public static void saveNow() {
    List<Player> savingPlayers;

    synchronized (playersToSave) {
      savingPlayers =
          playersToSave
              .stream()
              .map(Sponge.getServer()::getPlayer)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toList());

      playersToSave.clear();
    }

    savingPlayers.forEach(player -> savePlayerNow(player, false));
  }

  public static void breakBlock(Player player, BlockSnapshot block) {
    breakBlock(player, block.getState());
  }

  public static void breakBlock(Player player, BlockState block) {
    final String type = block.getType().getId();
    final String id = block.getId();

    final boolean storeType = PlayerLimits.shouldStore(player, type);
    final boolean storeId = PlayerLimits.shouldStore(player, id);

    if (storeType) setBlockCount(player, type, Math.max(0, getBlockCount(player, type) - 1));
    if (storeId) setBlockCount(player, id, Math.max(0, getBlockCount(player, id) - 1));

    if (storeType || storeId) savePlayer(player);
  }

  public static boolean placeBlock(Player player, BlockSnapshot block) {
    return placeBlock(player, block.getState());
  }

  public static boolean placeBlock(Player player, BlockState block) {
    final String type = block.getType().getId();
    final String id = block.getId();

    final int typeCount = getBlockCount(player, type);
    final int idCount = getBlockCount(player, id);

    final int typeLimit = PlayerLimits.getLimit(player, type);
    final int idLimit = PlayerLimits.getLimit(player, id);

    if (!canPlaceOneMore(player, type, typeLimit, typeCount)) {
      showLimitMessage(player, type, typeLimit);
      return false;
    } else if (!canPlaceOneMore(player, id, idLimit, idCount)) {
      showLimitMessage(player, id, idLimit);
      return false;
    }

    final boolean storeType = typeLimit > PlayerLimits.IGNORE;
    final boolean storeId = idLimit > PlayerLimits.IGNORE;

    if (storeType) setBlockCount(player, type, typeCount + 1);
    if (storeId) setBlockCount(player, id, idCount + 1);

    if (storeType || storeId) savePlayer(player);

    return true;
  }

  public static int getBlockCount(Player player, String block) {
    return getBlockCount(player.getUniqueId(), block);
  }

  public static int getBlockCount(UUID uuid, String block) {
    return playerBlockCounts
        .computeIfAbsent(uuid, x -> new HashMap<>())
        .computeIfAbsent(block, x -> 0);
  }

  public static void setBlockCount(Player player, String block, int count) {
    setBlockCount(player.getUniqueId(), block, count);
  }

  public static void setBlockCount(UUID uuid, String block, int count) {
    playerBlockCounts.computeIfAbsent(uuid, x -> new HashMap<>()).put(block, count);
  }

  private static boolean canPlaceOneMore(Player player, String block, int limit, int currentCount) {
    return (limit <= PlayerLimits.UNLIMITED) ? true : (currentCount < limit);
  }

  private static void showLimitMessage(Player player, String block, int limit) {
    player.sendMessage(
        ChatTypes.ACTION_BAR,
        PluginMessages.LIMIT_LIMIT_REACHED.getMessage(
            ImmutableMap.of("limit", Integer.toString(limit), "block", block)));
  }
}
