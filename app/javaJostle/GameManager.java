package app.javaJostle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.text.DecimalFormat;
import java.awt.image.BufferedImage;
import java.io.File;

public class GameManager {
    private ArrayList<String> robotOptions; // Includes "MyRobot" first, then others
    private ArrayList<String> mapOptions;   // Renamed from mapNames for consistency

    private JFrame frame; // Menu frame
    // private JComboBox<String> robotComboBox; // Replaced by multiple
    private JComboBox<String> mapComboBox;
    private JButton startButton;

    // Game-specific fields
    private JFrame gameFrame;
    private Game gamePanel;
    private Timer gameTimer;
    private JLabel timerLabel;
    private JLabel speedLabel;
    private JPanel robotStatusPanel;
    private JFrame gameOverFrame;

    private double cameraX = 0;
    private double cameraY = 0;
    private double zoomFactor = 1.0;
    private double gameSpeedFactor = 1.0;
    private int gameLoopCounter = 0;

    private final int SCROLL_STEP = 32;
    private final int GAME_TIMER_DELAY_MS = 40;
    private final double[] speedFactors = {0.25, 0.5, 1.0, 2.0, 4.0};
    private int currentSpeedIndex = 2;
    private static final DecimalFormat df = new DecimalFormat("0.##");

    private static final int MAX_TOTAL_ROBOT_SLOTS = 16; // Total capacity
    private ArrayList<JComboBox<String>> robotSlotComboBoxesList; // Changed to ArrayList
    private ArrayList<JPanel> robotStatDisplayPanelsList; // Changed to ArrayList
    private JPanel robotSelectionPanel; // Panel to hold all robot slots, will be scrollable
    private int currentRobotSlots = 0;


    public GameManager() {
        this.robotOptions = new ArrayList<>();
        this.mapOptions = new ArrayList<>();
        this.robotSlotComboBoxesList = new ArrayList<>(); // Initialize ArrayList
        this.robotStatDisplayPanelsList = new ArrayList<>(); // Initialize ArrayList
        loadRobotOptions(); // Populates this.robotOptions
        loadMapOptions();   // Populates this.mapOptions
        Utilities.loadImages();
        initMenu();
    }

