package com.jumping.gianluigi;

public final class Constants {
    // Virtual game resolution (drawn into, then scaled to screen)
    public static final float VW = 900f;
    public static final float VH = 500f;

    // Ground surface Y (screen coords: y=0 top, increases down)
    public static final float GROUND_Y = 415f;

    // Physics
    public static final float GRAVITY      = 1700f;   // downward accel (px/s²)
    public static final float MAX_FALL     = 1100f;   // max downward speed
    public static final float PLAYER_SPEED = 155f;    // horizontal auto-walk
    public static final float JUMP_VEL     = -860f;   // upward velocity on jump
    public static final float DOUBLE_JUMP_VEL = -720f; // second jump velocity

    // Player sprite size in virtual units (8×12 "pixels" × 4 px/pixel)
    public static final float PW = 32f;
    public static final float PH = 48f;

    // Enemy sprite size
    public static final float EW = 32f;
    public static final float EH = 32f;

    // Platform height
    public static final float PLATFORM_H = 12f;

    // Pixel art unit (1 game-pixel = 4 virtual units)
    public static final float P = 4f;

    // Level total width (155 px/s × ~65 s)
    public static final float LEVEL_W_BASE = 10100f;

    // Lives at start
    public static final int START_LIVES = 3;

    private Constants() {}
}
