package dev.aura.blocklimit.event;

import dev.aura.blocklimit.AuraBlockLimit;
import dev.aura.blocklimit.counter.BlockCounter;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

@RequiredArgsConstructor
public class PlayerEvents implements AutoCloseable {
  @Listener
  public void onPlayerJoin(ClientConnectionEvent.Join event) {
    BlockCounter.loadPlayer(event.getTargetEntity());
  }

  @Listener
  public void onPlayerLeave(ClientConnectionEvent.Disconnect event) throws IOException {
    BlockCounter.savePlayerNow(event.getTargetEntity());
  }

  public void saveAllPlayers() {
    for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) {
      BlockCounter.savePlayer(player);
    }

    AuraBlockLimit.getLogger().debug("Saved all player block counts");
  }

  @Override
  public void close() {
    saveAllPlayers();
  }
}
