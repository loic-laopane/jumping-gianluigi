package com.jumping.gianluigi.entities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.jumping.gianluigi.Constants;
import static com.jumping.gianluigi.Constants.P;

public class Enemy {
    public float x, y;         // top-left corner (screen coords)
    public float vy;
    public boolean alive;
    public boolean stomped;     // in squish death animation
    public float stompTimer;
    public float patrolLeft, patrolRight;
    public int dir;             // 1=right, -1=left
    public float speed;

    // Wobble animation
    private float animTimer;
    private int animFrame;

    public Enemy(float x, float patrolLeft, float patrolRight) {
        this.x = x;
        this.y = Constants.GROUND_Y - Constants.EH;
        this.alive = true;
        this.stomped = false;
        this.patrolLeft  = patrolLeft;
        this.patrolRight = patrolRight;
        this.dir = -1;  // walks left by default (toward player coming from left)
        this.speed = 65f;
    }

    public void update(float dt) {
        if (stomped) {
            stompTimer -= dt;
            vy += Constants.GRAVITY * dt * 0.5f;
            y += vy * dt;
            return;
        }

        x += dir * speed * dt;

        if (x <= patrolLeft) {
            x = patrolLeft;
            dir = 1;
        } else if (x + Constants.EW >= patrolRight) {
            x = patrolRight - Constants.EW;
            dir = -1;
        }

        animTimer += dt;
        if (animTimer > 0.2f) { animTimer = 0; animFrame = 1 - animFrame; }
    }

    /** Call when player stomps on top. Spawns particles via callback. */
    public void stomp() {
        stomped = true;
        alive   = false;
        vy      = -300f;
        stompTimer = 0.4f;
    }

    /** Bounding box used for collision. */
    public RectF getBounds() {
        return new RectF(x, y, x + Constants.EW, y + Constants.EH);
    }

    public void draw(Canvas canvas, Paint paint) {
        if (stomped) {
            // Flattened squish
            paint.setColor(0xFFAA3300);
            canvas.drawRect(x, y + P*6, x + Constants.EW, y + Constants.EH, paint);
            paint.setColor(0xFF771100);
            canvas.drawRect(x + P, y + P*6.5f, x + Constants.EW - P, y + P*7.5f, paint);
            return;
        }

        float px = x;
        float py = y;

        // Feet (alternating wobble)
        paint.setColor(0xFF331100);
        if (animFrame == 0) {
            canvas.drawRect(px,        py + P*7, px + P*3, py + P*8, paint);
            canvas.drawRect(px + P*5,  py + P*7, px + P*8, py + P*8, paint);
        } else {
            canvas.drawRect(px + P,    py + P*7, px + P*3, py + P*8, paint);
            canvas.drawRect(px + P*5,  py + P*7, px + P*7, py + P*8, paint);
        }

        // Body
        paint.setColor(0xFF993300);
        canvas.drawRect(px + P, py + P*3, px + P*7, py + P*7, paint);

        // Head
        paint.setColor(0xFFCC4400);
        canvas.drawRect(px, py, px + P*8, py + P*4, paint);

        // Eyes – angry
        paint.setColor(Color.WHITE);
        canvas.drawRect(px + P,       py + P, px + P*3, py + P*3, paint);
        canvas.drawRect(px + P*5,     py + P, px + P*7, py + P*3, paint);
        paint.setColor(Color.BLACK);
        canvas.drawRect(px + P*1.5f,  py + P*1.5f, px + P*2.5f, py + P*2.5f, paint);
        canvas.drawRect(px + P*5.5f,  py + P*1.5f, px + P*6.5f, py + P*2.5f, paint);

        // Angry brows
        paint.setColor(Color.BLACK);
        canvas.drawRect(px + P,   py + P*0.5f, px + P*3,  py + P,    paint);
        canvas.drawRect(px + P*5, py + P*0.5f, px + P*7,  py + P,    paint);

        // Mouth (angry line)
        paint.setColor(0xFF220000);
        canvas.drawRect(px + P*2, py + P*3, px + P*6, py + P*3.5f, paint);
    }
}
