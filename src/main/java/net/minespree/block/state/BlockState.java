package net.minespree.block.state;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import net.minespree.babel.Babel;
import net.minespree.babel.ComplexBabelMessage;
import net.minespree.block.BlockPlugin;
import net.minespree.block.game.BlockScoreboardFeature;
import net.minespree.block.game.CoreShardItem;
import net.minespree.block.game.cores.NeutralBlockCore;
import net.minespree.block.map.BlockMapData;
import net.minespree.feather.data.damage.event.MinespreeDeathEvent;
import net.minespree.feather.data.damage.objects.KillAssist;
import net.minespree.feather.data.gamedata.kits.KitExtension;
import net.minespree.feather.data.gamedata.kits.KitManager;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.control.SpawnHandler;
import net.minespree.rise.features.RespawnWaveFeature;
import net.minespree.rise.states.BaseGameState;
import net.minespree.rise.states.GameState;
import net.minespree.rise.teams.Team;
import net.minespree.wizard.util.SetupUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import java.util.*;

public abstract class BlockState extends BaseGameState {

    protected static Map<UUID, String> teams = Maps.newHashMap();

    BukkitTask task;
    @Getter
    String scoreboardName;
    @Getter @Setter
    int time;
    int length;

    RespawnWaveFeature respawnWaveFeature;

    @Getter
    BlockMapData data;
    @Getter
    BlockScoreboardFeature scoreboard;

    BlockState(String scoreboardName, int length) {
        this.scoreboardName = scoreboardName;
        this.length = length;
    }

