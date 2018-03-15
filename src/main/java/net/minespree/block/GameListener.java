package net.minespree.block;

import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.block.game.BlockCore;
import net.minespree.block.game.cores.NeutralBlockCore;
import net.minespree.block.game.cores.TeamBlockCore;
import net.minespree.block.map.BlockMapData;
import net.minespree.block.state.BuildState;
import net.minespree.cartographer.util.GameArea;
import net.minespree.feather.data.damage.impl.CombatTracker;
import net.minespree.feather.data.gamedata.kits.Tier;
import net.minespree.feather.data.gamedata.kits.event.TierSetEvent;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.control.Game;
import net.minespree.rise.control.GameManager;
import net.minespree.wizard.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class GameListener implements Listener {

    private final static BabelMessage CORE_DAMAGE = Babel.translate("bw_coreunderattack");

    @Getter
    private Set<Block> placedBlocks = new HashSet<>();

    private GameManager gameManager;
    private BlockMapData data;
    private Game game;

    public void initialize() {
        gameManager = RisePlugin.getPlugin().getGameManager();
        data = (BlockMapData) RisePlugin.getPlugin().getMapManager().getMapData();
        game = gameManager.getGameInProgress().get();

        Bukkit.getPluginManager().registerEvents(this, BlockPlugin.getPlugin());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        for (BlockCore core : data.getCores()) {
            if(core.getLocation().toLocation().equals(event.getBlock().getLocation())) {
                event.setCancelled(true);
                core.damage(event.getPlayer());
                return;
            }
        }
        if(!placedBlocks.contains(event.getBlock())) {
            Babel.translate("bw_break").sendMessage(event.getPlayer());
            event.setCancelled(true);
        } else if(!event.isCancelled() && !(RisePlugin.getPlugin().getGameStateManager().getCurrentState() instanceof BuildState)) {
            game.changeStatistic(event.getPlayer(), "blocksbroken", 1);
            if(placedBlocks.contains(event.getBlock())) {
                placedBlocks.remove(event.getBlock());
                event.getBlock().setType(Material.AIR);
            }
        }
    }

    @EventHandler (priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if(event.getBlock().getType() == Material.STAINED_CLAY) {
            if(event.getBlockPlaced().getLocation().getY() >= data.getBuildY()) {
                Babel.translate("bw_cantplace").sendMessage(event.getPlayer());
                event.setCancelled(true);
                return;
            }
            if (data.getBorder().inside(event.getBlockPlaced().getLocation())) {
                for (GameArea area : data.getAreas()) {
                    if (area.inside(event.getBlockPlaced().getLocation())) {
                        Babel.translate("bw_cantplace").sendMessage(event.getPlayer());
                        event.setCancelled(true);
                        return;
                    }
                }
            } else {
                event.setCancelled(true);
                Babel.translate("bw_outsideborder").sendMessage(event.getPlayer());
                return;
            }
            if (!event.isCancelled()) {
                game.changeStatistic(event.getPlayer(), "blocksplaced", 1);
                placedBlocks.add(event.getBlockPlaced());
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (RisePlugin.getPlugin().getGameStateManager().getCurrentState() instanceof BuildState)
            return;

        if (event.getFrom().getY() <= 0.0D) {
            if(gameManager.getSpectatorHandler().isSpectator(event.getPlayer())) {
                event.getPlayer().teleport(RisePlugin.getPlugin().getMapManager().getCurrentMap().getSpectatorLocation().toLocation());
            } else {
                CombatTracker.getInstance().damage(event.getPlayer(), 20.0, null, EntityDamageEvent.DamageCause.VOID);
            }
        } else {
            if (event.getFrom().distance(event.getTo()) > 0) {
                for (NeutralBlockCore core : data.getNeutralCores()) {
                    if (core.isActive() && core.getHolding() == event.getPlayer()) {
                        TeamBlockCore c = data.getTeamCores().get(core.getInPossession());
                        if (c.getTeam() == core.getInPossession() && c.getLocation().toLocation().distance(event.getPlayer().getLocation()) <= 3.0) {
                            core.destroy(event.getPlayer());
                        }
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTierSet(TierSetEvent event) {
        Player player = event.getPlayer();
        Tier tier = event.getTier();
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0));
        if(tier.getData() != null) {
            tier.getData().forEach((s, document) -> {
                if (s.equals("addEffect")) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.getByName(document.getString("type").toUpperCase()),
                            Integer.MAX_VALUE, document.getInteger("level")));
                } else if(s.equals("speed")) {
                    player.setWalkSpeed((float) document.getDouble("level").doubleValue());
                }
            });
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().clear();
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        data.getTeamCores().values().stream()
                .filter(core -> core.getLocation().toLocation().equals(event.getBlock().getLocation()) && !core.getTeam().hasPlayer(event.getPlayer()))
                .forEach(core -> core.getTeam().getPlayers().forEach(uuid -> MessageUtil.sendSubtitle(Bukkit.getPlayer(uuid), CORE_DAMAGE, 0, 40, 20)));
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) event.setCancelled(true);
    }

}