    private void loadRobotOptions() {
        robotOptions.clear();
        Set<String> foundRobotNames = new HashSet<>();

        robotOptions.add("MyRobot"); // Prioritize "MyRobot"
        foundRobotNames.add("MyRobot");

        File robotsDir = new File("app/resources/robots");
        if (robotsDir.exists() && robotsDir.isDirectory()) {
            File[] files = robotsDir.listFiles();
            if (files != null) {
                List<String> otherRobotNames = new ArrayList<>();
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".class") && !file.getName().contains("$")) {
                        String name = file.getName().substring(0, file.getName().length() - ".class".length());
                        if (!name.equals("MyRobot") && !foundRobotNames.contains(name)) {
                            otherRobotNames.add(name);
                        }
                    }
                }
                Collections.sort(otherRobotNames);
                robotOptions.addAll(otherRobotNames);
            }
        }
        if (robotOptions.size() == 1 && robotOptions.contains("MyRobot") && foundRobotNames.size() == 1 && !(new File(robotsDir, "MyRobot.class").exists())) {
            // If only "MyRobot" is hardcoded and its .class file doesn't exist in the dir,
            // it might be an issue. For now, we assume it's a valid option.
            // If no robots at all (even MyRobot.class is missing), add a fallback.
             if (!new File(robotsDir, "MyRobot.class").exists() && robotOptions.size() <=1 ) { // Check if MyRobot.class exists
                robotOptions.clear(); // Clear if MyRobot was just a placeholder
                robotOptions.add("DefaultBot"); // Fallback
                System.err.println("MyRobot.class not found and no other robots. Added DefaultBot.");
             }
        } else if (robotOptions.isEmpty()){
             robotOptions.add("DefaultBot"); // Fallback
             System.err.println("No robot class files found. Added DefaultBot.");
        }
    }

    private void loadMapOptions() { // Renamed from loadMapNames
        mapOptions.clear();
        File mapsDir = new File("app/resources/maps");
        if (mapsDir.exists() && mapsDir.isDirectory()) {
            File[] files = mapsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".txt")) {
                        String name = file.getName().substring(0, file.getName().length() - ".txt".length());
                        mapOptions.add(name);
                    }
                }
                Collections.sort(mapOptions);
            }
        }
        if (mapOptions.isEmpty()) {
            mapOptions.add("Standard");
            System.err.println("No map files found. Added Standard map.");
        }
    }

    private void initMenu() {
        frame = new JFrame("Java Jostle - Setup Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(Color.DARK_GRAY); // Optional: Darker background for the frame itself

        // Main panel for robot selections, will be scrollable
        robotSelectionPanel = new JPanel();
        robotSelectionPanel.setLayout(new BoxLayout(robotSelectionPanel, BoxLayout.Y_AXIS)); // Vertical layout for rows
        robotSelectionPanel.setBackground(Color.BLACK); // Black background for the selection area
        robotSelectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Initialize with 2 robot slots
        currentRobotSlots = 0; // Reset count
        robotSlotComboBoxesList.clear();
        robotStatDisplayPanelsList.clear();
        addRobotSlotRow(); // Adds the first row (which can contain up to 2 slots)
        
        // If you want exactly 2 slots to start, and addRobotSlotRow adds 2:
        // addRobotSlot(); // Slot 1
        // addRobotSlot(); // Slot 2
        // For a row-based approach (2 per row):
        // We'll call a method that adds a full row or fills the current one.
        // Let's refine addRobotSlot to handle this.
        // For simplicity, let's ensure the first two slots are added.
        // The addRobotSlotRow will handle creating rows.
        // The following will ensure at least two slots are attempted to be added.
        if(currentRobotSlots < 2) addRobotSlotToPanel();
        if(currentRobotSlots < 2) addRobotSlotToPanel();


        JScrollPane scrollPane = new JScrollPane(robotSelectionPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Remove border from scrollpane itself

        // Panel for map selection, add button, and start button
        JPanel bottomControlsPanel = new JPanel(new GridBagLayout());
        bottomControlsPanel.setBackground(Color.DARK_GRAY.darker());
        bottomControlsPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel mapLabelText = new JLabel("Choose Map:");
        mapLabelText.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        bottomControlsPanel.add(mapLabelText, gbc);

        if (this.mapOptions.isEmpty()) {
            this.mapOptions.add("Standard"); // Fallback
        }
        mapComboBox = new JComboBox<>(mapOptions.toArray(new String[0]));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.5;
        bottomControlsPanel.add(mapComboBox, gbc);

        JButton addRobotButton = new JButton("+ Add Robot Slot");
        addRobotButton.setFont(new Font("Arial", Font.PLAIN, 14));
        addRobotButton.addActionListener(e -> {
            if (currentRobotSlots < MAX_TOTAL_ROBOT_SLOTS) {
                addRobotSlotToPanel();
            }
            if (currentRobotSlots >= MAX_TOTAL_ROBOT_SLOTS) {
                addRobotButton.setEnabled(false); // Disable if max reached
            }
        });
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        bottomControlsPanel.add(addRobotButton, gbc);
        
        startButton = new JButton("Begin Jostle!");
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.5;
        bottomControlsPanel.add(startButton, gbc);
        startButton.addActionListener(e -> startGame());


        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomControlsPanel, BorderLayout.SOUTH);

        frame.setMinimumSize(new Dimension(800, 600)); // Adjusted minimum size
        frame.setSize(new Dimension(900, 750)); // Default size, increased width
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel currentRowPanel = null; // Keep track of the current row panel

    private void addRobotSlotRow() {
        currentRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)); // Max 2 slots per row
        currentRowPanel.setOpaque(false); // Inherit black background from robotSelectionPanel
        robotSelectionPanel.add(currentRowPanel);
        robotSelectionPanel.revalidate();
        robotSelectionPanel.repaint();
    }
    
    private void addRobotSlotToPanel() {
        if (currentRobotSlots >= MAX_TOTAL_ROBOT_SLOTS) return;

        // If current row is null or full (has 2 components already), create a new row
        if (currentRowPanel == null || currentRowPanel.getComponentCount() >= 2) {
            addRobotSlotRow();
        }

        ArrayList<String> comboBoxOptions = new ArrayList<>();
        comboBoxOptions.add("None");
        comboBoxOptions.addAll(this.robotOptions);

        JPanel slotPanel = new JPanel();
        slotPanel.setLayout(new BoxLayout(slotPanel, BoxLayout.Y_AXIS));
        slotPanel.setBorder(BorderFactory.createEtchedBorder());
        slotPanel.setOpaque(false); // Make slot panel transparent to show black background
        slotPanel.setAlignmentX(Component.CENTER_ALIGNMENT); 

        JLabel slotLabel = new JLabel("Robot " + (currentRobotSlots + 1));
        slotLabel.setForeground(Color.WHITE);
        slotLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        slotPanel.add(slotLabel);

        JComboBox<String> comboBox = new JComboBox<>(comboBoxOptions.toArray(new String[0]));
        comboBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        // Set a max size for the combobox to prevent it from becoming too wide
        comboBox.setMaximumSize(new Dimension(250, comboBox.getPreferredSize().height));
        robotSlotComboBoxesList.add(comboBox); 

        JPanel statDisplay = new JPanel(new BorderLayout());
        // Increased preferred width for the stat display area
        statDisplay.setPreferredSize(new Dimension(400, 260)); 
        statDisplay.setOpaque(false);
        statDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        robotStatDisplayPanelsList.add(statDisplay); 

        final int slotIndex = currentRobotSlots; 
        comboBox.addActionListener(e -> {
            String selectedName = (String) robotSlotComboBoxesList.get(slotIndex).getSelectedItem();
            updateStatDisplayForSlot(slotIndex, selectedName);
        });
        slotPanel.add(comboBox);
        slotPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        slotPanel.add(statDisplay);

        currentRowPanel.add(slotPanel); 
        currentRobotSlots++;

        updateStatDisplayForSlot(slotIndex, "None"); 

        robotSelectionPanel.revalidate();
        robotSelectionPanel.repaint();
        
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, robotSelectionPanel);
        if (scrollPane != null) {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            SwingUtilities.invokeLater(() -> vertical.setValue(vertical.getMaximum()));
        }
    }


    private void updateStatDisplayForSlot(int slotIndex, String robotName) {
        // Ensure slotIndex is within bounds of the dynamically growing lists
        if (slotIndex < 0 || slotIndex >= robotStatDisplayPanelsList.size()) {
            System.err.println("updateStatDisplayForSlot: slotIndex " + slotIndex + " out of bounds for " + robotStatDisplayPanelsList.size());
            return;
        }
        JPanel displayArea = robotStatDisplayPanelsList.get(slotIndex);
        displayArea.removeAll();
        displayArea.setBackground(Color.BLACK); // Ensure background consistency

        if (robotName != null && !robotName.equals("None")) {
            // Using Utilities.createRobot as per existing code in the prompt context
            Robot robotInstance = Utilities.createRobot(0, 0, robotName); 
            if (robotInstance != null) {
                JPanel statsPanel = createRobotDisplayPanel(robotInstance); // This is your existing method
                statsPanel.setOpaque(false); // Make stats panel transparent if createRobotDisplayPanel doesn't
                displayArea.add(statsPanel, BorderLayout.CENTER);
            } else {
                JLabel errorLabel = new JLabel(robotName + " (preview unavailable)");
                errorLabel.setForeground(Color.ORANGE);
                errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                displayArea.add(errorLabel, BorderLayout.CENTER);
            }
        } else {
             // Optional: display a placeholder if "None" is selected
            JLabel noneLabel = new JLabel("Select a Robot");
            noneLabel.setForeground(Color.GRAY);
            noneLabel.setHorizontalAlignment(SwingConstants.CENTER);
            displayArea.add(noneLabel, BorderLayout.CENTER);
        }
        displayArea.revalidate();
        displayArea.repaint();
    }


    private void startGame() {
        ArrayList<String> selectedRobotList = new ArrayList<>();
        // Iterate through the dynamically added combo boxes
        for (int i = 0; i < robotSlotComboBoxesList.size(); i++) { 
            String selectedName = (String) robotSlotComboBoxesList.get(i).getSelectedItem();
            if (selectedName != null && !selectedName.equals("None")) {
                selectedRobotList.add(selectedName);
            }
        }

        if (selectedRobotList.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select at least one robot to start the game.", "No Robots Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // You might want to enforce a minimum number of robots, e.g., at least 2 for a game.
        // For now, any number > 0 will proceed.

        String selectedMapName = (String) mapComboBox.getSelectedItem();

        if (frame != null) {
            frame.setVisible(false);
            frame.dispose(); // Dispose the menu frame
        }
        if (gameOverFrame != null) {
            gameOverFrame.dispose();
            gameOverFrame = null;
        }
        
        // The rest of your startGame logic (creating Game instance, gameFrame, etc.)
        // ...
        int maxGameDurationSeconds = 300; // Example
        gamePanel = new Game(selectedRobotList, selectedMapName, maxGameDurationSeconds * 1000 / GAME_TIMER_DELAY_MS);
        // ... (rest of gameFrame setup as you had before) ...

        gamePanel.setPreferredSize(new Dimension(Utilities.SCREEN_WIDTH, Utilities.SCREEN_HEIGHT));
        gamePanel.setBackground(Color.BLACK);
        gamePanel.setFocusable(true);

        gameFrame = new JFrame("Java Jostle - Game");
        gameFrame.setBackground(Color.BLACK);
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Or DISPOSE_ON_CLOSE if GameManager persists
        gameFrame.setLayout(new BorderLayout());

        JPanel bottomContainer = new JPanel();
        bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.Y_AXIS));
        bottomContainer.setBackground(Color.DARK_GRAY);

        JPanel controlPanelUI = new JPanel(new GridBagLayout()); // Renamed to avoid conflict
        controlPanelUI.setBackground(Color.GRAY);
        GridBagConstraints cgbc = new GridBagConstraints();
        cgbc.insets = new Insets(2, 5, 2, 5);

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftControls.setOpaque(false);
        JButton zoomInButton = new JButton("+");
        zoomInButton.setFont(new Font("Arial", Font.BOLD, 18));
        zoomInButton.addActionListener(e -> {
            double oldZoomFactor = this.zoomFactor;
            this.zoomFactor *= 1.5;
            adjustCameraForCenteredZoom(oldZoomFactor, this.zoomFactor);
            updateGameDisplayAndRepaint();
            if(gamePanel != null) gamePanel.requestFocusInWindow();
        });
        leftControls.add(zoomInButton);

        JButton zoomOutButton = new JButton("-");
        zoomOutButton.setFont(new Font("Arial", Font.BOLD, 18));
        zoomOutButton.addActionListener(e -> {
            double oldZoomFactor = this.zoomFactor;
            this.zoomFactor = Math.max(0.1, this.zoomFactor / 1.5);
            adjustCameraForCenteredZoom(oldZoomFactor, this.zoomFactor);
            updateGameDisplayAndRepaint();
            if(gamePanel != null) gamePanel.requestFocusInWindow();
        });
        leftControls.add(zoomOutButton);

        cgbc.gridx = 0; cgbc.gridy = 0; cgbc.weightx = 0.33; cgbc.anchor = GridBagConstraints.LINE_START;
        controlPanelUI.add(leftControls, cgbc);

        JPanel centerControls = new JPanel();
        centerControls.setLayout(new BoxLayout(centerControls, BoxLayout.Y_AXIS));
        centerControls.setOpaque(false);
        timerLabel = new JLabel("Time: 00:00:000");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        timerLabel.setForeground(Color.BLUE);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerControls.add(timerLabel);
        speedLabel = new JLabel("Speed: " + df.format(gameSpeedFactor) + "x");
        speedLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        speedLabel.setForeground(Color.WHITE);
        speedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerControls.add(speedLabel);
        cgbc.gridx = 1; cgbc.gridy = 0; cgbc.weightx = 0.34; cgbc.anchor = GridBagConstraints.CENTER;
        controlPanelUI.add(centerControls, cgbc);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);
        JButton speedToggleButton = new JButton("Speed");
        speedToggleButton.setFont(new Font("Arial", Font.PLAIN, 14));
        speedToggleButton.addActionListener(e -> {
            currentSpeedIndex = (currentSpeedIndex + 1) % speedFactors.length;
            gameSpeedFactor = speedFactors[currentSpeedIndex];
            speedLabel.setText("Speed: " + df.format(gameSpeedFactor) + "x");
            if(gamePanel != null) gamePanel.requestFocusInWindow();
        });
        rightControls.add(speedToggleButton);
        cgbc.gridx = 2; cgbc.gridy = 0; cgbc.weightx = 0.33; cgbc.anchor = GridBagConstraints.LINE_END;
        controlPanelUI.add(rightControls, cgbc);

        controlPanelUI.setPreferredSize(new Dimension(Utilities.SCREEN_WIDTH, 60));
        bottomContainer.add(controlPanelUI);

        robotStatusPanel = new JPanel();
        robotStatusPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        robotStatusPanel.setBackground(Color.DARK_GRAY.darker());
        robotStatusPanel.setPreferredSize(new Dimension(Utilities.SCREEN_WIDTH, 100)); // Adjust as needed
        bottomContainer.add(robotStatusPanel);

        gameFrame.add(gamePanel, BorderLayout.CENTER);
        gameFrame.add(bottomContainer, BorderLayout.SOUTH);
        gameFrame.pack();
        gameFrame.setMinimumSize(new Dimension(800, 700)); // Adjusted for new bottom panel
        gameFrame.setLocationRelativeTo(null);


        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                boolean moved = false;
                double scrollAmount = SCROLL_STEP / zoomFactor;
                if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) { cameraX -= scrollAmount; moved = true; }
                else if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) { cameraX += scrollAmount; moved = true; }
                else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) { cameraY -= scrollAmount; moved = true; }
                else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) { cameraY += scrollAmount; moved = true; }
                if (moved) updateGameDisplayAndRepaint();
            }
        });
         gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (gamePanel == null || !gamePanel.isFocusOwner()) {
                    gamePanel.requestFocusInWindow();
                }
                double mousePanelX = e.getX(); double mousePanelY = e.getY();
                double oldZoomFactor = zoomFactor; double worldMouseX, worldMouseY, newZoomFactor;
                if (e.getButton() == MouseEvent.BUTTON1) { newZoomFactor = zoomFactor * 1.5; }
                else if (e.getButton() == MouseEvent.BUTTON3) { newZoomFactor = Math.max(0.1, zoomFactor / 1.5); }
                else { return; }
                worldMouseX = (mousePanelX + cameraX) / oldZoomFactor;
                worldMouseY = (mousePanelY + cameraY) / oldZoomFactor;
                zoomFactor = newZoomFactor;
                cameraX = worldMouseX * newZoomFactor - mousePanelX;
                cameraY = worldMouseY * newZoomFactor - mousePanelY;
                updateGameDisplayAndRepaint();
            }
        });


        gameFrame.setVisible(true);
        gamePanel.requestFocusInWindow();

        if (gameTimer != null && gameTimer.isRunning()) gameTimer.stop();
        gameTimer = new Timer(GAME_TIMER_DELAY_MS, ae -> gameLoop());
        gameTimer.start();
        gameLoopCounter = 0;
        updateGameDisplayAndRepaint(); // Initial paint
    }

    private void adjustCameraForCenteredZoom(double oldZoomFactor, double newZoomFactor) {
        if (gamePanel == null || gamePanel.getWidth() == 0 || gamePanel.getHeight() == 0) return;
        double panelCenterX = gamePanel.getWidth() / 2.0;
        double panelCenterY = gamePanel.getHeight() / 2.0;
        double worldCenterX = (panelCenterX + cameraX) / oldZoomFactor;
        double worldCenterY = (panelCenterY + cameraY) / oldZoomFactor;
        cameraX = worldCenterX * newZoomFactor - panelCenterX;
        cameraY = worldCenterY * newZoomFactor - panelCenterY;
    }

    private void updateRobotStatusDisplay() {
        if (robotStatusPanel == null || gamePanel == null || gamePanel.getRobots() == null) return;
        robotStatusPanel.removeAll();
        for (Robot robot : gamePanel.getRobots()) {
            JPanel singleRobotPanel = new JPanel();
            singleRobotPanel.setOpaque(false);
            singleRobotPanel.setLayout(new BoxLayout(singleRobotPanel, BoxLayout.Y_AXIS));
            singleRobotPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Add border based on robot status
            if (!robot.isSuccessfulThink()) {
                singleRobotPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            } else if (robot.hasSpeedBoost()) {
                singleRobotPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
            } else if (robot.hasAttackBoost()) {
                singleRobotPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
            } else {
                // Optional: set a default border or no border if none of the conditions are met
                // singleRobotPanel.setBorder(null); // Or a default border
            }

            BufferedImage img = robot.getImage();
            JLabel imageLabel = (img != null) ? new JLabel(new ImageIcon(img.getScaledInstance(32, 32, Image.SCALE_SMOOTH))) : new JLabel("No Img");
            imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            singleRobotPanel.add(imageLabel);
            JLabel nameLabel = new JLabel(robot.getName());
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            singleRobotPanel.add(nameLabel);
            HealthBar healthBar = new HealthBar(robot.getHealth(), robot.getMaxHealth());
            healthBar.setPreferredSize(new Dimension(50, 10));
            healthBar.setAlignmentX(Component.CENTER_ALIGNMENT);
            singleRobotPanel.add(healthBar);
            robotStatusPanel.add(singleRobotPanel);
        }
        robotStatusPanel.revalidate();
        robotStatusPanel.repaint();
    }

    private void updateGameDisplayAndRepaint() {
        if (gamePanel != null && gameFrame != null && gameFrame.isVisible()) {
            gamePanel.setDisplayParameters(gamePanel.getWidth(), gamePanel.getHeight(), (int) Math.round(cameraX), (int) Math.round(cameraY), this.zoomFactor);
            gamePanel.repaint();
            if (timerLabel != null) {
                // gamePanel.getDuration() is the number of steps taken
                // gamePanel.getMaxDuration() is the total number of steps for the game
                // GAME_TIMER_DELAY_MS is the duration of one step in milliseconds

                int stepsTaken = gamePanel.getDuration();
                int maxSteps = gamePanel.getMaxDuration();
                int stepsRemaining = maxSteps - stepsTaken;
                if (stepsRemaining < 0) {
                    stepsRemaining = 0;
                }

                long remainingMilliseconds = (long) stepsRemaining * GAME_TIMER_DELAY_MS;

                long minutes = (remainingMilliseconds / 1000) / 60;
                long seconds = (remainingMilliseconds / 1000) % 60;
                long millis = remainingMilliseconds % 1000;

                timerLabel.setText(String.format("Time: %02d:%02d:%03d", minutes, seconds, millis));
            }
            if (speedLabel != null) speedLabel.setText("Speed: " + df.format(gameSpeedFactor) + "x");
            updateRobotStatusDisplay();
        }
    }

    private void gameLoop() {
        if (gamePanel == null) {
            if (gameTimer != null) gameTimer.stop();
            return;
        }

        if (gamePanel.isGameOver()) {
            if (gameTimer != null) gameTimer.stop();
            if (gameOverFrame == null || !gameOverFrame.isVisible()) {
                Robot winner = gamePanel.getWinner();
                showGameOverScreen(winner);
            }
            return;
        }
        
        double stepsToExecute = 0;
        double epsilon = 0.001;
        if (Math.abs(gameSpeedFactor - speedFactors[0]) < epsilon) { if (gameLoopCounter % 4 == 0) stepsToExecute = 1; }
        else if (Math.abs(gameSpeedFactor - speedFactors[1]) < epsilon) { if (gameLoopCounter % 2 == 0) stepsToExecute = 1; }
        else if (Math.abs(gameSpeedFactor - speedFactors[2]) < epsilon) { stepsToExecute = 1; }
        else if (Math.abs(gameSpeedFactor - speedFactors[3]) < epsilon) { stepsToExecute = 2; }
        else if (Math.abs(gameSpeedFactor - speedFactors[4]) < epsilon) { stepsToExecute = 4; }

        for (int i = 0; i < stepsToExecute; i++) {
            if (gamePanel.isGameOver()) break;
            gamePanel.step();
        }
        
        if (gamePanel.isGameOver()) {
            if (gameTimer != null) gameTimer.stop();
            if (gameOverFrame == null || !gameOverFrame.isVisible()) {
                Robot winner = gamePanel.getWinner();
                showGameOverScreen(winner);
            }
        } else {
            updateGameDisplayAndRepaint();
        }
        
        gameLoopCounter++;
        if (gameLoopCounter >= 10000) gameLoopCounter = 0;
    }

    public JPanel createRobotDisplayPanel(Robot robot) {
        JPanel robotDisplayPanel = new JPanel();
        robotDisplayPanel.setLayout(new BoxLayout(robotDisplayPanel, BoxLayout.Y_AXIS));
        robotDisplayPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // Ensure this panel's background is also transparent or matches the slot's background
        robotDisplayPanel.setOpaque(false); 
        // robotDisplayPanel.setBackground(new Color(40, 40, 40)); // Or set to a specific color if preferred over transparent

        JLabel nameLabel = new JLabel(robot.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 24));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        robotDisplayPanel.add(nameLabel);
        robotDisplayPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        BufferedImage img = robot.getImage();
        JLabel imageLabel;
        if (img != null) {
            // Keep image size reasonable, or scale it based on available space
            Image scaledImg = img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            imageLabel = new JLabel(new ImageIcon(scaledImg));
        } else {
            imageLabel = new JLabel("No Image");
            imageLabel.setPreferredSize(new Dimension(100, 100));
            imageLabel.setForeground(Color.WHITE);
        }
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        robotDisplayPanel.add(imageLabel);
        robotDisplayPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel statsContainerPanel = new JPanel();
        statsContainerPanel.setLayout(new BoxLayout(statsContainerPanel, BoxLayout.Y_AXIS));
        statsContainerPanel.setOpaque(false);
        statsContainerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        statsContainerPanel.add(new StatDisplayPanel("Health", robot.getHealthPoints()));
        statsContainerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        statsContainerPanel.add(new StatDisplayPanel("Speed", robot.getSpeedPoints()));
        statsContainerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        statsContainerPanel.add(new StatDisplayPanel("Attack Speed", robot.getAttackSpeedPoints()));
        statsContainerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        statsContainerPanel.add(new StatDisplayPanel("Projectile Str.", robot.getProjectileStrengthPoints()));
        
        robotDisplayPanel.add(statsContainerPanel);
        return robotDisplayPanel;
    }

    private void showGameOverScreen(Robot winner) {
        if (gameTimer != null) gameTimer.stop();
        if (gameFrame != null) { // Ensure gameFrame is disposed
            gameFrame.setVisible(false);
            gameFrame.dispose(); 
            gameFrame = null;
        }

        gameOverFrame = new JFrame("Game Over!");
        gameOverFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gameOverFrame.setSize(500, 650); // Adjusted size for better layout
        gameOverFrame.setLayout(new BorderLayout(10, 10));
        gameOverFrame.getContentPane().setBackground(Color.BLACK);
        // Ensure this frame is disposed if a new game starts or menu is re-initialized
        gameOverFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                gameOverFrame = null; // Allow GC and prevent issues if menu is shown next
            }
        });


        JLabel gameOverLabel = new JLabel("We have a winner!", SwingConstants.CENTER);
        gameOverLabel.setFont(new Font("Arial", Font.BOLD, 36));
        gameOverLabel.setForeground(Color.CYAN);
        gameOverFrame.add(gameOverLabel, BorderLayout.NORTH);

        if (winner != null) {
            JPanel winnerPanel = createRobotDisplayPanel(winner);
            gameOverFrame.add(winnerPanel, BorderLayout.CENTER);
        } else {
            JLabel noWinnerLabel = new JLabel("It's a draw or error!", SwingConstants.CENTER);
            noWinnerLabel.setFont(new Font("Arial", Font.BOLD, 28));
            noWinnerLabel.setForeground(Color.ORANGE);
            gameOverFrame.add(noWinnerLabel, BorderLayout.CENTER);
        }

        JButton continueButton = new JButton("Continue to Menu");
        continueButton.setFont(new Font("Arial", Font.BOLD, 20));
        continueButton.setFocusPainted(false);
        continueButton.addActionListener(e -> {
            if (gameOverFrame != null) {
                 gameOverFrame.dispose();
                 gameOverFrame = null;
            }
            // gameFrame is already disposed
            gamePanel = null; // Allow GC
            if (gameTimer != null) {
                gameTimer.stop(); 
                gameTimer = null;
            }
            
            cameraX = 0; cameraY = 0; zoomFactor = 1.0;
            gameSpeedFactor = 1.0; currentSpeedIndex = 2;
            gameLoopCounter = 0;
            
            // Re-initialize and show the main menu
            loadRobotOptions(); // Refresh options in case files changed
            loadMapOptions();
            initMenu(); 
        });
        // ... rest of showGameOverScreen
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10)); 
        buttonPanel.setOpaque(false);
        buttonPanel.add(continueButton);
        gameOverFrame.add(buttonPanel, BorderLayout.SOUTH);

        gameOverFrame.setLocationRelativeTo(null);
        gameOverFrame.setVisible(true);
    }

    // Inner class for StatDisplayPanel
    class StatDisplayPanel extends JPanel {
        private String statName;
        private int statValue;
        private static final int MAX_POINTS = 5;
        // Increased RECT_WIDTH and LABEL_WIDTH for a wider display
        private static final int RECT_WIDTH = 45; // Increased from 30
        private static final int RECT_HEIGHT = 20; // Slightly increased height
        private static final int RECT_SPACING = 5; // Slightly increased spacing
        private static final int LABEL_WIDTH = 150; // Increased from 130

        public StatDisplayPanel(String statName, int statValue) {
            this.statName = statName;
            this.statValue = statValue;
            // Recalculate panelWidth based on new dimensions
            int panelWidth = LABEL_WIDTH + (RECT_WIDTH + RECT_SPACING) * MAX_POINTS - RECT_SPACING + 20; // Added some padding
            setPreferredSize(new Dimension(panelWidth, RECT_HEIGHT + 10)); // Adjusted preferred height
            setOpaque(false); 
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.WHITE);
            FontMetrics fm = g2d.getFontMetrics();
            int stringY = (RECT_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(statName + ":", 5, stringY);

            int startX = LABEL_WIDTH;
            for (int i = 0; i < MAX_POINTS; i++) {
                if (i < statValue) {
                    g2d.setColor(new Color(0, 220, 255)); // Bright blue/cyan
                    g2d.fillRect(startX + i * (RECT_WIDTH + RECT_SPACING), 0, RECT_WIDTH, RECT_HEIGHT);
                } else {
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.fillRect(startX + i * (RECT_WIDTH + RECT_SPACING), 0, RECT_WIDTH, RECT_HEIGHT);
                }
                g2d.setColor(Color.BLACK); // Border for rectangles
                g2d.drawRect(startX + i * (RECT_WIDTH + RECT_SPACING), 0, RECT_WIDTH, RECT_HEIGHT);
            }
        }
    }

    class HealthBar extends JPanel {
        private int currentHealth;
        private int maxHealth;
        public HealthBar(int currentHealth, int maxHealth) {
            this.currentHealth = currentHealth;
            this.maxHealth = maxHealth;
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (maxHealth <= 0) return;
            int width = getWidth(); int height = getHeight();
            double healthPercentage = (double) currentHealth / maxHealth;
            g.setColor(Color.DARK_GRAY); g.fillRect(0, 0, width, height);
            if (healthPercentage > 0.7) g.setColor(Color.GREEN);
            else if (healthPercentage > 0.3) g.setColor(Color.YELLOW);
            else g.setColor(Color.RED);
            g.fillRect(0, 0, (int) (width * healthPercentage), height);
            g.setColor(Color.BLACK); g.drawRect(0, 0, width - 1, height - 1);
        }
    }
}