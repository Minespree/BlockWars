package net.minespree.block.game.cores;

import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.block.BlockPlugin;
import net.minespree.block.game.BlockCore;
import net.minespree.block.game.CoreShardItem;
import net.minespree.block.map.BlockMapData;
import net.minespree.block.state.BlockState;
import net.minespree.cartographer.util.GameLocation;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.teams.Team;
import net.minespree.wizard.floatingtext.types.PublicFloatingText;
import net.minespree.wizard.particle.ParticleType;
import net.minespree.wizard.util.FireworkUtil;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class NeutralBlockCore extends BlockCore {

    private static final BabelMessage BW_NEUTRALFTAKEN = Babel.translate("bw_neutralftaken");
    private static final BabelMessage BW_NEUTRALFTAKE = Babel.translate("bw_neutralftake");
    private static final BabelMessage BW_NEUTRALANNOUNCE = Babel.translate("bw_neutralannounce");
    private static final BabelMessage BW_NEUTRALALREADYTAKEN = Babel.translate("bw_neutralalreadytaken");
    private static final BabelMessage BW_NEUTRALTAKEN = Babel.translate("bw_neutraltaken");
    private static final BabelMessage BW_NEUTRALTAKE = Babel.translate("bw_neutraltake");
    private static final BabelMessage BW_NEUTRALCAPTURE = Babel.translate("bw_neutralcapture");

    private ArmorStand stand;

    private BukkitTask activeTask;
    @Getter
    private boolean active;

    @Getter
    private Team inPossession;
    @Getter
    private Player holding;
    private ItemStack previousHelmet;
    private boolean slownessBefore;

    private PublicFloatingText text;

    public NeutralBlockCore(GameLocation location) {
        super(location);
    }

    @Override
    public void initialize() {
        Location loc = location.toLocation();
        loc.getBlock().setType(Material.AIR);
        text = new PublicFloatingText(loc.add(0.5, -0.75, 0.5));
        text.hide();
    }

    @Override
    public void damage(Player player) {
        if(inPossession != null) {
            NetworkPlayer np = NetworkPlayer.of(holding);
            BW_NEUTRALALREADYTAKEN.sendMessage(player, inPossession.getColour() + np.getName());
            return;
        }
        holding = player;
        inPossession = RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeam(player);
        previousHelmet = player.getInventory().getHelmet();
        ItemStack banner = new ItemStack(Material.BANNER, 1, DyeColor.getByDyeData((byte) inPossession.getWoolColour()).getData());
        player.getInventory().setHelmet(banner);
        player.getInventory().setItem(8, banner);
        CoreShardItem.update(player);
        if(player.hasPotionEffect(PotionEffectType.SLOW))
            slownessBefore = true;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 0));
        NetworkPlayer np = NetworkPlayer.of(player);
        BW_NEUTRALTAKEN.broadcast(inPossession.getColour() + np.getName());
        BW_NEUTRALTAKE.sendMessage(player);
        text.setText(BW_NEUTRALFTAKEN, inPossession);
    }

    @Override
    public void destroy(Player player) {
        FireworkUtil.randomFirework(player.getEyeLocation(), 2, 2);
        player.getInventory().setHelmet(previousHelmet);
        TeamBlockCore core = ((BlockMapData) RisePlugin.getPlugin().getMapManager().getMapData()).getTeamCores().get(inPossession);
        if(core.getTeam() == inPossession && core.getLocation().toLocation().distance(player.getLocation()) < 3.0) {
            core.setHealth(core.getHealth() + 1);
        }
        if(!slownessBefore)
            holding.removePotionEffect(PotionEffectType.SLOW);
        BW_NEUTRALCAPTURE.broadcast(inPossession);
        getGame().changeStatistic(player, "neutralcores", 1);
        disable();
    }

    @Override
    public void disable() {
        active = false;
        Location loc = location.toLocation();
        loc.getBlock().setType(Material.AIR);
        if(text != null)
            text.hide();
        if(activeTask != null)
            activeTask.cancel();
        if (stand != null)
            stand.remove();
        drop();
    }

    public void activate() {
        if(isCoreAlive()) {
            if (active) return;
            active = true;
            if (stand != null) stand.remove();

            final double y = this.location.toLocation().getY() - 1.5;

            final Location location = this.location.toLocation().clone().add(0, 5, 0);
            location.add(0.5, 0, 0.5);
            stand = location.getWorld().spawn(location, ArmorStand.class);

            stand.setVisible(false);
            stand.setGravity(false);
            stand.setBasePlate(false);
            stand.setHelmet(new ItemStack(Material.WOOL));

            activeTask = Bukkit.getScheduler().runTaskTimer(BlockPlugin.getPlugin(), new BukkitRunnable() {
                int ticks = 0;
                int i = 0;
                float currentYaw = 0f;
                float yawDifference = 6f;

                public void run() {
                    ticks++;
                    if (ticks % 20 == 0 || ticks == 1) {
                        if (inPossession == null) {
                            if (i >= RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeams().size())
                                i = 0;
                            stand.setHelmet(new ItemStack(Material.WOOL, 1, (byte) RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeams().get(i).getWoolColour()));
                            i++;
                        } else {
                            stand.setHelmet(new ItemStack(Material.WOOL, 1, (byte) inPossession.getWoolColour()));
                        }
                    }

                    if(!isCoreAlive()) {
                        disable();
                        cancel();
                    } else {
                        Location l = location.subtract(0, 0.05, 0);
                        l.setYaw(currentYaw += yawDifference);
                        stand.teleport(l);

                        if (l.getY() <= y) {
                            activeTask.cancel();

                            text.setText(BW_NEUTRALFTAKE);
                            text.show();
                            BW_NEUTRALANNOUNCE.broadcast();
                            stand.remove();

                            startBlockTask();
                        }
                    }
                }
            }, 0L, 1L);
        }
    }

    public void startBlockTask() {
        final Location location = this.location.toLocation();
        ParticleType.ENCHANTMENT_TABLE.display(null, location.clone().add(0.5, 0.5, 0.5), null, 16, 0, 0, 0, 1f, 100, Bukkit.getOnlinePlayers());
        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(location, Sound.ZOMBIE_UNFECT, 1F, 1F));

        location.getBlock().setType(Material.WOOL);
        activeTask = Bukkit.getScheduler().runTaskTimer(BlockPlugin.getPlugin(), new BukkitRunnable() {
            int i = 0;
            public void run() {
                if (inPossession == null) {
                    if (i >= RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeams().size())
                        i = 0;
                    location.getBlock().setData((byte) RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeams().get(i).getWoolColour());
                    i++;
                } else {
                    location.getBlock().setData((byte) inPossession.getWoolColour());
                }
                if(!isCoreAlive()) {
                    disable();
                }
            }
        }, 0L, 10L);

    }

    public void drop() {
        if(text != null)
            text.setText(BW_NEUTRALFTAKE);
        if(holding != null && holding.isOnline()) {
            holding.getInventory().setHelmet(previousHelmet);
            holding.getInventory().setItem(7, null);
            holding.getInventory().setItem(8, null);
            if(!slownessBefore)
                holding.removePotionEffect(PotionEffectType.SLOW);
        }
        CoreShardItem.update(holding);
        inPossession = null;
        holding = null;
        previousHelmet = null;
        slownessBefore = false;
    }

    private boolean isCoreAlive() {
        BlockState state = getState();
        for (Team team : getTeamHandler().getTeams()) {
            if(state.getHealth(team) > 0) {
                return true;
            }
        }
        return false;
    }

}
