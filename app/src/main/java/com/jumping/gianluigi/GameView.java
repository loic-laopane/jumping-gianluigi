package com.jumping.gianluigi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
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
import com.jumping.gianluigi.sound.SoundManager;
import com.jumping.gianluigi.world.Level;
import com.jumping.gianluigi.world.LevelFactory;
import com.jumping.gianluigi.world.Platform;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    // ── States ────────────────────────────────────────────────────────────
    private enum State { MENU, PLAYING, DEAD, LEVEL_WIN, GAME_OVER }

    private static final long  FRAME_MS    = 1000 / 60;
    private static final float CAMERA_LEAD = Constants.VW * 0.30f;

    // ── Thread ────────────────────────────────────────────────────────────
    private GameThread thread;
    private volatile boolean running;

    // ── Render helpers ────────────────────────────────────────────────────
    private final Paint  paint  = new Paint();
    private final Matrix matrix = new Matrix();
    private float scale, offsetX, offsetY;

    // ── Game state ────────────────────────────────────────────────────────
    private State  state;
    private int    lives, score, currentLevel;

    // ── World ─────────────────────────────────────────────────────────────
    private Level         level;
    private Player        player;
    private List<Enemy>   enemies;
    private List<PowerUp> powerUps;
    private List<Particle> particles;

    // ── Camera ────────────────────────────────────────────────────────────
    private float cameraX;

    // ── Timers ────────────────────────────────────────────────────────────
    private float stateTimer;
    private long  lastTime = System.currentTimeMillis();

    // ── Sky gradient (cached) ─────────────────────────────────────────────
    private Paint skyPaint;

    // ── Decorative background ─────────────────────────────────────────────
    private float[] cloudX  = {80, 350, 620, 180, 750};
    private float[] cloudY  = {55,  35,  75,  95,  45};
    private float   cloudScroll;
    private float[] mountainX = {0, 220, 440, 660, 880, 1100};

    // ── Mute button ──────────────────────────────────────────────────────
    private RectF muteRect;

    public GameView(Context ctx) {
        super(ctx);
        getHolder().addCallback(this);
        setFocusable(true);
        paint.setAntiAlias(false);
        loadMenu();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override public void surfaceCreated(SurfaceHolder h) { updateScale(); startThread(); }
    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int h2) { updateScale(); }
    @Override public void surfaceDestroyed(SurfaceHolder h) { stopThread(); }

    public void pause()  { stopThread(); }
    public void resume() { if (!running) startThread(); }

    private void startThread() {
        running = true;
        lastTime = System.currentTimeMillis();
        thread = new GameThread();
        thread.start();
    }

    private void stopThread() {
        running = false;
        try { if (thread != null) thread.join(500); } catch (InterruptedException ignored) {}
    }

    // ── Input ─────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_DOWN) return true;

        // Mute button (screen coords, top-right)
        if (muteRect != null && muteRect.contains(ev.getX(), ev.getY())) {
            SoundManager.setMuted(!SoundManager.isMuted());
            return true;
        }

        switch (state) {
            case MENU:       startGame(1);           break;
            case PLAYING:    handleJump();            break;
            case DEAD:
                if (stateTimer <= 0) {
                    if (lives > 0) startGame(currentLevel);
                    else           loadMenu();
                }
                break;
            case LEVEL_WIN:
                if (stateTimer <= 0) startGame(currentLevel + 1);
                break;
            case GAME_OVER:  loadMenu();              break;
        }
        return true;
    }

    private void handleJump() {
        boolean wasOnGround  = player.onGround;
        boolean hadDoubleJump = player.hasDoubleJump;
        player.tryJump();
        if (player.onGround != wasOnGround || !player.onGround) {
            if (!hadDoubleJump && player.hasDoubleJump) {
                SoundManager.playJump();
            } else if (!player.hasDoubleJump && hadDoubleJump) {
                SoundManager.playDoubleJump();
            } else {
                SoundManager.playJump();
            }
        }
    }

    // ── Init ──────────────────────────────────────────────────────────────

    private void loadMenu() {
        state = State.MENU; lives = Constants.START_LIVES; score = 0; currentLevel = 1;
        player    = new Player(Constants.VW * 0.3f);
        enemies   = new ArrayList<>(); powerUps = new ArrayList<>(); particles = new ArrayList<>();
        cameraX   = 0;
        level     = LevelFactory.create(1);
    }

    private void startGame(int n) {
        currentLevel = n;
        level    = LevelFactory.create(n);
        player   = new Player(120f);
        cameraX  = 0;
        state    = State.PLAYING;
        stateTimer = 0;

        enemies = new ArrayList<>();
        for (float[] e : level.enemies) {
            Enemy en = new Enemy(e[0], e[1], e[2]);
            en.speed = 55f + (n - 1) * 14f;
            enemies.add(en);
        }
        powerUps  = new ArrayList<>();
        for (float[] p : level.powerUps)
            powerUps.add(new PowerUp(p[0], p[1], (int) p[2]));
        particles = new ArrayList<>();
    }

    // ── Game loop ─────────────────────────────────────────────────────────

    private void update() {
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastTime) / 1000f, 0.05f);
        lastTime = now;

        switch (state) {
            case MENU:      updateMenu(dt);    break;
            case PLAYING:   updatePlaying(dt); break;
            case DEAD:      updateDead(dt);    break;
            case LEVEL_WIN: stateTimer -= dt; if (stateTimer < 0) stateTimer = 0; scrollClouds(dt); break;
            case GAME_OVER: scrollClouds(dt);  break;
        }
    }

    private void updateMenu(float dt) {
        player.update(dt);
        if (player.y > Constants.GROUND_Y - Constants.PH) player.land(Constants.GROUND_Y);
        scrollClouds(dt);
    }

    private void updatePlaying(float dt) {
        player.update(dt);
        resolveCollisions();
        checkFall();

        for (Enemy en : enemies) en.update(dt);
        for (PowerUp pu : powerUps) pu.update(dt);

        checkEnemyCollisions();
        checkPowerUpCollisions();
        checkFinish();

        Iterator<Particle> pit = particles.iterator();
        while (pit.hasNext()) { Particle p = pit.next(); p.update(dt); if (p.isDead()) pit.remove(); }

        float target = player.x - CAMERA_LEAD;
        cameraX = Math.max(0, Math.min(target, level.width - Constants.VW));

        scrollClouds(dt);
    }

    private void updateDead(float dt) {
        player.update(dt);
        stateTimer -= dt;
        if (stateTimer < 0) stateTimer = 0;
        scrollClouds(dt);
    }

    // ── Collision ─────────────────────────────────────────────────────────

    private void resolveCollisions() {
        float pL = player.x, pR = player.x + Constants.PW, pB = player.y + Constants.PH;

        for (float[] seg : level.groundSegs) {
            if (pR > seg[0] && pL < seg[1] && player.vy >= 0
                    && pB >= Constants.GROUND_Y && player.y < Constants.GROUND_Y) {
                player.land(Constants.GROUND_Y);
                return;
            }
        }
        for (Platform pl : level.platforms) {
            if (pR > pl.x && pL < pl.right() && player.vy >= 0
                    && pB >= pl.y && pB <= pl.y + Constants.PLATFORM_H + player.vy * 0.018f) {
                player.land(pl.y);
                return;
            }
        }
    }

    private void checkFall() {
        if (player.y > Constants.VH + 60) killPlayer();
    }

    private void checkEnemyCollisions() {
        if (player.dead) return;
        RectF pb = player.getBounds();
        for (Enemy en : enemies) {
            if (!en.alive) continue;
            RectF eb = en.getBounds();
            if (!RectF.intersects(pb, eb)) continue;

            if (player.vy > 0 && pb.bottom <= en.y + Constants.EH * 0.35f) {
                en.stomp();
                score += 100;
                SoundManager.playStomp();
                spawnExplosion(en.x + Constants.EW / 2f, en.y + Constants.EH / 2f);
                player.vy = Constants.JUMP_VEL * 0.5f;
            } else if (!player.isInvincible()) {
                killPlayer();
            }
        }
    }

    private void checkPowerUpCollisions() {
        RectF pb = player.getBounds();
        for (PowerUp pu : powerUps) {
            if (pu.collected) continue;
            if (RectF.intersects(pb, pu.getBounds())) {
                pu.collected = true;
                player.collectPowerUp(pu.type);
                score += 200;
                SoundManager.playPowerUp();
            }
        }
    }

    private void checkFinish() {
        if (player.x + Constants.PW >= level.finishX) {
            score += 500 + currentLevel * 150;
            state = State.LEVEL_WIN;
            stateTimer = 2.5f;
            SoundManager.playLevelWin();
        }
    }

    private void killPlayer() {
        if (player.dead) return;
        player.die();
        SoundManager.playDie();
        lives--;
        state = lives > 0 ? State.DEAD : State.GAME_OVER;
        stateTimer = 2.0f;
    }

    // ── Particles ─────────────────────────────────────────────────────────

    private void spawnExplosion(float cx, float cy) {
        Random rng = new Random();
        int[] cols = {0xFFFF4400, 0xFFFF8800, 0xFFFFDD00, 0xFFFF2200, 0xFFFFFFFF, 0xFF88FF00};
        for (int i = 0; i < 20; i++) {
            double a = Math.PI * 2 * i / 20;
            float spd = 160 + rng.nextFloat() * 280;
            float vx = (float) Math.cos(a) * spd;
            float vy = (float) Math.sin(a) * spd - 80;
            float sz = 4f + rng.nextFloat() * 10f;
            particles.add(new Particle(cx, cy, vx, vy, 0.5f + rng.nextFloat() * 0.4f,
                    cols[rng.nextInt(cols.length)], sz));
        }
    }

    // ── Background helpers ────────────────────────────────────────────────

    private void scrollClouds(float dt) { cloudScroll += dt * 18f; }

    // ── Scale ─────────────────────────────────────────────────────────────

    private void updateScale() {
        int sw = getWidth(), sh = getHeight();
        if (sw == 0 || sh == 0) return;
        scale   = Math.min((float) sw / Constants.VW, (float) sh / Constants.VH);
        offsetX = (sw - Constants.VW * scale) / 2f;
        offsetY = (sh - Constants.VH * scale) / 2f;
        muteRect = new RectF(sw - 80, 8, sw - 8, 56);
        buildSky(sh);
    }

    private void buildSky(int sh) {
        skyPaint = new Paint();
        skyPaint.setShader(new LinearGradient(0, 0, 0, sh,
                new int[]{0xFF2980B9, 0xFF6DD5FA, 0xFFB8E4FF}, null, Shader.TileMode.CLAMP));
    }

    // ── Rendering ─────────────────────────────────────────────────────────

    private void render(Canvas canvas) {
        int sw = getWidth(), sh = getHeight();
        if (sw == 0 || sh == 0) return;

        // Sky
        if (skyPaint != null) canvas.drawRect(0, 0, sw, sh, skyPaint);
        else { paint.setColor(0xFF4FC3F7); canvas.drawRect(0, 0, sw, sh, paint); }

        // World matrix
        matrix.reset();
        matrix.setScale(scale, scale);
        matrix.postTranslate(offsetX - cameraX * scale, offsetY);
        canvas.save();
        canvas.setMatrix(matrix);

        drawMountains(canvas);
        drawClouds(canvas);
        drawTrees(canvas);
        drawWorld(canvas);
        drawEntities(canvas);

        for (Particle p : particles) p.draw(canvas, paint);
        drawFinishFlag(canvas);

        canvas.restore();

        drawHUD(canvas, sw, sh);
        drawMuteButton(canvas, sw);
        drawOverlay(canvas, sw, sh);
    }

    // ── Background layers ─────────────────────────────────────────────────

    private void drawMountains(Canvas canvas) {
        // Far mountains (parallax 0.12x)
        float parallax = cameraX * 0.12f;
        paint.setColor(0xFF5B7FA6);
        for (float mx : mountainX) {
            float bx = ((mx - parallax % 1200) % 1200 + cameraX);
            drawTriangle(canvas, bx, Constants.GROUND_Y, bx + 280, 180, bx + 560, Constants.GROUND_Y);
        }
        // Slightly nearer ridge
        paint.setColor(0xFF7FA8C9);
        for (float mx : mountainX) {
            float bx = ((mx + 140 - parallax * 1.5f % 1200) % 1200 + cameraX);
            drawTriangle(canvas, bx, Constants.GROUND_Y, bx + 200, 230, bx + 400, Constants.GROUND_Y);
        }
    }

    private final Path triPath = new Path();
    private void drawTriangle(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3) {
        triPath.reset();
        triPath.moveTo(x1, y1); triPath.lineTo(x2, y2); triPath.lineTo(x3, y3); triPath.close();
        c.drawPath(triPath, paint);
    }

    private void drawClouds(Canvas canvas) {
        paint.setColor(0xCCFFFFFF);
        for (int i = 0; i < cloudX.length; i++) {
            float cx = ((cloudX[i] + cloudScroll * 0.8f + cameraX * 0.22f) % (Constants.VW + 250)) - 125 + cameraX;
            float cy = cloudY[i];
            // Blocky pixel-art cloud
            canvas.drawRect(cx,      cy + 8,  cx + 70, cy + 24, paint);
            canvas.drawRect(cx + 14, cy,      cx + 56, cy + 20, paint);
            canvas.drawRect(cx + 6,  cy + 4,  cx + 64, cy + 22, paint);
        }
    }

    private void drawTrees(Canvas canvas) {
        // Near trees (parallax 0.45x)
        float p = cameraX * 0.45f;
        for (int i = 0; i < 20; i++) {
            float tx = ((i * 460f - p % 9200) % 9200 + cameraX);
            float ty = Constants.GROUND_Y;
            // Trunk
            paint.setColor(0xFF5D4037);
            canvas.drawRect(tx + 10, ty - 55, tx + 22, ty, paint);
            // Foliage
            paint.setColor(0xFF2E7D32);
            canvas.drawRect(tx,      ty - 100, tx + 32, ty - 50, paint);
            canvas.drawRect(tx + 4,  ty - 120, tx + 28, ty - 95, paint);
            paint.setColor(0xFF388E3C);
            canvas.drawRect(tx + 2,  ty - 110, tx + 30, ty - 55, paint);
        }
    }

    private void drawWorld(Canvas canvas) {
        // Ground grass top strip
        paint.setColor(0xFF4CAF50);
        for (float[] seg : level.groundSegs)
            canvas.drawRect(seg[0], Constants.GROUND_Y, seg[1], Constants.GROUND_Y + 9, paint);
        // Ground dirt
        paint.setColor(0xFF795548);
        for (float[] seg : level.groundSegs)
            canvas.drawRect(seg[0], Constants.GROUND_Y + 9, seg[1], Constants.VH + 20, paint);
        // Dirt detail lines
        paint.setColor(0xFF6D4C41);
        for (float[] seg : level.groundSegs)
            for (float bx = seg[0]; bx < seg[1]; bx += 34)
                canvas.drawRect(bx, Constants.GROUND_Y + 14, bx + 28, Constants.GROUND_Y + 16, paint);

        // Platforms
        for (Platform pl : level.platforms) {
            paint.setColor(0xFF66BB6A);  // grass top
            canvas.drawRect(pl.x, pl.y, pl.right(), pl.y + 5, paint);
            paint.setColor(0xFF8D6E63);  // wood body
            canvas.drawRect(pl.x, pl.y + 5, pl.right(), pl.bottom(), paint);
            paint.setColor(0xFF5D4037);  // edges
            canvas.drawRect(pl.x, pl.y + 5, pl.x + 5, pl.bottom(), paint);
            canvas.drawRect(pl.right() - 5, pl.y + 5, pl.right(), pl.bottom(), paint);
        }
    }

    private void drawEntities(Canvas canvas) {
        for (PowerUp pu : powerUps) if (!pu.collected) pu.draw(canvas, paint);
        for (Enemy en : enemies)    en.draw(canvas, paint);
        if (player.dead) player.drawDead(canvas, paint);
        else             player.draw(canvas, paint);
    }

    private void drawFinishFlag(Canvas canvas) {
        float fx = level.finishX, gy = Constants.GROUND_Y;
        paint.setColor(0xFFBDBDBD);
        canvas.drawRect(fx + 2, gy - 130, fx + 8, gy, paint);
        paint.setColor(0xFFE53935);
        canvas.drawRect(fx + 8, gy - 130, fx + 52, gy - 108, paint);
        paint.setColor(Color.WHITE);
        canvas.drawRect(fx + 8, gy - 120, fx + 52, gy - 116, paint);
        // Finish text (tiny pixels)
        paint.setColor(0xFFFFFFFF);
        canvas.drawRect(fx + 12, gy - 128, fx + 48, gy - 125, paint);
    }

    // ── HUD ──────────────────────────────────────────────────────────────

    private void drawHUD(Canvas canvas, int sw, int sh) {
        float ts = scale * 1.1f;
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setShadowLayer(2 * scale, 0, scale, Color.BLACK);

        float hx = offsetX + 10 * scale;
        float hy = offsetY + 22 * scale;

        // Lives
        paint.setColor(Color.WHITE);
        paint.setTextSize(18 * ts);
        canvas.drawText("❤ x" + lives, hx, hy, paint);

        // Score
        paint.setTextSize(13 * ts);
        canvas.drawText(String.format("SCORE %06d", score), hx, hy + 22 * scale, paint);

        // Level
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(14 * ts);
        canvas.drawText("NVL " + currentLevel, offsetX + Constants.VW * scale - 10 * scale, hy, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // Active power-up
        if (state == State.PLAYING && player.powerUp != PowerUp.NONE) {
            String pname = (player.powerUp == PowerUp.STAR)       ? "★ ÉTOILE"
                         : (player.powerUp == PowerUp.SPEED)      ? "⚡ VITESSE"
                         :                                           "↑ SUPER SAUT";
            paint.setColor(0xFFFFDD00);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(13 * ts);
            canvas.drawText(pname, sw / 2f, hy, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        paint.setShadowLayer(0, 0, 0, 0);
        paint.setAntiAlias(false);
    }

    // ── Mute button ───────────────────────────────────────────────────────

    private void drawMuteButton(Canvas canvas, int sw) {
        if (muteRect == null) return;
        paint.setAntiAlias(true);
        paint.setColor(0x88000000);
        canvas.drawRoundRect(muteRect, 8, 8, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(24);
        canvas.drawText(SoundManager.isMuted() ? "🔇" : "🔊",
                muteRect.centerX(), muteRect.centerY() + 8, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setAntiAlias(false);
    }

    // ── Overlay screens ───────────────────────────────────────────────────

    private void drawOverlay(Canvas canvas, int sw, int sh) {
        float cx = sw / 2f, cy = sh / 2f;
        paint.setAntiAlias(true);
        switch (state) {
            case MENU:
                box(canvas, cx, cy, sw, sh);
                text(canvas, "JUMPING GIANLUIGI", 0xFFFFDD00, 34, cx, cy - 44 * scale);
                text(canvas, "Tap pour commencer !", 0xFFFFFFFF, 17, cx, cy + 8 * scale);
                text(canvas, "TAP = saut  •  TAP TAP = double saut", 0xFFCCCCFF, 12, cx, cy + 36 * scale);
                text(canvas, "Saute SUR les ennemis pour les éclater !", 0xFFCCCCFF, 12, cx, cy + 54 * scale);
                break;
            case DEAD:
                box(canvas, cx, cy, sw, sh);
                text(canvas, "RATÉ !", 0xFFFF4444, 30, cx, cy - 18 * scale);
                text(canvas, "Vies : " + lives, 0xFFFFFFFF, 15, cx, cy + 14 * scale);
                if (stateTimer <= 0)
                    text(canvas, "Tap pour réessayer", 0xFFFFDD00, 13, cx, cy + 38 * scale);
                break;
            case GAME_OVER:
                box(canvas, cx, cy, sw, sh);
                text(canvas, "GAME OVER", 0xFFFF2222, 34, cx, cy - 28 * scale);
                text(canvas, "Score final : " + score, 0xFFFFFFFF, 16, cx, cy + 10 * scale);
                text(canvas, "Tap pour recommencer", 0xFFFFDD00, 13, cx, cy + 36 * scale);
                break;
            case LEVEL_WIN:
                box(canvas, cx, cy, sw, sh);
                text(canvas, "NIVEAU " + currentLevel + " TERMINÉ !", 0xFF88FF44, 28, cx, cy - 22 * scale);
                text(canvas, "Score : " + score, 0xFFFFFFFF, 15, cx, cy + 8 * scale);
                if (stateTimer <= 0)
                    text(canvas, "Tap → niveau suivant", 0xFFFFDD00, 13, cx, cy + 36 * scale);
                break;
        }
        paint.setAntiAlias(false);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.DEFAULT);
    }

    private void box(Canvas c, float cx, float cy, int sw, int sh) {
        paint.setColor(0xCC000022);
        c.drawRect(cx - sw * 0.42f, cy - sh * 0.22f, cx + sw * 0.42f, cy + sh * 0.22f, paint);
        paint.setColor(0xFF1A237E);
        c.drawRect(cx - sw * 0.42f, cy - sh * 0.22f, cx + sw * 0.42f, cy - sh * 0.18f, paint);
    }

    private void text(Canvas c, String s, int color, float sp, float cx, float cy) {
        paint.setColor(color);
        paint.setTextSize(sp * scale);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        c.drawText(s, cx, cy, paint);
    }

    // ── Game thread ───────────────────────────────────────────────────────

    private class GameThread extends Thread {
        @Override public void run() {
            while (running) {
                long t0 = System.currentTimeMillis();
                update();
                SurfaceHolder holder = getHolder();
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) synchronized (holder) { render(canvas); }
                } finally {
                    if (canvas != null) holder.unlockCanvasAndPost(canvas);
                }
                long sleep = FRAME_MS - (System.currentTimeMillis() - t0);
                if (sleep > 0) try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }
    }
}
