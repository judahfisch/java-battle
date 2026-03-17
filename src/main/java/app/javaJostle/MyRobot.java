package app.javaJostle;

import java.util.ArrayList;

public class MyRobot extends RobotFilter {
    public MyRobot(int x, int y){
        super(x, y, 3, 2, 2, 3,"MyRobot", "myRobot.png", "defaultProjectile.png");
        
        // Health: 3, Speed: 2, Attack Speed: 2, Projectile Strength: 3
        // Total = 10
    }

    public void think(final ArrayList<Robot> robots, final ArrayList<Projectile> projectiles, final Map map, final ArrayList<PowerUp> powerups) {
        
                
    }
}