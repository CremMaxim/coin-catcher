import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

class Game extends JPanel {

    private static class Interval {
        private final double start;
        private final double end;

        private Interval(double start, double end) {
            this.start = start;
            this.end = end;
        }
    }

    private static class WalkableSegment {
        private final double minX;
        private final double maxX;
        private final double floorY;

        private WalkableSegment(double minX, double maxX, double floorY) {
            this.minX = minX;
            this.maxX = maxX;
            this.floorY = floorY;
        }

        private boolean contains(double playerX, double playerY) {
            return Math.abs(playerY - floorY) < 0.6 && playerX >= minX - 0.6 && playerX <= maxX + 0.6;
        }
    }

    private static class VerticalMovementResult {
        private final double playerY;
        private final double velocityY;
        private final boolean grounded;

        private VerticalMovementResult(double playerY, double velocityY, boolean grounded) {
            this.playerY = playerY;
            this.velocityY = velocityY;
            this.grounded = grounded;
        }
    }

    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 500;
    private static final int GROUND_Y = 400;
    private static final int PLAYER_SIZE = 30;
    private static final int COIN_SIZE = 18;
    private static final int START_X = 40;
    private static final int START_Y = GROUND_Y - PLAYER_SIZE;
    private static final int COIN_TIME_LIMIT_MS = 5000;
    private static final double MOVE_SPEED = 4.0;
    private static final double JUMP_SPEED = -13.5;
    private static final double GRAVITY = 0.65;
    private static final double MAX_FALL_SPEED = 14.0;

    // Colors used in the level
    private static final Color SKY_COLOR = new Color(0, 50, 200);
    private static final Color GROUND_DARK = Color.DARK_GRAY;
    private static final Color GROUND_MID = new Color(50, 50, 50);
    private static final Color GROUND_LIGHT = Color.GRAY;
    private static final Color BLOCK_COLOR = new Color(120, 120, 120);
    private static final Color PLAYER_COLOR = new Color(255, 220, 60);
    private static final Color COIN_COLOR = new Color(255, 210, 0);
    private static final Color COIN_SHINE_COLOR = new Color(255, 245, 170);

    private final List<Rectangle> platforms = new ArrayList<>();
    private final List<Rectangle> obstacles = new ArrayList<>();
    private final List<Point> coinSpawnPoints = new ArrayList<>();
    private final Random random = new Random();

    private double playerX = START_X;
    private double playerY = START_Y;
    private double velocityX;
    private double velocityY;
    private Rectangle coinBounds;
    private int score;
    private long coinDeadline;

    private boolean movingLeft;
    private boolean movingRight;
    private boolean jumpPressed;
    private boolean grounded;

    public Game() {
        setBackground(SKY_COLOR);
        setLayout(null);
        setFocusable(true);

        createPlatforms();
        createObstacles();
        createCoinSpawnPoints();
        setupKeyBindings();
        resetRun();

        Timer gameTimer = new Timer(16, e -> updateGame());
        gameTimer.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawGround(g);
        drawFloatingBlocks(g);
        drawObstacles(g);
        drawCoin(g);
        drawPlayer(g);
        drawHud(g);
    }

    private void createPlatforms() {
        platforms.clear();
        platforms.add(new Rectangle(0, GROUND_Y, WINDOW_WIDTH, WINDOW_HEIGHT - GROUND_Y));

        platforms.add(new Rectangle(80, 300, 60, 20));
        platforms.add(new Rectangle(200, 250, 60, 20));
        platforms.add(new Rectangle(330, 300, 60, 20));
        platforms.add(new Rectangle(30, 220, 60, 20));
        platforms.add(new Rectangle(150, 180, 60, 20));
        platforms.add(new Rectangle(280, 200, 60, 20));
        platforms.add(new Rectangle(380, 240, 60, 20));
        platforms.add(new Rectangle(100, 330, 60, 20));
    }

    private void createObstacles() {
        obstacles.clear();

        obstacles.add(new Rectangle(175, GROUND_Y - 20, 30, 20));
        obstacles.add(new Rectangle(305, GROUND_Y - 20, 28, 20));
        obstacles.add(new Rectangle(420, GROUND_Y - 20, 30, 20));
    }

