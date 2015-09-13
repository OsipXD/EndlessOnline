package ru.endlesscode.endlessonline;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Created by OsipXD on 13.09.2015
 * It is part of the EndlessOnline.
 * All rights reserved 2014 - 2015 � �EndlessCode Group�
 */
class PlayerListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        EndlessOnline.getInstance().updateOnline();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        EndlessOnline.getInstance().updateOnline();
    }
}