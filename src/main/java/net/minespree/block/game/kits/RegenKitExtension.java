package net.minespree.block.game.kits;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minespree.block.BlockPlugin;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.data.gamedata.kits.KitExtension;
import net.minespree.feather.data.gamedata.kits.Tier;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.events.SpectatorSetEvent;
import net.minespree.rise.teams.TeamHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class RegenKitExtension extends KitExtension {

    private BukkitTask task;

    private Map<UUID, RegenData> data = new ConcurrentHashMap<>();

    protected TeamHandler teamHandler;

    public RegenKitExtension(String kitId) {
        super(GameRegistry.Type.BLOCKWARS, kitId);
    }

    public void start() {
        super.start();

        teamHandler = RisePlugin.getPlugin().getGameManager().getTeamHandler().get();
        if(task != null)
            task.cancel();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (RegenData regenData : data.values()) {
                    if(regenData.getTotalUnlockTime() <= System.currentTimeMillis()) {
                        regenData.amount--;
                        finish(regenData.getPlayer());
                        if(regenData.amount <= 0) {
                            data.remove(regenData.getPlayer().getUniqueId());
                        } else {
                            regenData.totalUnlockTime = System.currentTimeMillis() + regenData.getUnlockTime();
                        }
                    }
                }
            }
        }.runTaskTimer(BlockPlugin.getPlugin(), 1L, 1L);
    }

    public void stop() {
        super.stop();

        if(task != null)
            task.cancel();
        task = null;
    }

    public void use(Player player, long unlockTime, int amount) {
            if(data.containsKey(player.getUniqueId())) {
                data.get(player.getUniqueId()).amount += amount;
            } else {
                data.put(player.getUniqueId(), new RegenData(player, unlockTime, System.currentTimeMillis() + unlockTime, amount));
            }
    }

    public boolean canUse(Player player) {
        return getMillisRemaining(player) == 0L;
    }

    public long getMillisRemaining(Player player) {
        if(data.containsKey(player.getUniqueId())) {
            return data.get(player.getUniqueId()).totalUnlockTime - System.currentTimeMillis();
        }
        return 0L;
    }

    public abstract void finish(Player player);

    @Override
    public void setKit(Player player, Tier tier) {
        data.remove(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        super.onQuit(event);

        data.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onSpectator(SpectatorSetEvent event) {
        data.remove(event.getPlayer().getUniqueId());
    }

    @Data @Getter @Setter
    private class RegenData {

        private final Player player;
        @NonNull
        private long unlockTime, totalUnlockTime;
        @NonNull
        private int amount;
    }

}
