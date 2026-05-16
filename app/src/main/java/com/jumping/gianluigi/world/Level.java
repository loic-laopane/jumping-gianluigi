package com.jumping.gianluigi.world;

import java.util.List;

public class Level {
    public final int number;
    public final float width;
    public final float finishX;

    /** Ground segments as {startX, endX} pairs. */
    public final float[][] groundSegs;

    /** Floating platforms. */
    public final List<Platform> platforms;

    /** Enemies as {startX, groundSurface, patrolLeft, patrolRight}. */
    public final float[][] enemies;

    /** Power-ups as {x, y, type}: type 0=STAR, 1=SPEED, 2=SUPER_JUMP. */
    public final float[][] powerUps;

    public Level(int number, float width, float finishX,
                 float[][] groundSegs, List<Platform> platforms,
                 float[][] enemies, float[][] powerUps) {
        this.number    = number;
        this.width     = width;
        this.finishX   = finishX;
        this.groundSegs = groundSegs;
        this.platforms  = platforms;
        this.enemies    = enemies;
        this.powerUps   = powerUps;
    }
}
