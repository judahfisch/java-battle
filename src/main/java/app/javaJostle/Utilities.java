package app.javaJostle;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

public class Utilities {
    public static BufferedImage WALL_IMAGE;
    public static BufferedImage GRASS_IMAGE;
    public static BufferedImage MUD_IMAGE;
    public static BufferedImage ROBOT_ERROR;
    public static BufferedImage DEFAULT_PROJECTILE_IMAGE;
    public static BufferedImage HEALTH_PACK_IMAGE;
    public static BufferedImage SPEED_PACK_IMAGE;
    public static BufferedImage ATTACK_PACK_IMAGE;

    public static final int WALL = 0;
    public static final int GRASS = 1;
    public static final int MUD = 2;

    public static final double POWER_UP_SPAWN_CHANCE = 0.003;

    public static final int SCREEN_WIDTH = 800;
    public static final int SCREEN_HEIGHT = 600;
    public static final int TILE_SIZE = 32; // Default tile size
    public static final int PROJECTILE_SIZE = 10; // Default projectile size
    public static final int POWER_UP_SIZE = 20; // Default power-up size
    public static final int ROBOT_SIZE = 28; // Default robot size

    public static ArrayList<Integer> keysPressed = new ArrayList<>();
    
    // Static cache for loaded robot classes and persistent URLClassLoader
    private static URLClassLoader robotClassLoader = null;
    private static Map<String, Class<?>> loadedRobotClasses = new HashMap<>();
    private static final Map<String, BufferedImage> imageCache = new ConcurrentHashMap<>();

