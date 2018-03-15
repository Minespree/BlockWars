package net.minespree.block.game.kits;

import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.block.BlockPlugin;
import net.minespree.block.game.BlockScoreboardFeature;
import net.minespree.block.map.BlockMapData;
import net.minespree.block.state.BlockState;
import net.minespree.block.state.BuildState;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.teams.Team;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class NinjaKitExtension extends RegenKitExtension {

    @Getter
    private static Set<String> using = new HashSet<>();

    @Getter
    private static final Consumer<Player> removeInvis = (player) -> {
        if(player != null && player.isOnline() && using.contains(player.getName())) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            Babel.translate("bw_notinvisible").sendMessage(player);
            getScoreboard().removeFromTeam(player, getTeam(player).getName().toString() + "invis");
            getScoreboard().addToTeam(player, getTeam(player).getName().toString());
            using.remove(player.getName());
        }
    };

    private BlockMapData data;

    public NinjaKitExtension() {
        super("bw_ninja");
    }

    @Override
    public void start() {
        super.start();

        data = (BlockMapData) RisePlugin.getPlugin().getMapManager().getMapData();
    }

    @Override
    public void finish(Player player) {
        removeInvis.accept(player);
    }

    private static BlockScoreboardFeature getScoreboard() {
        return ((BlockState) RisePlugin.getPlugin().getGameStateManager().getCurrentState()).getScoreboard();
    }

    private static Team getTeam(Player player) {
        return RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeam(player);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if(isUsing(event.getPlayer()) && event.getPlayer().getItemInHand().getType() == Material.INK_SACK) {
            if(canUse(player)) {
                Document document = getDocument(event.getPlayer(), "ninja");
                final int length = document.getInteger("length"), regen = document.getInteger("regen");
                use(player, (length + regen) * 50L, 1);
                getScoreboard().addToTeam(player, getTeam(player).getName().toString() + "invis");
                Bukkit.getScheduler().runTaskLater(BlockPlugin.getPlugin(), () -> removeInvis.accept(player), length);
                event.getPlayer().addPotionEffect(new PotionEffect(
                        PotionEffectType.INVISIBILITY,
                        length,
                        0, true, false));
                Babel.translate("bw_invisible").sendMessage(player);
                using.add(player.getName());
            } else {
                double millis = getMillisRemaining(player);
                Babel.translate("bw_cantinvisible").sendMessage(player, String.format("%.1f", millis / 1000d));
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if(!(RisePlugin.getPlugin().getGameStateManager().getCurrentState() instanceof BuildState)) {
            if(event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
                if(((Player) event.getDamager()).hasPotionEffect(PotionEffectType.INVISIBILITY) && !teamHandler.areSameTeam((Player) event.getDamager(), (Player) event.getEntity())) {
                    removeInvis.accept((Player) event.getDamager());
                }
            }
        }
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if(isUsing(event.getPlayer())) {
            data.getTeamCores().values().stream()
                    .filter(core -> core.getLocation().toLocation().equals(event.getBlock().getLocation()) && !core.getTeam().hasPlayer(event.getPlayer()))
                    .forEach(core -> removeInvis.accept(event.getPlayer()));
            data.getNeutralCores().stream()
                    .filter(core -> core.getLocation().toLocation().equals(event.getBlock().getLocation()))
                    .forEach(core -> removeInvis.accept(event.getPlayer()));
        }
    }

}
