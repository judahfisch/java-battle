package app.robots;

import app.background.*;
import java.util.List;

public class MyRobot extends Robot {
    public MyRobot(int x, int y){
        super(x, y, 3, 2, 2, 3,"MyRobot", "myRobot.png", "defaultProjectile.png");
        // Health: 3, Speed: 2, Attack Speed: 2, Projectile Strength: 3
        // Total = 10
    }

    @Override
    public void think(List<Robot> robots, List<Projectile> projectiles, Map map, List<PowerUp> powerups) {
        // Put your think logic here
        
    }
}