    public static BufferedImage loadImage(String imgName) {
        if (imgName == null) {
            return null;
        }

        BufferedImage cached = imageCache.get(imgName);
        if (cached != null) {
            return cached;
        }

        try {
            // Ensure the full path is constructed correctly
            BufferedImage img = ImageIO.read(new File("src/main/resources/images/" + imgName));
            BufferedImage cropped = cropToContent(img);
            imageCache.put(imgName, cropped);
            return cropped;
        } catch (IOException e) {
            System.err.println("Failed to load image: " + imgName + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void loadImages() {
        WALL_IMAGE = loadImage("wall.png");
        GRASS_IMAGE = loadImage("grass.png");
        MUD_IMAGE = loadImage("mud.png");
        ROBOT_ERROR = loadImage("robotError.png");
        DEFAULT_PROJECTILE_IMAGE = loadImage("defaultProjectile.png");

        if (WALL_IMAGE == null) {
            System.err.println("WALL_IMAGE could not be loaded.");
        }
        if (GRASS_IMAGE == null) {
            System.err.println("GRASS_IMAGE could not be loaded.");
        }
        if (MUD_IMAGE == null) {
            System.err.println("MUD_IMAGE could not be loaded.");
        }
    }

    public static BufferedImage cropToContent(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();

        int minX = width;
        int minY = height;
        int maxX = 0;
        int maxY = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = src.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;

                if (alpha > 0) {
                    if (x < minX)
                        minX = x;
                    if (y < minY)
                        minY = y;
                    if (x > maxX)
                        maxX = x;
                    if (y > maxY)
                        maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        return src.getSubimage(minX, minY, (maxX - minX + 1), (maxY - minY + 1));
    }

    public static void handleKeyPressed(int keyCode) {
        if (!keysPressed.contains(keyCode)) {
            keysPressed.add(keyCode);
        }
    }

    public static void handleKeyReleased(int keyCode) {
        for (int i = 0; i < keysPressed.size(); i++) {
            if (keysPressed.get(i) == keyCode) {
                keysPressed.remove(i);
                break;
            }
        }
    }

    // For testing purposes only
    private static boolean[] testKeyStates = new boolean[256];

    // Test helper methods
    public static void resetKeyStates() {
        Arrays.fill(testKeyStates, false);
    }

    public static void setKeyPressed(int keyCode, boolean isPressed) {
        if (keyCode < testKeyStates.length) {
            testKeyStates[keyCode] = isPressed;
        }
    }

    private static boolean inTestMode = false;

    public static void setTestMode(boolean enabled) {
        inTestMode = enabled;
    }

    public static boolean isKeyPressed(int keyCode) {
        if (inTestMode) {
            return keyCode < testKeyStates.length && testKeyStates[keyCode];
        } else {
            // Original implementation
            return keysPressed.contains(keyCode);
        }
    }

    /**
     * Preload all robot classes from the robots resource directory.
     * This should be called once during application startup to make all robots available.
     */
    public static void preloadRobotClasses() {
        File robotsResourceDir = new File("src/main/resources/robots");
        loadedRobotClasses.clear();
        
        if (!robotsResourceDir.exists() || !robotsResourceDir.isDirectory()) {
            System.err.println("Robots resource directory not found: " + robotsResourceDir.getAbsolutePath());
            return;
        }
        
        try {
            // Create the persistent URLClassLoader if not already created
            if (robotClassLoader == null) {
                URL robotsUrl = robotsResourceDir.toURI().toURL();
                robotClassLoader = new URLClassLoader(new URL[] { robotsUrl }, Utilities.class.getClassLoader());
                System.out.println("Created persistent robot class loader: " + robotsUrl);
            }
            
            // Recursively find and load all .class files
            loadClassesFromDirectory(robotsResourceDir, "");
            
            System.out.println("Preloaded " + loadedRobotClasses.size() + " robot classes");
        } catch (Exception e) {
            System.err.println("Failed to preload robot classes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Recursively load classes from a directory.
     */
    private static void loadClassesFromDirectory(File dir, String packagePrefix) {
        if (!dir.isDirectory()) {
            return;
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Recurse into subdirectories
                String newPackagePrefix = packagePrefix.isEmpty() ? file.getName() : packagePrefix + "." + file.getName();
                loadClassesFromDirectory(file, newPackagePrefix);
            } else if (file.isFile() && file.getName().endsWith(".class")) {
                // Load the class
                String className = file.getName().substring(0, file.getName().length() - ".class".length());
                String fullClassName = packagePrefix.isEmpty() ? className : packagePrefix + "." + className;
                
                try {
                    Class<?> loadedClass = robotClassLoader.loadClass(fullClassName);
                    if (!RobotFilter.class.isAssignableFrom(loadedClass)) {
                        continue;
                    }

                    loadedClass.getConstructor(int.class, int.class);
                    loadedRobotClasses.put(className, loadedClass); // Store only verified robot filters
                    System.out.println("Preloaded robot filter: " + fullClassName + " (cached as " + className + ")");
                } catch (Throwable t) {
                    System.err.println("Failed to preload class " + fullClassName + ": " + t.getMessage());
                }
            }
        }
    }

    public static ArrayList<String> getLoadedRobotNames() {
        ArrayList<String> names = new ArrayList<>(loadedRobotClasses.keySet());
        Collections.sort(names);
        return names;
    }
    
    /**
     * Get a preloaded robot class by simple name.
     */
    public static Class<?> getRobotClass(String className) {
        return loadedRobotClasses.get(className);
    }

    public static Robot createRobot(int x, int y, String className) {
        // Get the preloaded class from cache
        Class<?> loadedClass = getRobotClass(className);
        
        if (loadedClass == null) {
            System.err.println("Robot class not preloaded: " + className);
            return null;
        }
        
        try {
            // loadedRobotClasses only contains validated RobotFilter classes.
            Constructor<?> constructor = loadedClass.getConstructor(int.class, int.class);
            RobotFilter filter = (RobotFilter) constructor.newInstance(x, y);
            Robot robot = Robot.createFromFilter(x, y, filter);

            System.out.println("Created robot instance: " + className);
            return robot;
        } catch (NoSuchMethodException e) {
            System.err.println("Constructor (int, int) not found for " + className + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error instantiating robot class " + className + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
}