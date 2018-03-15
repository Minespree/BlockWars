package net.minespree.block;

import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.block.commands.TestingCommands;
import net.minespree.block.game.BlockSpawnHandler;
import net.minespree.block.game.kits.*;
import net.minespree.block.map.BlockMapData;
import net.minespree.block.state.BuildState;
import net.minespree.feather.command.system.CommandManager;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.player.stats.local.SessionStatRegistry;
import net.minespree.feather.player.stats.local.StatType;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.control.Gamemode;
import net.minespree.rise.features.RespawnWaveFeature;
import net.minespree.rise.util.InformationBook;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class BlockPlugin extends JavaPlugin {

    @Getter
    private static BlockPlugin plugin;

    public void onEnable() {
        plugin = this;

        CommandManager.getInstance().registerClass(TestingCommands.class);

        RisePlugin.getPlugin().getGameManager().setGamemode(Gamemode.builder()
                .plugin(plugin)
                .extraDataLoader(map -> new BlockMapData())
                .initialGameState(BuildState::new)
                .teamHandler(true)
                .spawnHandler(BlockSpawnHandler::new)
                .features(Collections.singletonList(game -> new RespawnWaveFeature(8, game)))
                .game(GameRegistry.Type.BLOCKWARS)
                .statisticSize(27)
                .statisticMap(new HashMap<String, StatType>() {
                    {put("kills", new StatType("bw_kills", SessionStatRegistry.Sorter.HIGHEST_SCORE, false, 5, 3, GameRegistry.Type.BLOCKWARS));}
                    {put("assists", new StatType("bw_assists", SessionStatRegistry.Sorter.HIGHEST_SCORE, true, 2, 11, GameRegistry.Type.BLOCKWARS));}
                    {put("deaths", new StatType("bw_deaths", SessionStatRegistry.Sorter.HIGHEST_SCORE, false, 0, 4, GameRegistry.Type.BLOCKWARS));}
                    {put("eliminations", new StatType("bw_eliminations", SessionStatRegistry.Sorter.HIGHEST_SCORE, true, 0, 5, GameRegistry.Type.BLOCKWARS));}
                    {put("coredamage", new StatType("bw_coredamage", SessionStatRegistry.Sorter.HIGHEST_SCORE, false, 5, 12, GameRegistry.Type.BLOCKWARS));}
                    {put("coredestroyed", new StatType("bw_coresdestroyed", SessionStatRegistry.Sorter.HIGHEST_SCORE, true, 0, 13, GameRegistry.Type.BLOCKWARS));}
                    {put("neutralcores", new StatType("bw_neutralcores", SessionStatRegistry.Sorter.HIGHEST_SCORE, false, 5, 22, GameRegistry.Type.BLOCKWARS));}
                    {put("blocksbroken", new StatType("bw_blocksbroken", SessionStatRegistry.Sorter.HIGHEST_SCORE, false, 0, 15, GameRegistry.Type.BLOCKWARS));}
                    {put("blocksplaced", new StatType("bw_blocksplaced", SessionStatRegistry.Sorter.HIGHEST_SCORE, false, 0, 14, GameRegistry.Type.BLOCKWARS));}
                    {put("win", new StatType("bw_win", SessionStatRegistry.Sorter.HIGHEST_SCORE, true, 25, -1, GameRegistry.Type.BLOCKWARS));}
                    {put("draw", new StatType("bw_draw", SessionStatRegistry.Sorter.HIGHEST_SCORE, true, 10, -1, GameRegistry.Type.BLOCKWARS));}
                    {put("loss", new StatType("bw_loss", SessionStatRegistry.Sorter.HIGHEST_SCORE, true, 0, -1, GameRegistry.Type.BLOCKWARS));}
                    {put("gamesPlayed", new StatType("bw_gameplayed", SessionStatRegistry.Sorter.HIGHEST_SCORE, true, 0, -1, GameRegistry.Type.BLOCKWARS));}
                    {put("timePlayed", new StatType("bw_timeplayed", SessionStatRegistry.Sorter.HIGHEST_SCORE,true, 0, -1,
                            (p, o) -> p.getPersistentStats().getLongStatistics(GameRegistry.Type.BLOCKWARS).increment("bw_timeplayed", (Long) o)));}
                })
                .informationBook(new InformationBook(Babel.translate("blockwars_information_title"),"blockwars_information_page1", "blockwars_information_page2", "blockwars_information_page3"))
                .kitExtensions(Arrays.asList(new BomberKitExtension(), new IcemanKitExtension(),
                        new MinerKitExtension(), new NinjaKitExtension(), new ArcherKitExtension()))
                .build());
    }

}
