import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class HazardManager {

    private static class MovingObstacle {
        private final Rectangle bounds;
        private final int startX;
        private final int startY;
        private final int minPosition;
        private final int maxPosition;
        private final int initialStep;
        private final boolean vertical;
        private int step;

        MovingObstacle(int x, int y, int width, int height, int minPosition, int maxPosition,
                       int initialStep, boolean vertical) {
            this.bounds = new Rectangle(x, y, width, height);
            this.startX = x;
            this.startY = y;
            this.minPosition = minPosition;
            this.maxPosition = maxPosition;
            this.initialStep = initialStep;
            this.step = initialStep;
            this.vertical = vertical;
        }

        void update() {
            if (vertical) {
                bounds.y += step;
                if (bounds.y <= minPosition) {
                    bounds.y = minPosition;
                    step = Math.abs(step);
                } else if (bounds.y >= maxPosition) {
                    bounds.y = maxPosition;
                    step = -Math.abs(step);
                }
            } else {
                bounds.x += step;
                if (bounds.x <= minPosition) {
                    bounds.x = minPosition;
                    step = Math.abs(step);
                } else if (bounds.x >= maxPosition) {
                    bounds.x = maxPosition;
                    step = -Math.abs(step);
                }
            }
        }

        void reset() {
            bounds.x = startX;
            bounds.y = startY;
            step = initialStep;
        }
    }

    private static class Enemy {
        private final Rectangle bounds;
        private final int startX;
        private final int startY;
        private final double speed;

        Enemy(int x, int y, int size, double speed) {
            this.bounds = new Rectangle(x, y, size, size);
            this.startX = x;
            this.startY = y;
            this.speed = speed;
        }

        void update(Rectangle playerBounds) {
            double enemyCenterX = bounds.getCenterX();
            double enemyCenterY = bounds.getCenterY();
            double targetX = playerBounds.getCenterX();
            double targetY = playerBounds.getCenterY();
            double deltaX = targetX - enemyCenterX;
            double deltaY = targetY - enemyCenterY;
            double distance = Math.hypot(deltaX, deltaY);

            if (distance < 0.001) {
                return;
            }

            double step = Math.min(speed, distance);
            bounds.x = (int) Math.round(bounds.x + deltaX / distance * step);
            bounds.y = (int) Math.round(bounds.y + deltaY / distance * step);

            bounds.x = Math.max(0, Math.min(bounds.x, GameConfig.WINDOW_WIDTH - bounds.width));
            bounds.y = Math.max(0, Math.min(bounds.y, GameConfig.WINDOW_HEIGHT - bounds.height));
        }

        void reset() {
            bounds.x = startX;
            bounds.y = startY;
        }
    }

    private final List<Rectangle> staticObstacles = new ArrayList<>();
    private final List<MovingObstacle> movingObstacles = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();

    HazardManager() {
        createStaticObstacles();
        createMovingObstacles();
        createEnemies();
    }

    List<Rectangle> getStaticObstacles() {
        return Collections.unmodifiableList(staticObstacles);
    }


    void reset() {
        for (MovingObstacle obstacle : movingObstacles) {
            obstacle.reset();
        }

        for (Enemy enemy : enemies) {
            enemy.reset();
        }
    }

    void update(Rectangle playerBounds) {
        for (MovingObstacle obstacle : movingObstacles) {
            obstacle.update();
        }

        for (Enemy enemy : enemies) {
            enemy.update(playerBounds);
        }
    }

    boolean intersectsHazard(Rectangle rectangle) {
        for (Rectangle obstacle : staticObstacles) {
            if (rectangle.intersects(obstacle)) {
                return true;
            }
        }

        for (MovingObstacle obstacle : movingObstacles) {
            if (rectangle.intersects(obstacle.bounds)) {
                return true;
            }
        }

        for (Enemy enemy : enemies) {
            if (rectangle.intersects(enemy.bounds)) {
                return true;
            }
        }

        return false;
    }

    void draw(Graphics g) {
        Graphics2D graphics2D = (Graphics2D) g.create();

        for (Rectangle obstacle : staticObstacles) {
            drawSpikeObstacle(graphics2D, obstacle);
        }

        for (MovingObstacle obstacle : movingObstacles) {
            drawSpikeObstacle(graphics2D, obstacle.bounds);
        }

        for (Enemy enemy : enemies) {
            graphics2D.setColor(new Color(30, 30, 30));
            graphics2D.fillOval(enemy.bounds.x, enemy.bounds.y, enemy.bounds.width, enemy.bounds.height);

            graphics2D.setColor(new Color(255, 80, 80));
            graphics2D.fillOval(enemy.bounds.x + 5, enemy.bounds.y + 6, 6, 6);
            graphics2D.fillOval(enemy.bounds.x + enemy.bounds.width - 11, enemy.bounds.y + 6, 6, 6);
        }

        graphics2D.dispose();
    }

    private void createStaticObstacles() {
        staticObstacles.clear();
        staticObstacles.add(new Rectangle(240, GameConfig.GROUND_Y - 30, 50, 30));
        staticObstacles.add(new Rectangle(520, GameConfig.GROUND_Y - 30, 52, 30));
        staticObstacles.add(new Rectangle(860, GameConfig.GROUND_Y - 30, 50, 30));
    }

    private void createMovingObstacles() {
        movingObstacles.clear();
        // One horizontal spike patrol.
        movingObstacles.add(new MovingObstacle(220, 215, 50, 30, 180, 880, 4, false));
    }

    private void createEnemies() {
        enemies.clear();
        enemies.add(new Enemy(350, GameConfig.GROUND_Y - GameConfig.ENEMY_SIZE, GameConfig.ENEMY_SIZE, 2.0));
    }

    private void drawSpikeObstacle(Graphics2D graphics2D, Rectangle obstacle) {
        int spikeCount = Math.max(1, obstacle.width / 10);
        int spikeWidth = obstacle.width / spikeCount;

        for (int spikeIndex = 0; spikeIndex < spikeCount; spikeIndex++) {
            int leftX = obstacle.x + spikeIndex * spikeWidth;
            int rightX = spikeIndex == spikeCount - 1 ? obstacle.x + obstacle.width : leftX + spikeWidth;
            int centerX = (leftX + rightX) / 2;

            Polygon spike = new Polygon();
            spike.addPoint(leftX, obstacle.y + obstacle.height);
            spike.addPoint(centerX, obstacle.y);
            spike.addPoint(rightX, obstacle.y + obstacle.height);

            graphics2D.setColor(new Color(210, 40, 40));
            graphics2D.fillPolygon(spike);
            graphics2D.setColor(new Color(120, 10, 10));
            graphics2D.drawPolygon(spike);
        }
    }
}

