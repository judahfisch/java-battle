package app.javaJostle;

import java.util.ArrayList;

public class Rock extends RobotFilter {
    public Rock(int x, int y){
        super(x, y, 5, 1, 3, 1,"Rock", "rock.png", "rock.png");
        
        // Health: 5, Speed: 1, Attack Speed: 3, Projectile Strength: 1
        // Total = 10
    }

    public void think(final ArrayList<Robot> robots, final ArrayList<Projectile> projectiles, final Map map, final ArrayList<PowerUp> powerups) {
        //rock robot is not smart and doesn't think very well. 
                
    }

}