package net.minespree.block.commands;

import net.minespree.block.game.cores.NeutralBlockCore;
import net.minespree.block.state.BlockState;
import net.minespree.feather.command.system.annotation.Command;
import net.minespree.feather.command.system.annotation.Param;
import net.minespree.feather.player.rank.Rank;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.states.GameState;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class TestingCommands {
    @Command(names = "neutralcore", requiredRank = Rank.ADMIN, hideFromHelp = true)
    public static void neutralCore(Player player) {
        GameState state = RisePlugin.getPlugin().getGameStateManager().getCurrentState();
        if (state instanceof BlockState) {
            BlockState bs = (BlockState) state;
            NeutralBlockCore core = bs.getData().getNeutralCores().get(ThreadLocalRandom.current().nextInt(bs.getData().getNeutralCores().size()));
            if (core != null) {
                core.activate();
                player.sendMessage(ChatColor.GREEN + "Activate core...");
            }
        }
    }

    @Command(names = "hurrystate", requiredRank = Rank.ADMIN, hideFromHelp = true)
    public static void hurryState(Player player) {
        GameState state = RisePlugin.getPlugin().getGameStateManager().getCurrentState();
        if (state instanceof BlockState) {
            BlockState bs = (BlockState) state;
            bs.setTime(1);
        }
    }

    @Command(names = "time", requiredRank = Rank.ADMIN, hideFromHelp = true)
    public static void time(Player player, @Param(name = "Time") String time) {
        GameState state = RisePlugin.getPlugin().getGameStateManager().getCurrentState();
        if (state instanceof BlockState) {
            BlockState bs = (BlockState) state;
            bs.setTime(Integer.valueOf(time));
        }
    }
}
