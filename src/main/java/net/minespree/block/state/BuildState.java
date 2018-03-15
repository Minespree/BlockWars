package net.minespree.block.state;

import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.babel.ComplexBabelMessage;
import net.minespree.block.game.BlockCore;
import net.minespree.block.game.CoreShardItem;
import net.minespree.cartographer.util.GameArea;
import net.minespree.feather.data.gamedata.kits.KitExtension;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.control.SpawnHandler;
import net.minespree.rise.states.GameState;
import net.minespree.rise.teams.Team;
import net.minespree.wizard.util.Chat;
import net.minespree.wizard.util.MessageUtil;
import net.minespree.wizard.util.SetupUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BuildState extends BlockState {

    private final static BabelMessage SW_FIGHT = Babel.translate("sw_fight");

    private final static String[] COLOURS = new String[] {Chat.RED, Chat.GOLD, Chat.YELLOW, Chat.AQUA, Chat.CYAN, Chat.BLUE};

    private Map<UUID, Integer> blocks = new HashMap<>();

    public BuildState() {
        super(ChatColor.AQUA.toString() + ChatColor.BOLD + "Build", 90);
    }

    public void onStart(GameState previous) {
        super.onStart(previous);

        teams.clear();
        CoreShardItem.getShards().clear();

        teamHandler.enableTeamChat();

        data.getListener().initialize();

        scoreboard.onStart();

        mapManager.getCurrentWorld().get().setTime(mapManager.getCurrentMap().getTime());
        mapManager.getCurrentWorld().get().setGameRuleValue("doDaylightCycle", "false");

        data.getCores().forEach(BlockCore::initialize);

        Bukkit.getOnlinePlayers().forEach(player -> {
            Team team = teamHandler.getTeam(player);
            MessageUtil.sendSubtitle(player, Babel.translate("bw_buildstart"), 20, 100, 20);
            SetupUtil.setupPlayer(player, false);
            gamemode.getSpawnHandler().get().spawnPlayer(player, SpawnHandler.SpawnReason.GAME_START);
            teams.put(player.getUniqueId(), team.getName().getKey());
            player.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE));
            int amount = team.size() <= 2 ? 64 : (team.size() == 3 ? 43 : 32);
            player.getInventory().addItem(new ItemStack(Material.STAINED_CLAY, amount, team.getWoolColour()));
        });
        gamemode.getKitExtensions().forEach(KitExtension::start);
    }

    public void onStop(@Nonnull GameState next) {
        super.onStop(next);

        GameArea border = data.getWall();
        for(int y = (int) border.getYMin(); y <= border.getYMax(); y++) {
            for(int z = (int) border.getZMin(); z <= border.getZMax(); z++) {
                for(int x = (int) border.getXMin(); x <= border.getXMax(); x++) {
                    Block block = border.getPos1().toLocation().getWorld().getBlockAt(x, y, z);
                    for (ItemStack item : data.getWallItems()) {
                        if(item.getType() == block.getType() && block.getData() == item.getData().getData()) {
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if(time <= 5) {
            ComplexBabelMessage message = new ComplexBabelMessage();
            message.append(Chat.BOLD + COLOURS[time]);
            if(time == 0) {
                message.append(SW_FIGHT);
            } else message.append(time + "");
            Bukkit.getOnlinePlayers().forEach(player -> MessageUtil.sendTitle(player, message, 0, 30, 20));
        }
    }

    public void timerEnd() {
        RisePlugin.getPlugin().getGameStateManager().changeState(new FightState());
    }

    @Override
    public void onJoin(Player player) {
        super.onJoin(player);

        Team team = teamHandler.getTeam(player);
        if(getHealth(team) > 0) {
            player.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE));
            if(blocks.containsKey(player.getUniqueId())) {
                player.getInventory().addItem(new ItemStack(Material.STAINED_CLAY, blocks.get(player.getUniqueId()), team.getWoolColour()));
            } else {
                int amount = team.size() <= 2 ? 64 : (team.size() == 3 ? 43 : 32);
                player.getInventory().addItem(new ItemStack(Material.STAINED_CLAY, amount, team.getWoolColour()));
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(event.getPlayer().getLocation().getY() < 0) {
            gamemode.getSpawnHandler().get().spawnPlayer(event.getPlayer(), SpawnHandler.SpawnReason.RESPAWN);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.STAINED_CLAY) {
            blocks.put(event.getPlayer().getUniqueId(), event.getItemInHand() == null ? 0 : event.getItemInHand().getAmount());
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if(event.getItem().getItemStack().getType() == Material.STAINED_CLAY) {
            blocks.put(event.getPlayer().getUniqueId(), blocks.getOrDefault(event.getPlayer().getUniqueId(), 0) + 1);
            event.setCancelled(false);
        }
    }

}
