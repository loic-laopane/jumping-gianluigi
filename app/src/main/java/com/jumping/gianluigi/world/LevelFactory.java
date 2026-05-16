package com.jumping.gianluigi.world;

import com.jumping.gianluigi.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds all game levels.
 * Physics reference (GRAVITY=1550, JUMP=-630, SPEED=138):
 *   max jump height  ≈ 128 px  (single)
 *   air time         ≈ 0.81 s
 *   max gap (single) ≈ 112 px
 *   max gap (double) ≈ 195 px
 *
 * Platform heights above ground:
 *   "low"  = 90-100 px  → reachable with single jump
 *   "high" = 115-125 px → needs a precise single or easy double
 */
public class LevelFactory {

    private static final float GY = Constants.GROUND_Y;

    public static Level create(int n) {
        switch (n) {
            case 1: return level1();
            case 2: return level2();
            case 3: return level3();
            default: return generated(n);
        }
    }

    // ── LEVEL 1 – Tutorial/Easy ────────────────────────────────────────────
    // Gaps: 70-90 px  |  Enemies: slow (55 px/s)  |  7 enemies
    private static Level level1() {
        float w = Constants.LEVEL_W_BASE;

        // Ground: mostly solid, gaps get progressively slightly larger
        float[][] ground = {
            {0,     1600},  // long safe start
            {1670,  2900},  // gap 70
            {2970,  4200},  // gap 70
            {4290,  5600},  // gap 90
            {5700,  7000},  // gap 100
            {7100,  8400},  // gap 100
            {8500,  w}
        };

        List<Platform> plats = new ArrayList<>();
        // Low platforms bridging/over gaps – reachable with single jump
        plats.add(new Platform(1500, GY - 95,  180)); // before gap 1
        plats.add(new Platform(2800, GY - 95,  160)); // before gap 2
        plats.add(new Platform(3200, GY - 95,  160)); // floating
        plats.add(new Platform(4100, GY - 95,  180)); // before gap 3
        plats.add(new Platform(5500, GY - 110, 160)); // slightly higher
        plats.add(new Platform(6900, GY - 100, 160));
        plats.add(new Platform(8300, GY - 100, 160));
        // Bonus high platform with power-up
        plats.add(new Platform(3700, GY - 170, 140)); // needs double

        // Enemies: {x, patrolLeft, patrolRight}
        float[][] enemies = {
            {600,   350,  1300},
            {1800,  1670, 2600},
            {2400,  1800, 2700},
            {3500,  3000, 4000},
            {4800,  4400, 5400},
            {6200,  5900, 6800},
            {7800,  7300, 8200},
        };

        // Power-ups: {x, y, type}
        float[][] pups = {
            {900,  GY - 130, PowerUpDef.STAR},
            {3750, GY - 215, PowerUpDef.SUPER_JUMP},  // on high platform
            {6500, GY - 130, PowerUpDef.SPEED},
        };

        return new Level(1, w, w - 180, ground, plats, enemies, pups);
    }

    // ── LEVEL 2 – Medium ───────────────────────────────────────────────────
    // Gaps: 100-130 px  |  Enemies: medium (80 px/s)  |  10 enemies
    private static Level level2() {
        float w = Constants.LEVEL_W_BASE + 400f;

        float[][] ground = {
            {0,     1400},
            {1520,  2700},  // gap 120
            {2840,  4100},  // gap 140
            {4250,  5500},  // gap 150
            {5660,  6900},  // gap 160
            {7080,  8200},  // gap 180
            {8380,  w}
        };

        List<Platform> plats = new ArrayList<>();
        // Staircase-style platforms at varied heights
        plats.add(new Platform(1380, GY - 95,  160));
        plats.add(new Platform(1600, GY - 115, 140));
        plats.add(new Platform(2650, GY - 100, 160));
        plats.add(new Platform(2900, GY - 120, 140));
        plats.add(new Platform(4100, GY - 110, 160));
        plats.add(new Platform(4400, GY - 105, 150));
        plats.add(new Platform(5500, GY - 100, 160));
        plats.add(new Platform(5800, GY - 130, 140));
        plats.add(new Platform(6950, GY - 115, 160));
        plats.add(new Platform(7300, GY - 120, 140));
        // High bonus platforms
        plats.add(new Platform(2200, GY - 185, 130));
        plats.add(new Platform(6000, GY - 200, 130));

        float[][] enemies = {
            {500,   300,  1200},
            {900,   600,  1400},
            {1700,  1520, 2500},
            {2300,  1900, 2700},
            {3200,  2900, 3900},
            {3800,  3200, 4100},
            {4800,  4400, 5300},
            {5900,  5700, 6600},
            {6400,  6000, 6900},
            {7600,  7200, 8200},
        };

        float[][] pups = {
            {700,   GY - 130, PowerUpDef.SPEED},
            {2250,  GY - 230, PowerUpDef.STAR},       // high platform
            {6050,  GY - 245, PowerUpDef.SUPER_JUMP}, // high platform
        };

        return new Level(2, w, w - 180, ground, plats, enemies, pups);
    }

