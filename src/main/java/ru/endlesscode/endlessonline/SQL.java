/*
 * This file is part of EndlessOnline.
 * Copyright (C) 2017 Osip Fatkullin
 *
 * EndlessOnline is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EndlessOnline is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EndlessOnline.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.endlesscode.endlessonline;

import com.jcraft.jsch.JSchException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by OsipXD on 13.09.2015
 * It is part of the EndlessOnline.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
class SQL {
    private final Thread executor = new Thread(new SQLExecutor());

    private final String url;
    private final String username;
    private final String password;
    private final String table;
    private final String server;

    private FileConfiguration config;
    private boolean killed;
    private String query;

    private SSHTunnel tunnel;
    private Connection connection;

    public SQL() {
        this.reloadConfig();

        String host = this.config.getString("sql.host");
        String db = this.config.getString("sql.db");
        int port = this.config.getInt("sql.port");

        this.username = this.config.getString("sql.user");
        this.password = this.config.getString("sql.pass");
        this.table = this.config.getString("sql.table");
        this.server = this.config.getString("server-name");

        if (this.config.getBoolean("tunnel.enabled")) {
            try {
                this.tunnel = new SSHTunnel(this.config, host, port);
            } catch (JSchException e) {
                this.url = null;
                EndlessOnline.getInstance().getLogger().severe("Error when configuring SSH tunnel: " + e);
                this.killed = true;
                return;
            }

            this.url = "jdbc:mysql://localhost:" + this.tunnel.getLocalPort() + "/" + db;
        } else {
            this.url = "jdbc:mysql://" + host + ":" + port + "/" + db;
            this.tunnel = null;
        }

        try {
            this.connect();
            EndlessOnline.getInstance().getLogger().info("Test SQL connection was successful!");
            this.disconnect();
        } catch (SQLException | JSchException e) {
            this.killed = true;
            EndlessOnline.getInstance().getLogger().severe("Test SQL connection failed: " + e);
            e.printStackTrace();
            return;
        }

        this.killed = false;
    }

    private void connect() throws SQLException, JSchException {
        if (this.tunnel != null) {
            this.tunnel.connect();
        }

        if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }

        this.connection = DriverManager.getConnection(this.url, this.username, this.password);
    }

    private void disconnect() throws SQLException {
        if (this.tunnel != null) {
            this.tunnel.disconnect();
        }

        if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }
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

    void reloadConfig() {
        this.config = EndlessOnline.getInstance().getConfig();
    }

    void clearOnline(int maxOnline) {
        this.updateOnline(-1, maxOnline, new ArrayList<String>(0));
    }

    void updateOnline(int online, int maxOnline, List<String> playerList) {
        this.query = "UPDATE " + this.table + " SET \n" +
                "  online = " + online + ", \n" +
                "  capacity = " + Bukkit.getMaxPlayers() + ", \n" +
                "  players = '" + playerList + "',\n" +
                "  max_online = " + maxOnline + "\n" +
                "WHERE server = '" + this.server + "'";
        this.executeQuery();
    }

    boolean tableExists() {
        EndlessOnline.getInstance().getLogger().info("Checking the existence of the table...");
        try {
            this.connect();
            DatabaseMetaData meta = this.connection.getMetaData();
            ResultSet rs = meta.getTables(null, null, this.table, null);

            if (rs.next()) {
                return true;
            }

            this.disconnect();
        } catch (SQLException | JSchException e) {
            this.killed = true;
            EndlessOnline.getInstance().getLogger().severe("Connection to DB failed: " + e);
        }

        return false;
    }

    int getMaxOnline() {
        try {
            this.connect();
            PreparedStatement statement = this.connection.prepareStatement("SELECT max_online, last_update FROM " + this.table +
                    " WHERE server = '" + this.server + "'");
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt("max_online");
            }

            this.disconnect();
        } catch (SQLException | JSchException e) {
            this.killed = true;
            e.printStackTrace();
        }

        return -1;
    }

    boolean serverExists() {
        EndlessOnline.getInstance().getLogger().info("Checking the server existence in the table...");
        try {
            this.connect();
            PreparedStatement statement = this.connection.prepareStatement("SELECT 1 FROM " + this.table +
                    " WHERE server = '" + this.server + "'");
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return true;
            }

            this.disconnect();
        } catch (SQLException | JSchException e) {
            this.killed = true;
            e.printStackTrace();
        }

        return false;
    }

    void createTable() {
        EndlessOnline.getInstance().getLogger().info("Creating table...");
        this.query = "CREATE TABLE " + this.table + " (\n" +
                "  server         VARCHAR(30)   NOT NULL,\n" +
                "  online         INT(4)        DEFAULT '-1',\n" +
                "  capacity       INT(4)        NOT NULL,\n" +
                "  players        VARCHAR(8000) DEFAULT NULL,\n" +
                "  max_online     INT(4)        DEFAULT '0',\n" +
                "  last_update    TIMESTAMP     NOT NULL\n" +
                ")";
        this.executeQuery();
        this.addServer();
    }

    void addServer() {
        EndlessOnline.getInstance().getLogger().info("Adding server to table...");
        this.query = "INSERT INTO " + this.table + " (server, capacity)\n" +
                "  VALUE ('" + this.server + "', " + Bukkit.getMaxPlayers() + ")";
        this.executeQuery();

        try {
            this.executor.join();
        } catch (InterruptedException ignored) {
        }

        if (this.killed) {
            EndlessOnline.getInstance().getLogger().warning("Failed to add server to DB!");
        }
    }

    boolean isKilled() {
        return this.killed;
    }

    private class SQLExecutor implements Runnable {
        @Override
        public void run() {
            try {
                SQL.this.connect();
                PreparedStatement statement = SQL.this.connection.prepareStatement(SQL.this.query);
                statement.executeUpdate();
                SQL.this.disconnect();
            } catch (SQLException | JSchException e) {
                EndlessOnline.getInstance().getLogger().severe("Connection to DB failed: " + e);
                SQL.this.killed = true;
            }
        }
    }
}
