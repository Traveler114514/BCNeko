package com.tr.bcneko;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NekoManager {
    // 主人关系 (猫娘UUID -> 主人UUID)
    private static final Map<UUID, UUID> ownerRelations = new ConcurrentHashMap<>();
    
    // 猫娘模式 (猫娘UUID -> 模式名称)
    private static final Map<UUID, String> nekoModes = new ConcurrentHashMap<>();
    
    // 待处理请求 (接收者UUID -> 请求者UUID)
    private static final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>();
    
    // 待处理解除请求 (接收者UUID -> 请求者UUID)
    private static final Map<UUID, UUID> pendingUnbinds = new ConcurrentHashMap<>();

    // 添加关系
    public static void bind(Player owner, Player neko) {
        ownerRelations.put(neko.getUniqueId(), owner.getUniqueId());
        nekoModes.put(neko.getUniqueId(), "normal");
    }

    // 删除关系
    public static void unbind(Player neko) {
        ownerRelations.remove(neko.getUniqueId());
        nekoModes.remove(neko.getUniqueId());
    }

    // 获取主人
    public static Player getOwner(Player neko) {
        UUID ownerId = ownerRelations.get(neko.getUniqueId());
        return ownerId != null ? Bukkit.getPlayer(ownerId) : null;
    }

    // 获取猫娘
    public static Player getNeko(Player owner) {
        for (Map.Entry<UUID, UUID> entry : ownerRelations.entrySet()) {
            if (entry.getValue().equals(owner.getUniqueId())) {
                return Bukkit.getPlayer(entry.getKey());
            }
        }
        return null;
    }

    // 添加请求
    public static void addRequest(Player sender, Player receiver) {
        pendingRequests.put(receiver.getUniqueId(), sender.getUniqueId());
    }
    
    // 添加解除请求
    public static void addUnbindRequest(Player sender, Player receiver) {
        pendingUnbinds.put(receiver.getUniqueId(), sender.getUniqueId());
    }

    // 处理请求
    public static boolean handleRequest(Player receiver, boolean accept) {
        if (!pendingRequests.containsKey(receiver.getUniqueId())) return false;
        
        UUID senderId = pendingRequests.remove(receiver.getUniqueId());
        Player sender = Bukkit.getPlayer(senderId);
        
        if (sender == null || !sender.isOnline()) return false;
        
        if (accept) {
            bind(sender, receiver);
            sender.sendMessage("§a" + receiver.getName() + " 已成为你的猫娘！");
            receiver.sendMessage("§a你已与 " + sender.getName() + " 绑定！");
        } else {
            sender.sendMessage("§c" + receiver.getName() + " 拒绝了你的请求");
        }
        return true;
    }
    
    // 处理解除请求
    public static boolean handleUnbindRequest(Player receiver, boolean accept) {
        if (!pendingUnbinds.containsKey(receiver.getUniqueId())) return false;
        
        UUID senderId = pendingUnbinds.remove(receiver.getUniqueId());
        Player sender = Bukkit.getPlayer(senderId);
        
        if (sender == null || !sender.isOnline()) return false;
        
        if (accept) {
            // 解除关系
            if (isNeko(sender)) {
                unbind(sender);
            } else if (isNeko(receiver)) {
                unbind(receiver);
            }
            sender.sendMessage("§a你与 " + receiver.getName() + " 的关系已解除");
            receiver.sendMessage("§a你与 " + sender.getName() + " 的关系已解除");
        } else {
            sender.sendMessage("§c" + receiver.getName() + " 拒绝了你的解除请求");
        }
        return true;
    }

    // 检查是否已有关系
    public static boolean isBound(Player player) {
        return ownerRelations.containsKey(player.getUniqueId()) || 
               ownerRelations.containsValue(player.getUniqueId());
    }

    // 检查是否是猫娘
    public static boolean isNeko(Player player) {
        return ownerRelations.containsKey(player.getUniqueId());
    }
    
    // 检查是否是主人
    public static boolean isOwner(Player player) {
        return ownerRelations.containsValue(player.getUniqueId());
    }
    
    // 获取关系中的猫娘（如果玩家是猫娘返回自己，如果是主人返回猫娘）
    public static Player getBoundCat(Player player) {
        if (isNeko(player)) {
            return player;
        } else if (isOwner(player)) {
            return getNeko(player);
        }
        return null;
    }

    // 设置猫娘模式
    public static void setNekoMode(Player neko, String mode) {
        if (ownerRelations.containsKey(neko.getUniqueId())) {
            nekoModes.put(neko.getUniqueId(), mode);
        }
    }

    // 获取猫娘模式
    public static String getNekoMode(Player neko) {
        return nekoModes.getOrDefault(neko.getUniqueId(), "normal");
    }

    // 从YAML加载数据
    public static void loadData(FileConfiguration config) {
        ownerRelations.clear();
        nekoModes.clear();
        pendingUnbinds.clear();

        ConfigurationSection relations = config.getConfigurationSection("relations");
        if (relations != null) {
            for (String nekoId : relations.getKeys(false)) {
                String ownerId = relations.getString(nekoId);
                if (ownerId != null) {
                    UUID nekoUUID = UUID.fromString(nekoId);
                    UUID ownerUUID = UUID.fromString(ownerId);
                    ownerRelations.put(nekoUUID, ownerUUID);
                }
            }
        }

        ConfigurationSection modes = config.getConfigurationSection("modes");
        if (modes != null) {
            for (String nekoId : modes.getKeys(false)) {
                String mode = modes.getString(nekoId);
                if (mode != null) {
                    UUID nekoUUID = UUID.fromString(nekoId);
                    nekoModes.put(nekoUUID, mode);
                }
            }
        }
    }

    // 保存数据到YAML
    public static void saveData(FileConfiguration config) {
        config.set("relations", null);
        config.set("modes", null);
        
        // 保存关系
        ConfigurationSection relations = config.createSection("relations");
        for (Map.Entry<UUID, UUID> entry : ownerRelations.entrySet()) {
            relations.set(entry.getKey().toString(), entry.getValue().toString());
        }
        
        // 保存模式
        ConfigurationSection modes = config.createSection("modes");
        for (Map.Entry<UUID, String> entry : nekoModes.entrySet()) {
            modes.set(entry.getKey().toString(), entry.getValue());
        }
    }
}
