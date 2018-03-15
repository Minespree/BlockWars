package net.minespree.block.game;

import com.google.common.collect.Maps;
import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelStringMessageType;
import net.minespree.block.BlockPlugin;
import net.minespree.block.game.cores.TeamBlockCore;
import net.minespree.block.map.BlockMapData;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.teams.Team;
import net.minespree.wizard.gui.PerPlayerInventoryGUI;
import net.minespree.wizard.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
public class CoreShardItem {

    private final static BabelStringMessageType BW_CANT_AFFORD = Babel.translate("bw_cant_afford");
    private final static BabelStringMessageType BW_PURCHASED = Babel.translate("bw_purchased");
    private final static BabelStringMessageType BW_CORE_SHARDS = Babel.translate("bw_core_shards");
    private final static BabelStringMessageType BW_SHARDS_PRICE = Babel.translate("bw_shards_price");
    private final static BabelStringMessageType BW_EXTRA_HEALTH = Babel.translate("bw_extra_health");
    private final static BabelStringMessageType BW_STRENGTH = Babel.translate("bw_strength");
    private final static BabelStringMessageType BW_RESISTANCE = Babel.translate("bw_resistance");
    private final static BabelStringMessageType BW_BLOCKS = Babel.translate("bw_blocks");
    private final static BabelStringMessageType BW_CORE_HEALTH = Babel.translate("bw_core_health");
    private final static BabelStringMessageType BW_PLAYER_BOUGHT = Babel.translate("bw_player_bought");

    @Getter
    private static PerPlayerInventoryGUI gui;
    @Getter
    private static Map<UUID, Integer> shards = Maps.newHashMap();

    private final static CoreShardItem HEART = new CoreShardItem(BW_EXTRA_HEALTH, new ItemBuilder(Material.GOLDEN_APPLE), 5, 4, player -> {
        player.setMaxHealth(player.getMaxHealth() + 2.0);
        player.setHealth(player.getHealth() + 2.0);
    });
    private final static CoreShardItem STRENGTH = new CoreShardItem(BW_STRENGTH, new ItemBuilder(Material.IRON_SWORD), 4, 4, player -> {
        for (UUID uuid : RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeam(player).getAlive()) {
            Bukkit.getPlayer(uuid).addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 300, 0));
        }
    });
    private final static CoreShardItem RESISTANCE = new CoreShardItem(BW_RESISTANCE, new ItemBuilder(Material.IRON_CHESTPLATE), 3, 3, player -> {
        for (UUID uuid : RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeam(player).getAlive()) {
            Bukkit.getPlayer(uuid).addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 600, 0));
        }
    });
    private final static CoreShardItem BLOCKS = new CoreShardItem(BW_BLOCKS, p -> new ItemBuilder(Material.STAINED_CLAY).amount(10)
            .durability(RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeam(p).getWoolColour()).build(p), 2, 3, player -> {
        short colour = RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeam(player).getWoolColour();
        player.getInventory().addItem(new ItemStack(Material.STAINED_CLAY, 10, colour));
    });
    private final static CoreShardItem CORE_HEALTH = new CoreShardItem(BW_CORE_HEALTH, p -> new ItemBuilder(Material.WOOL)
            .durability(RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeam(p).getWoolColour()).build(p), 6, 5, player -> {
        BlockMapData data = (BlockMapData) RisePlugin.getPlugin().getMapManager().getMapData();
        Team team = RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeam(player);
        TeamBlockCore core = data.getTeamCores().get(team);
        core.setHealth(core.getHealth() + 1);
    });

    private BabelStringMessageType name;
    private int slot, price;
    private Consumer<Player> shardConsumer;

    public CoreShardItem(BabelStringMessageType name, Function<Player, ItemStack> menuItem, int slot, int price, Consumer<Player> shardConsumer) {
        this.name = name;
        this.slot = slot;
        this.price = price;
        this.shardConsumer = shardConsumer;

        if(gui == null) {
            gui = new PerPlayerInventoryGUI(Babel.translate("bw_core_shard_title"), 9, BlockPlugin.getPlugin());
        }
        gui.setItem(slot, p -> {
            ItemBuilder builder = new ItemBuilder(menuItem.apply(p)).displayName(name);
            builder.lore("");
            builder.lore(BW_SHARDS_PRICE, price);
            return builder.build(p);
        }, (p, type) -> {
            if(shards.getOrDefault(p.getUniqueId(), 0) >= price) {
                shardConsumer.accept(p);
                shards.put(p.getUniqueId(), shards.getOrDefault(p.getUniqueId(), 0) - price);
                update(p);
                BW_PURCHASED.sendMessage(p, name.toString(p));
                for (UUID uuid : RisePlugin.getPlugin().getGameManager().getTeamHandler().get().getTeam(p).getAlive()) {
                    Player player = Bukkit.getPlayer(uuid);
                    BW_PLAYER_BOUGHT.sendMessage(Bukkit.getPlayer(uuid), NetworkPlayer.of(p).getName(), name.toString(player));
                }
            } else {
                BW_CANT_AFFORD.sendMessage(p);
            }
            p.closeInventory();
        });
    }

    public CoreShardItem(BabelStringMessageType name, ItemBuilder menuItem, int slot, int price, Consumer<Player> shardConsumer) {
        this(name, menuItem::build, slot, price, shardConsumer);
    }

    public static void update(Player player) {
        if(player == null)
            return;
        if(shards.getOrDefault(player.getUniqueId(), 0) > 0) {
            ItemBuilder builder = new ItemBuilder(Material.PRISMARINE_SHARD, shards.get(player.getUniqueId())).displayName(BW_CORE_SHARDS, shards.get(player.getUniqueId()));
            ItemStack item = player.getInventory().getItem(8);
            if(item != null && item.getType() != Material.PRISMARINE_SHARD) {
                player.getInventory().setItem(7, item);
            }
            player.getInventory().setItem(8, builder.build(player));
        } else if(player.getInventory().getItem(8) != null && player.getInventory().getItem(8).getType() == Material.PRISMARINE_SHARD) {
            player.getInventory().setItem(8, player.getInventory().getItem(7));
            player.getInventory().setItem(7, null);
        }
    }

}
