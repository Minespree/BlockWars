package net.minespree.block.game;

import net.minespree.block.game.kits.NinjaKitExtension;
import net.minespree.block.state.BlockState;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.settings.FeatherSettings;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.features.ScoreboardFeature;
import net.minespree.rise.teams.Team;
import net.minespree.rise.teams.TeamHandler;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.NameTagVisibility;

import java.util.stream.Collectors;

public class BlockScoreboardFeature extends ScoreboardFeature {

    public BlockScoreboardFeature(Plugin plugin) {
        super(plugin, ChatColor.RED.toString() + ChatColor.BOLD + "BlockWars");
    }

    public void initialize(Player player) {
        if(!scoreboardData.containsKey(player.getUniqueId())) {
            onStart(player);
        }
        BlockState state = (BlockState) RisePlugin.getPlugin().getGameStateManager().getCurrentState();
        setScore(player, "ip", ChatColor.GOLD + "play.minespree.net", 0);
        setScore(player, "gameId", ChatColor.GRAY + Bukkit.getServerName(), 1);
        setScore(player, "blank1", " ", 2);
        TeamHandler teamHandler = RisePlugin.getPlugin().getGameManager().getTeamHandler().get();
        teamHandler.getTeams().forEach(team -> {
            String invisTeam = team.getName().toString() + "invis";
            NetworkPlayer np = NetworkPlayer.of(player);
            String prefix = team.getColour().toString() + ((boolean) np.getSettings().getValue(FeatherSettings.COLOR_BLIND) ? team.getColourBlindCode() + " " : "");
            setTeamScore(player, team);
            setTeamName(player, team, prefix, null);
            addTeam(player, invisTeam);
            setNameTagVisibility(player, invisTeam, NameTagVisibility.HIDE_FOR_OTHER_TEAMS);
            setTeamName(invisTeam, prefix, "");
            team.getPlayers().stream()
                    .map(Bukkit::getPlayer).filter(p -> NinjaKitExtension.getUsing().contains(p.getName()))
                    .collect(Collectors.toList()).forEach(p -> addToTeam(player, p, invisTeam));
        });
        setScore(player, "blank2", "  ", 4);
        setScore(player, "time", ChatColor.YELLOW.toString() + getTime(state.getTime()), 5);
        setScore(player, "state", state.getScoreboardName(), 6);
    }

    public void update(BlockState state) {
        updateScore("time", ChatColor.YELLOW.toString() + getTime(state.getTime()));
    }

    private String getTime(int time) {
        return DurationFormatUtils.formatDuration(time * 1000, "mm:ss");
    }

    public void setTeamScore(Team team) {
        scoreboardData.keySet().forEach(uuid -> setTeamScore(Bukkit.getPlayer(uuid), team));
    }

    public void setTeamScore(Player player, Team team) {
        int health = ((BlockState) RisePlugin.getPlugin().getGameStateManager().getCurrentState()).getHealth(team);
        if(health > 0) {
            setScore(player, team.getName().toString(), team.getColour() + "⬛ " + ChatColor.BOLD + team.getName().toString() + " " + ChatColor.GOLD + health, 3);
        } else {
            if(team.getAlive().size() > 0) {
                setScore(player, team.getName().toString(), team.getColour().toString() + ChatColor.GRAY + "⬛ " + team.getColour() + ChatColor.BOLD + team.getName().toString() + ChatColor.RED + " ✖", 3);
            } else {
                removeScore(player, team.getName().toString());
            }
        }
    }

}
