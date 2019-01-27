package dev.aura.blocklimit.util.database;

import dev.aura.blocklimit.AuraBlockLimit;
import dev.aura.blocklimit.config.Config;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import org.spongepowered.api.entity.living.player.Player;

@SuppressFBWarnings(
  value = {
    "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
    "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE"
  },
  justification = "The database name needs to be dynamic in order to allow prefixes"
)
public class DataSource {
  private final DatabaseConnection connection;
  @Getter private final Config.Storage storageConfig;

  private final String tableBlocks;
  private final String tableBlocksColumnID;
  private final String tableBlocksColumnBlock;

  private final String tableBlockCounts;
  private final String tableBlockCountsColumnUUID;
  private final String tableBlockCountsColumnBlockID;
  private final String tableBlockCountsColumnCount;

  private final String getBlockIDSubQuery;
  private final String getBlockNameSubQuery;

  private final String getBlockQuery;
  private final String addBlockQuery;
  private final String saveBlockCountQuery;
  private final String getBlockCountsQuery;

  public static String getPlayerString(Player player) {
    if (player == null) return "<unknown>";
    else return player.getName() + " (" + player.getUniqueId().toString() + ')';
  }

  private static byte[] getBytesFromUUID(UUID uuid) {
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());

    return bb.array();
  }

  public DataSource() throws SQLException {
    storageConfig = AuraBlockLimit.getConfig().getStorage();

    if (storageConfig.isH2()) {
      connection = new H2DatabaseConnection(storageConfig.getH2());
    } else if (storageConfig.isMySQL()) {
      connection = new MysqlDatabaseConnection(storageConfig.getMysql());
    } else throw new IllegalArgumentException("Invalid storage Engine!");

    tableBlocks = getTableName("blocks");
    tableBlocksColumnID = "ID";
    tableBlocksColumnBlock = "Block";

    tableBlockCounts = getTableName("blockcounts");
    tableBlockCountsColumnUUID = "UUID";
    tableBlockCountsColumnBlockID = "BlockID";
    tableBlockCountsColumnCount = "Count";

    prepareTables();

    StringBuilder getBlockIDStr = new StringBuilder();
    StringBuilder getBlockNameStr = new StringBuilder();

    getBlockIDStr
        .append("SELECT ")
        .append(tableBlocksColumnID)
        .append(" FROM ")
        .append(tableBlocks)
        .append(" WHERE ")
        .append(tableBlocksColumnBlock)
        .append(" = ? LIMIT 1");
    getBlockNameStr
        .append("SELECT ")
        .append(tableBlocksColumnBlock)
        .append(" FROM ")
        .append(tableBlocks)
        .append(" WHERE ")
        .append(tableBlocksColumnID)
        .append(" = ")
        .append(tableBlockCountsColumnBlockID)
        .append(" LIMIT 1");

    getBlockIDSubQuery = getBlockIDStr.toString();
    getBlockNameSubQuery = getBlockNameStr.toString();

    StringBuilder getBlockStr = new StringBuilder();
    StringBuilder addBlockStr = new StringBuilder();
    StringBuilder saveBlockCountStr = new StringBuilder();
    StringBuilder getBlockCountsStr = new StringBuilder();

    getBlockStr
        .append("SELECT 1 FROM ")
        .append(tableBlocks)
        .append(" WHERE ")
        .append(tableBlocksColumnBlock)
        .append("= ? LIMIT 1");
    addBlockStr
        .append("INSERT INTO ")
        .append(tableBlocks)
        .append(" (")
        .append(tableBlocksColumnBlock)
        .append(") VALUES (?)");
    saveBlockCountStr
        .append("REPLACE INTO ")
        .append(tableBlockCounts)
        .append(" (")
        .append(tableBlockCountsColumnUUID)
        .append(", ")
        .append(tableBlockCountsColumnBlockID)
        .append(", ")
        .append(tableBlockCountsColumnCount)
        .append(") VALUES (?, (")
        .append(getBlockIDSubQuery)
        .append("), ?)");
    getBlockCountsStr
        .append("SELECT (")
        .append(getBlockNameSubQuery)
        .append(") AS Block, ")
        .append(tableBlockCountsColumnCount)
        .append(" FROM ")
        .append(tableBlockCounts)
        .append(" WHERE ")
        .append(tableBlockCountsColumnUUID)
        .append(" = ?");

    getBlockQuery = getBlockStr.toString();
    addBlockQuery = addBlockStr.toString();
    saveBlockCountQuery = saveBlockCountStr.toString();
    getBlockCountsQuery = getBlockCountsStr.toString();
  }

  public void saveBlockCounts(Player player, Map<String, Integer> blockCounts) {
    String playerName = getPlayerString(player);

    try (PreparedStatement getBlock = connection.getPreparedStatement(getBlockQuery);
        PreparedStatement addBlock = connection.getPreparedStatement(addBlockQuery);
        PreparedStatement saveBlockCount = connection.getPreparedStatement(saveBlockCountQuery);
        Connection connectionGetBlock = getBlock.getConnection();
        Connection connectionAddBlock = addBlock.getConnection();
        Connection connectionSaveBlockCount = saveBlockCount.getConnection()) {
      AuraBlockLimit.getLogger().debug("Saving block counts for player " + playerName);

      final byte[] uuid = getBytesFromUUID(player.getUniqueId());
      String block;
      int count;

      for (Map.Entry<String, Integer> entry : blockCounts.entrySet()) {
        block = entry.getKey();
        count = entry.getValue();

        getBlock.setString(1, block);

        try (ResultSet result = getBlock.executeQuery()) {
          getBlock.clearParameters();

          if (!result.next()) {
            addBlock.setString(1, block);

            addBlock.executeUpdate();
            addBlock.clearParameters();
          }

          saveBlockCount.setBytes(1, uuid);
          saveBlockCount.setString(2, block);
          saveBlockCount.setInt(3, count);

          saveBlockCount.executeUpdate();
          saveBlockCount.clearParameters();
        }
      }
    } catch (SQLException e) {
      AuraBlockLimit.getLogger().error("Could not save invetory for player " + playerName, e);
    }
  }

  public Optional<Map<String, Integer>> getBlockCounts(Player player) {
    String playerName = getPlayerString(player);

    try (PreparedStatement getBlockCounts = connection.getPreparedStatement(getBlockCountsQuery);
        Connection connection = getBlockCounts.getConnection()) {
      AuraBlockLimit.getLogger().debug("Loading block counts  for player " + playerName);

      getBlockCounts.setBytes(1, getBytesFromUUID(player.getUniqueId()));

      try (ResultSet result = getBlockCounts.executeQuery()) {
        getBlockCounts.clearParameters();

        if (!result.next()) return Optional.empty();

        final Map<String, Integer> resultMap = new HashMap<>();

        do {
          resultMap.put(result.getString("Block"), result.getInt("Count"));
        } while (result.next());

        return Optional.of(resultMap);
      }
    } catch (SQLException e) {
      AuraBlockLimit.getLogger().error("Could not load block counts for player " + playerName, e);

      return Optional.empty();
    }
  }

  private String getTableName(String baseName) {
    String name;

    if (storageConfig.isH2()) {
      name = baseName;
    } else if (storageConfig.isMySQL()) {
      name = storageConfig.getMysql().getTablePrefix() + baseName;
    } else return null;

    name = name.replaceAll("`", "``");

    return '`' + name + '`';
  }

  private void prepareTables() {
    try {
      StringBuilder createTableBlocks = new StringBuilder();
      StringBuilder createTableBlockCounts = new StringBuilder();

      createTableBlocks
          .append("CREATE TABLE IF NOT EXISTS ")
          .append(tableBlocks)
          .append(" (")
          .append(tableBlocksColumnID)
          .append(" INT NOT NULL AUTO_INCREMENT, ")
          .append(tableBlocksColumnBlock)
          .append(" VARCHAR(128) NOT NULL, PRIMARY KEY (")
          .append(tableBlocksColumnID)
          .append("), UNIQUE (")
          .append(tableBlocksColumnBlock)
          .append(")) DEFAULT CHARSET=utf8mb4");
      createTableBlockCounts
          .append("CREATE TABLE IF NOT EXISTS ")
          .append(tableBlockCounts)
          .append(" (")
          .append(tableBlockCountsColumnUUID)
          .append(" BINARY(16) NOT NULL, ")
          .append(tableBlockCountsColumnBlockID)
          .append(" INT NOT NULL, ")
          .append(tableBlockCountsColumnCount)
          .append(" INT NOT NULL, PRIMARY KEY (")
          .append(tableBlockCountsColumnUUID)
          .append(", ")
          .append(tableBlockCountsColumnBlockID)
          .append("), FOREIGN KEY (")
          .append(tableBlockCountsColumnBlockID)
          .append(") REFERENCES ")
          .append(tableBlocks)
          .append('(')
          .append(tableBlocksColumnID)
          .append(")) DEFAULT CHARSET=utf8mb4");

      connection.executeStatement(createTableBlocks.toString());
      connection.executeStatement(createTableBlockCounts.toString());

      AuraBlockLimit.getLogger().debug("Created tables");
    } catch (SQLException e) {
      AuraBlockLimit.getLogger().error("Could not create tables!", e);
    }
  }
}
