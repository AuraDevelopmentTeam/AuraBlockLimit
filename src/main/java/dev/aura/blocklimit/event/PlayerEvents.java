package dev.aura.blocklimit.event;

import dev.aura.blocklimit.AuraBlockLimit;
import dev.aura.blocklimit.counter.BlockCounter;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
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

  @Listener
  public void onBlockBreak(ChangeBlockEvent.Break event, @First Player player) {
    for (Transaction<BlockSnapshot> block : event.getTransactions()) {
      BlockCounter.breakBlock(player, block.getOriginal());
    }
  }

  @Listener
  public void onBlockPlace(ChangeBlockEvent.Place event, @First Player player) {
    for (Transaction<BlockSnapshot> block : event.getTransactions()) {
      BlockCounter.placeBlock(player, block.getFinal());
    }
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
