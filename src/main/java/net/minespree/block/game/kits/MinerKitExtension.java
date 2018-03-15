package net.minespree.block.game.kits;

import net.minespree.block.state.BlockState;
import net.minespree.block.state.BuildState;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.data.gamedata.kits.KitExtension;
import net.minespree.feather.data.gamedata.kits.Tier;
import net.minespree.rise.RisePlugin;
import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.concurrent.ThreadLocalRandom;

public class MinerKitExtension extends KitExtension {

    public MinerKitExtension() {
        super(GameRegistry.Type.BLOCKWARS, "bw_miner");
    }

    @Override
    public void setKit(Player player, Tier tier) {}

    @EventHandler (priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        if(!event.isCancelled() && isUsing(event.getPlayer())) {
            if(!(RisePlugin.getPlugin().getGameStateManager().getCurrentState() instanceof BuildState)) {
                Document document = getDocument(event.getPlayer(), "drop");
                if(ThreadLocalRandom.current().nextInt(100) <= document.getInteger("percent")) {
                    ((BlockState) RisePlugin.getPlugin().getGameStateManager().getCurrentState()).blockBreak(event.getBlock());
                }
            }
        }
    }

}
