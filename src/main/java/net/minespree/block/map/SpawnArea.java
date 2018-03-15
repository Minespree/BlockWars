package net.minespree.block.map;

import lombok.Getter;
import lombok.Setter;
import net.minespree.cartographer.util.GameArea;
import net.minespree.cartographer.util.GameLocation;
import org.bukkit.Location;

import java.util.concurrent.ThreadLocalRandom;

@Getter @Setter
public class SpawnArea extends GameArea {

    private float yaw, pitch;

    public SpawnArea(GameLocation pos1, GameLocation pos2, float yaw, float pitch) {
        super(pos1, pos2);

        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Location randomLocation() {
        return new GameLocation(ThreadLocalRandom.current().nextInt((int) getXMin(), (int) getXMax()), getYMin(), ThreadLocalRandom.current().nextInt((int) getZMin(), (int) getZMax()), yaw, pitch).toLocation();
    }

}
