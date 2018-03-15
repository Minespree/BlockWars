package net.minespree.block.game;

import lombok.Getter;
import net.minespree.block.state.BlockState;
import net.minespree.cartographer.util.GameLocation;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.control.Game;
import net.minespree.rise.teams.TeamHandler;
import org.bukkit.entity.Player;

public abstract class BlockCore {

    @Getter
    protected GameLocation location;

    public BlockCore(GameLocation location) {
        this.location = location;
    }

    public abstract void initialize();
    public abstract void damage(Player player);
    public abstract void destroy(Player player);
    public abstract void disable();

    protected TeamHandler getTeamHandler() {
        return RisePlugin.getPlugin().getGameManager().getTeamHandler().get();
    }

    protected Game getGame() {
        return RisePlugin.getPlugin().getGameManager().getGameInProgress().get();
    }

    protected BlockState getState() {
        return (BlockState) RisePlugin.getPlugin().getGameStateManager().getCurrentState();
    }

}
