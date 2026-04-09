package app.background;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

class RestrictedRobotClassLoader extends URLClassLoader {
    private static final Set<String> ALLOWED_PACKAGE_PREFIXES = Set.of(
        "app.background.",
        "app.robots.",
        "java.lang.",
        "java.util."
    );

    private static final Set<String> ALLOWED_EXACT_CLASSES = Set.of(
        "java.lang.invoke.StringConcatFactory",
        "java.lang.invoke.MethodHandles",
        "java.lang.invoke.MethodHandles$Lookup",
        "java.lang.invoke.MethodType",
        "java.lang.invoke.CallSite"
    );

    private static final Set<String> BLOCKED_EXACT_CLASSES = Set.of(
        "java.lang.System",
        "java.lang.Runtime",
        "java.lang.Process",
        "java.lang.ProcessBuilder",
        "java.lang.Thread",
        "java.lang.Runnable",
        "java.lang.ThreadGroup",
        "java.lang.ThreadLocal",
        "java.lang.ClassLoader",
        "java.util.Timer",
        "java.util.TimerTask",
        "java.util.ServiceLoader"
    );

    private static final Set<String> BLOCKED_PACKAGE_MATCHERS = Set.of(
        "java.lang.reflect.",
        "java.lang.invoke.",
        "java.lang.management.",
        "java.util.concurrent.",
        "java.util.logging.",
        "java.util.prefs."
    );

