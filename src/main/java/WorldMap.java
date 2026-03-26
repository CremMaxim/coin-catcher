import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

class WorldMap {

    private static class Interval {
        private final double start;
        private final double end;

        Interval(double start, double end) {
            this.start = start;
            this.end = end;
        }
    }

    private static class WalkableSegment {
        private final double minX;
        private final double maxX;
        private final double floorY;

        WalkableSegment(double minX, double maxX, double floorY) {
            this.minX = minX;
            this.maxX = maxX;
            this.floorY = floorY;
        }

        boolean contains(double x, double y) {
            return Math.abs(y - floorY) < 0.6 && x >= minX - 0.6 && x <= maxX + 0.6;
        }
    }

    private static class VerticalMovementResult {
        private final double y;
        private final double velocityY;
        private final boolean grounded;

        VerticalMovementResult(double y, double velocityY, boolean grounded) {
            this.y = y;
            this.velocityY = velocityY;
            this.grounded = grounded;
        }
    }

    private final HazardManager hazards;
    private final List<Rectangle> platforms = new ArrayList<>();
    private final List<Point> coinSpawnPoints = new ArrayList<>();

    WorldMap(HazardManager hazards) {
        this.hazards = hazards;
        createPlatforms();
        createCoinSpawnPoints();
    }

    List<Rectangle> getPlatforms() {
        return platforms;
    }


    Rectangle pickCoinSpawn(Random random, Rectangle playerBounds) {
        List<Point> available = new ArrayList<>();

        for (Point point : coinSpawnPoints) {
            Rectangle spawn = new Rectangle(point.x, point.y, GameConfig.COIN_SIZE, GameConfig.COIN_SIZE);
            if (!spawn.intersects(playerBounds)) {
                available.add(point);
            }
        }

        List<Point> pool = available.isEmpty() ? coinSpawnPoints : available;
        Point point = pool.get(random.nextInt(pool.size()));
        return new Rectangle(point.x, point.y, GameConfig.COIN_SIZE, GameConfig.COIN_SIZE);
    }

    void draw(Graphics g) {
        drawBackgroundDecor(g);
        drawGround(g);
        drawFloatingBlocks(g);
    }

    // Decorative layers only; they are not part of collision geometry.
    private void drawBackgroundDecor(Graphics g) {
        Graphics2D graphics2D = (Graphics2D) g.create();

        int width = GameConfig.WINDOW_WIDTH;

        drawHill(graphics2D, -120, GameConfig.GROUND_Y + 18, 460, 200, new Color(40, 150, 70));
        drawHill(graphics2D, width / 4 - 120, GameConfig.GROUND_Y + 22, 520, 240, new Color(55, 170, 80));
        drawHill(graphics2D, width / 2 + 30, GameConfig.GROUND_Y + 18, 500, 210, new Color(45, 160, 75));

        // Keep clouds below the HUD text area for readability.
        drawCloud(graphics2D, 120, 145, 1.1);
        drawCloud(graphics2D, width / 2 - 120, 165, 1.2);
        drawCloud(graphics2D, width - 330, 150, 1.1);

        graphics2D.dispose();
    }

    private void drawHill(Graphics2D graphics2D, int x, int y, int width, int height, Color color) {
        graphics2D.setColor(color);
        graphics2D.fillOval(x, y - height, width, height);
    }

    private void drawCloud(Graphics2D graphics2D, int x, int y, double scale) {
        int w1 = (int) Math.round(44 * scale);
        int h1 = (int) Math.round(24 * scale);
        int w2 = (int) Math.round(50 * scale);
        int h2 = (int) Math.round(30 * scale);
        int w3 = (int) Math.round(42 * scale);
        int h3 = (int) Math.round(24 * scale);
        int gap1 = (int) Math.round(20 * scale);
        int gap2 = (int) Math.round(50 * scale);

        graphics2D.setColor(new Color(255, 255, 255, 220));
        graphics2D.fillOval(x, y, w1, h1);
        graphics2D.fillOval(x + gap1, y - (int) Math.round(10 * scale), w2, h2);
        graphics2D.fillOval(x + gap2, y, w3, h3);
    }

