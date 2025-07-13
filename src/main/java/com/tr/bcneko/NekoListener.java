package com.tr.bcneko;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.util.Vector;

public class NekoListener implements Listener {

    // 猫娘攻击限制
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;

        Player attacker = (Player) e.getDamager();

        // 检查是否是猫娘
        if (!NekoManager.isNeko(attacker)) return;

        // 攻击目标必须是玩家
        if (!(e.getEntity() instanceof Player)) return;

        Player target = (Player) e.getEntity();
        Player owner = NekoManager.getOwner(attacker);
        String mode = NekoManager.getNekoMode(attacker);

        // 特殊规则：shy模式下可以攻击主人
        if ("shy".equalsIgnoreCase(mode) && target.equals(owner)) {
            // 允许攻击主人
            return;
        }

        // 模式检查：normal模式不允许攻击任何玩家
        if ("normal".equalsIgnoreCase(mode)) {
            e.setCancelled(true);
            applyKnockback(target);
            return;
        }

        // aggressive模式：只允许攻击主人以外的玩家
        if ("aggressive".equalsIgnoreCase(mode)) {
            if (target.equals(owner)) {
                e.setCancelled(true);
                applyKnockback(target);
            }
            // 否则，允许攻击（不取消事件）
            return;
        }

        // shy模式：不允许攻击除主人外的其他玩家
        if ("shy".equalsIgnoreCase(mode)) {
            e.setCancelled(true);
            applyKnockback(target);
        }
    }

    // 应用击退效果
    private void applyKnockback(Player target) {
        Vector direction = target.getLocation().getDirection().multiply(-0.5);
        target.setVelocity(direction);
    }

    // 聊天后缀
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (NekoManager.isNeko(player)) {
            Player owner = NekoManager.getOwner(player);
            String suffix = " -" + (owner != null ? owner.getName() + "的猫娘" : "流浪猫娘");

            // 根据模式添加不同后缀
            switch (NekoManager.getNekoMode(player).toLowerCase()) {
                case "aggressive":
                    e.setMessage(e.getMessage() + " (凶猛)" + suffix);
                    break;
                case "shy":
                    e.setMessage(e.getMessage() + " (害羞)" + suffix);
                    break;
                default:
                    e.setMessage(e.getMessage() + suffix);
            }
        }
    }
}
