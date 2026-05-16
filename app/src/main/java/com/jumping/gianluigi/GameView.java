package com.jumping.gianluigi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jumping.gianluigi.effects.Particle;
import com.jumping.gianluigi.entities.Enemy;
import com.jumping.gianluigi.entities.Player;
import com.jumping.gianluigi.entities.PowerUp;
import com.jumping.gianluigi.world.Level;
import com.jumping.gianluigi.world.LevelFactory;
import com.jumping.gianluigi.world.Platform;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    // ── Game states ────────────────────────────────────────────────────────
    private enum State { MENU, PLAYING, DEAD, LEVEL_WIN, GAME_OVER }

    // ── Constants ──────────────────────────────────────────────────────────
    private static final int   TARGET_FPS  = 60;
    private static final long  FRAME_MS    = 1000 / TARGET_FPS;
    private static final float CAMERA_LEAD = Constants.VW * 0.30f;

    // ── Game thread ────────────────────────────────────────────────────────
    private GameThread thread;
    private volatile boolean running;

    // ── Rendering ──────────────────────────────────────────────────────────
    private final Paint  paint  = new Paint();
    private final Matrix matrix = new Matrix();
    private float scaleX, scaleY, scale, offsetX, offsetY;

    // ── Game state ─────────────────────────────────────────────────────────
    private State  state;
    private int    lives;
    private int    score;
    private int    currentLevel;

    // ── World ──────────────────────────────────────────────────────────────
    private Level         level;
    private Player        player;
    private List<Enemy>   enemies;
    private List<PowerUp> powerUps;
    private List<Particle> particles;

    // ── Camera ─────────────────────────────────────────────────────────────
    private float cameraX;

    // ── Transition timer ───────────────────────────────────────────────────
    private float stateTimer;

    // ── Background sky gradient (cached) ──────────────────────────────────
    private Paint skyPaint;

    // ── Cloud positions (decorative) ───────────────────────────────────────
    private float[] cloudX, cloudY;

    public GameView(Context ctx) {
        super(ctx);
        getHolder().addCallback(this);
        setFocusable(true);
        paint.setAntiAlias(false);  // crisp pixel art
        initClouds();
        loadMenu();
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        updateScale();
        running = true;
        thread = new GameThread();
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int fmt, int w, int h2) {
        updateScale();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        try { thread.join(500); } catch (InterruptedException ignored) {}
    }

    public void pause()  { running = false; }
    public void resume() {
        if (!running) { running = true; thread = new GameThread(); thread.start(); }
    }

    // ── Input ──────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            handleTap();
        }
        return true;
    }

    private void handleTap() {
        switch (state) {
            case MENU:
                startGame(1);
                break;
            case PLAYING:
                player.tryJump();
                break;
            case DEAD:
                if (stateTimer <= 0) {
                    if (lives > 0) startGame(currentLevel);
                    else           loadMenu();
                }
                break;
            case LEVEL_WIN:
                if (stateTimer <= 0) startGame(currentLevel + 1);
                break;
            case GAME_OVER:
                loadMenu();
                break;
        }
    }

    // ── Initialization ─────────────────────────────────────────────────────

    private void loadMenu() {
        state = State.MENU;
        lives = Constants.START_LIVES;
        score = 0;
        currentLevel = 1;
        player   = new Player(Constants.VW * 0.3f);
        enemies  = new ArrayList<>();
        powerUps = new ArrayList<>();
        particles= new ArrayList<>();
        cameraX  = 0;
        level    = LevelFactory.create(1);
    }

    private void startGame(int lvlNum) {
        currentLevel = lvlNum;
        level  = LevelFactory.create(lvlNum);
        player = new Player(120f);
        cameraX = 0;
        state  = State.PLAYING;
        stateTimer = 0;

        // Spawn enemies from level data
        enemies = new ArrayList<>();
        for (float[] e : level.enemies) {
            Enemy en = new Enemy(e[0], e[1], e[2]);
            en.speed = 65f + (lvlNum - 1) * 10f;
            enemies.add(en);
        }

        // Spawn power-ups
        powerUps = new ArrayList<>();
        for (float[] p : level.powerUps) {
            powerUps.add(new PowerUp(p[0], p[1], (int) p[2]));
        }

        particles = new ArrayList<>();
    }

    // ── Game loop ──────────────────────────────────────────────────────────

    private long lastTime = System.currentTimeMillis();

    private void update() {
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastTime) / 1000f, 0.05f);
        lastTime = now;

        switch (state) {
            case MENU:
                updateMenu(dt);
                break;
            case PLAYING:
                updatePlaying(dt);
                break;
            case DEAD:
                updateDead(dt);
                break;
            case LEVEL_WIN:
                updateLevelWin(dt);
                break;
        }
    }

    private void updateMenu(float dt) {
        // Animate demo player
        player.update(dt);
        if (player.y > Constants.GROUND_Y - Constants.PH)
            player.land(Constants.GROUND_Y);
        scrollClouds(dt);
    }

    private void updatePlaying(float dt) {
        player.update(dt);
        resolvePlayerCollisions();
        checkFall();

        for (Enemy en : enemies) en.update(dt);
        for (PowerUp pu : powerUps) pu.update(dt);

        checkEnemyCollisions();
        checkPowerUpCollisions();
        checkFinish();

        Iterator<Particle> pit = particles.iterator();
        while (pit.hasNext()) {
            Particle p = pit.next();
            p.update(dt);
            if (p.isDead()) pit.remove();
        }

        // Camera: follow player with lead
        float targetCam = player.x - CAMERA_LEAD;
        cameraX = Math.max(0, Math.min(targetCam, level.width - Constants.VW));

        scrollClouds(dt);
    }

    private void updateDead(float dt) {
        player.update(dt);
        stateTimer -= dt;
        if (stateTimer < 0) stateTimer = 0;
        scrollClouds(dt);
    }

    private void updateLevelWin(float dt) {
        stateTimer -= dt;
        if (stateTimer < 0) stateTimer = 0;
        scrollClouds(dt);
    }

    // ── Collision detection ────────────────────────────────────────────────

    private void resolvePlayerCollisions() {
        float pL = player.x;
        float pR = player.x + Constants.PW;
        float pB = player.y + Constants.PH;

        // ── Ground segments ──
        for (float[] seg : level.groundSegs) {
            if (pR > seg[0] && pL < seg[1]) {
                // Within x range of segment
                if (player.vy >= 0 && pB >= Constants.GROUND_Y && player.y < Constants.GROUND_Y) {
                    player.land(Constants.GROUND_Y);
                    break;
                }
            }
        }

        // ── Platforms ──
        for (Platform plat : level.platforms) {
            if (pR > plat.x && pL < plat.right()) {
                float prevBottom = player.y + Constants.PH - player.vy * 0.016f; // approx prev frame
                if (player.vy >= 0 && pB >= plat.y && prevBottom <= plat.y + 4) {
                    player.land(plat.y);
                    break;
                }
            }
        }
    }

    private void checkFall() {
        if (player.y > Constants.VH + 50) {
            killPlayer();
        }
    }

    private void checkEnemyCollisions() {
        if (player.dead) return;
        RectF pb = player.getBounds();

        for (Enemy en : enemies) {
            if (!en.alive) continue;
            RectF eb = en.getBounds();
            if (!RectF.intersects(pb, eb)) continue;

            // Stomp: player bottom hits enemy top from above
            float overlapTop = en.y + Constants.EH * 0.3f;
            if (player.vy > 0 && pb.bottom <= overlapTop + 16 && pb.top < en.y) {
                // Stomp enemy
                stompEnemy(en);
                player.vy = Constants.JUMP_VEL * 0.55f; // bounce
            } else if (!player.isInvincible()) {
                killPlayer();
            }
        }
    }

    private void stompEnemy(Enemy en) {
        en.stomp();
        score += 100;
        spawnExplosion(en.x + Constants.EW / 2, en.y + Constants.EH / 2);
    }

    private void checkPowerUpCollisions() {
        RectF pb = player.getBounds();
        for (PowerUp pu : powerUps) {
            if (pu.collected) continue;
            if (RectF.intersects(pb, pu.getBounds())) {
                pu.collected = true;
                player.collectPowerUp(pu.type);
                score += 200;
            }
        }
    }

    private void checkFinish() {
        if (player.x + Constants.PW >= level.finishX) {
            score += 500 + currentLevel * 100;
            state = State.LEVEL_WIN;
            stateTimer = 2.5f;
        }
    }

    private void killPlayer() {
        if (player.dead) return;
        player.die();
        lives--;
        state = State.DEAD;
        stateTimer = 2.0f;
        if (lives <= 0) state = State.GAME_OVER;
    }

    // ── Particle explosion ────────────────────────────────────────────────

    private void spawnExplosion(float cx, float cy) {
        Random rng = new Random();
        int[] colors = {0xFFFF4400, 0xFFFF8800, 0xFFFFDD00, 0xFFFF2200, 0xFFFFFFFF};
        for (int i = 0; i < 18; i++) {
            float angle = (float)(Math.PI * 2 * i / 18);
            float spd   = 150 + rng.nextFloat() * 250;
            float vx    = (float) Math.cos(angle) * spd;
            float vy    = (float) Math.sin(angle) * spd - 100;
            int   col   = colors[rng.nextInt(colors.length)];
            float sz    = 4f + rng.nextFloat() * 8f;
            particles.add(new Particle(cx, cy, vx, vy, 0.6f + rng.nextFloat() * 0.4f, col, sz));
        }
    }

    // ── Clouds ────────────────────────────────────────────────────────────

    private void initClouds() {
        cloudX = new float[]{100, 400, 650, 200, 800};
        cloudY = new float[]{60,  40,  80,  100, 50};
    }

    private float cloudScroll;
    private void scrollClouds(float dt) {
        cloudScroll += dt * 20f;
        if (cloudScroll > Constants.VW) cloudScroll = 0;
    }

    // ── Scale ──────────────────────────────────────────────────────────────

    private void updateScale() {
        int sw = getWidth();
        int sh = getHeight();
        if (sw == 0 || sh == 0) return;
        scaleX = (float) sw / Constants.VW;
        scaleY = (float) sh / Constants.VH;
        scale  = Math.min(scaleX, scaleY);
        offsetX = (sw - Constants.VW * scale) / 2f;
        offsetY = (sh - Constants.VH * scale) / 2f;
        buildSky(sh);
    }

    private void buildSky(int sh) {
        skyPaint = new Paint();
        LinearGradient grad = new LinearGradient(
                0, 0, 0, sh,
                new int[]{0xFF4FC3F7, 0xFF81D4FA, 0xFFB3E5FC},
                null, Shader.TileMode.CLAMP);
        skyPaint.setShader(grad);
    }

    // ── Rendering ─────────────────────────────────────────────────────────

    private void render(Canvas canvas) {
        int sw = getWidth();
        int sh = getHeight();

        // Sky background (full screen, no matrix)
        if (skyPaint != null) canvas.drawRect(0, 0, sw, sh, skyPaint);
        else { paint.setColor(0xFF4FC3F7); canvas.drawRect(0, 0, sw, sh, paint); }

        // Apply view matrix (scale + camera offset)
        matrix.reset();
        matrix.setScale(scale, scale);
        matrix.postTranslate(offsetX - cameraX * scale, offsetY);
        canvas.save();
        canvas.setMatrix(matrix);

        drawClouds(canvas);
        drawWorld(canvas);
        drawEntities(canvas);
        drawParticles(canvas);
        drawFinishFlag(canvas);

        canvas.restore();

        // HUD (no matrix – screen space)
        drawHUD(canvas, sw, sh);

        // Overlay messages
        drawOverlay(canvas, sw, sh);
    }

    private void drawClouds(Canvas canvas) {
        paint.setColor(0xCCFFFFFF);
        for (int i = 0; i < cloudX.length; i++) {
            float cx = ((cloudX[i] + cloudScroll + cameraX * 0.2f) % (Constants.VW + 200)) - 100;
            float cy = cloudY[i];
            // Blocky pixel cloud
            canvas.drawRect(cx,        cy + 8,  cx + 60,  cy + 24, paint);
            canvas.drawRect(cx + 12,   cy,      cx + 48,  cy + 16, paint);
            canvas.drawRect(cx + 8,    cy + 4,  cx + 52,  cy + 20, paint);
        }
    }

    private void drawWorld(Canvas canvas) {
        // Ground segments
        paint.setColor(0xFF4CAF50);
        for (float[] seg : level.groundSegs) {
            // Grass top strip
            canvas.drawRect(seg[0], Constants.GROUND_Y, seg[1], Constants.GROUND_Y + 8, paint);
        }
        paint.setColor(0xFF795548);
        for (float[] seg : level.groundSegs) {
            // Dirt body
            canvas.drawRect(seg[0], Constants.GROUND_Y + 8, seg[1], Constants.VH + 20, paint);
        }
        // Dirt pixel detail lines
        paint.setColor(0xFF6D4C41);
        for (float[] seg : level.groundSegs) {
            for (float bx = seg[0]; bx < seg[1]; bx += 32) {
                canvas.drawRect(bx, Constants.GROUND_Y + 12, bx + 28, Constants.GROUND_Y + 14, paint);
                canvas.drawRect(bx + 4, Constants.GROUND_Y + 20, bx + 30, Constants.GROUND_Y + 22, paint);
            }
        }

        // Platforms
        for (Platform plat : level.platforms) {
            // Top (grass green)
            paint.setColor(0xFF4CAF50);
            canvas.drawRect(plat.x, plat.y, plat.right(), plat.y + 4, paint);
            // Body (wood brown)
            paint.setColor(0xFF8D6E63);
            canvas.drawRect(plat.x, plat.y + 4, plat.right(), plat.bottom(), paint);
            // Edge pixels
            paint.setColor(0xFF5D4037);
            canvas.drawRect(plat.x, plat.y + 4, plat.x + 4, plat.bottom(), paint);
            canvas.drawRect(plat.right() - 4, plat.y + 4, plat.right(), plat.bottom(), paint);
        }
    }

    private void drawEntities(Canvas canvas) {
        // Power-ups
        for (PowerUp pu : powerUps) {
            if (!pu.collected) pu.draw(canvas, paint);
        }

        // Enemies
        for (Enemy en : enemies) {
            en.draw(canvas, paint);
        }

        // Player
        if (player.dead) {
            player.drawDead(canvas, paint);
        } else {
            player.draw(canvas, paint);
        }
    }

    private void drawParticles(Canvas canvas) {
        for (Particle p : particles) {
            p.draw(canvas, paint);
        }
    }

    private void drawFinishFlag(Canvas canvas) {
        float fx = level.finishX;
        float gy = Constants.GROUND_Y;
        // Pole
        paint.setColor(0xFFCCCCCC);
        canvas.drawRect(fx, gy - 120, fx + 6, gy, paint);
        // Flag (animated)
        paint.setColor(0xFFFF3333);
        canvas.drawRect(fx + 6, gy - 120, fx + 46, gy - 100, paint);
        paint.setColor(0xFFFFFFFF);
        canvas.drawRect(fx + 6, gy - 112, fx + 46, gy - 108, paint);
    }

    // ── HUD ───────────────────────────────────────────────────────────────

    private void drawHUD(Canvas canvas, int sw, int sh) {
        float hudScale = scale * 1.1f;
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextSize(18 * hudScale);
        paint.setAntiAlias(true);

        // Lives (hearts)
        float hx = offsetX + 10 * scale;
        float hy = offsetY + 22 * scale;
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(2 * scale, 0, 1 * scale, Color.BLACK);
        canvas.drawText("❤ x" + lives, hx, hy, paint);

        // Score
        String scoreStr = "SCORE " + String.format("%06d", score);
        paint.setTextSize(14 * hudScale);
        canvas.drawText(scoreStr, hx, hy + 22 * scale, paint);

        // Level
        String lvlStr = "LVL " + currentLevel;
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(14 * hudScale);
        canvas.drawText(lvlStr, offsetX + Constants.VW * scale - 10 * scale, hy, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // Power-up indicator
        if (state == State.PLAYING && player.powerUp != PowerUp.NONE) {
            String pname = (player.powerUp == PowerUp.STAR) ? "STAR"
                    : (player.powerUp == PowerUp.SPEED) ? "VITESSE"
                    : "SUPER SAUT";
            paint.setColor(0xFFFFDD00);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(12 * hudScale);
            canvas.drawText("★ " + pname + " ★", sw / 2f, hy, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        paint.setShadowLayer(0, 0, 0, 0);
        paint.setAntiAlias(false);
    }

    // ── Overlay (menu / gameover / win) ───────────────────────────────────

    private void drawOverlay(Canvas canvas, int sw, int sh) {
        float cx = sw / 2f;
        float cy = sh / 2f;
        paint.setAntiAlias(true);

        switch (state) {
            case MENU:
                drawOverlayBox(canvas, cx, cy, sw, sh);
                paint.setColor(0xFFFFDD00);
                paint.setTextSize(scale * 36);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                canvas.drawText("JUMPING GIANLUIGI", cx, cy - scale * 40, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(scale * 18);
                canvas.drawText("Tap pour commencer !", cx, cy + scale * 10, paint);
                paint.setTextSize(scale * 13);
                canvas.drawText("TAP = saut    TAP TAP = double saut", cx, cy + scale * 40, paint);
                canvas.drawText("Saute SUR les ennemis pour les tuer !", cx, cy + scale * 58, paint);
                break;

            case DEAD:
                drawOverlayBox(canvas, cx, cy, sw, sh);
                paint.setColor(0xFFFF4444);
                paint.setTextSize(scale * 32);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                canvas.drawText("RATÉ !", cx, cy - scale * 20, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(scale * 16);
                canvas.drawText("Vies restantes : " + lives, cx, cy + scale * 15, paint);
                if (stateTimer <= 0) {
                    paint.setColor(0xFFFFDD00);
                    canvas.drawText("Tap pour réessayer", cx, cy + scale * 40, paint);
                }
                break;

            case GAME_OVER:
                drawOverlayBox(canvas, cx, cy, sw, sh);
                paint.setColor(0xFFFF2222);
                paint.setTextSize(scale * 36);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                canvas.drawText("GAME OVER", cx, cy - scale * 30, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(scale * 18);
                canvas.drawText("Score final : " + score, cx, cy + scale * 10, paint);
                paint.setColor(0xFFFFDD00);
                paint.setTextSize(scale * 14);
                canvas.drawText("Tap pour recommencer", cx, cy + scale * 40, paint);
                break;

            case LEVEL_WIN:
                drawOverlayBox(canvas, cx, cy, sw, sh);
                paint.setColor(0xFF88FF44);
                paint.setTextSize(scale * 32);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                canvas.drawText("NIVEAU " + currentLevel + " TERMINÉ !", cx, cy - scale * 25, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(scale * 16);
                canvas.drawText("Score : " + score, cx, cy + scale * 10, paint);
                if (stateTimer <= 0) {
                    paint.setColor(0xFFFFDD00);
                    paint.setTextSize(scale * 14);
                    canvas.drawText("Tap pour le niveau suivant", cx, cy + scale * 38, paint);
                }
                break;
        }

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setAntiAlias(false);
    }

    private void drawOverlayBox(Canvas canvas, float cx, float cy, int sw, int sh) {
        paint.setColor(0xCC000022);
        canvas.drawRect(cx - sw * 0.4f, cy - sh * 0.22f, cx + sw * 0.4f, cy + sh * 0.22f, paint);
        paint.setColor(0xFF3344AA);
        canvas.drawRect(cx - sw * 0.4f, cy - sh * 0.22f, cx + sw * 0.4f, cy - sh * 0.20f, paint);
    }

    // ── Game Thread ────────────────────────────────────────────────────────

    private class GameThread extends Thread {
        @Override
        public void run() {
            while (running) {
                long t0 = System.currentTimeMillis();
                update();

                SurfaceHolder holder = getHolder();
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        synchronized (holder) { render(canvas); }
                    }
                } finally {
                    if (canvas != null) holder.unlockCanvasAndPost(canvas);
                }

                long elapsed = System.currentTimeMillis() - t0;
                long sleep   = FRAME_MS - elapsed;
                if (sleep > 0) {
                    try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
                }
            }
        }
    }
}
