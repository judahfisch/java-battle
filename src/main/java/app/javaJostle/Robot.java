package app.javaJostle;

import java.util.ArrayList;
import java.awt.image.BufferedImage;

public abstract class Robot {
    public static final class ThinkSnapshot {
        private final ArrayList<Robot> robots;
        private final ArrayList<Projectile> projectiles;
        private final Map map;
        private final ArrayList<PowerUp> powerUps;

        private ThinkSnapshot(ArrayList<Robot> robots, ArrayList<Projectile> projectiles, Map map, ArrayList<PowerUp> powerUps) {
            this.robots = robots;
            this.projectiles = projectiles;
            this.map = map;
            this.powerUps = powerUps;
        }
    }

    private static final ThreadLocal<ThinkSnapshot> ACTIVE_THINK_SNAPSHOT = new ThreadLocal<>();

    // attribute points
    private int healthPoints;
    private int speedPoints;
    private int attackSpeedPoints;
    private int projectileStrengthPoints;

    // attribute calculated values
    private int health;
    private int maxHealth;
    private int speed;
    private int attackMaxCooldown;
    protected int attackCurCooldown;
    private int projectileSpeed;
    private int projectileDamage;
    private String name;
    private BufferedImage image;
    private BufferedImage projectileImage;
    private boolean successfulThink = true;

    private int x;
    private int y;

    protected int xMovement;
    protected int yMovement;
    protected int xTarget;
    protected int yTarget;
    protected boolean shoot;

    // Power-up effect fields
    private static final int BOOST_DURATION_TICKS = 150;
    private int speedBoostDuration = 0;
    private int attackBoostDuration = 0;

    private int originalSpeed;
    private int originalProjectileSpeed;
    private int originalProjectileDamage;


    public Robot(int x, int y, int healthPoints, int speedPoints, int attackSpeedPoints, int projectileStrengthPoints,
            String robotName, String imageName, String projectileImageName) {
        // all values need to be from 1-5, summing to 10 in total
        int sum = healthPoints + speedPoints + attackSpeedPoints + projectileStrengthPoints;

        if (healthPoints < 1) {
            throw new IllegalArgumentException("Health points must be at least 1");
        } else if (speedPoints < 1) {
            throw new IllegalArgumentException("Speed points must be at least 1");
        } else if (attackSpeedPoints < 1) {
            throw new IllegalArgumentException("Attack speed points must be at least 1");
        } else if (projectileStrengthPoints < 1) {
            throw new IllegalArgumentException("Projectile strength points must be at least 1");
        } else if (sum != 10) {
            throw new IllegalArgumentException("The sum of all points must equal 10");
        } else if (healthPoints > 5) {
            throw new IllegalArgumentException("Health points must not exceed 5");
        } else if (speedPoints > 5) {
            throw new IllegalArgumentException("Speed points must not exceed 5");
        } else if (attackSpeedPoints > 5) {
            throw new IllegalArgumentException("Attack speed points must not exceed 5");
        } else if (projectileStrengthPoints > 5) {
            throw new IllegalArgumentException("Projectile strength points must not exceed 5");
        }
        this.image = Utilities.loadImage(imageName);
        if (this.image == null) {
            this.image = Utilities.ROBOT_ERROR;
        }
        this.projectileImage = Utilities.loadImage(projectileImageName);
        if (this.projectileImage == null) {
            this.projectileImage = Utilities.DEFAULT_PROJECTILE_IMAGE;
        }

        this.x = x;
        this.y = y;
        this.name = robotName;

        this.healthPoints = healthPoints;
        this.speedPoints = speedPoints;
        this.attackSpeedPoints = attackSpeedPoints;
        this.projectileStrengthPoints = projectileStrengthPoints;

        // give stats based on points
        this.health = 30 + healthPoints * 20; // 50 - 130
        this.maxHealth = this.health;
        this.speed = 2 + speedPoints; // 3 - 7 (int)
        this.attackMaxCooldown = 22 - attackSpeedPoints * 2; // 20 - 12
        this.attackCurCooldown = attackMaxCooldown;
        this.projectileSpeed = 5 + projectileStrengthPoints; // 6 - 10
        this.projectileDamage = 10 + projectileStrengthPoints * 3; // 13 - 25

        this.originalSpeed = this.speed;
        this.originalProjectileSpeed = this.projectileSpeed;
        this.originalProjectileDamage = this.projectileDamage;
    }

    public final boolean canAttack() {
        return attackCurCooldown <= 0;
    }

