package net.minespree.block.game;

import net.minespree.block.BlockPlugin;
import net.minespree.block.map.BlockMapData;
import net.minespree.block.map.SpawnArea;
import net.minespree.block.state.BuildState;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.player.PlayerManager;
import net.minespree.feather.player.implementations.KittedPlayer;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.control.SpawnHandler;
import net.minespree.rise.teams.Team;
import net.minespree.wizard.util.SetupUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Map;

public class BlockSpawnHandler implements SpawnHandler {

    public void spawnPlayer(@Nonnull Player player, @Nonnull SpawnReason reason) {
        player.setFallDistance(0.0f);
        Bukkit.getScheduler().runTaskLater(BlockPlugin.getPlugin(), () -> player.setNoDamageTicks(100), 1L);
        Map<Team, SpawnArea> spawns = ((BlockMapData) RisePlugin.getPlugin().getMapManager().getMapData()).getTeamSpawns();
        Team team = RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeam(player);
        if(team != null) {
            RisePlugin.getPlugin().getGameManager().getSpectatorHandler().removeSpectator(player);
            player.teleport(spawns.get(team).randomLocation());
            if (!(RisePlugin.getPlugin().getGameStateManager().getCurrentState() instanceof BuildState)) {
                SetupUtil.setupPlayer(player, false);
                player.setSaturation(0f);
                CoreShardItem.update(player);
                KittedPlayer kp = (KittedPlayer) PlayerManager.getInstance().getPlayer(player);
                kp.getDefaultKit(GameRegistry.Type.BLOCKWARS).getTier().set(player);
            }
        }
    }

}