    private void createCoinSpawnPoints() {
        coinSpawnPoints.clear();

        List<WalkableSegment> walkableSegments = createWalkableSegments();
        boolean[] reachableSegments = findReachableSegments(walkableSegments);
        Rectangle startBounds = new Rectangle(START_X, START_Y, PLAYER_SIZE, PLAYER_SIZE);

        for (int i = 0; i < walkableSegments.size(); i++) {
            if (!reachableSegments[i]) {
                continue;
            }

            WalkableSegment segment = walkableSegments.get(i);
            double width = segment.maxX - segment.minX;
            int spotCount = Math.max(1, (int) Math.floor(width / 90.0) + 1);

            for (int spotIndex = 0; spotIndex < spotCount; spotIndex++) {
                double ratio = (spotIndex + 1.0) / (spotCount + 1.0);
                double playerSpotX = segment.minX + width * ratio;
                int coinX = (int) Math.round(playerSpotX + (PLAYER_SIZE - COIN_SIZE) / 2.0);
                int coinY = (int) Math.round(segment.floorY + PLAYER_SIZE - COIN_SIZE);
                Rectangle candidateCoin = new Rectangle(coinX, coinY, COIN_SIZE, COIN_SIZE);

                if (!candidateCoin.intersects(startBounds) && !intersectsObstacle(candidateCoin)) {
                    coinSpawnPoints.add(new Point(coinX, coinY));
                }
            }
        }

        if (coinSpawnPoints.isEmpty()) {
            coinSpawnPoints.add(new Point(100 - COIN_SIZE / 2, GROUND_Y - COIN_SIZE));
        }
    }

    private List<WalkableSegment> createWalkableSegments() {
        List<WalkableSegment> walkableSegments = new ArrayList<>();

        for (Rectangle platform : platforms) {
            double minX = platform.x;
            double maxX = platform.getMaxX() - PLAYER_SIZE;

            if (maxX < minX) {
                continue;
            }

            List<Interval> blockedIntervals = getBlockedIntervals(platform, minX, maxX);
            addWalkableSegmentsForPlatform(walkableSegments, platform.getY() - PLAYER_SIZE, minX, maxX, blockedIntervals);
        }

        return walkableSegments;
    }

    private List<Interval> getBlockedIntervals(Rectangle platform, double minX, double maxX) {
        List<Interval> blockedIntervals = new ArrayList<>();

        for (Rectangle obstacle : obstacles) {
            boolean sitsOnPlatform = obstacle.y + obstacle.height == platform.y;
            boolean overlapsPlatform = obstacle.x < platform.x + platform.width && obstacle.x + obstacle.width > platform.x;

            if (sitsOnPlatform && overlapsPlatform) {
                double blockedStart = Math.max(minX, obstacle.x - PLAYER_SIZE + 1.0);
                double blockedEnd = Math.min(maxX, obstacle.x + obstacle.width - 1.0);

                if (blockedStart <= blockedEnd) {
                    blockedIntervals.add(new Interval(blockedStart, blockedEnd));
                }
            }
        }

        blockedIntervals.sort(Comparator.comparingDouble(interval -> interval.start));
        return blockedIntervals;
    }

    private void addWalkableSegmentsForPlatform(List<WalkableSegment> walkableSegments, double floorY, double minX,
                                                double maxX, List<Interval> blockedIntervals) {
        double currentStart = minX;

        for (Interval blockedInterval : blockedIntervals) {
            if (blockedInterval.start > currentStart) {
                walkableSegments.add(new WalkableSegment(currentStart, blockedInterval.start - 1.0, floorY));
            }

            currentStart = Math.max(currentStart, blockedInterval.end + 1.0);
        }

        if (currentStart <= maxX) {
            walkableSegments.add(new WalkableSegment(currentStart, maxX, floorY));
        }
    }

    private boolean[] findReachableSegments(List<WalkableSegment> walkableSegments) {
        boolean[] reachableSegments = new boolean[walkableSegments.size()];
        int startSegmentIndex = -1;

        for (int i = 0; i < walkableSegments.size(); i++) {
            if (walkableSegments.get(i).contains(START_X, START_Y)) {
                startSegmentIndex = i;
                break;
            }
        }

        if (startSegmentIndex < 0) {
            return reachableSegments;
        }

        List<Integer> frontier = new ArrayList<>();
        frontier.add(startSegmentIndex);
        reachableSegments[startSegmentIndex] = true;

        for (int cursor = 0; cursor < frontier.size(); cursor++) {
            int sourceSegmentIndex = frontier.get(cursor);

            for (int targetSegmentIndex = 0; targetSegmentIndex < walkableSegments.size(); targetSegmentIndex++) {
                if (reachableSegments[targetSegmentIndex] || sourceSegmentIndex == targetSegmentIndex) {
                    continue;
                }

                if (canReachSegment(walkableSegments, sourceSegmentIndex, targetSegmentIndex)) {
                    reachableSegments[targetSegmentIndex] = true;
                    frontier.add(targetSegmentIndex);
                }
            }
        }

        return reachableSegments;
    }

