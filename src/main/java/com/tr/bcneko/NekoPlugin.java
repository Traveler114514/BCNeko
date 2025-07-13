package com.tr.bcneko;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class NekoPlugin extends JavaPlugin {

    private static NekoPlugin instance;
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        instance = this;
        // 确保数据文件夹存在
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        loadData();
        Bukkit.getPluginManager().registerEvents(new NekoListener(), this);
        getLogger().info("BCNeko 猫娘插件已启动！");
    }

    @Override
    public void onDisable() {
        saveData();
        getLogger().info("BCNeko 猫娘数据已保存！");
    }

    public static NekoPlugin getInstance() {
        return instance;
    }

    private void loadData() {
        dataFile = new File(getDataFolder(), "nekodata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        NekoManager.loadData(dataConfig);
    }

    public void saveData() {
        if (dataConfig != null && dataFile != null) {
            try {
                NekoManager.saveData(dataConfig);
                dataConfig.save(dataFile);
            } catch (IOException e) {
                getLogger().severe("保存数据失败: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以使用此命令");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("bcneko")) {
            if (args.length == 0) {
                player.sendMessage("§c用法: /bcneko [get <玩家> | gets <玩家> | accept | deny | follow | change <模式> | del]");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "get":
                    if (args.length != 2) {
                        player.sendMessage("§c用法: /bcneko get <玩家>");
                        return true;
                    }
                    handleGetCommand(player, args[1], false);
                    return true;

                case "gets":
                    if (args.length != 2) {
                        player.sendMessage("§c用法: /bcneko gets <玩家>");
                        return true;
                    }
                    handleGetCommand(player, args[1], true);
                    return true;

                case "accept":
                    if (NekoManager.handleRequest(player, true)) {
                        player.sendMessage("§a已接受绑定请求");
                        saveData(); // 保存关系变更
                    } else if (NekoManager.handleUnbindRequest(player, true)) {
                        player.sendMessage("§a已接受解除请求");
                        saveData();
                    } else {
                        player.sendMessage("§c没有待处理的请求");
                    }
                    return true;

                case "deny":
                    if (NekoManager.handleRequest(player, false)) {
                        player.sendMessage("§c已拒绝绑定请求");
                    } else if (NekoManager.handleUnbindRequest(player, false)) {
                        player.sendMessage("§c已拒绝解除请求");
                    } else {
                        player.sendMessage("§c没有待处理的请求");
                    }
                    return true;

                case "follow":
                    handleFollowCommand(player);
                    return true;

                case "change":
                    if (args.length != 2) {
                        player.sendMessage("§c用法: /bcneko change <模式>");
                        return true;
                    }
                    handleChangeCommand(player, args[1]);
                    return true;
                    
                case "del":
                    handleDelCommand(player);
                    return true;

                default:
                    player.sendMessage("§c未知命令");
                    return true;
            }
        }
        return false;
    }

    private void handleGetCommand(Player player, String targetName, boolean reverse) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§c玩家不存在或不在线");
            return;
        }

        if (player.equals(target)) {
            player.sendMessage("§c不能对自己使用此命令");
            return;
        }

        if (NekoManager.isBound(player)) {
            player.sendMessage("§c你已绑定猫娘或已被绑定！");
            return;
        }

        if (NekoManager.isBound(target)) {
            player.sendMessage("§c该玩家已有绑定关系！");
            return;
        }

        // 修复：正确处理 gets 命令的请求方向
        Player requester = reverse ? player : player;
        Player requested = reverse ? target : target;

        NekoManager.addRequest(requester, requested);
        player.sendMessage("§a已向 " + target.getName() + " 发送请求");

        if (reverse) {
            target.sendMessage("§b" + player.getName() + " 想成为你的猫娘！输入 §e/bcneko accept §b接受或 §c/bcneko deny §b拒绝");
        } else {
            target.sendMessage("§b" + player.getName() + " 想让你成为猫娘！输入 §e/bcneko accept §b接受或 §c/bcneko deny §b拒绝");
        }
    }
    
    private void handleDelCommand(Player player) {
        // 检查玩家是否有绑定关系
        if (!NekoManager.isBound(player)) {
            player.sendMessage("§c你没有绑定关系");
            return;
        }
        
        // 获取关系中的另一方
        Player partner = null;
        if (NekoManager.isNeko(player)) {
            partner = NekoManager.getOwner(player);
        } else if (NekoManager.isOwner(player)) {
            partner = NekoManager.getNeko(player);
        }
        
        if (partner == null || !partner.isOnline()) {
            player.sendMessage("§c无法找到关系中的另一方");
            return;
        }
        
        // 发送解除请求
        NekoManager.addUnbindRequest(player, partner);
        player.sendMessage("§a已向 " + partner.getName() + " 发送解除关系请求");
        partner.sendMessage("§b" + player.getName() + " 请求解除关系，输入 §e/bcneko accept §b接受或 §c/bcneko deny §b拒绝");
    }
    
    private void handleFollowCommand(Player player) {
        Player cat = null;
        
        if (NekoManager.isOwner(player)) {
            // 主人召唤猫娘
            cat = NekoManager.getNeko(player);
            if (cat == null) {
                player.sendMessage("§c你没有猫娘");
                return;
            }
            cat.teleport(player.getLocation());
            player.sendMessage("§a已召唤猫娘到身边");
            cat.sendMessage("§a主人召唤了你");
        } else if (NekoManager.isNeko(player)) {
            // 猫娘传送到主人
            Player owner = NekoManager.getOwner(player);
            if (owner == null || !owner.isOnline()) {
                player.sendMessage("§c主人不在线");
                return;
            }
            player.teleport(owner.getLocation());
            player.sendMessage("§a已传送到主人身边");
        } else {
            player.sendMessage("§c你没有绑定关系");
        }
    }
    
    private void handleChangeCommand(Player player, String mode) {
        // 只有主人可以切换猫娘模式
        if (!NekoManager.isOwner(player)) {
            player.sendMessage("§c只有主人可以切换猫娘模式");
            return;
        }
        
        Player neko = NekoManager.getNeko(player);
        if (neko == null || !neko.isOnline()) {
            player.sendMessage("§c你的猫娘不在线");
            return;
        }
        
        NekoManager.setNekoMode(neko, mode);
        player.sendMessage("§a已将猫娘模式切换为: " + mode);
        neko.sendMessage("§a主人已将你的模式切换为: " + mode);
        saveData();
    }
}
