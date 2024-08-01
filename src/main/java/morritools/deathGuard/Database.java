package morritools.deathGuard;

import java.io.File;
import java.sql.*;

public class Database {
    private final DeathGuard plugin;
    private Connection connection;

    public Database(DeathGuard plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            File databaseDir = new File("plugins/DeathGuard");
            if (!databaseDir.exists()) {
                boolean created = databaseDir.mkdirs();
                if (created) {
                    plugin.getLogger().config("Directory created successfully.");
                } else {
                    plugin.getLogger().config("Directory already exists or could not be created.");
                }
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + new File(databaseDir, "database.db").getAbsolutePath());
            plugin.getLogger().info("Connected to SQLite database");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Disconnected from SQLite database");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS player_data (" +
                "player_uuid TEXT PRIMARY KEY, " +
                "data_strings TEXT NOT NULL);";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            plugin.getLogger().info("Table created or already exists.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertPlayerData(String playerUUID, String newDataString) {
        String selectSQL = "SELECT data_strings FROM player_data WHERE player_uuid = ?";
        String existingData = "";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setString(1, playerUUID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                existingData = rs.getString("data_strings");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String updatedData = existingData.isEmpty() ? newDataString : existingData + "|" + newDataString;

        String insertOrUpdateSQL = "INSERT INTO player_data(player_uuid, data_strings) VALUES(?, ?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET data_strings = excluded.data_strings";
        try (PreparedStatement pstmt = connection.prepareStatement(insertOrUpdateSQL)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, updatedData);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerData(String playerUUID) {
        String selectSQL = "SELECT data_strings FROM player_data WHERE player_uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setString(1, playerUUID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("data_strings");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getNextDeathId(String playerUUID) {
        String playerData = getPlayerData(playerUUID);
        if (playerData == null || playerData.isEmpty()) {
            return 1;
        }

        String[] entries = playerData.split("\\|");
        int numberOfEntries = entries.length;

        return numberOfEntries + 1;
    }
    public void purgeAllData() {
        String deleteSQL = "DELETE FROM player_data";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(deleteSQL);
            plugin.getLogger().info("All death data has been purged.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void purgeUserData(String uuid) {
        String selectSQL = "SELECT player_uuid FROM player_data WHERE player_uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String playerUUID = rs.getString("player_uuid");
                String deleteSQL = "DELETE FROM player_data WHERE player_uuid = ?";
                try (PreparedStatement deletePstmt = connection.prepareStatement(deleteSQL)) {
                    deletePstmt.setString(1, playerUUID);
                    deletePstmt.executeUpdate();
                    plugin.getLogger().info("Death data for player " + uuid + " has been purged.");
                }
            } else {
                plugin.getLogger().warning("No data found for player " + uuid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
