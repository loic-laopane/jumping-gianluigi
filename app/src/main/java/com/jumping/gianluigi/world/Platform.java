package com.jumping.gianluigi.world;

/** A floating platform the player can stand on. */
public class Platform {
    public final float x;
    public final float y;      // top surface Y (screen coords – smaller = higher)
    public final float width;

    public Platform(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public float right()  { return x + width; }
    public float bottom() { return y + com.jumping.gianluigi.Constants.PLATFORM_H; }
}
