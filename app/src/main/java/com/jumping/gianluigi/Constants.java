package com.jumping.gianluigi;

public final class Constants {
    // Virtual game resolution
    public static final float VW = 900f;
    public static final float VH = 500f;

    // Ground surface Y (screen coords: y=0 top, increases down)
    public static final float GROUND_Y = 415f;

    // ── Physics (rebalanced: lower jumps, tighter gaps) ───────────────────
    public static final float GRAVITY         = 1550f;  // gentler gravity
    public static final float MAX_FALL        = 950f;
    public static final float PLAYER_SPEED    = 138f;   // a bit slower → more control
    public static final float JUMP_VEL        = -630f;  // lower jump (was -860)
    public static final float DOUBLE_JUMP_VEL = -530f;  // lower double-jump (was -720)

    // ── Player sprite (8×12 game-pixels, 1 gp = P=4 virtual units) ───────
    public static final float PW = 32f;
    public static final float PH = 48f;

    // ── Enemy sprite ──────────────────────────────────────────────────────
    public static final float EW = 32f;
    public static final float EH = 32f;

    // Platform height
    public static final float PLATFORM_H = 12f;

    // Pixel art unit
    public static final float P = 4f;

    // Level width base (~72 s at 138 px/s)
    public static final float LEVEL_W_BASE = 9936f;

    // Starting lives
    public static final int START_LIVES = 3;

    private Constants() {}
}