    // ── LEVEL 3 – Hard ────────────────────────────────────────────────────
    // Gaps: 130-160 px  |  Enemies: fast (105 px/s)  |  13 enemies
    // Many gaps require well-timed double-jump with platform stepping
    private static Level level3() {
        float w = Constants.LEVEL_W_BASE + 900f;

        float[][] ground = {
            {0,     1200},
            {1360,  2400},  // gap 160
            {2600,  3700},  // gap 200
            {3920,  5000},  // gap 220
            {5220,  6100},  // gap 220
            {6380,  7200},  // gap 280
            {7540,  8500},  // gap 340
            {8880,  w}
        };

        List<Platform> plats = new ArrayList<>();
        // Stepping stones across large gaps
        plats.add(new Platform(1200, GY - 100, 140));
        plats.add(new Platform(1450, GY - 120, 120));
        plats.add(new Platform(2400, GY - 105, 140));
        plats.add(new Platform(2680, GY - 125, 120));
        plats.add(new Platform(3720, GY - 110, 140));
        plats.add(new Platform(3980, GY - 130, 120));
        plats.add(new Platform(5050, GY - 110, 140));
        plats.add(new Platform(5300, GY - 130, 120));
        plats.add(new Platform(6120, GY - 115, 130));
        plats.add(new Platform(6500, GY - 120, 130));
        plats.add(new Platform(7380, GY - 115, 140));
        plats.add(new Platform(7700, GY - 120, 130));
        plats.add(new Platform(8680, GY - 110, 140));
        // Skyway bonus platforms
        plats.add(new Platform(1900, GY - 195, 120));
        plats.add(new Platform(4500, GY - 205, 120));
        plats.add(new Platform(7000, GY - 200, 120));

        float[][] enemies = {
            {400,   200,  1000},
            {700,   400,  1200},
            {1600,  1400, 2200},
            {2000,  1600, 2400},
            {2700,  2600, 3500},
            {3200,  2800, 3700},
            {4100,  3900, 4700},
            {4600,  4200, 5000},
            {5400,  5300, 5900},
            {5700,  5400, 6100},
            {6600,  6500, 7000},
            {7200,  6900, 7600},
            {8200,  8100, 9000},
        };

        float[][] pups = {
            {600,   GY - 130, PowerUpDef.SPEED},
            {1950,  GY - 240, PowerUpDef.STAR},
            {4550,  GY - 250, PowerUpDef.SUPER_JUMP},
            {7050,  GY - 245, PowerUpDef.SPEED},
        };

        return new Level(3, w, w - 180, ground, plats, enemies, pups);
    }

    // ── PROCEDURAL GENERATOR for levels 4+ ────────────────────────────────
    private static Level generated(int n) {
        Random rng = new Random(n * 98317L);
        float w   = Constants.LEVEL_W_BASE + (n - 3) * 500f;
        float diff = Math.min((n - 1) * 0.12f, 0.90f);

        float minGap = 80  + diff * 80;
        float maxGap = 110 + diff * 110;

        List<float[]>   groundList = new ArrayList<>();
        List<Platform>  platList   = new ArrayList<>();
        List<float[]>   enemyList  = new ArrayList<>();
        List<float[]>   pupList    = new ArrayList<>();

        // Safe starting stretch
        groundList.add(new float[]{0, 1000});
        float cur = 1000;
        int  segIdx = 0;

        while (cur < w - 600) {
            float gap = minGap + rng.nextFloat() * (maxGap - minGap);

            // One or two stepping platforms across the gap
            float p1x = cur + gap * 0.35f;
            platList.add(new Platform(p1x, GY - (90 + rng.nextFloat() * 30), 130));
            if (gap > 130) {
                float p2x = cur + gap * 0.65f;
                platList.add(new Platform(p2x, GY - (95 + rng.nextFloat() * 25), 120));
            }

            cur += gap;
            float seg = 700 + rng.nextFloat() * 700;
            float segEnd = Math.min(cur + seg, w);
            groundList.add(new float[]{cur, segEnd});

            // Enemies per segment
            int eCount = 1 + (int)(diff * 2.5f);
            float ex = cur + 150;
            float eSpeed = 55f + diff * 60f;
            for (int i = 0; i < eCount && ex + 200 < segEnd; i++) {
                enemyList.add(new float[]{ex, ex - 80, Math.min(ex + 350 + rng.nextFloat()*200, segEnd - 80)});
                ex += 350 + rng.nextFloat() * 250;
            }

            // Occasional bonus platform + power-up
            if (rng.nextFloat() < 0.4f) {
                float bx = cur + 200 + rng.nextFloat() * 400;
                platList.add(new Platform(bx, GY - 190, 120));
                pupList.add(new float[]{bx + 40, GY - 235, rng.nextInt(3)});
            }

            segIdx++;
            cur = segEnd;
        }
        groundList.add(new float[]{cur, w});

        return new Level(n, w, w - 180,
                groundList.toArray(new float[0][]),
                platList,
                enemyList.toArray(new float[0][]),
                pupList.toArray(new float[0][]));
    }

    // Power-up type constants mirror (avoids circular imports)
    private static final class PowerUpDef {
        static final int STAR       = 0;
        static final int SPEED      = 1;
        static final int SUPER_JUMP = 2;
    }
}
