package net.minespree.block.state;

import net.minespree.babel.Babel;
import net.minespree.rise.states.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public class OvertimeState extends BlockState {

    public OvertimeState() {
        super(ChatColor.DARK_RED.toString() + ChatColor.BOLD + "Overtime", 120);
    }

    public void onStart(GameState previous) {
        super.onStart(previous);

        Bukkit.getOnlinePlayers().forEach(player -> scoreboard.initialize(player));

        data.getListener().getPlacedBlocks().forEach(block -> block.setType(Material.AIR));
        Babel.translate("bw_blockscleared").broadcast();
    }

    public void timerEnd() {
        outputWinner();
    }
}
