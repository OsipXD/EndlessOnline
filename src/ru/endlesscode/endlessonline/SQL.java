package ru.endlesscode.endlessonline;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by OsipXD on 13.09.2015
 * It is part of the EndlessOnline.
 * All rights reserved 2014 - 2015 © «EndlessCode Group»
 */
class SQL {
    private final Thread executor = new SQLExecutor();

    private final String url;
    private final String username;
    private final String password;

    private FileConfiguration config;
    private boolean killed;
    private String query;

    public SQL() {
        this.reloadConfig();
        this.url = "jdbc:mysql://" + this.config.getString("sql.host") + ":" + this.config.getString("sql.port") + "/" + this.config.getString("sql.db");
        this.username = this.config.getString("sql.user");
        this.password = this.config.getString("sql.pass");
        this.killed = false;
    }

    private void executeQuery() {
        if (this.killed) {
            return;
        }

        if (this.executor.isAlive()) {
            try {
                this.executor.join();
            } catch (InterruptedException ignored) {
            }
        }

        this.executor.run();
    }

    public void reloadConfig() {
        this.config = EndlessOnline.getInstance().getConfig();
    }

    public void updateOnline(int online) {
        List<String> playerList = new ArrayList<>();
        for (Player player : EndlessOnline.getInstance().getServer().getOnlinePlayers()) {
            playerList.add(player.getName());
        }

        this.query = "UPDATE " + this.config.getString("sql.table")
                + " SET online = " + online
                + ", max_online = " + Bukkit.getMaxPlayers()
                + ", players = '" + playerList + "' "
                + "WHERE server = '" + this.config.getString("server-name") + "';";
        this.executeQuery();
    }

    public boolean tableExists() {
        EndlessOnline.getInstance().getLogger().info("Checking the existence of the table...");
        try {
            Connection conn = DriverManager.getConnection(SQL.this.url, SQL.this.username, SQL.this.password);
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, this.config.getString("sql.table"), null);

            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            this.killed = true;
            EndlessOnline.getInstance().getLogger().severe("SQL Error: " + e);
        }

        return false;
    }

    public boolean serverExists() {
        EndlessOnline.getInstance().getLogger().info("Checking the server existence in the table...");
        try {
            Connection conn = DriverManager.getConnection(SQL.this.url, SQL.this.username, SQL.this.password);
            PreparedStatement statement = conn.prepareStatement("SELECT 1 FROM " + this.config.getString("sql.table") + " WHERE server = '" + this.config.getString("server-name") + "'");
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            this.killed = true;
            e.printStackTrace();
        }

        return false;
    }

    public void createTable() {
        EndlessOnline.getInstance().getLogger().info("Creating table...");
        this.query = "CREATE TABLE " + this.config.getString("sql.table") + " (\n" +
                "server varchar(30) NOT NULL,\n" +
                "online int(3) DEFAULT '-1',\n" +
                "max_online int(3) NOT NULL,\n" +
                "players varchar(8000) DEFAULT NULL);";
        this.executeQuery();
        this.addServer();
    }

    public void addServer() {
        EndlessOnline.getInstance().getLogger().info("Adding server to table...");
        this.query = "INSERT INTO " + this.config.getString("sql.table") + " (server, max_online) " +
                "VALUE ('" + this.config.getString("server-name") + "', " + Bukkit.getMaxPlayers() + ");";
        this.executeQuery();

        try {
            this.executor.join();
        } catch (InterruptedException ignored) {
        }

        if (this.killed) {
            EndlessOnline.getInstance().getLogger().warning("Failed to add server to DB!");
        }
    }

    public boolean isKilled() {
        return this.killed;
    }

    private class SQLExecutor extends Thread {
        @Override
        public void run() {
            try {
                Connection conn = DriverManager.getConnection(SQL.this.url, SQL.this.username, SQL.this.password);
                PreparedStatement statement = conn.prepareStatement(SQL.this.query);
                statement.executeUpdate();
            } catch (SQLException e) {
                EndlessOnline.getInstance().getLogger().severe("SQL Error: " + e);
                SQL.this.killed = true;
            }
        }
    }
}
