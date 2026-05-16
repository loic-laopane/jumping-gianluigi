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

    // Power-up
    public int   powerUp = PowerUp.NONE;
    public float powerUpTimer;

    // Death animation
    public float deadVy;
    public float deadTimer;

    // Walk animation (0 or 1, alternates)
    private float animTimer;
    private int   animFrame;

    // Star flicker
    private float starFlicker;

    public Player(float startX) {
        x        = startX;
        y        = Constants.GROUND_Y - Constants.PH;
        onGround = true;
        powerUp  = PowerUp.NONE;
    }

    public void update(float dt) {
        if (dead) {
            deadVy += Constants.GRAVITY * dt;
            y      += deadVy * dt;
            deadTimer -= dt;
            return;
        }

        vy += Constants.GRAVITY * dt;
        if (vy > Constants.MAX_FALL) vy = Constants.MAX_FALL;

        float spd = (powerUp == PowerUp.SPEED) ? Constants.PLAYER_SPEED * 1.6f : Constants.PLAYER_SPEED;
        x += spd * dt;
        y += vy * dt;

        if (powerUp != PowerUp.NONE) {
            powerUpTimer -= dt;
            if (powerUpTimer <= 0) powerUp = PowerUp.NONE;
        }

        // Walk animation: faster when speed power-up active
        float animSpeed = (powerUp == PowerUp.SPEED) ? 0.09f : 0.14f;
        if (onGround) {
            animTimer += dt;
            if (animTimer > animSpeed) { animTimer = 0; animFrame = 1 - animFrame; }
        }
        if (powerUp == PowerUp.STAR) starFlicker += dt;

        onGround = false; // reset each frame; collision sets it
    }

    public void tryJump() {
        if (dead) return;
        float jv  = (powerUp == PowerUp.SUPER_JUMP) ? Constants.JUMP_VEL * 1.28f : Constants.JUMP_VEL;
        float djv = (powerUp == PowerUp.SUPER_JUMP) ? Constants.DOUBLE_JUMP_VEL * 1.28f : Constants.DOUBLE_JUMP_VEL;

        if (onGround) {
            vy = jv;
            onGround      = false;
            hasDoubleJump = true;
        } else if (hasDoubleJump) {
            vy = djv;
            hasDoubleJump = false;
        }
    }

    public void land(float surfaceY) {
        y             = surfaceY - Constants.PH;
        vy            = 0;
        onGround      = true;
        hasDoubleJump = false;
    }

    public void die() {
        if (dead || powerUp == PowerUp.STAR) return;
        dead      = true;
        deadVy    = -480f;
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

    // ── Drawing ───────────────────────────────────────────────────────────

    public void draw(Canvas canvas, Paint paint) {
        float px = x;
        float py = y;

        // Star power aura (flicker)
        if (powerUp == PowerUp.STAR && (int)(starFlicker * 9) % 2 == 0) {
            paint.setColor(0x80FFFF00);
            canvas.drawRect(px - P*2, py - P*2, px + Constants.PW + P*2,
                            py + Constants.PH + P*2, paint);
        }

        // Determine swing direction from anim frame
        // When animFrame==0: left leg forward → right arm forward
        // When animFrame==1: right leg forward → left arm forward
        float swing = onGround ? (animFrame == 0 ? 1f : -1f) : 0.3f;

        // ── ARMS (drawn behind body) ───────────────────────────────────────
        // Left arm (opposite to right leg)
        float laOff = -swing * P * 1.5f;  // swings back when right leg forward
        paint.setColor(0xFF002395);  // sleeve
        canvas.drawRect(px - P*0.5f + laOff, py + P*5,
                        px + P*1.5f + laOff, py + P*6.5f, paint);
        paint.setColor(0xFFB87040);  // skin forearm
        canvas.drawRect(px - P*1f + laOff, py + P*6.5f,
                        px + P*1f  + laOff, py + P*8f, paint);

        // Right arm (opposite to left leg)
        float raOff = swing * P * 1.5f;   // swings forward when right leg forward
        paint.setColor(0xFF002395);  // sleeve
        canvas.drawRect(px + P*6.5f + raOff, py + P*5,
                        px + P*8.5f + raOff, py + P*6.5f, paint);
        paint.setColor(0xFFB87040);  // skin forearm
        canvas.drawRect(px + P*7f   + raOff, py + P*6.5f,
                        px + P*9f   + raOff, py + P*8f, paint);

        // ── JERSEY body ───────────────────────────────────────────────────
        paint.setColor(0xFF002395);
        canvas.drawRect(px + P, py + P*5, px + P*7, py + P*9, paint);

        // French collar: white then red stripe
        paint.setColor(Color.WHITE);
        canvas.drawRect(px + P*3,    py + P*5, px + P*4,    py + P*5.8f, paint);
        paint.setColor(0xFFEF4135);
        canvas.drawRect(px + P*4,    py + P*5, px + P*5,    py + P*5.8f, paint);
        paint.setColor(0xFF002395);
        canvas.drawRect(px + P*5,    py + P*5, px + P*6,    py + P*5.8f, paint);

        // Number "10" on chest (white pixel-art digits)
        paint.setColor(Color.WHITE);
        // "1": single vertical bar
        canvas.drawRect(px + P*1.5f, py + P*6.2f, px + P*2f,  py + P*8.5f, paint);
        // "0": hollow rectangle
        canvas.drawRect(px + P*2.4f, py + P*6.2f, px + P*3.7f, py + P*6.7f, paint); // top
        canvas.drawRect(px + P*2.4f, py + P*8f,   px + P*3.7f, py + P*8.5f, paint); // bottom
        canvas.drawRect(px + P*2.4f, py + P*6.2f, px + P*2.9f, py + P*8.5f, paint); // left
        canvas.drawRect(px + P*3.2f, py + P*6.2f, px + P*3.7f, py + P*8.5f, paint); // right

        // FFF badge (tiny gold star on right chest)
        paint.setColor(0xFFFFCC00);
        canvas.drawRect(px + P*5.5f, py + P*5.5f, px + P*6.5f, py + P*6.5f, paint);
        paint.setColor(0xFF002395);  // star shape cutout
        canvas.drawRect(px + P*5.5f, py + P*5.8f, px + P*5.9f, py + P*6.2f, paint);
        canvas.drawRect(px + P*6.1f, py + P*5.8f, px + P*6.5f, py + P*6.2f, paint);

        // ── HEAD ──────────────────────────────────────────────────────────
        // Hair (short curly crop, very dark)
        paint.setColor(0xFF0D0500);
        canvas.drawRect(px + P,    py,       px + P*7, py + P*2.5f, paint);
        canvas.drawRect(px + P*0.5f, py + P*0.5f, px + P*1.5f, py + P*2,   paint); // left ear hairline
        canvas.drawRect(px + P*6.5f, py + P*0.5f, px + P*7.5f, py + P*2,   paint); // right ear hairline
        // Hair texture (slightly lighter swirls)
        paint.setColor(0xFF1C0A02);
        canvas.drawRect(px + P*2,    py + P*0.5f, px + P*3,   py + P*1.2f, paint);
        canvas.drawRect(px + P*4,    py + P*0.3f, px + P*5,   py + P*1f,   paint);
        canvas.drawRect(px + P*5.8f, py + P*0.5f, px + P*6.8f, py + P*1.2f, paint);

        // Face (dark warm brown – Mbappé skin tone)
        paint.setColor(0xFFB07838);
        canvas.drawRect(px + P*1.5f, py + P*2f,  px + P*6.5f, py + P*5.2f, paint);

        // Ears
        paint.setColor(0xFFA06828);
        canvas.drawRect(px + P,      py + P*2.5f, px + P*1.5f, py + P*3.8f, paint);
        canvas.drawRect(px + P*6.5f, py + P*2.5f, px + P*7,    py + P*3.8f, paint);

        // Eyebrows (thick, expressive)
        paint.setColor(0xFF1A0A00);
        canvas.drawRect(px + P*1.8f, py + P*2.3f, px + P*3.5f, py + P*2.8f, paint);
        canvas.drawRect(px + P*4.5f, py + P*2.3f, px + P*6.2f, py + P*2.8f, paint);

        // Eyes – white sclera
        paint.setColor(Color.WHITE);
        canvas.drawRect(px + P*1.8f, py + P*2.8f, px + P*3.5f, py + P*3.7f, paint);
        canvas.drawRect(px + P*4.5f, py + P*2.8f, px + P*6.2f, py + P*3.7f, paint);
        // Dark iris
        paint.setColor(0xFF2C1200);
        canvas.drawRect(px + P*2.3f, py + P*2.8f, px + P*3.1f, py + P*3.7f, paint);
        canvas.drawRect(px + P*4.9f, py + P*2.8f, px + P*5.7f, py + P*3.7f, paint);
        // Eye highlight
        paint.setColor(Color.WHITE);
        canvas.drawRect(px + P*2.3f, py + P*2.8f, px + P*2.7f, py + P*3.2f, paint);
        canvas.drawRect(px + P*4.9f, py + P*2.8f, px + P*5.3f, py + P*3.2f, paint);

        // Nose (broad, prominent)
        paint.setColor(0xFF8C5C28);
        canvas.drawRect(px + P*3,    py + P*3.7f, px + P*5,   py + P*4.2f, paint);
        canvas.drawRect(px + P*2.3f, py + P*3.9f, px + P*3.2f, py + P*4.5f, paint);
        canvas.drawRect(px + P*4.8f, py + P*3.9f, px + P*5.7f, py + P*4.5f, paint);

        // Mouth – wide Mbappé smile
        paint.setColor(0xFF5C2800);
        canvas.drawRect(px + P*2.3f, py + P*4.5f, px + P*5.7f, py + P*4.9f, paint);
        // Teeth
        paint.setColor(Color.WHITE);
        canvas.drawRect(px + P*2.7f, py + P*4.6f, px + P*5.3f, py + P*4.9f, paint);
        // Smile corner dimples
        paint.setColor(0xFF8C5C28);
        canvas.drawRect(px + P*2,    py + P*4.3f, px + P*2.5f, py + P*4.8f, paint);
        canvas.drawRect(px + P*5.5f, py + P*4.3f, px + P*6,    py + P*4.8f, paint);

        // ── SHORTS ────────────────────────────────────────────────────────
        paint.setColor(0xFF001166);
        canvas.drawRect(px + P, py + P*9, px + P*7, py + P*10.5f, paint);
        // Shorts stripe
        paint.setColor(Color.WHITE);
        canvas.drawRect(px + P, py + P*9, px + P*7, py + P*9.3f, paint);

        // ── LEGS (animated) ───────────────────────────────────────────────
        if (animFrame == 0 || !onGround) {
            // Left leg forward, right leg back
            paint.setColor(0xFFB07838); // shin skin
            canvas.drawRect(px + P*0.5f, py + P*10.5f, px + P*3,    py + P*11.8f, paint);
            canvas.drawRect(px + P*4,    py + P*10f,   px + P*6.5f, py + P*11f,   paint);
            // Left shoe (forward)
            paint.setColor(0xFF111111);
            canvas.drawRect(px - P*0.5f, py + P*11.5f, px + P*3.5f, py + P*12.3f, paint);
            // Right shoe (back)
            canvas.drawRect(px + P*3.5f, py + P*10.7f, px + P*6.5f, py + P*11.5f, paint);
            // Soccer ball near left (leading) foot
            drawBall(canvas, paint, px - P, py + P*11.8f);
        } else {
            // Right leg forward, left leg back
            paint.setColor(0xFFB07838);
            canvas.drawRect(px + P*4,    py + P*10.5f, px + P*7,    py + P*11.8f, paint);
            canvas.drawRect(px + P*0.5f, py + P*10f,   px + P*3,    py + P*11f,   paint);
            // Right shoe (forward)
            paint.setColor(0xFF111111);
            canvas.drawRect(px + P*3.5f, py + P*11.5f, px + P*7.5f, py + P*12.3f, paint);
            // Left shoe (back)
            canvas.drawRect(px + P*0.5f, py + P*10.7f, px + P*3.5f, py + P*11.5f, paint);
            // Soccer ball near right (leading) foot
            drawBall(canvas, paint, px + P*6, py + P*11.8f);
        }
    }

    /** Pixel-art soccer ball (2.5×2.5 game pixels). */
    private static void drawBall(Canvas canvas, Paint paint, float bx, float by) {
        // White base
        paint.setColor(Color.WHITE);
        canvas.drawRect(bx, by, bx + P*2.5f, by + P*2.5f, paint);
        // Black pentagon patches
        paint.setColor(Color.BLACK);
        canvas.drawRect(bx + P*0.8f, by,          bx + P*1.7f, by + P*0.7f, paint);  // top
        canvas.drawRect(bx,          by + P*0.8f,  bx + P*0.7f, by + P*1.7f, paint);  // left
        canvas.drawRect(bx + P*1.8f, by + P*0.8f,  bx + P*2.5f, by + P*1.7f, paint);  // right
        canvas.drawRect(bx + P*0.5f, by + P*1.8f,  bx + P*2f,   by + P*2.5f, paint);  // bottom
        canvas.drawRect(bx + P,      by + P,        bx + P*1.5f, by + P*1.5f, paint);  // center
    }

    /** Dead player: upside-down with X eyes. */
    public void drawDead(Canvas canvas, Paint paint) {
        float px = x;
        float py = y;
        paint.setColor(0xFF002395);
        canvas.drawRect(px, py, px + Constants.PW, py + Constants.PH * 0.6f, paint);
        paint.setColor(0xFFB07838);
        canvas.drawRect(px + P*2, py + Constants.PH * 0.6f, px + P*6, py + Constants.PH, paint);
        // X eyes
        paint.setColor(Color.WHITE);
        canvas.drawRect(px + P*1.5f, py + Constants.PH * 0.65f, px + P*3.5f, py + Constants.PH * 0.75f, paint);
        canvas.drawRect(px + P*4.5f, py + Constants.PH * 0.65f, px + P*6.5f, py + Constants.PH * 0.75f, paint);
    }
}
