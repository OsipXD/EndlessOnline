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
 * All rights reserved 2014 - 2015 � �EndlessCode Group�
 */
public class EndlessOnline extends JavaPlugin {
    private static EndlessOnline instance;

    private SQL sql;
    private int online;

    public static EndlessOnline getInstance() {
        return instance;
    }

    public void onEnable() {
        instance = this;

        this.saveDefaultConfig();
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
        if (this.sql.isKilled()) {
            this.getLogger().warning("Please configure your SQL connection");
        } else {
            this.getLogger().info("Setting status to offline...");
            this.sql.clearOnline();
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        Player player = null;

        if (args.length == 0) {
            sender.sendMessage(ChatColor.BOLD + "Online now: " + ChatColor.DARK_GREEN + this.online + "/" + Bukkit.getMaxPlayers());
            return true;
        }

        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("about")) {
            sender.sendMessage(ChatColor.DARK_PURPLE + this.getDescription().getFullName());
        } else {
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
                    this.sql.clearOnline();
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
    }

    public void updateOnline(boolean async) {
        BukkitRunnable refresher = new BukkitRunnable() {
            @Override
            public void run() {
                if (sql.isKilled()) {
                    return;
                }

                List<String> playerList = new ArrayList<>();
                for (Player player : EndlessOnline.getInstance().getServer().getOnlinePlayers()) {
                    playerList.add(player.getName());
                }

                sql.updateOnline(online = getServer().getOnlinePlayers().size(), playerList);
            }
        };

        if (async) {
            refresher.runTaskLaterAsynchronously(this, 10);
        } else {
            refresher.run();
        }
    }
}