package always.io.kr.hide_and_seek;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GameListener implements Listener {

    private final Hide_and_seek plugin;

    public GameListener(Hide_and_seek plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        GameManager gm = plugin.getManager();
        if (!gm.isRunning()) return;

        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {
            // 산타가 요정을 때렸을 때
            if (attacker.equals(gm.getTagger()) && gm.isCitizen(victim)) {
                event.setCancelled(true);

                // 사탕 지팡이 확인
                ItemStack item = attacker.getInventory().getItemInMainHand();
                if (item.getType() == Material.BLAZE_ROD) {
                    gm.catchCitizen(victim);
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        GameManager gm = plugin.getManager();
        if (!gm.isRunning()) return;

        Player p = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            // 1. 산타의 루돌프 코 사용
            if (p.equals(gm.getTagger()) && item.getType() == Material.GLOW_BERRIES) {
                event.setCancelled(true);
                item.setAmount(item.getAmount() - 1);

                gm.broadcast(Component.text("🔴 산타가 루돌프의 코를 밝혔어요! 5초 동안 숨은 요정들이 반짝거려요!", NamedTextColor.RED));

                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    if (gm.isCitizen(online) && !gm.isCaught(online)) {
                        online.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
                    }
                }
                // 루돌프 코 소리 (경험치 소리 비슷하게)
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
            }

            // 2. 요정의 기적의 별(열쇠) 사용 - Material을 NETHER_STAR로 변경
            if (gm.isCitizen(p) && !gm.isCaught(p) && item.getType() == Material.NETHER_STAR) {
                if (item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().contains("기적")) {
                    event.setCancelled(true);

                    if (gm.getCaughtCount() == 0) {
                        p.sendMessage(Component.text("잡혀간 친구가 없어서 별을 쓸 수 없어요. 아껴두세요! ⭐", NamedTextColor.YELLOW));
                        return;
                    }

                    item.setAmount(item.getAmount() - 1);
                    gm.reviveRandomCitizen(p);
                    p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1f); // 마법 소리
                }
            }
        }
    }
}