    private void createPlatforms() {
        platforms.clear();
        platforms.add(new Rectangle(0, GameConfig.GROUND_Y, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT - GameConfig.GROUND_Y));

        // Larger platforms with more spacing for wide-screen play.
        platforms.add(new Rectangle(50, 480, 190, 30));
        platforms.add(new Rectangle(300, 430, 190, 30));
        platforms.add(new Rectangle(590, 370, 210, 30));
        platforms.add(new Rectangle(860, 440, 190, 30));
        platforms.add(new Rectangle(170, 310, 200, 30));
        platforms.add(new Rectangle(480, 260, 200, 30));
        platforms.add(new Rectangle(790, 300, 210, 30));
    }

    // Precompute valid coin spots so respawn is fast and consistent.
    private void createCoinSpawnPoints() {
        coinSpawnPoints.clear();

        List<WalkableSegment> segments = createWalkableSegments();
        boolean[] reachable = findReachableSegments(segments);
        Rectangle startBounds = new Rectangle(GameConfig.START_X, GameConfig.START_Y, GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);

        for (int i = 0; i < segments.size(); i++) {
            if (!reachable[i]) {
                continue;
            }

            WalkableSegment segment = segments.get(i);
            double width = segment.maxX - segment.minX;
            int spotCount = Math.max(1, (int) Math.floor(width / 90.0) + 1);

            for (int spotIndex = 0; spotIndex < spotCount; spotIndex++) {
                double ratio = (spotIndex + 1.0) / (spotCount + 1.0);
                double playerSpotX = segment.minX + width * ratio;
                int coinX = (int) Math.round(playerSpotX + (GameConfig.PLAYER_SIZE - GameConfig.COIN_SIZE) / 2.0);
                int coinY = (int) Math.round(segment.floorY + GameConfig.PLAYER_SIZE - GameConfig.COIN_SIZE);
                Rectangle candidate = new Rectangle(coinX, coinY, GameConfig.COIN_SIZE, GameConfig.COIN_SIZE);

                if (!candidate.intersects(startBounds) && !hazards.intersectsHazard(candidate)) {
                    coinSpawnPoints.add(new Point(coinX, coinY));
                }
            }
        }

        if (coinSpawnPoints.isEmpty()) {
            coinSpawnPoints.add(new Point(100 - GameConfig.COIN_SIZE / 2, GameConfig.GROUND_Y - GameConfig.COIN_SIZE));
        }
    }

    private List<WalkableSegment> createWalkableSegments() {
        List<WalkableSegment> result = new ArrayList<>();

        for (Rectangle platform : platforms) {
            double minX = platform.x;
            double maxX = platform.getMaxX() - GameConfig.PLAYER_SIZE;
            if (maxX < minX) {
                continue;
            }

            List<Interval> blocked = getBlockedIntervals(platform, minX, maxX);
            addWalkableSegmentsForPlatform(result, platform.getY() - GameConfig.PLAYER_SIZE, minX, maxX, blocked);
        }

        return result;
    }

    private List<Interval> getBlockedIntervals(Rectangle platform, double minX, double maxX) {
        List<Interval> blocked = new ArrayList<>();

        for (Rectangle obstacle : hazards.getStaticObstacles()) {
            boolean sitsOnPlatform = obstacle.y + obstacle.height == platform.y;
            boolean overlaps = obstacle.x < platform.x + platform.width && obstacle.x + obstacle.width > platform.x;

            if (sitsOnPlatform && overlaps) {
                double start = Math.max(minX, obstacle.x - GameConfig.PLAYER_SIZE + 1.0);
                double end = Math.min(maxX, obstacle.x + obstacle.width - 1.0);
                if (start <= end) {
                    blocked.add(new Interval(start, end));
                }
            }
        }

        blocked.sort(Comparator.comparingDouble(interval -> interval.start));
        return blocked;
    }

