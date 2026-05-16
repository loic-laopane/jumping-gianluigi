package com.jumping.gianluigi.world;

import com.jumping.gianluigi.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LevelFactory {

    public static Level create(int levelNum) {
        switch (levelNum) {
            case 1: return level1();
            case 2: return level2();
            case 3: return level3();
            default: return generateRandom(levelNum);
        }
    }

    // ── LEVEL 1 ─ Easy: small gaps, few enemies ────────────────────────────
    private static Level level1() {
        float gY = Constants.GROUND_Y;
        float w  = Constants.LEVEL_W_BASE;

        float[][] ground = {
            {0,    1800},
            {1920, 3400},
            {3550, 5200},
            {5400, 7100},
            {7280, 8900},
            {9000, w}
        };

        List<Platform> plats = new ArrayList<>();
        plats.add(new Platform(1200, gY - 140, 200));
        plats.add(new Platform(2600, gY - 160, 200));
        plats.add(new Platform(4100, gY - 150, 200));
        plats.add(new Platform(5900, gY - 170, 200));
        plats.add(new Platform(7800, gY - 150, 200));

        // enemies: {x, patrolLeft, patrolRight}  (ground enemies)
        float[][] enemies = {
            {700,   500,  1400},
            {1600,  1200, 2500},
            {2800,  2000, 3200},
            {3800,  3600, 4800},
            {5600,  5400, 6600},
            {7500,  7300, 8200},
            {9200,  9000, 9900}
        };

        // power-ups: {x, y, type}
        float[][] pups = {
            {1000, gY - 180, 0},  // STAR
            {5100, gY - 180, 1},  // SPEED
            {8500, gY - 180, 2}   // SUPER_JUMP
        };

        return new Level(1, w, w - 200, ground, plats, enemies, pups);
    }

    // ── LEVEL 2 ─ Medium: wider gaps, more enemies ─────────────────────────
    private static Level level2() {
        float gY = Constants.GROUND_Y;
        float w  = Constants.LEVEL_W_BASE + 500;

        float[][] ground = {
            {0,    1600},
            {1800, 3100},
            {3360, 4900},
            {5160, 6700},
            {7000, 8400},
            {8700, w}
        };

        List<Platform> plats = new ArrayList<>();
        plats.add(new Platform(1000, gY - 150, 200));
        plats.add(new Platform(1700, gY - 120, 160));  // bridge over gap
        plats.add(new Platform(2200, gY - 160, 200));
        plats.add(new Platform(3200, gY - 180, 180));
        plats.add(new Platform(4000, gY - 140, 160));  // bridge
        plats.add(new Platform(4960, gY - 120, 160));  // bridge
        plats.add(new Platform(5700, gY - 200, 200));
        plats.add(new Platform(6800, gY - 160, 160));  // bridge
        plats.add(new Platform(7800, gY - 180, 200));
        plats.add(new Platform(8600, gY - 120, 160));  // bridge

        float[][] enemies = {
            {600,   400,  1300},
            {1200,  900,  1600},
            {2000,  1800, 2800},
            {2700,  2200, 3100},
            {3600,  3400, 4600},
            {4400,  3800, 4900},
            {5600,  5200, 6400},
            {6300,  5800, 6700},
            {7200,  7000, 8000},
            {8000,  7500, 8400},
            {9100,  8800, 10000}
        };

        float[][] pups = {
            {800,  gY - 190, 2},
            {4500, gY - 190, 0},
            {7500, gY - 190, 1}
        };

        return new Level(2, w, w - 200, ground, plats, enemies, pups);
    }

    // ── LEVEL 3 ─ Hard: big gaps, fast enemies, platform hopping ──────────
    private static Level level3() {
        float gY = Constants.GROUND_Y;
        float w  = Constants.LEVEL_W_BASE + 1000;

        float[][] ground = {
            {0,    1400},
            {1700, 2800},
            {3200, 4200},
            {4700, 5600},
            {6200, 7000},
            {7600, 8500},
            {9100, w}
        };

        // Many platforms to cross the large gaps
        List<Platform> plats = new ArrayList<>();
        plats.add(new Platform(1400, gY - 130, 160));
        plats.add(new Platform(1550, gY - 130, 160));
        plats.add(new Platform(2850, gY - 160, 180));
        plats.add(new Platform(3050, gY - 160, 180));
        plats.add(new Platform(4250, gY - 140, 160));
        plats.add(new Platform(4450, gY - 140, 160));
        plats.add(new Platform(5660, gY - 180, 180));
        plats.add(new Platform(5900, gY - 180, 180));
        plats.add(new Platform(7050, gY - 160, 160));
        plats.add(new Platform(7300, gY - 160, 160));
        plats.add(new Platform(8550, gY - 140, 160));
        plats.add(new Platform(8750, gY - 140, 160));
        // High platforms with power-ups
        plats.add(new Platform(2000, gY - 240, 160));
        plats.add(new Platform(5200, gY - 260, 160));
        plats.add(new Platform(8000, gY - 240, 160));

        float[][] enemies = {
            {500,  300,  1200},
            {900,  600,  1400},
            {2000, 1800, 2700},
            {2400, 2000, 2800},
            {3400, 3200, 4100},
            {3700, 3300, 4200},
            {4900, 4700, 5500},
            {5200, 4800, 5600},
            {6400, 6200, 6900},
            {6700, 6300, 7000},
            {7800, 7600, 8400},
            {8100, 7700, 8500},
            {9300, 9100, 10500}
        };

        float[][] pups = {
            {2050, gY - 290, 0},  // star on high platform
            {5250, gY - 310, 2},  // super-jump on high platform
            {8050, gY - 290, 1}   // speed on high platform
        };

        return new Level(3, w, w - 200, ground, plats, enemies, pups);
    }

    // ── RANDOM LEVEL ─ for levels 4+ ──────────────────────────────────────
    private static Level generateRandom(int num) {
        Random rng = new Random(num * 31337L);
        float gY = Constants.GROUND_Y;
        float w  = Constants.LEVEL_W_BASE + (num - 3) * 400f;

        float difficulty = Math.min((num - 1) * 0.15f, 1.0f);
        float minGap = 120 + difficulty * 80;
        float maxGap = 200 + difficulty * 120;

        List<float[]> groundList = new ArrayList<>();
        List<Platform> plats = new ArrayList<>();
        List<float[]> enemyList = new ArrayList<>();
        List<float[]> pupList = new ArrayList<>();

        float cursor = 0;
        groundList.add(new float[]{cursor, cursor + 1000});
        cursor += 1000;

        while (cursor < w - 600) {
            float gap = minGap + rng.nextFloat() * (maxGap - minGap);
            // Bridge platform over gap
            float bridgeX = cursor + gap * 0.4f;
            plats.add(new Platform(bridgeX, gY - 100 - rng.nextFloat() * 80, 160));

            cursor += gap;
            float seg = 800 + rng.nextFloat() * 600;
            float segEnd = Math.min(cursor + seg, w);
            groundList.add(new float[]{cursor, segEnd});

            // Enemies on segment
            int eCount = 1 + (int)(difficulty * 2);
            float ex = cursor + 200;
            for (int i = 0; i < eCount && ex + 200 < segEnd; i++) {
                enemyList.add(new float[]{ex, ex - 100, Math.min(ex + 400, segEnd - 100)});
                ex += 400 + rng.nextFloat() * 300;
            }

            // Occasional power-up
            if (rng.nextFloat() < 0.4f) {
                int t = rng.nextInt(3);
                pupList.add(new float[]{cursor + 200, gY - 180, t});
            }

            cursor = segEnd;
        }
        groundList.add(new float[]{cursor, w});

        return new Level(num, w, w - 200,
            groundList.toArray(new float[0][]),
            plats,
            enemyList.toArray(new float[0][]),
            pupList.toArray(new float[0][]));
    }
}
