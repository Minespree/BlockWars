package net.minespree.block.game.kits;

import net.minespree.block.state.BlockState;
import net.minespree.block.state.BuildState;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.teams.Team;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftTNTPrimed;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class BomberKitExtension extends RegenKitExtension {

    private static Map<TNTPrimed, Team> tntTeam = new HashMap<>();


    public BomberKitExtension() {
        super("bw_bomber");
    }

    @Override
    public void finish(Player player) {
        int slot = getDocument(player, "tnt").getInteger("slot");
        ItemStack item = player.getInventory().getItem(slot);
        int amount = item == null ? 0 : item.getAmount();
        if(item != null && item.getType() != Material.TNT) {
            amount = 0;
            for (int i = 0; i < player.getInventory().getContents().length; i++) {
                if(player.getInventory().getContents()[i] == null) {
                    player.getInventory().setItem(i, item);
                    break;
                }
            }
        }
        player.getInventory().setItem(slot, new ItemStack(Material.TNT, amount + 1));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if(event.getBlockPlaced().getType() == Material.TNT && isUsing(event.getPlayer())) {
            TNTPrimed tnt = (TNTPrimed) event.getBlock().getWorld().spawnEntity(event.getBlock().getLocation(), EntityType.PRIMED_TNT);
            tntTeam.put(tnt, teamHandler.getTeam(event.getPlayer()));
            ((CraftTNTPrimed) tnt).getHandle().projectileSource = event.getPlayer();
            event.getBlock().setType(Material.AIR);
            event.setCancelled(false);
            use(event.getPlayer(), getDocument(event.getPlayer(), "tnt")
                    .getInteger("regen").longValue() * 50L, 1);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if(!(RisePlugin.getPlugin().getGameStateManager().getCurrentState() instanceof BuildState)) {
            if(event.getDamager() instanceof TNTPrimed && event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                TNTPrimed tnt = (TNTPrimed) event.getDamager();
                if(teamHandler.getTeam(player) == tntTeam.get(tnt)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if(event.getEntity() instanceof TNTPrimed) {
            event.blockList().removeIf(block -> !((BlockState) RisePlugin.getPlugin().getGameStateManager().getCurrentState()).canBreak(block)
                    || block.getData() == tntTeam.get(event.getEntity()).getWoolColour());
        }
    }

}