    private void addWalkableSegmentsForPlatform(List<WalkableSegment> result, double floorY, double minX,
                                                double maxX, List<Interval> blocked) {
        double currentStart = minX;

        for (Interval interval : blocked) {
            if (interval.start > currentStart) {
                result.add(new WalkableSegment(currentStart, interval.start - 1.0, floorY));
            }
            currentStart = Math.max(currentStart, interval.end + 1.0);
        }

        if (currentStart <= maxX) {
            result.add(new WalkableSegment(currentStart, maxX, floorY));
        }
    }

    private boolean[] findReachableSegments(List<WalkableSegment> segments) {
        boolean[] reachable = new boolean[segments.size()];
        int startIndex = -1;

        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).contains(GameConfig.START_X, GameConfig.START_Y)) {
                startIndex = i;
                break;
            }
        }

        if (startIndex < 0) {
            return reachable;
        }

        List<Integer> frontier = new ArrayList<>();
        frontier.add(startIndex);
        reachable[startIndex] = true;

        for (int cursor = 0; cursor < frontier.size(); cursor++) {
            int sourceIndex = frontier.get(cursor);

            for (int targetIndex = 0; targetIndex < segments.size(); targetIndex++) {
                if (reachable[targetIndex] || sourceIndex == targetIndex) {
                    continue;
                }

                if (canReachSegment(segments, sourceIndex, targetIndex)) {
                    reachable[targetIndex] = true;
                    frontier.add(targetIndex);
                }
            }
        }

        return reachable;
    }

    private boolean canReachSegment(List<WalkableSegment> segments, int sourceIndex, int targetIndex) {
        WalkableSegment source = segments.get(sourceIndex);
        WalkableSegment target = segments.get(targetIndex);

        if (target.floorY < source.floorY - getMaximumJumpRise() - 2.0) {
            return false;
        }

        for (double takeoffX : getTakeoffPositions(source)) {
            if (simulateJumpToSegment(segments, sourceIndex, targetIndex, takeoffX)) {
                return true;
            }
        }

        return false;
    }

    private List<Double> getTakeoffPositions(WalkableSegment segment) {
        List<Double> positions = new ArrayList<>();
        double width = segment.maxX - segment.minX;

        addTakeoffPosition(positions, segment.minX);
        addTakeoffPosition(positions, segment.maxX);
        addTakeoffPosition(positions, segment.minX + width / 2.0);

        if (width > 40) {
            addTakeoffPosition(positions, segment.minX + width * 0.25);
            addTakeoffPosition(positions, segment.minX + width * 0.75);
        }

        return positions;
    }

    private void addTakeoffPosition(List<Double> positions, double candidate) {
        for (double existing : positions) {
            if (Math.abs(existing - candidate) < 1.0) {
                return;
            }
        }

        positions.add(candidate);
    }

    private boolean simulateJumpToSegment(List<WalkableSegment> segments, int sourceIndex, int targetIndex, double takeoffX) {
        WalkableSegment source = segments.get(sourceIndex);
        WalkableSegment target = segments.get(targetIndex);

        double x = Math.max(source.minX, Math.min(takeoffX, source.maxX));
        double y = source.floorY;
        double velocityY = GameConfig.JUMP_SPEED;

        for (int step = 0; step < 240; step++) {
            double velocityX = getSteeringVelocity(x, target);

            x = moveSimulatedPlayerHorizontally(x, y, velocityX);
            if (hazards.intersectsHazard(getBounds(x, y))) {
                return false;
            }

            VerticalMovementResult movement = moveSimulatedPlayerVertically(x, y, velocityY);
            y = movement.y;
            velocityY = movement.velocityY;

            if (hazards.intersectsHazard(getBounds(x, y))) {
                return false;
            }

            if (didLandOnTargetOrOtherSegment(segments, sourceIndex, targetIndex, x, y, movement.grounded, step)) {
                return findStandingSegmentIndex(segments, x, y) == targetIndex;
            }
        }

        return false;
    }

    private double moveSimulatedPlayerHorizontally(double x, double y, double velocityX) {
        x += velocityX;
        x = resolveHorizontalPosition(x, y, velocityX);
        return Math.max(0, Math.min(x, GameConfig.WINDOW_WIDTH - (double) GameConfig.PLAYER_SIZE));
    }

    private VerticalMovementResult moveSimulatedPlayerVertically(double x, double y, double velocityY) {
        velocityY = Math.min(velocityY + GameConfig.GRAVITY, GameConfig.MAX_FALL_SPEED);
        y += velocityY;
        return resolveVerticalPosition(x, y, velocityY);
    }

    private boolean didLandOnTargetOrOtherSegment(List<WalkableSegment> segments, int sourceIndex,
                                                   int targetIndex, double x, double y,
                                                   boolean groundedState, int step) {
        if (!groundedState) {
            return false;
        }

        int landedIndex = findStandingSegmentIndex(segments, x, y);
        return landedIndex == targetIndex
                || (landedIndex >= 0 && landedIndex != sourceIndex)
                || (step > 0 && landedIndex == sourceIndex);
    }

    private double getSteeringVelocity(double x, WalkableSegment target) {
        if (x < target.minX) {
            return GameConfig.MOVE_SPEED;
        }

        if (x > target.maxX) {
            return -GameConfig.MOVE_SPEED;
        }

        return 0;
    }

    private double resolveHorizontalPosition(double x, double y, double velocityX) {
        Rectangle bounds = getBounds(x, y);

        for (Rectangle platform : platforms) {
            if (!bounds.intersects(platform)) {
                continue;
            }

            if (velocityX > 0) {
                x = platform.getX() - GameConfig.PLAYER_SIZE;
            } else if (velocityX < 0) {
                x = platform.getMaxX();
            }

            bounds = getBounds(x, y);
        }

        return x;
    }

    private VerticalMovementResult resolveVerticalPosition(double x, double y, double velocityY) {
        boolean grounded = false;
        Rectangle bounds = getBounds(x, y);

        for (Rectangle platform : platforms) {
            if (!bounds.intersects(platform)) {
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

            bounds = getBounds(x, y);
        }

        return new VerticalMovementResult(y, velocityY, grounded);
    }

    private int findStandingSegmentIndex(List<WalkableSegment> segments, double x, double y) {
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).contains(x, y)) {
                return i;
            }
        }

        return -1;
    }

    private double getMaximumJumpRise() {
        double jumpRise = 0;
        double velocityY = GameConfig.JUMP_SPEED;

        while (velocityY < 0) {
            jumpRise += Math.abs(velocityY);
            velocityY += GameConfig.GRAVITY;
        }

        return jumpRise;
    }

    private Rectangle getBounds(double x, double y) {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
    }

    private void drawGround(Graphics g) {
        g.setColor(GameConfig.GROUND_DARK);
        g.fillRect(0, GameConfig.GROUND_Y, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT - GameConfig.GROUND_Y);

        g.setColor(GameConfig.GROUND_MID);
        g.fillRect(0, GameConfig.GROUND_Y, GameConfig.WINDOW_WIDTH, 20);

        g.setColor(GameConfig.GROUND_LIGHT);
        g.fillRect(0, GameConfig.GROUND_Y, GameConfig.WINDOW_WIDTH, 10);
    }

    private void drawFloatingBlocks(Graphics g) {
        g.setColor(GameConfig.BLOCK_COLOR);
        for (int i = 1; i < platforms.size(); i++) {
            Rectangle platform = platforms.get(i);
            g.fillRect(platform.x, platform.y, platform.width, platform.height);
        }
    }
}

