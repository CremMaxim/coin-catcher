import java.awt.*;
import java.util.List;

class Player {
    private double x;
    private double y;
    private double velocityX;
    private double velocityY;
    private boolean grounded;
    private int facingDirection;

    Player() {
        reset();
    }

    void reset() {
        x = GameConfig.START_X;
        y = GameConfig.START_Y;
        velocityX = 0;
        velocityY = 0;
        grounded = true;
        facingDirection = 1;
    }

    void setHorizontalInput(boolean movingLeft, boolean movingRight) {
        velocityX = 0;
        if (movingLeft && !movingRight) {
            velocityX = -GameConfig.MOVE_SPEED;
            facingDirection = -1;
        } else if (movingRight && !movingLeft) {
            velocityX = GameConfig.MOVE_SPEED;
            facingDirection = 1;
        }
    }

    void tryJump(boolean jumpPressed) {
        if (jumpPressed && grounded) {
            velocityY = GameConfig.JUMP_SPEED;
            grounded = false;
        }
    }

    void update(List<Rectangle> platforms) {
        applyGravity();
        moveHorizontally(platforms);
        moveVertically(platforms);
        keepInsideWindow();
    }

    Rectangle getBounds() {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
    }

    double getX() {
        return x;
    }

    double getY() {
        return y;
    }

    int getFacingDirection() {
        return facingDirection;
    }

    private void applyGravity() {
        velocityY = Math.min(velocityY + GameConfig.GRAVITY, GameConfig.MAX_FALL_SPEED);
    }

    private void moveHorizontally(List<Rectangle> platforms) {
        x += velocityX;
        Rectangle playerBounds = getBounds();

        for (Rectangle platform : platforms) {
            if (!playerBounds.intersects(platform)) {
                continue;
            }

            if (velocityX > 0) {
                x = platform.getX() - GameConfig.PLAYER_SIZE;
            } else if (velocityX < 0) {
                x = platform.getMaxX();
            }

            playerBounds = getBounds();
        }
    }

    private void moveVertically(List<Rectangle> platforms) {
        grounded = false;
        y += velocityY;
        Rectangle playerBounds = getBounds();

        for (Rectangle platform : platforms) {
            if (!playerBounds.intersects(platform)) {
                continue;
            }

            if (velocityY > 0) {
                y = platform.getY() - GameConfig.PLAYER_SIZE;
                velocityY = 0;
                grounded = true;
            } else if (velocityY < 0) {
                y = platform.getMaxY();
                velocityY = 0;
            }

            playerBounds = getBounds();
        }
    }

    private void keepInsideWindow() {
        if (x < 0) {
            x = 0;
        }

        if (x + GameConfig.PLAYER_SIZE > GameConfig.WINDOW_WIDTH) {
            x = GameConfig.WINDOW_WIDTH - GameConfig.PLAYER_SIZE;
        }

        if (y + GameConfig.PLAYER_SIZE > GameConfig.WINDOW_HEIGHT) {
            y = GameConfig.WINDOW_HEIGHT - GameConfig.PLAYER_SIZE;
            velocityY = 0;
            grounded = true;
        }
    }
}