    private boolean canReachSegment(List<WalkableSegment> walkableSegments, int sourceSegmentIndex, int targetSegmentIndex) {
        WalkableSegment sourceSegment = walkableSegments.get(sourceSegmentIndex);
        WalkableSegment targetSegment = walkableSegments.get(targetSegmentIndex);

        if (targetSegment.floorY < sourceSegment.floorY - getMaximumJumpRise() - 2.0) {
            return false;
        }

        for (double takeoffX : getTakeoffPositions(sourceSegment)) {
            if (simulateJumpToSegment(walkableSegments, sourceSegmentIndex, targetSegmentIndex, takeoffX)) {
                return true;
            }
        }

        return false;
    }

    private List<Double> getTakeoffPositions(WalkableSegment segment) {
        List<Double> takeoffPositions = new ArrayList<>();
        double width = segment.maxX - segment.minX;

        addTakeoffPosition(takeoffPositions, segment.minX);
        addTakeoffPosition(takeoffPositions, segment.maxX);
        addTakeoffPosition(takeoffPositions, segment.minX + width / 2.0);

        if (width > 40) {
            addTakeoffPosition(takeoffPositions, segment.minX + width * 0.25);
            addTakeoffPosition(takeoffPositions, segment.minX + width * 0.75);
        }

        return takeoffPositions;
    }

    private void addTakeoffPosition(List<Double> takeoffPositions, double candidate) {
        for (double existing : takeoffPositions) {
            if (Math.abs(existing - candidate) < 1.0) {
                return;
            }
        }

        takeoffPositions.add(candidate);
    }

    private boolean simulateJumpToSegment(List<WalkableSegment> walkableSegments, int sourceSegmentIndex,
                                          int targetSegmentIndex, double takeoffX) {
        WalkableSegment sourceSegment = walkableSegments.get(sourceSegmentIndex);
        WalkableSegment targetSegment = walkableSegments.get(targetSegmentIndex);

        double simulatedPlayerX = Math.max(sourceSegment.minX, Math.min(takeoffX, sourceSegment.maxX));
        double simulatedPlayerY = sourceSegment.floorY;
        double simulatedVelocityY = JUMP_SPEED;

        for (int step = 0; step < 240; step++) {
            double simulatedVelocityX = getSteeringVelocity(simulatedPlayerX, targetSegment);

            simulatedPlayerX = moveSimulatedPlayerHorizontally(simulatedPlayerX, simulatedPlayerY, simulatedVelocityX);
            if (intersectsObstacle(getBounds(simulatedPlayerX, simulatedPlayerY))) {
                return false;
            }

            VerticalMovementResult movementResult = moveSimulatedPlayerVertically(simulatedPlayerX, simulatedPlayerY,
                    simulatedVelocityY);
            simulatedPlayerY = movementResult.playerY;
            simulatedVelocityY = movementResult.velocityY;

            if (intersectsObstacle(getBounds(simulatedPlayerX, simulatedPlayerY))) {
                return false;
            }

            if (didLandOnTargetOrOtherSegment(walkableSegments, sourceSegmentIndex, targetSegmentIndex,
                    simulatedPlayerX, simulatedPlayerY, movementResult.grounded, step)) {
                return findStandingSegmentIndex(walkableSegments, simulatedPlayerX, simulatedPlayerY) == targetSegmentIndex;
            }
        }

        return false;
    }

    private double moveSimulatedPlayerHorizontally(double simulatedPlayerX, double simulatedPlayerY,
                                                   double simulatedVelocityX) {
        simulatedPlayerX += simulatedVelocityX;
        simulatedPlayerX = resolveHorizontalPosition(simulatedPlayerX, simulatedPlayerY, simulatedVelocityX);
        return Math.max(0, Math.min(simulatedPlayerX, WINDOW_WIDTH - 1.0 * PLAYER_SIZE));
    }

    private VerticalMovementResult moveSimulatedPlayerVertically(double simulatedPlayerX, double simulatedPlayerY,
                                                                 double simulatedVelocityY) {
        simulatedVelocityY = Math.min(simulatedVelocityY + GRAVITY, MAX_FALL_SPEED);
        simulatedPlayerY += simulatedVelocityY;
        return resolveVerticalPosition(simulatedPlayerX, simulatedPlayerY, simulatedVelocityY);
    }

