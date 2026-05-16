package com.jumping.gianluigi.entities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.jumping.gianluigi.Constants;
import static com.jumping.gianluigi.Constants.P;

public class Player {
    public float x, y;
    public float vy;
    public boolean onGround;
    public boolean hasDoubleJump;
    public boolean dead;

    public int powerUp;         // PowerUp.NONE / STAR / SPEED / SUPER_JUMP
    public float powerUpTimer;

    // Death animation
    public float deadVy;
    public float deadTimer;

    // Walk animation
    private float animTimer;
    private int   animFrame;

    // Star flicker
    private float starFlicker;

    public Player(float startX) {
        x = startX;
        y = Constants.GROUND_Y - Constants.PH;
        vy = 0;
        onGround = true;
        hasDoubleJump = false;
        dead = false;
        powerUp = PowerUp.NONE;
    }

    public void update(float dt) {
        if (dead) {
            deadVy += Constants.GRAVITY * dt;
            y += deadVy * dt;
            deadTimer -= dt;
            return;
        }

        // Gravity
        vy += Constants.GRAVITY * dt;
        if (vy > Constants.MAX_FALL) vy = Constants.MAX_FALL;

        // Horizontal speed
        float spd = (powerUp == PowerUp.SPEED) ? Constants.PLAYER_SPEED * 1.6f : Constants.PLAYER_SPEED;
        x += spd * dt;
        y += vy * dt;

        // Power-up countdown
        if (powerUp != PowerUp.NONE) {
            powerUpTimer -= dt;
            if (powerUpTimer <= 0) powerUp = PowerUp.NONE;
        }

        // Walk animation (only when on ground)
        if (onGround) {
            animTimer += dt;
            if (animTimer > 0.14f) { animTimer = 0; animFrame = 1 - animFrame; }
        }

        if (powerUp == PowerUp.STAR) starFlicker += dt;

        // Reset onGround each frame; collision detection will set it back if needed
        onGround = false;
    }

    /** Called by input handler (tap / double-tap). */
    public void tryJump() {
        if (dead) return;
        float jv = (powerUp == PowerUp.SUPER_JUMP) ? Constants.JUMP_VEL * 1.3f : Constants.JUMP_VEL;
        float djv = (powerUp == PowerUp.SUPER_JUMP) ? Constants.DOUBLE_JUMP_VEL * 1.3f : Constants.DOUBLE_JUMP_VEL;

        if (onGround) {
            vy = jv;
            onGround = false;
            hasDoubleJump = true;
        } else if (hasDoubleJump) {
            vy = djv;
            hasDoubleJump = false;
        }
    }

    /** Call when player lands on a surface. */
    public void land(float surfaceY) {
        y  = surfaceY - Constants.PH;
        vy = 0;
        onGround    = true;
        hasDoubleJump = false;
    }

    public void die() {
        if (dead) return;
        if (powerUp == PowerUp.STAR) return;  // invincible
        dead    = true;
        deadVy  = -500f;
        deadTimer = 1.8f;
        vy = 0;
    }

    public boolean isInvincible() { return powerUp == PowerUp.STAR; }

    public void collectPowerUp(int type) {
        powerUp      = type;
        powerUpTimer = 7f;
    }

    public RectF getBounds() {
        return new RectF(x, y, x + Constants.PW, y + Constants.PH);
    }

