package app.javaJostle;

import java.util.ArrayList;

public abstract class RobotFilter {
    private final int healthPoints;
    private final int speedPoints;
    private final int attackSpeedPoints;
    private final int projectileStrengthPoints;
    private final String name;
    private final String imageName;
    private final String projectileImageName;

    protected int xMovement;
    protected int yMovement;
    protected int xTarget;
    protected int yTarget;
    protected boolean shoot;
    protected int x;
    protected int y;
    protected int speed;
    protected int health;
    protected int maxHealth;
    protected boolean canAttack;

    protected RobotFilter(int x, int y, int healthPoints, int speedPoints, int attackSpeedPoints, int projectileStrengthPoints, String name, String imageName, String projectileImageName) {
        this.x = x;
        this.y = y;
        this.healthPoints = healthPoints;
        this.speedPoints = speedPoints;
        this.attackSpeedPoints = attackSpeedPoints;
        this.projectileStrengthPoints = projectileStrengthPoints;
        this.name = name;
        this.imageName = imageName;
        this.projectileImageName = projectileImageName;
    }

    public final int getXMovement() {
        return xMovement;
    }

    public final int getYMovement() {
        return yMovement;
    }

    public final boolean canShoot() {
        return shoot;
    }

    final int getHealthPoints() {
        return healthPoints;
    }

    final int getSpeedPoints() {
        return speedPoints;
    }

    final int getAttackSpeedPoints() {
        return attackSpeedPoints;
    }

    final int getProjectileStrengthPoints() {
        return projectileStrengthPoints;
    }

    final String getName() {
        return name;
    }

    final String getImageName() {
        return imageName;
    }

    final String getProjectileImageName() {
        return projectileImageName;
    }

    public final int getXTarget() {
        return xTarget;
    }

    public final int getYTarget() {
        return yTarget;
    }

    protected final void shootAtLocation(int x, int y) {
        shoot = true;
        xTarget = x;
        yTarget = y;
    }

    public abstract void think(final ArrayList<Robot> robots, final ArrayList<Projectile> projectiles, final Map map, final ArrayList<PowerUp> powerups);
}