    private boolean didLandOnTargetOrOtherSegment(List<WalkableSegment> walkableSegments, int sourceSegmentIndex,
                                                  int targetSegmentIndex, double simulatedPlayerX,
                                                  double simulatedPlayerY, boolean groundedState, int step) {
        if (!groundedState) {
            return false;
        }

        int landedSegmentIndex = findStandingSegmentIndex(walkableSegments, simulatedPlayerX, simulatedPlayerY);
        return landedSegmentIndex == targetSegmentIndex
                || (landedSegmentIndex >= 0 && landedSegmentIndex != sourceSegmentIndex)
                || (step > 0 && landedSegmentIndex == sourceSegmentIndex);
    }

    private double getSteeringVelocity(double simulatedPlayerX, WalkableSegment targetSegment) {
        if (simulatedPlayerX < targetSegment.minX) {
            return MOVE_SPEED;
        }

        if (simulatedPlayerX > targetSegment.maxX) {
            return -MOVE_SPEED;
        }

        return 0;
    }

    private double resolveHorizontalPosition(double simulatedPlayerX, double simulatedPlayerY, double simulatedVelocityX) {
        Rectangle simulatedPlayerBounds = getBounds(simulatedPlayerX, simulatedPlayerY);

        for (Rectangle platform : platforms) {
            if (!simulatedPlayerBounds.intersects(platform)) {
                continue;
            }

            if (simulatedVelocityX > 0) {
                simulatedPlayerX = platform.getX() - PLAYER_SIZE;
            } else if (simulatedVelocityX < 0) {
                simulatedPlayerX = platform.getMaxX();
            }

            simulatedPlayerBounds = getBounds(simulatedPlayerX, simulatedPlayerY);
        }

        return simulatedPlayerX;
    }

    private VerticalMovementResult resolveVerticalPosition(double simulatedPlayerX, double simulatedPlayerY,
                                                           double simulatedVelocityY) {
        boolean simulatedGrounded = false;
        Rectangle simulatedPlayerBounds = getBounds(simulatedPlayerX, simulatedPlayerY);

        for (Rectangle platform : platforms) {
            if (!simulatedPlayerBounds.intersects(platform)) {
                continue;
            }

            if (simulatedVelocityY > 0) {
                simulatedPlayerY = platform.getY() - PLAYER_SIZE;
                simulatedVelocityY = 0;
                simulatedGrounded = true;
            } else if (simulatedVelocityY < 0) {
                simulatedPlayerY = platform.getMaxY();
                simulatedVelocityY = 0;
            }

            simulatedPlayerBounds = getBounds(simulatedPlayerX, simulatedPlayerY);
        }

        return new VerticalMovementResult(simulatedPlayerY, simulatedVelocityY, simulatedGrounded);
    }

    private int findStandingSegmentIndex(List<WalkableSegment> walkableSegments, double simulatedPlayerX,
                                         double simulatedPlayerY) {
        for (int i = 0; i < walkableSegments.size(); i++) {
            if (walkableSegments.get(i).contains(simulatedPlayerX, simulatedPlayerY)) {
                return i;
            }
        }

        return -1;
    }

    private double getMaximumJumpRise() {
        double jumpRise = 0;
        double simulatedVelocityY = JUMP_SPEED;

        while (simulatedVelocityY < 0) {
            jumpRise += Math.abs(simulatedVelocityY);
            simulatedVelocityY += GRAVITY;
        }

        return jumpRise;
    }

    private void resetRun() {
        playerX = START_X;
        playerY = START_Y;
        velocityX = 0;
        velocityY = 0;
        movingLeft = false;
        movingRight = false;
        jumpPressed = false;
        grounded = true;
        score = 0;
        spawnCoin();
    }

    private void spawnCoin() {
        List<Point> availableSpawnPoints = new ArrayList<>();
        Rectangle playerBounds = getPlayerBounds();

        for (Point spawnPoint : coinSpawnPoints) {
            Rectangle spawnBounds = new Rectangle(spawnPoint.x, spawnPoint.y, COIN_SIZE, COIN_SIZE);
            if (!spawnBounds.intersects(playerBounds)) {
                availableSpawnPoints.add(spawnPoint);
            }
        }

        List<Point> spawnPool = availableSpawnPoints.isEmpty() ? coinSpawnPoints : availableSpawnPoints;
        Point spawnPoint = spawnPool.get(random.nextInt(spawnPool.size()));

        coinBounds = new Rectangle(spawnPoint.x, spawnPoint.y, COIN_SIZE, COIN_SIZE);
        coinDeadline = System.currentTimeMillis() + COIN_TIME_LIMIT_MS;
    }

