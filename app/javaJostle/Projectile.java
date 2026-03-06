package app.javaJostle;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Projectile {
    private double x;
    private double y;
    // private double speed; // Replaced by projectileSpeed
    private double angle; // Will be calculated
    private Robot owner;
    private boolean alive = true;

    // New fields based on the constructor
    private int projectileSpeed;
    private int projectileDamage;
    private BufferedImage projectileImage;

    public Projectile(double x, double y, int xTarget, int yTarget, int projectileSpeed, int projectileDamage,
            BufferedImage projectileImage, Robot owner) {
        this.x = x;
        this.y = y;
        this.projectileSpeed = projectileSpeed;
        this.projectileDamage = projectileDamage;
        this.projectileImage = projectileImage;
        this.owner = owner;

        // Calculate the angle towards the target
        this.angle = Math.atan2(yTarget - this.y, xTarget - this.x);
    }

    public void update(Game game) {
        if (!alive) {
            return; // Do nothing if already destroyed
        }

        int subSteps = 5; // Number of steps to break the movement into for collision detection
        double totalDx = this.projectileSpeed * Math.cos(this.angle);
        double totalDy = this.projectileSpeed * Math.sin(this.angle);

        double subStepDx = totalDx / subSteps;
        double subStepDy = totalDy / subSteps;

        for (int i = 0; i < subSteps; i++) {
            if (!alive)
                break; // Stop if destroyed in a previous sub-step

            double currentProjectileX = x + subStepDx;
            double currentProjectileY = y + subStepDy;

            // Define the four corners of the projectile for collision detection
            // Top-left is (currentProjectileX, currentProjectileY)
            // Top-right is (currentProjectileX + PROJECTILE_SIZE -1, currentProjectileY)
            // Bottom-left is (currentProjectileX, currentProjectileY + PROJECTILE_SIZE -1)
            // Bottom-right is (currentProjectileX + PROJECTILE_SIZE -1, currentProjectileY + PROJECTILE_SIZE -1)
            // We will check these points. Note: PROJECTILE_SIZE is the dimension.
            // For tile checking, we care about any point within the projectile's area.
            // For robot checking, we'll use AABB intersection.

            // 1. Wall Collision Check
            Map map = game.getMap();
            if (map != null && map.getTiles() != null && map.getTiles().length > 0 && map.getTiles()[0].length > 0) {
                // Points to check for wall collision (corners of the projectile)
                double[] pX = {
                    currentProjectileX, 
                    currentProjectileX + Utilities.PROJECTILE_SIZE -1, 
                    currentProjectileX, 
                    currentProjectileX + Utilities.PROJECTILE_SIZE -1
                };
                double[] pY = {
                    currentProjectileY, 
                    currentProjectileY, 
                    currentProjectileY + Utilities.PROJECTILE_SIZE -1, 
                    currentProjectileY + Utilities.PROJECTILE_SIZE -1
                };

                for (int corner = 0; corner < 4; corner++) {
                    int tileCol = (int) (pX[corner] / Utilities.TILE_SIZE);
                    int tileRow = (int) (pY[corner] / Utilities.TILE_SIZE);

                    // Check map boundaries and wall collision for each corner
                    if (tileRow < 0 || tileRow >= map.getTiles().length ||
                            tileCol < 0 || tileCol >= map.getTiles()[0].length ||
                            map.getTiles()[tileRow][tileCol] == Utilities.WALL) {
                        this.destroy();
                        return; // Projectile is destroyed, stop further processing
                    }
                }
            }

            // 2. Robot Collision Check (AABB Intersection)
            ArrayList<Robot> robots = game.getRobots();
            if (robots != null) {
                for (Robot robot : robots) {
                    if (robot.isAlive() && robot != this.owner) {
                        double robotX = robot.getX();
                        double robotY = robot.getY();
                        
                        // Projectile's bounding box
                        double projX1 = currentProjectileX;
                        double projY1 = currentProjectileY;
                        double projX2 = currentProjectileX + Utilities.PROJECTILE_SIZE;
                        double projY2 = currentProjectileY + Utilities.PROJECTILE_SIZE;

                        // Robot's bounding box
                        double robX1 = robotX;
                        double robY1 = robotY;
                        double robX2 = robotX + Utilities.ROBOT_SIZE;
                        double robY2 = robotY + Utilities.ROBOT_SIZE;

                        // Check for AABB intersection
                        if (projX1 < robX2 && projX2 > robX1 &&
                            projY1 < robY2 && projY2 > robY1) {
                            
                            robot.takeDamage(this.projectileDamage);
                            this.destroy();
                            return; // Projectile is destroyed, stop further processing
                        }
                    }
                }
            }

            // If no collision in this sub-step, update position
            x = currentProjectileX;
            y = currentProjectileY;
        }
    }

    public void destroy() {
        this.alive = false;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    // Getter for the calculated angle
    public double getAngle() {
        return angle;
    }

    public Robot getOwner() {
        return owner;
    }

    public int getProjectileSpeed() {
        return projectileSpeed;
    }

    public int getProjectileDamage() {
        return projectileDamage;
    }

    public BufferedImage getProjectileImage() {
        return projectileImage;
    }
    public boolean isAlive() {
        return alive;
    }
}