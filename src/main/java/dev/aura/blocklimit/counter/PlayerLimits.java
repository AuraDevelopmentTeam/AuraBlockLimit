package dev.aura.blocklimit.counter;

import dev.aura.blocklimit.permission.PermissionRegistry;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.spongepowered.api.entity.living.player.Player;

@UtilityClass
public class PlayerLimits {
  private static final String PREFIX = PermissionRegistry.BASE + ".limit.";

  public static int getLimit(Player player, String block) {
    return player.getOption(PREFIX + block).flatMap(PlayerLimits::parseInt).orElse(-1);
  }

  public static boolean canPlaceOneMore(Player player, String block, int currentCount) {
    final int limit = getLimit(player, block);

    return (limit < 0) ? true : (currentCount < limit);
  }

  private static Optional<Integer> parseInt(String value) {
    try {
      return Optional.of(Integer.parseInt(value));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}
