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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.bukkit.configuration.Configuration;

/**
 * Created by OsipXD on 22.09.2016
 * It is part of the EndlessOnline.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
class SSHTunnel {
    private final String host;
    private final int port;
    private final String remoteHost;
    private final int remotePort;
    private final String username;
    private final String password;
    private final int localPort;
    private Session session = null;

    SSHTunnel(Configuration config, String sqlHost, int sqlPort) throws JSchException {
        EndlessOnline.getInstance().getLogger().info("Configuring SSH Tunnel...");
        this.host = config.getString("tunnel.host");
        this.port = config.getInt("tunnel.port.ssh");
        this.username = config.getString("tunnel.user");
        this.password = config.getString("tunnel.pass");
        this.localPort = config.getInt("tunnel.port.local");
        this.remoteHost = sqlHost;
        this.remotePort = sqlPort;

        this.connect();
        EndlessOnline.getInstance().getLogger().info("Test SSH connection was successful!");
        this.disconnect();
    }

    int getLocalPort() {
        return localPort;
    }

    void connect() throws JSchException {
        if (this.session == null || !this.session.isConnected()) {
            final JSch jsch = new JSch();
            this.session = jsch.getSession(username, host, port);
            this.session.setPassword(password);
            this.session.setConfig("StrictHostKeyChecking", "no");
            this.session.connect();
            this.session.setPortForwardingL(localPort, remoteHost, remotePort);
        }
    }

    void disconnect() {
        if (this.session != null && this.session.isConnected()) {
            this.session.disconnect();
        }
    }
}
