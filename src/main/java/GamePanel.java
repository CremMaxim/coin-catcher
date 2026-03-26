import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Random;

class GamePanel extends JPanel {

    private final Player player;
    private final HazardManager hazards;
    private final WorldMap world;
    private final Random random = new Random();

    private Rectangle coinBounds;
    private int score;
    private long coinDeadline;

    private boolean movingLeft;
    private boolean movingRight;
    private boolean jumpPressed;

    GamePanel() {
        setBackground(GameConfig.SKY_COLOR);
        setLayout(null);
        setFocusable(true);

        hazards = new HazardManager();
        world = new WorldMap(hazards);
        player = new Player();

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

        Graphics2D graphics2D = (Graphics2D) g.create();

        // Scale the logical scene so every element grows/shrinks with window size.
        double scaleX = getWidth() / (double) GameConfig.WINDOW_WIDTH;
        double scaleY = getHeight() / (double) GameConfig.WINDOW_HEIGHT;
        double scale = Math.min(scaleX, scaleY);
        int offsetX = (int) Math.round((getWidth() - GameConfig.WINDOW_WIDTH * scale) / 2.0);
        int offsetY = (int) Math.round((getHeight() - GameConfig.WINDOW_HEIGHT * scale) / 2.0);

        graphics2D.translate(offsetX, offsetY);
        graphics2D.scale(scale, scale);
        graphics2D.setClip(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        world.draw(graphics2D);
        hazards.draw(graphics2D);
        drawCoin(graphics2D);
        drawPlayer(graphics2D);
        drawHud(graphics2D);

        graphics2D.dispose();
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

    private void resetRun() {
        player.reset();
        hazards.reset();
        movingLeft = false;
        movingRight = false;
        jumpPressed = false;
        score = 0;
        spawnCoin();
    }

    private void spawnCoin() {
        coinBounds = world.pickCoinSpawn(random, player.getBounds());
        coinDeadline = System.currentTimeMillis() + GameConfig.COIN_TIME_LIMIT_MS;
    }

    private void updateGame() {
        player.setHorizontalInput(movingLeft, movingRight);
        player.tryJump(jumpPressed);
        player.update(world.getPlatforms());

        hazards.update(player.getBounds());

        if (hazards.intersectsHazard(player.getBounds())) {
            resetRun();
            repaint();
            return;
        }

        updateCoinState();
        repaint();
    }

    private void updateCoinState() {
        if (coinBounds != null && player.getBounds().intersects(coinBounds)) {
            score += 10;
            spawnCoin();
            return;
        }

        if (System.currentTimeMillis() >= coinDeadline) {
            resetRun();
        }
    }

    private void drawPlayer(Graphics g) {
        int x = (int) Math.round(player.getX());
        int y = (int) Math.round(player.getY());

        g.setColor(GameConfig.PLAYER_COLOR);
        g.fillRect(x, y, GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);

        g.setColor(Color.BLACK);
        g.drawRect(x, y, GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);

        // Pupils shift to the side the player is facing.
        int eyeY = y + 9;
        int leftEyeX = x + 7;
        int rightEyeX = x + 17;
        int pupilOffset = player.getFacingDirection() < 0 ? -1 : 1;

        g.setColor(Color.WHITE);
        g.fillOval(leftEyeX, eyeY, 7, 7);
        g.fillOval(rightEyeX, eyeY, 7, 7);

        g.setColor(Color.BLACK);
        g.fillOval(leftEyeX + 2 + pupilOffset, eyeY + 2, 3, 3);
        g.fillOval(rightEyeX + 2 + pupilOffset, eyeY + 2, 3, 3);
    }

    private void drawCoin(Graphics g) {
        if (coinBounds == null) {
            return;
        }

        g.setColor(GameConfig.COIN_COLOR);
        g.fillOval(coinBounds.x, coinBounds.y, coinBounds.width, coinBounds.height);

        g.setColor(GameConfig.COIN_SHINE_COLOR);
        g.fillOval(coinBounds.x + 4, coinBounds.y + 3, 6, 6);

        g.setColor(Color.ORANGE.darker());
        g.drawOval(coinBounds.x, coinBounds.y, coinBounds.width, coinBounds.height);
    }

    private void drawHud(Graphics g) {
        double timeLeftSeconds = Math.max(0, coinDeadline - System.currentTimeMillis()) / 1000.0;

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Move: A/D or Arrow Keys   Jump: W / Up / Space", 20, 30);
        g.drawString("Score: " + score, 20, 55);
        g.drawString(String.format("Coin disappears in: %.1fs", timeLeftSeconds), 20, 78);
        g.drawString("Avoid spikes and chasing enemies.", 20, 101);
    }
}

