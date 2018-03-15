package net.minespree.block.map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import net.minespree.block.BlockPlugin;
import net.minespree.block.GameListener;
import net.minespree.block.game.BlockCore;
import net.minespree.block.game.BlockScoreboardFeature;
import net.minespree.block.game.cores.NeutralBlockCore;
import net.minespree.block.game.cores.TeamBlockCore;
import net.minespree.cartographer.maps.BlockwarsGameMap;
import net.minespree.cartographer.maps.GameMap;
import net.minespree.cartographer.util.ColourData;
import net.minespree.cartographer.util.GameArea;
import net.minespree.rise.control.maps.MapData;
import net.minespree.rise.teams.Team;
import net.minespree.rise.teams.TeamSetting;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public final class BlockMapData implements MapData {

    private BlockwarsGameMap gameMap;
    private GameArea border, wall;
    private int buildY;
    private Map<Team, SpawnArea> teamSpawns = Maps.newHashMap();
    private Map<Team, TeamBlockCore> teamCores = Maps.newHashMap();
    private List<NeutralBlockCore> neutralCores;
    private List<ItemStack> wallItems;
    private List<GameArea> areas = Lists.newArrayList();
    private BlockScoreboardFeature scoreboard = new BlockScoreboardFeature(BlockPlugin.getPlugin());
    private List<BlockCore> cores = Lists.newArrayList();
    private GameListener listener = new GameListener();

    @Override
    public void create(GameMap map) {
        gameMap = (BlockwarsGameMap) map;

        for (ColourData data : gameMap.getTeamCores().keySet()) {
            Team team = new Team(data, gameMap.getTeamSlots().get(data));
            team.addSetting(TeamSetting.NO_DAMAGE);
            team.addSetting(TeamSetting.COLOUR_ITEMS);
            GameArea spawnArea = gameMap.getTeamSpawns().get(data);
            Pair<Float, Float> direction = gameMap.getTeamSpawnDirection().get(data);
            teamCores.put(team, new TeamBlockCore(gameMap.getTeamCores().get(data), team));
            teamSpawns.put(team, new SpawnArea(spawnArea.getPos1(), spawnArea.getPos2(), direction.getLeft(), direction.getRight()));
        }
        neutralCores = gameMap.getNeutralCores().stream().map(NeutralBlockCore::new).collect(Collectors.toList());

        border = gameMap.getBorder();
        wall = gameMap.getWall();
        buildY = gameMap.getBuildY();
        wallItems = gameMap.getWallItems();
        areas = Lists.newArrayList(gameMap.getDisabledAreas());

        cores.addAll(teamCores.values());
        cores.addAll(neutralCores);

        cores.forEach(core -> areas.add(new GameArea(core.getLocation().clone().add(1, 1, 1),
                core.getLocation().clone().subtract(1, 1, 1))));
        teamSpawns.values().forEach(area -> areas.add(new GameArea(area.getPos1(), area.getPos2())));
    }
}
