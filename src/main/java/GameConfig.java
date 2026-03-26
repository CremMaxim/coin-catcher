import java.awt.*;

final class GameConfig {
    private GameConfig() {
    }

    static final int WINDOW_WIDTH = 1100;
    static final int WINDOW_HEIGHT = 700;
    static final int GROUND_Y = WINDOW_HEIGHT - 100;

    static final int PLAYER_SIZE = 30;
    static final int ENEMY_SIZE = 26;
    static final int COIN_SIZE = 18;

    static final int START_X = 40;
    static final int START_Y = GROUND_Y - PLAYER_SIZE;

    static final int COIN_TIME_LIMIT_MS = 10000;

    static final double MOVE_SPEED = 4.0;
    static final double JUMP_SPEED = -16.5;
    static final double GRAVITY = 0.65;
    static final double MAX_FALL_SPEED = 14.0;

    static final Color SKY_COLOR = new Color(0, 50, 200);
    static final Color GROUND_DARK = Color.DARK_GRAY;
    static final Color GROUND_MID = new Color(50, 50, 50);
    static final Color GROUND_LIGHT = Color.GRAY;
    static final Color BLOCK_COLOR = new Color(120, 120, 120);
    static final Color PLAYER_COLOR = new Color(255, 220, 60);
    static final Color COIN_COLOR = new Color(255, 210, 0);
    static final Color COIN_SHINE_COLOR = new Color(255, 245, 170);
}