    private void collectCoin() {
        score += 10;
        spawnCoin();
    }

    private void setupKeyBindings() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        bindKey(inputMap, actionMap, "pressed LEFT", "left-pressed", true, false, false);
        bindKey(inputMap, actionMap, "released LEFT", "left-released", false, false, false);

        bindKey(inputMap, actionMap, "pressed A", "a-pressed", true, false, false);
        bindKey(inputMap, actionMap, "released A", "a-released", false, false, false);

        bindKey(inputMap, actionMap, "pressed RIGHT", "right-pressed", false, true, false);
        bindKey(inputMap, actionMap, "released RIGHT", "right-released", false, false, false);

        bindKey(inputMap, actionMap, "pressed D", "d-pressed", false, true, false);
        bindKey(inputMap, actionMap, "released D", "d-released", false, false, false);

        bindKey(inputMap, actionMap, "pressed SPACE", "space-pressed", false, false, true);
        bindKey(inputMap, actionMap, "released SPACE", "space-released", false, false, false);

        bindKey(inputMap, actionMap, "pressed UP", "up-pressed", false, false, true);
        bindKey(inputMap, actionMap, "released UP", "up-released", false, false, false);

        bindKey(inputMap, actionMap, "pressed W", "w-pressed", false, false, true);
        bindKey(inputMap, actionMap, "released W", "w-released", false, false, false);
    }

    private void bindKey(InputMap inputMap, ActionMap actionMap, String keystroke, String actionName,
                         boolean leftState, boolean rightState, boolean jumpState) {
        inputMap.put(KeyStroke.getKeyStroke(keystroke), actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (keystroke.contains("LEFT") || keystroke.contains("A")) {
                    movingLeft = leftState;
                }

                if (keystroke.contains("RIGHT") || keystroke.contains("D")) {
                    movingRight = rightState;
                }

                if (keystroke.contains("SPACE") || keystroke.contains("UP") || keystroke.contains("W")) {
                    jumpPressed = jumpState;
                }
            }
        });
    }

    private void updateGame() {
        updateHorizontalMovement();
        applyJump();
        applyGravity();
        movePlayerHorizontally();
        movePlayerVertically();
        keepPlayerInsideWindow();

        if (checkObstacleCollision()) {
            repaint();
            return;
        }

        updateCoinState();
        repaint();
    }

    private boolean checkObstacleCollision() {
        if (intersectsObstacle(getPlayerBounds())) {
            resetRun();
            return true;
        }

        return false;
    }

    private void updateCoinState() {
        if (coinBounds != null && getPlayerBounds().intersects(coinBounds)) {
            collectCoin();
            return;
        }

        if (System.currentTimeMillis() >= coinDeadline) {
            resetRun();
        }
    }

    private void updateHorizontalMovement() {
        velocityX = 0;

        if (movingLeft && !movingRight) {
            velocityX = -MOVE_SPEED;
        } else if (movingRight && !movingLeft) {
            velocityX = MOVE_SPEED;
        }
    }

    private void applyJump() {
        if (jumpPressed && grounded) {
            velocityY = JUMP_SPEED;
            grounded = false;
        }
    }

    private void applyGravity() {
        velocityY = Math.min(velocityY + GRAVITY, MAX_FALL_SPEED);
    }

    private void movePlayerHorizontally() {
        playerX += velocityX;

        Rectangle playerBounds = getPlayerBounds();
        for (Rectangle platform : platforms) {
            if (!playerBounds.intersects(platform)) {
                continue;
            }

            if (velocityX > 0) {
                playerX = platform.getX() - PLAYER_SIZE;
            } else if (velocityX < 0) {
                playerX = platform.getMaxX();
            }

            playerBounds = getPlayerBounds();
        }
    }

    private void movePlayerVertically() {
        grounded = false;
        playerY += velocityY;

        Rectangle playerBounds = getPlayerBounds();
        for (Rectangle platform : platforms) {
            if (!playerBounds.intersects(platform)) {
                continue;
            }

            if (velocityY > 0) {
                playerY = platform.getY() - PLAYER_SIZE;
                velocityY = 0;
                grounded = true;
            } else if (velocityY < 0) {
                playerY = platform.getMaxY();
                velocityY = 0;
            }

            playerBounds = getPlayerBounds();
        }
    }

    private void keepPlayerInsideWindow() {
        if (playerX < 0) {
            playerX = 0;
        }

        if (playerX + PLAYER_SIZE > WINDOW_WIDTH) {
            playerX = WINDOW_WIDTH - 1.0 * PLAYER_SIZE;
        }

        if (playerY + PLAYER_SIZE > WINDOW_HEIGHT) {
            playerY = WINDOW_HEIGHT - 1.0 * PLAYER_SIZE;
            velocityY = 0;
            grounded = true;
        }
    }

    private Rectangle getPlayerBounds() {
        return new Rectangle((int) Math.round(playerX), (int) Math.round(playerY), PLAYER_SIZE, PLAYER_SIZE);
    }

    private Rectangle getBounds(double x, double y) {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), PLAYER_SIZE, PLAYER_SIZE);
    }

    private boolean intersectsObstacle(Rectangle rectangle) {
        for (Rectangle obstacle : obstacles) {
            if (rectangle.intersects(obstacle)) {
                return true;
            }
        }

        return false;
    }

    private void drawPlayer(Graphics g) {
        g.setColor(PLAYER_COLOR);
        g.fillRect((int) Math.round(playerX), (int) Math.round(playerY), PLAYER_SIZE, PLAYER_SIZE);

        g.setColor(Color.BLACK);
        g.drawRect((int) Math.round(playerX), (int) Math.round(playerY), PLAYER_SIZE, PLAYER_SIZE);
    }

    private void drawCoin(Graphics g) {
        if (coinBounds == null) {
            return;
        }

        g.setColor(COIN_COLOR);
        g.fillOval(coinBounds.x, coinBounds.y, coinBounds.width, coinBounds.height);

        g.setColor(COIN_SHINE_COLOR);
        g.fillOval(coinBounds.x + 4, coinBounds.y + 3, 6, 6);

        g.setColor(Color.ORANGE.darker());
        g.drawOval(coinBounds.x, coinBounds.y, coinBounds.width, coinBounds.height);
    }

    private void drawObstacles(Graphics g) {
        Graphics2D graphics2D = (Graphics2D) g.create();

        for (Rectangle obstacle : obstacles) {
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

        graphics2D.dispose();
    }

    private void drawHud(Graphics g) {
        double timeLeftSeconds = Math.max(0, coinDeadline - System.currentTimeMillis()) / 1000.0;

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Move: A/D or Arrow Keys   Jump: W / Up / Space", 20, 30);
        g.drawString("Score: " + score, 20, 55);
        g.drawString(String.format("Coin disappears in: %.1fs", timeLeftSeconds), 20, 78);
        g.drawString("Avoid the red spikes.", 20, 101);
    }

    private void drawGround(Graphics g) {
        g.setColor(GROUND_DARK);
        g.fillRect(0, GROUND_Y, WINDOW_WIDTH, WINDOW_HEIGHT - GROUND_Y);

        g.setColor(GROUND_MID);
        g.fillRect(0, GROUND_Y, WINDOW_WIDTH, 20);

        g.setColor(GROUND_LIGHT);
        g.fillRect(0, GROUND_Y, WINDOW_WIDTH, 10);
    }

    private void drawFloatingBlocks(Graphics g) {
        g.setColor(BLOCK_COLOR);

        for (int i = 1; i < platforms.size(); i++) {
            Rectangle platform = platforms.get(i);
            g.fillRect(platform.x, platform.y, platform.width, platform.height);
        }
    }
}

class MainMenu extends JPanel {

    private final JFrame window;

    public MainMenu(JFrame window) {
        this.window = window;

        setLayout(null);
        setBackground(new Color(0, 150, 0));

        createStartButton();
    }

    private void createStartButton() {
        JButton startButton = new JButton("Start");
        startButton.setBounds(150, 300, 200, 100);
        startButton.setBackground(Color.CYAN);

        startButton.addActionListener(e -> startGame());

        add(startButton);
    }

    private void startGame() {
        Game game = new Game();

        window.getContentPane().removeAll();
        window.add(game);

        window.revalidate();
        window.repaint();
        game.requestFocusInWindow();
    }
}

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Java Window");

            window.setSize(500, 500);
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            MainMenu menu = new MainMenu(window);
            window.add(menu);

            window.setResizable(false);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}