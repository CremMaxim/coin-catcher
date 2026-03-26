import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

class MainMenu extends JPanel {

    private final JFrame window;
    private final JLabel titleLabel;
    private final JButton startButton;

    public MainMenu(JFrame window) {
        this.window = window;

        setLayout(null);
        setBackground(new Color(0, 150, 0));

        titleLabel = createTitleLabel();
        startButton = createStartButton();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateMenuLayout();
            }
        });

        SwingUtilities.invokeLater(this::updateMenuLayout);
    }

    private JLabel createTitleLabel() {
        JLabel label = new JLabel("Coin Catcher", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 52));
        label.setForeground(Color.WHITE);
        add(label);
        return label;
    }

    private JButton createStartButton() {
        JButton startButton = new JButton("Start");
        startButton.setBackground(Color.CYAN);

        startButton.addActionListener(e -> startGame());

        add(startButton);
        return startButton;
    }

    private void updateMenuLayout() {
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        int titleWidth = Math.min(700, Math.max(360, panelWidth - 80));
        int titleHeight = 90;
        int buttonWidth = 220;
        int buttonHeight = 100;

        int titleX = (panelWidth - titleWidth) / 2;
        int titleY = Math.max(60, panelHeight / 5);
        int buttonX = (panelWidth - buttonWidth) / 2;
        int buttonY = titleY + titleHeight + 60;

        titleLabel.setBounds(titleX, titleY, titleWidth, titleHeight);
        startButton.setBounds(buttonX, buttonY, buttonWidth, buttonHeight);
    }

    private void startGame() {
        GamePanel game = new GamePanel();

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
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            window.setResizable(true);
            window.setSize(1100, 700);
            window.setMinimumSize(new Dimension(800, 500));

            MainMenu menu = new MainMenu(window);
            window.add(menu);

            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}