package net.minespree.block.game.kits;

import net.minespree.block.state.BuildState;
import net.minespree.rise.RisePlugin;
import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class IcemanKitExtension extends RegenKitExtension {

    public IcemanKitExtension() {
        super("bw_iceman");
    }

    @Override
    public void finish(Player player) {
        int slot = getDocument(player, "snowball").getInteger("slot");
        ItemStack item = player.getInventory().getItem(slot);
        int amount = item == null ? 0 : item.getAmount();
        if(item != null && item.getType() != Material.SNOW_BALL) {
            amount = 0;
            for (int i = 0; i < player.getInventory().getContents().length; i++) {
                if(player.getInventory().getContents()[i] == null) {
                    player.getInventory().setItem(i, item);
                    break;
                }
            }
        }
        player.getInventory().setItem(slot, new ItemStack(Material.SNOW_BALL, amount + 1));
    }

    @EventHandler
    public void onProjectileShoot(ProjectileLaunchEvent event) {
        if(event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();
            if(event.getEntity() instanceof Snowball && isUsing(player)) {
                use(player, getDocument(player, "snowball").getInteger("regen").longValue() * 50L, 1);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if(!(RisePlugin.getPlugin().getGameStateManager().getCurrentState() instanceof BuildState)) {
            if(event.getDamager() instanceof Snowball && event.getEntity() instanceof Player) {
                Player entity = (Player) event.getEntity();
                Player player = (Player) ((Snowball) event.getDamager()).getShooter();
                System.out.println(entity.getName() + " " + player.getName());
                if(!teamHandler.areSameTeam(entity, player)) {
                    Document document = getDocument(player, "snowball");
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, document.getInteger("length"), document.getInteger("slow")));
                    event.setCancelled(false);
                } else event.setCancelled(true);
            }
        }
    }

}
