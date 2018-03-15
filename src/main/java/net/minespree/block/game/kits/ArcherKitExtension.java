package net.minespree.block.game.kits;

import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.data.gamedata.kits.KitExtension;
import net.minespree.feather.data.gamedata.kits.Tier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class ArcherKitExtension extends KitExtension {

    public ArcherKitExtension() {
        super(GameRegistry.Type.BLOCKWARS, "bw_archer");
    }

    @Override
    public void setKit(Player player, Tier tier) {}

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if(event.getItem().getItemStack().getType() == Material.ARROW) {
            if (isUsing(event.getPlayer())) {
                event.setCancelled(false);
            } else {
                event.setCancelled(true);
            }
        }
    }

}