    // ── Pixel-art drawing ──────────────────────────────────────────────────
    public void draw(Canvas canvas, Paint paint) {
        float px = x;
        float py = y;

        // Star aura
        if (powerUp == PowerUp.STAR) {
            boolean show = ((int)(starFlicker * 8) % 2 == 0);
            if (show) {
                paint.setColor(0x80FFFF00);
                canvas.drawRect(px - P*2, py - P*2,
                        px + Constants.PW + P*2, py + Constants.PH + P*2, paint);
            }
        }

        // ── HEAD ─ dark skin tone (pixel art portrait) ──────────────────
        // Hair (black, short crop – Mbappé style)
        paint.setColor(0xFF0D0500);
        canvas.drawRect(px + P, py,            px + P*7, py + P*2, paint);
        canvas.drawRect(px,     py + P*0.5f,   px + P*2, py + P*3, paint);
        canvas.drawRect(px + P*6, py + P*0.5f, px + P*8, py + P*3, paint);

        // Face skin (dark brown – realistic)
        paint.setColor(0xFFBF8040);
        canvas.drawRect(px + P*2, py + P*2, px + P*6, py + P*5, paint);

        // Eyes – white then dark pupil
        paint.setColor(Color.WHITE);
        canvas.drawRect(px + P*2,       py + P*2.5f, px + P*3.5f, py + P*3.5f, paint);
        canvas.drawRect(px + P*4.5f,    py + P*2.5f, px + P*6,    py + P*3.5f, paint);
        paint.setColor(0xFF111111);
        canvas.drawRect(px + P*2.5f,    py + P*2.8f, px + P*3.2f, py + P*3.2f, paint);
        canvas.drawRect(px + P*4.8f,    py + P*2.8f, px + P*5.5f, py + P*3.2f, paint);

        // Smile
        paint.setColor(0xFF996633);
        canvas.drawRect(px + P*3, py + P*4, px + P*5, py + P*4.5f, paint);

        // ── BODY ─ French blue jersey ────────────────────────────────────
        paint.setColor(0xFF00209F);
        canvas.drawRect(px + P, py + P*5, px + P*7, py + P*9, paint);

        // White chest stripe
        paint.setColor(Color.WHITE);
        canvas.drawRect(px + P*3, py + P*5, px + P*5, py + P*6.5f, paint);

        // Red accent (French flag collar)
        paint.setColor(0xFFEF4135);
        canvas.drawRect(px + P*2, py + P*5, px + P*3, py + P*5.5f, paint);

        // ── LEGS ─ animated walk ──────────────────────────────────────────
        // Shorts (dark blue)
        paint.setColor(0xFF001166);
        canvas.drawRect(px + P, py + P*9, px + P*7, py + P*10.5f, paint);

        if (animFrame == 0 || !onGround) {
            // Left leg forward, right leg back
            paint.setColor(0xFFBF8040);  // skin (lower leg)
            canvas.drawRect(px + P, py + P*10.5f, px + P*3, py + P*11.5f, paint);
            canvas.drawRect(px + P*4, py + P*10, px + P*6, py + P*11, paint);
            // Shoes
            paint.setColor(0xFF111111);
            canvas.drawRect(px,        py + P*11,   px + P*3,  py + P*12, paint);
            canvas.drawRect(px + P*4,  py + P*10.5f, px + P*6.5f, py + P*11.5f, paint);
        } else {
            // Right leg forward, left leg back
            paint.setColor(0xFFBF8040);
            canvas.drawRect(px + P*4, py + P*10.5f, px + P*6, py + P*11.5f, paint);
            canvas.drawRect(px + P,   py + P*10,    px + P*3, py + P*11,    paint);
            // Shoes
            paint.setColor(0xFF111111);
            canvas.drawRect(px + P*4,  py + P*11,    px + P*7,  py + P*12, paint);
            canvas.drawRect(px,        py + P*10.5f, px + P*2.5f, py + P*11.5f, paint);
        }
    }

    /** Draw dead player (upside down, spinning – simplified as flipped rectangle). */
    public void drawDead(Canvas canvas, Paint paint) {
        float px = x;
        float py = y;
        paint.setColor(0xFF00209F);
        canvas.drawRect(px, py, px + Constants.PW, py + Constants.PH * 0.6f, paint);
        paint.setColor(0xFFBF8040);
        canvas.drawRect(px + P*2, py + Constants.PH * 0.6f, px + P*6, py + Constants.PH, paint);
        // X eyes
        paint.setColor(Color.WHITE);
        canvas.drawRect(px + P*2, py + Constants.PH * 0.7f, px + P*3, py + Constants.PH * 0.8f, paint);
        canvas.drawRect(px + P*5, py + Constants.PH * 0.7f, px + P*6, py + Constants.PH * 0.8f, paint);
    }
}