    public RestrictedRobotClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!isAllowedByPackage(name)) {
            throw new ClassNotFoundException("Access denied (not in allowlist): " + name);
        }

        if (isBlocked(name)) {
            throw new ClassNotFoundException("Access denied by policy: " + name);
        }

        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);

            if (loaded == null) {
                if (name.startsWith("app.robots.")) {
                    try {
                        loaded = findClass(name);
                    } catch (ClassNotFoundException e) {
                        loaded = super.loadClass(name, false);
                    }
                } else {
                    loaded = super.loadClass(name, false);
                }
            }

            if (resolve) {
                resolveClass(loaded);
            }

            return loaded;
        }
    }

    private static boolean isBlocked(String name) {
        if (ALLOWED_EXACT_CLASSES.contains(name)) {
            return false;
        }

        for (String blockedClass : BLOCKED_EXACT_CLASSES) {
            if (name.equals(blockedClass)) {
                return true;
            }
        }

        for (String blockedPackage : BLOCKED_PACKAGE_MATCHERS) {
            if (name.contains(blockedPackage)) {
                if (ALLOWED_EXACT_CLASSES.contains(name)) {
                    return false;
                }
                return true;
            }
        }

        return false;
    }

    private static boolean isAllowedByPackage(String name) {
        if (ALLOWED_EXACT_CLASSES.contains(name)) {
            return true;
        }

        for (String prefix : ALLOWED_PACKAGE_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }
}

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

    public static final int GAME_DELAY = 5;

    public static final int SCREEN_WIDTH = 800;
    public static final int SCREEN_HEIGHT = 600;
    public static final int TILE_SIZE = 32; // Default tile size
    public static final int PROJECTILE_SIZE = 10; // Default projectile size
    public static final int POWER_UP_SIZE = 20; // Default power-up size
    public static final int ROBOT_SIZE = 28; // Default robot size

    static ArrayList<Integer> keysPressed = new ArrayList<>();
    
    // Each robot class is loaded by its own isolated RestrictedRobotClassLoader
    private static java.util.Map<String, Class<?>> loadedRobotClasses = new HashMap<>();
    private static java.util.Map<String, RestrictedRobotClassLoader> robotLoaders = new HashMap<>();
    private static final java.util.Map<String, BufferedImage> imageCache = new ConcurrentHashMap<>();
    private static final String ROBOT_PACKAGE = "app.robots";

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

    static void handleKeyPressed(int keyCode) {
        if (!keysPressed.contains(keyCode)) {
            keysPressed.add(keyCode);
        }
    }

    static void handleKeyReleased(int keyCode) {
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
    static void resetKeyStates() {
        Arrays.fill(testKeyStates, false);
    }

    static void setKeyPressed(int keyCode, boolean isPressed) {
        if (keyCode < testKeyStates.length) {
            testKeyStates[keyCode] = isPressed;
        }
    }

    private static boolean inTestMode = false;

    static void setTestMode(boolean enabled) {
        inTestMode = enabled;
    }

    static boolean isKeyPressed(int keyCode) {
        if (inTestMode) {
            return keyCode < testKeyStates.length && testKeyStates[keyCode];
        } else {
            // Original implementation
            return keysPressed.contains(keyCode);
        }
    }

    /**
     * Preload all robot classes from the robots resource directory.
     * Each robot is isolated in its own RestrictedRobotClassLoader.
     */
    static void preloadRobotClasses() {
        File robotsResourceDir = new File("src/main/resources/robots");
        loadedRobotClasses.clear();
        robotLoaders.clear();
        
        if (!robotsResourceDir.exists() || !robotsResourceDir.isDirectory()) {
            System.err.println("Robots resource directory not found: " + robotsResourceDir.getAbsolutePath());
            return;
        }
        
        try {
            // Recursively find and load all .class files as package-qualified names.
            loadClassesFromDirectory(robotsResourceDir, robotsResourceDir);
            
            System.out.println("Preloaded " + loadedRobotClasses.size() + " robot classes in isolated loaders");
        } catch (Exception e) {
            System.err.println("Failed to preload robot classes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Recursively load classes from a directory, each in its own isolated RestrictedRobotClassLoader.
     */
    static void loadClassesFromDirectory(File dir, File rootDir) {
        if (!dir.isDirectory()) {
            return;
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                loadClassesFromDirectory(file, rootDir);
            } else if (file.isFile() && file.getName().endsWith(".class")) {
                String relativeClassPath = rootDir.toPath().relativize(file.toPath()).toString();
                String fullClassName = relativeClassPath
                        .replace(File.separatorChar, '.')
                    .replaceAll("\\.class$", "");

                if (!fullClassName.startsWith(ROBOT_PACKAGE + ".") || fullClassName.contains("$")) {
                    continue;
                }

                String className = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
                
                try {
                    // Create an isolated RestrictedRobotClassLoader for this specific robot
                    URL robotsUrl = rootDir.toURI().toURL();
                    RestrictedRobotClassLoader isolatedLoader = new RestrictedRobotClassLoader(
                            new URL[] { robotsUrl }, 
                            Utilities.class.getClassLoader()
                    );
                    
                    // Store the loader to keep it alive and prevent garbage collection
                    robotLoaders.put(fullClassName, isolatedLoader);
                    
                    // Load the robot class using its isolated loader
                    Class<?> loadedClass = isolatedLoader.loadClass(fullClassName);

                    // Security check: reject classes that escaped to parent/system loader.
                    if (loadedClass.getClassLoader() != isolatedLoader) {
                        throw new SecurityException(
                            "Robot class was not defined by its restricted loader: " + fullClassName +
                            " (actual loader=" + loadedClass.getClassLoader() + ")"
                        );
                    }

                    if (!Robot.class.isAssignableFrom(loadedClass)) {
                        continue;
                    }

                    loadedClass.getConstructor(int.class, int.class);
                    loadedRobotClasses.put(className, loadedClass);
                    System.out.println("Preloaded robot in isolated loader: " + fullClassName + " (cached as " + className + ")");
                } catch (Throwable t) {
                    System.err.println("Failed to preload class " + fullClassName + ": " + t.getMessage());
                }
            }
        }
    }

    static ArrayList<String> getLoadedRobotNames() {
        ArrayList<String> names = new ArrayList<>(loadedRobotClasses.keySet());
        return names;
    }
    
    /**
     * Get a preloaded robot class by simple name.
     */
    static Class<?> getRobotClass(String className) {
        return loadedRobotClasses.get(className);
    }

    static Robot createRobot(int x, int y, String className) {
        // Get the preloaded class from cache
        Class<?> loadedClass = getRobotClass(className);
        
        if (loadedClass == null) {
            System.err.println("Robot class not preloaded: " + className);
            return null;
        }

        if (!(loadedClass.getClassLoader() instanceof RestrictedRobotClassLoader)) {
            System.err.println("Refusing to instantiate robot not loaded by restricted loader: " + className +
                    " (actual loader=" + loadedClass.getClassLoader() + ")");
            return null;
        }
        
        try {
            // loadedRobotClasses only contains validated Robot classes.
            Constructor<?> constructor = loadedClass.getConstructor(int.class, int.class);
            Robot robot = (Robot) constructor.newInstance(x, y);

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