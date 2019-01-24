package dev.aura.blocklimit.util.database;

import dev.aura.blocklimit.config.Config;
import java.sql.SQLException;

public class MysqlDatabaseConnection extends DatabaseConnection {
  private static final String URLFormat = "jdbc:mysql://%s:%s@%s:%d/%s";

  /**
   * Opens a MySQL database connection.
   *
   * @param mysql MySQL config object
   * @throws SQLException
   */
  public MysqlDatabaseConnection(Config.Storage.MySQL mysql) throws SQLException {
    super(
        String.format(
            URLFormat,
            mysql.getUser(),
            mysql.getPassword(),
            mysql.getHost(),
            mysql.getPort(),
            mysql.getDatabase()));
  }
}