    public static Robot createFromFilter(int x, int y, RobotFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("RobotFilter cannot be null");
        }
        return new FilterRobot(x, y, filter);
    }

    public static ThinkSnapshot createThinkSnapshot(ArrayList<Robot> robots, ArrayList<Projectile> projectiles, Map map, ArrayList<PowerUp> powerUps, Robot selfRobot) {
        java.util.HashMap<Robot, Robot> ownerMap = new java.util.HashMap<>();
        ArrayList<Robot> robotViews = new ReadOnlyArrayList<>(deepCopyRobots(robots, ownerMap, selfRobot));
        ArrayList<Projectile> projectileViews = new ReadOnlyArrayList<>(deepCopyProjectiles(projectiles, ownerMap, selfRobot));
        Map mapView = deepCopyMap(map);
        ArrayList<PowerUp> powerUpViews = new ReadOnlyArrayList<>(deepCopyPowerUps(powerUps));
        return new ThinkSnapshot(robotViews, projectileViews, mapView, powerUpViews);
    }

    public static void beginThinkContext(ThinkSnapshot snapshot, Robot selfRobot) {
        ACTIVE_THINK_SNAPSHOT.set(snapshot);
    }

    public static void endThinkContext() {
        ACTIVE_THINK_SNAPSHOT.remove();
    }

    private static ThinkSnapshot currentThinkSnapshot() {
        return ACTIVE_THINK_SNAPSHOT.get();
    }

    protected final void shootAtLocation(int x, int y) {
        xTarget = x;
        yTarget = y;
        shoot = true;
    }

    public abstract void think(final ArrayList<Robot> robots, final ArrayList<Projectile> projectiles, final Map map, final ArrayList<PowerUp> powerups);

    private boolean isPointOkay(int pX, int pY, Map gameMap, ArrayList<Robot> allRobots) {
        if (gameMap == null || gameMap.getTiles() == null)
            return false;
        int[][] mapTiles = gameMap.getTiles();
        int mapRows = mapTiles.length;
        if (mapRows == 0)
            return false;
        int mapCols = mapTiles[0].length;
        if (mapCols == 0)
            return false;

        // 1. Map Boundary Check for the point
        if (pX < 0 || pY < 0 || pX >= mapCols * Utilities.TILE_SIZE || pY >= mapRows * Utilities.TILE_SIZE) {
            return false;
        }

        // 2. Wall Tile Check for the point
        int tileCol = (int) (pX / Utilities.TILE_SIZE);
        int tileRow = (int) (pY / Utilities.TILE_SIZE);
        if (tileRow < 0 || tileRow >= mapRows || tileCol < 0 || tileCol >= mapCols) {
            return false;
        }
        if (mapTiles[tileRow][tileCol] == Utilities.WALL) {
            return false;
        }

        // 3. Other Robot Check for the point
        if (allRobots != null) {
            for (Robot otherRobot : allRobots) {
                if (otherRobot == this || !otherRobot.isAlive()) {
                    continue;
                }
                if (pX >= otherRobot.getX() && pX < (otherRobot.getX() + Utilities.ROBOT_SIZE) &&
                        pY >= otherRobot.getY() && pY < (otherRobot.getY() + Utilities.ROBOT_SIZE)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean canMoveTo(int targetX, int targetY, Game game) {
        // Define the 4 corners at this potential new position
        // Top-left, Top-right, Bottom-left, Bottom-right
        int c1x = targetX;
        int c1y = targetY;
        int c2x = targetX + Utilities.ROBOT_SIZE - 1; // Use -1 for inclusive edge if ROBOT_SIZE is a dimension
        int c2y = targetY;
        int c3x = targetX;
        int c3y = targetY + Utilities.ROBOT_SIZE - 1;
        int c4x = targetX + Utilities.ROBOT_SIZE - 1;
        int c4y = targetY + Utilities.ROBOT_SIZE - 1;

        // Use spatial grid: only check robots in cells this bounding box touches
        int minCX = targetX / Utilities.TILE_SIZE;
        int maxCX = (targetX + Utilities.ROBOT_SIZE - 1) / Utilities.TILE_SIZE;
        int minCY = targetY / Utilities.TILE_SIZE;
        int maxCY = (targetY + Utilities.ROBOT_SIZE - 1) / Utilities.TILE_SIZE;
        ArrayList<Robot> nearbyRobots = new ArrayList<>();
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cy = minCY; cy <= maxCY; cy++) {
                for (Robot r : game.getRobotsInCell(cx, cy)) {
                    if (!nearbyRobots.contains(r)) nearbyRobots.add(r);
                }
            }
        }

        return isPointOkay(c1x, c1y, game.getMap(), nearbyRobots) &&
                isPointOkay(c2x, c2y, game.getMap(), nearbyRobots) &&
                isPointOkay(c3x, c3y, game.getMap(), nearbyRobots) &&
                isPointOkay(c4x, c4y, game.getMap(), nearbyRobots);
    }

    private boolean isTileMud(int currentX, int currentY, Map gameMap) {
        if (gameMap == null || gameMap.getTiles() == null)
            return false;
        int[][] mapTiles = gameMap.getTiles();
        int mapRows = mapTiles.length;
        if (mapRows == 0)
            return false;
        int mapCols = mapTiles[0].length;
        if (mapCols == 0)
            return false;

        int[] cornersX = { currentX, currentX + Utilities.ROBOT_SIZE - 1, currentX,
                currentX + Utilities.ROBOT_SIZE - 1 };
        int[] cornersY = { currentY, currentY, currentY + Utilities.ROBOT_SIZE - 1,
                currentY + Utilities.ROBOT_SIZE - 1 };

        for (int i = 0; i < 4; i++) {
            int tileCol = (int) (cornersX[i] / Utilities.TILE_SIZE);
            int tileRow = (int) (cornersY[i] / Utilities.TILE_SIZE);

            if (tileRow >= 0 && tileRow < mapRows && tileCol >= 0 && tileCol < mapCols) {
                if (mapTiles[tileRow][tileCol] == Utilities.MUD) {
                    return true;
                }
            }
        }
        return false;
    }
    public final void applyPowerUpEffect(String type) {
        System.out.println(this.name + " picked up " + type + " power-up!");
        switch (type) {
            case "health":
                this.health += this.maxHealth / 2;
                System.out.println(this.name + " new health: " + this.health);
                break;
            case "speed":
                if (speedBoostDuration == 0) { // Only apply if not already boosted
                    this.originalSpeed = this.speed; // Store current speed if it was somehow changed by other means
                }
                this.speed = this.originalSpeed * 2; // Apply boost based on original
                this.speedBoostDuration = BOOST_DURATION_TICKS;
                System.out.println(this.name + " new speed: " + this.speed + " for " + BOOST_DURATION_TICKS + " ticks.");
                break;
            case "attack":
                if (attackBoostDuration == 0) { // Only apply if not already boosted
                    this.originalProjectileSpeed = this.projectileSpeed;
                    this.originalProjectileDamage = this.projectileDamage;
                }
                this.projectileSpeed = this.originalProjectileSpeed * 2;
                this.projectileDamage = this.originalProjectileDamage * 2;
                this.attackBoostDuration = BOOST_DURATION_TICKS;
                System.out.println(this.name + " new projectile speed: " + this.projectileSpeed + ", new damage: " + this.projectileDamage + " for " + BOOST_DURATION_TICKS + " ticks.");
                break;
            default:
                System.err.println("Unknown power-up type: " + type);
                break;
        }
    }

    private void updatePowerUpEffects() {
        if (speedBoostDuration > 0) {
            speedBoostDuration--;
            if (speedBoostDuration == 0) {
                this.speed = this.originalSpeed; // Revert to original speed
                System.out.println(this.name + " speed boost wore off. Speed reverted to " + this.speed);
            }
        }
        if (attackBoostDuration > 0) {
            attackBoostDuration--;
            if (attackBoostDuration == 0) {
                this.projectileSpeed = this.originalProjectileSpeed; // Revert
                this.projectileDamage = this.originalProjectileDamage; // Revert
                System.out.println(this.name + " attack boost wore off. Projectile stats reverted.");
            }
        }
    }
    public final void step(Game game) {// DONT CHANGE
        if(!isAlive()) {
            return; // If the robot is dead, do nothing
        }
        updatePowerUpEffects();

        if(Math.abs(xMovement) + Math.abs(yMovement) > 1) {
            throw new IllegalArgumentException("You can only move in one direction at a time, use xMovement and yMovement to set the direction");
        }
        // shoot
        if (shoot && canAttack()) {
            Projectile p = new Projectile(x + Utilities.ROBOT_SIZE / 2 - Utilities.PROJECTILE_SIZE / 2, y + Utilities.ROBOT_SIZE / 2 - Utilities.PROJECTILE_SIZE / 2, xTarget, yTarget, projectileSpeed, projectileDamage, projectileImage,
                    this);
            game.addProjectile(p);
            attackCurCooldown = attackMaxCooldown;
        }

        // --- MOVEMENT ---
        int effectiveSpeed = this.speed; // this.speed is int
        if (isTileMud(this.x, this.y, game.getMap())) {
            effectiveSpeed /= 2.0;
        }

        for (int i = 0; i < effectiveSpeed; i++) { // Iterate 'effectiveSpeedSteps' times
            int potentialNextX = x + xMovement;
            int potentialNextY = y + yMovement; // Y remains unchanged for X movement
            if (canMoveTo(potentialNextX, potentialNextY, game)) {
                x = potentialNextX;
                y = potentialNextY; // Update Y only if X movement is successful
            } else {
                break; // Collision detected, stop moving in X
            }
        }

        // --- END MOVEMENT ---

        if (attackCurCooldown > 0) {
            attackCurCooldown--;
        }

        // clear out the things that should be changed in think
        xMovement = 0;
        yMovement = 0;
        xTarget = 0;
        yTarget = 0;
        shoot = false;
    }

    public final int getHealth() {
        return health;
    }

    public final int getMaxHealth() {
        return maxHealth;
    }

    public final int getSpeed() {
        return speed;
    }

    public final String getName() {
        return name;
    }

    public final BufferedImage getImage() {
        return image;
    }

    public final BufferedImage getProjectileImage() {
        return projectileImage;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getXMovement() {
        return xMovement;
    }

    public final int getYMovement() {
        return yMovement;
    }

    public final int getHealthPoints() {
        return healthPoints;
    }

    public final int getSpeedPoints() {
        return speedPoints;
    }

    public final int getAttackSpeedPoints() {
        return attackSpeedPoints;
    }

    public final int getProjectileStrengthPoints() {
        return projectileStrengthPoints;
    }

    public final void takeDamage(int amount) {
        this.health -= amount;
        if (this.health < 0) {
            this.health = 0;
        }
    }

    public final boolean isAlive() {
        return this.health > 0;
    }

    public boolean isSelf() {
        return false;
    }

    public final void setSuccessfulThink(boolean successful) {
       
        this.successfulThink = successful;
    }

    public final boolean isSuccessfulThink() {
        return successfulThink;
    }

    // Getters for power-up effects
    public final boolean hasSpeedBoost() {
        return speedBoostDuration > 0;
    }

    public final boolean hasAttackBoost() {
        return attackBoostDuration > 0;
    }

    private static ArrayList<Robot> deepCopyRobots(ArrayList<Robot> source, java.util.HashMap<Robot, Robot> ownerMap, Robot selfRobot) {
        ArrayList<Robot> copies = new ArrayList<>();
        if (source == null) {
            return copies;
        }

        for (Robot robot : source) {
            RobotView copy = new RobotView(robot, robot == selfRobot);
            copyState(copy, robot);
            ownerMap.put(robot, copy);
            copies.add(copy);
        }
        return copies;
    }

    private static void copyState(Robot target, Robot source) {
        target.health = source.health;
        target.maxHealth = source.maxHealth;
        target.speed = source.speed;
        target.attackCurCooldown = source.attackCurCooldown;
        target.x = source.x;
        target.y = source.y;
        target.xMovement = source.xMovement;
        target.yMovement = source.yMovement;
        target.successfulThink = source.successfulThink;
    }

    private static ArrayList<Projectile> deepCopyProjectiles(ArrayList<Projectile> source, java.util.HashMap<Robot, Robot> ownerMap, Robot selfRobot) {
        ArrayList<Projectile> copies = new ArrayList<>();
        if (source == null) {
            return copies;
        }

        for (Projectile projectile : source) {
            Robot ownerCopy = ownerMap.get(projectile.getOwner());
            if (ownerCopy == null && projectile.getOwner() != null) {
                ownerCopy = new RobotView(projectile.getOwner(), projectile.getOwner() == selfRobot);
                copyState(ownerCopy, projectile.getOwner());
            }

            int targetX = (int) Math.round(projectile.getX() + Math.cos(projectile.getAngle()) * 1000.0);
            int targetY = (int) Math.round(projectile.getY() + Math.sin(projectile.getAngle()) * 1000.0);
            Projectile copy = new Projectile(
                    projectile.getX(),
                    projectile.getY(),
                    targetX,
                    targetY,
                    projectile.getProjectileSpeed(),
                    projectile.getProjectileDamage(),
                    projectile.getProjectileImage(),
                    ownerCopy);
            if (!projectile.isAlive()) {
                copy.destroy();
            }
            copies.add(copy);
        }
        return copies;
    }

    private static Map deepCopyMap(Map source) {
        if (source == null || source.getTiles() == null) {
            return new Map(new int[0][0]);
        }

        int[][] srcTiles = source.getTiles();
        int[][] copiedTiles = new int[srcTiles.length][];
        for (int row = 0; row < srcTiles.length; row++) {
            copiedTiles[row] = srcTiles[row].clone();
        }
        return new Map(copiedTiles);
    }

    private static ArrayList<PowerUp> deepCopyPowerUps(ArrayList<PowerUp> source) {
        ArrayList<PowerUp> copies = new ArrayList<>();
        if (source == null) {
            return copies;
        }

        for (PowerUp powerUp : source) {
            copies.add(new PowerUp(powerUp.getX(), powerUp.getY(), powerUp.getType(), powerUp.getImage()));
        }
        return copies;
    }

    private static final class ReadOnlyArrayList<E> extends ArrayList<E> {
        private ReadOnlyArrayList(java.util.Collection<? extends E> source) {
            super(source);
        }

        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException("Snapshot list is read-only");
        }

        @Override
        public void add(int index, E element) {
            throw new UnsupportedOperationException("Snapshot list is read-only");
        }

        @Override
        public boolean addAll(java.util.Collection<? extends E> c) {
            throw new UnsupportedOperationException("Snapshot list is read-only");
        }

        @Override
        public boolean addAll(int index, java.util.Collection<? extends E> c) {
            throw new UnsupportedOperationException("Snapshot list is read-only");
        }

        @Override
        public E remove(int index) {
            throw new UnsupportedOperationException("Snapshot list is read-only");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Snapshot list is read-only");
        }

        @Override
        public boolean removeAll(java.util.Collection<?> c) {
            throw new UnsupportedOperationException("Snapshot list is read-only");
        }

        @Override
        public boolean retainAll(java.util.Collection<?> c) {
            throw new UnsupportedOperationException("Snapshot list is read-only");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Snapshot list is read-only");
        }

        @Override
        public E set(int index, E element) {
            throw new UnsupportedOperationException("Snapshot list is read-only");
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException("Snapshot list is read-only");
        }
    }

    private static final class RobotView extends Robot {
        private final boolean self;

        private RobotView(Robot source, boolean self) {
            super(source.getX(), source.getY(),
                    source.getHealthPoints(),
                    source.getSpeedPoints(),
                    source.getAttackSpeedPoints(),
                    source.getProjectileStrengthPoints(),
                    source.getName(),
                    "robotError.png",
                    "defaultProjectile.png");
            this.self = self;
        }

        @Override
        public void think(final ArrayList<Robot> robots, final ArrayList<Projectile> projectiles, final Map map,
            final ArrayList<PowerUp> powerups) {
            // Snapshot robot; no thinking behavior.
        }

        @Override
        public boolean isSelf() {
            return self;
        }
    }

    private static final class FilterRobot extends Robot {
        private final RobotFilter filter;

        private FilterRobot(int x, int y, RobotFilter filter) {
            super(x, y,
                    filter.getHealthPoints(),
                    filter.getSpeedPoints(),
                    filter.getAttackSpeedPoints(),
                    filter.getProjectileStrengthPoints(),
                    filter.getName(),
                    filter.getImageName(),
                    filter.getProjectileImageName());
            this.filter = filter;
        }

        @Override
        public void think(final ArrayList<Robot> robots,
            final ArrayList<Projectile> projectiles,
            final Map map,
            final ArrayList<PowerUp> powerups) {
            ThinkSnapshot snapshot = currentThinkSnapshot();
            ArrayList<Robot> robotViews;
            ArrayList<Projectile> projectileViews;
            Map mapView;
            ArrayList<PowerUp> powerUpViews;

            if (snapshot != null) {
                robotViews = snapshot.robots;
                projectileViews = snapshot.projectiles;
                mapView = snapshot.map;
                powerUpViews = snapshot.powerUps;
            } else {
                java.util.HashMap<Robot, Robot> ownerMap = new java.util.HashMap<>();
                robotViews = deepCopyRobots(robots, ownerMap, this);
                projectileViews = deepCopyProjectiles(projectiles, ownerMap, this);
                mapView = deepCopyMap(map);
                powerUpViews = deepCopyPowerUps(powerups);
            }

            filter.canAttack = canAttack();
            filter.x = getX();
            filter.y = getY();
            filter.speed = getSpeed();
            filter.health = getHealth();
            filter.maxHealth = getMaxHealth();

            filter.think(robotViews, projectileViews, mapView, powerUpViews);

            xMovement = filter.getXMovement();
            yMovement = filter.getYMovement();
            if (filter.canShoot()) {
                shootAtLocation(filter.getXTarget(), filter.getYTarget());
            }

            filter.xMovement = 0;
            filter.yMovement = 0;
            filter.shoot = false;
            filter.xTarget = 0;
            filter.yTarget = 0;
        }
    }
}