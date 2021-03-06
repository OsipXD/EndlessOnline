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

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Created by OsipXD on 13.09.2015
 * It is part of the EndlessOnline.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
class PlayerListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        EndlessOnline.getInstance().updateOnline(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        EndlessOnline.getInstance().updateOnline(true);
    }
}
