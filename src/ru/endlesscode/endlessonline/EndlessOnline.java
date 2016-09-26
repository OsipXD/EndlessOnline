package ru.endlesscode.endlessonline;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by OsipXD on 13.09.2015
 * It is part of the EndlessOnline.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
public class EndlessOnline extends JavaPlugin {
    private static EndlessOnline instance;

    private SQL sql;
    private int online;
    private int maxOnline;

    static EndlessOnline getInstance() {
        return instance;
    }

    public void onEnable() {
        instance = this;

        this.saveDefaultConfig();

        if (!this.getConfig().getBoolean("enabled")) {
            getPluginLoader().disablePlugin(this);
            return;
        }

        this.sql = new SQL();
        this.initSQL();

        PluginManager pm = this.getServer().getPluginManager();
        this.updateOnline(false);
        pm.registerEvents(new PlayerListener(), this);

        if (this.sql.isKilled()) {
            pm.disablePlugin(this);
        }
    }

    public void onDisable() {
        if (this.sql == null) {
            return;
        }

        if (this.sql.isKilled()) {
            this.getLogger().warning("Please configure your SQL connection");
        } else {
            this.getLogger().info("Setting status to offline...");
            this.sql.clearOnline(this.maxOnline);
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        Player player = null;

        if (args.length == 0) {
            String line = String.format(getConfig().getString("message"), this.online, Bukkit.getMaxPlayers());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
            return true;
        }

        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args[0].equalsIgnoreCase("refresh")) {
            if (player == null || player.hasPermission("eonline.refresh")) {
                sender.sendMessage("Updating online value...");
                this.updateOnline(false);
                sender.sendMessage("New online: " + ChatColor.DARK_GREEN + this.online + "/" + Bukkit.getMaxPlayers());
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (player != null && !player.hasPermission("eonline.reload")) {
                sender.sendMessage("You are not able to perform this command.");
            } else {
                sender.sendMessage("Reloading...");
                this.sql.clearOnline(this.maxOnline);
                this.sql.reloadConfig();
                this.reloadConfig();
                this.initSQL();
                this.updateOnline(false);

                if (this.sql.isKilled()) {
                    this.getServer().getPluginManager().disablePlugin(this);
                    return false;
                }

                sender.sendMessage("Successfully reloaded!");
            }
        } else if (player == null || player.hasPermission("eonline.help")) {
            return false;
        }

        return true;
    }

    private void initSQL() {
        if (!this.sql.tableExists() && !this.sql.isKilled()) {
            this.sql.createTable();

            if (this.sql.isKilled()) {
                this.getLogger().warning("Table not created!");
            }
        }

        if (!this.sql.isKilled() && !this.sql.serverExists()) {
            this.sql.addServer();
        }

        if (!this.sql.isKilled() && (this.maxOnline = this.sql.getMaxOnline()) == -1) {
            this.getLogger().warning("Can't get online record from table!");
        }
    }

    void updateOnline(boolean async) {
        BukkitRunnable refresher = new BukkitRunnable() {
            @Override
            public void run() {
                if (sql.isKilled()) {
                    return;
                }

                List<String> playerList = new ArrayList<>();
                for (Player player : getServer().getOnlinePlayers()) {
                    playerList.add(player.getName());
                }

                online = getServer().getOnlinePlayers().size();
                maxOnline = online > maxOnline ? online : maxOnline;
                sql.updateOnline(online, maxOnline, playerList);
            }
        };

        if (async) {
            refresher.runTaskLaterAsynchronously(this, 10);
        } else {
            refresher.run();
        }
    }
}