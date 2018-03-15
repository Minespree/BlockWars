package net.minespree.block.game.cores;

import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.babel.ComplexBabelMessage;
import net.minespree.block.BlockPlugin;
import net.minespree.block.game.BlockCore;
import net.minespree.block.game.CoreShardItem;
import net.minespree.cartographer.util.GameLocation;
import net.minespree.pirate.cosmetics.CosmeticManager;
import net.minespree.pirate.cosmetics.CosmeticType;
import net.minespree.pirate.cosmetics.games.blockwars.CoreDestructionCosmetic;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.features.RespawnWaveFeature;
import net.minespree.rise.teams.Team;
import net.minespree.wizard.floatingtext.types.PublicFloatingText;
import net.minespree.wizard.particle.ParticleType;
import net.minespree.wizard.particle.effect.BlockOutlineEffect;
import net.minespree.wizard.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TeamBlockCore extends BlockCore {

    private static final BabelMessage BW_HEALTH = Babel.translate("bw_health");
    private static final BabelMessage BW_OWNTEAMCORE = Babel.translate("bw_ownteamcore");
    private static final BabelMessage BW_CORECOOLDOWN = Babel.translate("bw_corecooldown");
    private static final BabelMessage BW_COREDAMAGE = Babel.translate("bw_coredamage");
    private static final BabelMessage BW_COREDESTROY = Babel.translate("bw_coredestroy");
    private static final BabelMessage BW_YOURCOREDESTROYED = Babel.translate("bw_yourcoredestroyed");
    private static final BabelMessage BW_COREDESTROY_BY = Babel.translate("bw_coredestroy_by");
    private static final BabelMessage BW_CANTRESPAWN = Babel.translate("bw_cantrespawn");

    private PublicFloatingText floatingText;
    private long damageCooldown = 0;

    private CoreDestructionCosmetic destructionCosmetic;

    @Getter
    private Team team;
    @Getter
    private int health;

    public TeamBlockCore(GameLocation location, Team team) {
        super(location);

        this.team = team;
        this.health = 10;
    }

    @Override
    public void initialize() {
        floatingText = new PublicFloatingText(getLocation().toLocation().add(0.5, -0.5, 0.5));
        if (team.getPlayers().size() == 0) {
            disable();
            return;
        } else {
            location.toLocation().getBlock().setType(Material.WOOL);
            location.toLocation().getBlock().setData((byte) team.getWoolColour());
            floatingText.show();
        }
        update();
    }

    @Override
    public void damage(Player player) {
        if (player != null && getTeamHandler().getTeam(player) == team) {
            BW_OWNTEAMCORE.sendMessage(player);
            return;
        }
        if (player != null && System.currentTimeMillis() < damageCooldown) {
            BW_CORECOOLDOWN.sendMessage(player);
            return;
        }
        if(player != null) {
            CoreShardItem.getShards().put(player.getUniqueId(), CoreShardItem.getShards().getOrDefault(player.getUniqueId(), 0) + 1);
            CoreShardItem.update(player);
        }
        setHealth(health - 1);
        Location loc = location.toLocation();
        Block block = loc.getBlock();
        boolean airAlready = block.getType() == Material.AIR;
        if(!airAlready)
            damageCooldown = System.currentTimeMillis() + 5000;
        loc.getWorld().playSound(loc, Sound.EXPLODE, 1F, 2F + (1 / (0.1F * (health <= 0 ? 1 : health))));
        for (UUID uuid : team.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.playSound(p.getLocation(), Sound.EXPLODE, 1F, 2F + (1 / (0.1F * (health <= 0 ? 1 : health))));
            }
        }
        if (health > 0 && player != null) {
            BW_COREDAMAGE.sendMessage(player, health);
        }
        if(player != null) {
            RisePlugin.getPlugin().getGameManager().getGameInProgress().get().changeStatistic(player, "coredamage", 1);
        }
        if (health <= 0) {
            destroy(player);
            return;
        }

        block.setType(Material.AIR); // glass
        Location location = block.getLocation().add(0.5, 0.5, 0.5);
        BlockOutlineEffect effect = new BlockOutlineEffect(BlockPlugin.getPlugin(), ParticleType.REDSTONE, location);

        if(!airAlready) {
            effect.iterations = 18;
            effect.speed = 1.1f;
            effect.color = team.getRgbColour();
            effect.particles = 7;
            effect.edgeLength = 1f;

            effect.start(Bukkit.getOnlinePlayers());

            block.setData((byte) team.getWoolColour());
            Bukkit.getScheduler().runTaskLater(BlockPlugin.getPlugin(), () -> {
                block.setType(Material.WOOL);
                block.setData((byte) team.getWoolColour());
            }, 100L);
        }
    }

    @Override
    public void destroy(Player player) {
        disable();
        if(player != null) {
            CosmeticManager.getCosmeticManager().getSelectedCosmetic(player, CosmeticType.BW_CORE_DESTROY)
                    .ifPresent(cosmetic -> {
                        destructionCosmetic = (CoreDestructionCosmetic) cosmetic;
                        destructionCosmetic.destroy(location.toLocation().getBlock());
                    });
            BW_COREDESTROY.sendMessage(player);
            getGame().changeStatistic(player, "coredestroyed", 1);
        }
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.playSound(location.toLocation(), Sound.IRONGOLEM_DEATH, 3F, 1F);
            BW_COREDESTROY_BY.sendMessage(p, team);
            if (team.hasPlayer(p)) {
                MessageUtil.sendTitle(p, BW_YOURCOREDESTROYED, 20, 60, 20);
                MessageUtil.sendSubtitle(p, BW_CANTRESPAWN.toString(player), 20, 60, 20);
            } else {
                MessageUtil.sendTitle(player, BW_COREDESTROY_BY.toString(team), 20, 60, 20);
            }
        });
        for (UUID uuid : team.getAlive()) {
            Player p = Bukkit.getPlayer(uuid);
            getGame().getFeatures().stream()
                    .filter(feature -> feature instanceof RespawnWaveFeature)
                    .map(feature -> (RespawnWaveFeature) feature)
                    .forEach(feature -> {
                        if (feature.getCurrentWave().contains(p.getUniqueId()) || feature.getNextWave().contains(p.getUniqueId())) {
                            team.kill(p);
                        }
                        feature.exclude(p);
                    });
        }
        if (team.getAlive().size() == 0) {
            getState().getScoreboard().setTeamScore(team);
            getState().checkWinners();
        }
    }

    @Override
    public void disable() {
        health = 0;
        floatingText.hide();
        Location loc = location.toLocation();
        loc.getBlock().setType(Material.AIR); // TODO : Explosion effect & player specific core destroy effect
    }

    public void setHealth(int health) {
        boolean revive = this.health == 0 && health == 1;
        this.health = health;

        if(revive) {
            floatingText.show();
            Location loc = location.toLocation();
            loc.getBlock().setType(Material.WOOL);
            loc.getBlock().setData((byte) team.getWoolColour());
            team.getDead().clear();
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                getGame().getFeatures().stream()
                        .filter(feature -> feature instanceof RespawnWaveFeature)
                        .forEach(feature -> {
                            ((RespawnWaveFeature) feature).getExcluded().remove(player.getUniqueId());
                            if(RisePlugin.getPlugin().getGameManager().getSpectatorHandler().isSpectator(player)) {
                                ((RespawnWaveFeature) feature).getNextWave().add(player.getUniqueId());
                            }
                        });
            }
            if(destructionCosmetic != null) {
                destructionCosmetic.clear(loc.getBlock());
            }
        }

        update();
    }

    public void update() {
        floatingText.setText(new ComplexBabelMessage()
                .append(team.getColour().toString())
                .append(BW_HEALTH, team.getColourBlindCode(), health));
        getState().getScoreboard().setTeamScore(team);
    }

}