    public void onStart(GameState previous) {
        this.time = length;
        game.getFeatures().stream()
                .filter(feature -> feature instanceof RespawnWaveFeature)
                .forEach(feature -> respawnWaveFeature = (RespawnWaveFeature) feature);
        gameManager = RisePlugin.getPlugin().getGameManager();
        kitHandler = KitManager.getInstance();
        if(gameManager.getGamemode().isPresent() && gameManager.getTeamHandler().isPresent() && gameManager.getGameInProgress().isPresent()) {
            data = (BlockMapData) RisePlugin.getPlugin().getMapManager().getMapData();
            scoreboard = data.getScoreboard();
            task = Bukkit.getScheduler().runTaskTimer(BlockPlugin.getPlugin(), () -> {
                time--;
                if(time <= 5) {
                    Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.NOTE_PLING, 1F, 1F - (time == 0 ? 0F : (time / 20))));
                }
                tick();
                scoreboard.update(this);
                if (time <= 0) {
                    task.cancel();
                    timerEnd();
                }
            }, 20L, 20L);
        }
    }

    public void onStop(@Nonnull GameState next) {
        if(task != null)
            task.cancel();
    }

    public boolean canBreak(Block block) {
        return data.getListener().getPlacedBlocks().contains(block);
    }

    public boolean blockBreak(Block block) {
        if(canBreak(block)) {
            data.getListener().getPlacedBlocks().remove(block);
            block.breakNaturally();
        }
        return false;
    }

    public abstract void timerEnd();

    public void checkWinners() {
        int teamsAlive = 0;
        for (Team team : teamHandler.getTeams()) {
            if(team.getAlive().size() > 0) {
                teamsAlive++;
            }
        }
        if(teamsAlive <= 1) {
            outputWinner();
        }
    }

    void outputWinner() {
        HandlerList.unregisterAll(data.getListener());
        teamHandler.disableTeamChat();
        data.getNeutralCores().forEach(NeutralBlockCore::disable);
        gamemode.getKitExtensions().forEach(KitExtension::stop);
        Map<Team, Integer> winner = new HashMap<>();
        for (Team team : teamHandler.getTeams()) {
            if(team.getAlive().size() == 0)
                continue;
            winner.put(team, getHealth(team));
        }
        int max = (Collections.max(winner.values()));
        List<Team> w = new ArrayList<>();
        for (Map.Entry<Team, Integer> entry : winner.entrySet()) {
            if (entry.getValue() == max) {
                w.add(entry.getKey());
            }
        }
        w.forEach(team -> Bukkit.getOnlinePlayers().stream()
                .filter(player -> team.getPlayers().contains(player.getUniqueId()))
                .forEach(player -> game.changeStatistic(player, "loss", 1)));
        if(w.size() == 1) {
            w.get(0).getPlayers().forEach(uuid -> game.changeStatistic(Bukkit.getPlayer(uuid), "win", 1));
            Team team = w.get(0);
            game.endGame(new ComplexBabelMessage().append(Babel.translate("bw_winner"), team), team.getName().toString());
        } else {
            w.forEach(team -> team.getPlayers().forEach(uuid -> game.changeStatistic(Bukkit.getPlayer(uuid), "draw", 1)));
            game.endGame(Babel.translate("bw_draw"), "Draw");
        }
    }

    public int getHealth(Team team) {
        return data.getTeamCores().containsKey(team) ? data.getTeamCores().get(team).getHealth() : 0;
    }

    public void tick() {}

    @Override
    public void onJoin(Player player) {
        Team team = null;
        if(!teams.containsKey(player.getUniqueId())) {
            for (Team t : teamHandler.getTeams()) {
                if (team == null || t.getPlayers().size() < team.getPlayers().size()) {
                    team = t;
                }
            }
        } else {
            for (Team t : teamHandler.getTeams()) {
                if(t.getName().getKey().equals(teams.get(player.getUniqueId())) && t.getPlayers().size() > 0) {
                    team = t;
                }
            }
        }
        scoreboard.onStart(player);
        if(team == null) {
            spectatorHandler.setSpectator(player);
            return;
        }
        team.addPlayer(player);
        teams.put(player.getUniqueId(), team.getName().getKey());
        int health = getHealth(team);
        if(health == 0 || spectatorHandler.isSpectator(player)) {
            if(health == 0) {
                team.kill(player);
            }
            spectatorHandler.setSpectator(player);
        } else {
            SetupUtil.setupPlayer(player, false);
            if(RisePlugin.getPlugin().getGameStateManager().getCurrentState() instanceof BuildState) {
                gamemode.getSpawnHandler().get().spawnPlayer(player, SpawnHandler.SpawnReason.RESPAWN);
            } else {
                RisePlugin.getPlugin().getGameManager().getSpectatorHandler().setSpectator(player);
                respawnWaveFeature.getNextWave().add(player.getUniqueId());
            }
        }
    }

    @Override
    public void onQuit(Player player) {
        scoreboard.onStop(player);

        String teamName = teams.getOrDefault(player.getUniqueId(), "");
        teamHandler.getTeams().stream().filter(t -> t.getName().getKey().equals(teamName)).findFirst()
                .ifPresent(t -> {
                    if(t.getAlive().size() == 0) {
                        data.getTeamCores().get(t).disable();
                        scoreboard.setTeamScore(t);
                        checkWinners();
                    }
                });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        data.getNeutralCores().stream()
                .filter(core -> core.getHolding() == event.getEntity())
                .forEach(NeutralBlockCore::drop);
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onPlayerDeath(MinespreeDeathEvent event) {
        Player player = event.getPlayer();
        game.changeStatistic(player, "deaths", 1);

        event.getLife().getLastDamagingPlayer().ifPresent(killer -> {
            game.changeStatistic(killer, "kills", 1); // Covers all possible scenarios.
        });

        for (KillAssist assist : event.getAssists()) {
            Babel.translate("assisted_killing_player").sendMessage(assist.getPlayer(), NetworkPlayer.of(player).colorName(), assist.getPercent());
            game.changeStatistic(assist.getPlayer(), "assists", 1);
        }

        Team team = teamHandler.getTeam(player);
        boolean destroyed = getHealth(team) == 0;
        if(destroyed && team.getAlive().size() > 0) {
            team.kill(player);
            game.changeStatistic(player, "eliminations", 1);
            Babel.translate("bw_eliminated").broadcast(team.getColour() + NetworkPlayer.of(player).getName());
            RisePlugin.getPlugin().getGameManager().getSpectatorHandler().setSpectator(player);
            ((BlockState) RisePlugin.getPlugin().getGameStateManager().getCurrentState()).getScoreboard().setTeamScore(team);
            respawnWaveFeature.exclude(player);
            ((BlockState) RisePlugin.getPlugin().getGameStateManager().getCurrentState()).checkWinners();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if(event.getItem() != null && event.getItem().getType() == Material.PRISMARINE_SHARD) {
            if(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                CoreShardItem.getGui().open(event.getPlayer());
            }
        }
    }

}