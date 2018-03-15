package net.minespree.block.state;

import net.minespree.babel.Babel;
import net.minespree.block.game.cores.NeutralBlockCore;
import net.minespree.block.game.cores.TeamBlockCore;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.control.SpawnHandler;
import net.minespree.rise.states.GameState;
import net.minespree.wizard.util.MessageUtil;
import net.minespree.wizard.util.SetupUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FightState extends BlockState {

    private NeutralBlockCore lastActive;
    private int nextNeutral;

    public FightState() {
        super(ChatColor.RED.toString() + ChatColor.BOLD + "Fight", 420);
    }

    public void onStart(GameState previous) {
        super.onStart(previous);

        Bukkit.getOnlinePlayers().forEach(player -> scoreboard.initialize(player));
        nextNeutral = ThreadLocalRandom.current().nextInt(50) + 20;
        Bukkit.getOnlinePlayers().forEach(player -> {
            MessageUtil.sendSubtitle(player, Babel.translate("bw_fightstart"), 20, 100, 20);
            SetupUtil.setupPlayer(player, false);
            gamemode.getSpawnHandler().get().spawnPlayer(player, SpawnHandler.SpawnReason.GAME_START);
        });
        mapManager.getCurrentWorld().ifPresent(world -> world.getEntities().stream().filter(entity -> entity instanceof Item).forEach(Entity::remove));
    }

    public void tick() {
        if(nextNeutral == 0) {
            List<NeutralBlockCore> cores = data.getNeutralCores();
            if (cores.size() > 0) {
                NeutralBlockCore core = cores.get(ThreadLocalRandom.current().nextInt(cores.size()));
                if (!core.isActive()) {
                    core.activate();
                    lastActive = core;
                    nextNeutral = ThreadLocalRandom.current().nextInt(50) + 30;
                }
            }
        } else if (lastActive == null || !lastActive.isActive() && nextNeutral > 0) nextNeutral--;
    }

    public void timerEnd() {
        for (TeamBlockCore blockCore : data.getTeamCores().values()) {
            if(blockCore.getHealth() > 0 || blockCore.getTeam().getPlayers().size() > 0) {
                RisePlugin.getPlugin().getGameStateManager().changeState(new OvertimeState());
                return;
            }
        }
        checkWinners();
    }
}
