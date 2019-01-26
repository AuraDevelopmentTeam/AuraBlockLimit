package dev.aura.blocklimit.counter;

import dev.aura.blocklimit.AuraBlockLimit;
import dev.aura.blocklimit.permission.PermissionRegistry;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.spongepowered.api.entity.living.player.Player;

@UtilityClass
public class PlayerLimits {
  public static final int UNLIMITED = -1;
  public static final int IGNORE = -2;

  private static final String PREFIX = PermissionRegistry.BASE + ".limit.";

  // TODO: Add shortterm cache
  public static int getLimit(Player player, String block) {
    return player
        .getOption(PREFIX + block)
        .flatMap(PlayerLimits::parseInt)
        .orElseGet(
            () -> AuraBlockLimit.getConfig().getStorage().getIgnoreUnset() ? IGNORE : UNLIMITED);
  }

  public static boolean shouldStore(Player player, String block) {
    final int limit = getLimit(player, block);

    return limit > IGNORE;
  }

  private static Optional<Integer> parseInt(String value) {
    try {
      return Optional.of(Integer.parseInt(value));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}
