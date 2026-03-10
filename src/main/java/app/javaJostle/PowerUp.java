package app.javaJostle;

import java.awt.image.BufferedImage;

public class PowerUp {
    private BufferedImage image;
    private double x;
    private double y;
    private String type;
    public PowerUp(double x, double y) {
        double r = Math.random();
        if(r < .33) {
            this.type = "health";
            this.image = Utilities.loadImage("healthPack.png");
        } else if(r < .66) {
            this.type = "speed";
            this.image = Utilities.loadImage("speedPack.png");
        } else {
            this.type = "attack";
            this.image = Utilities.loadImage("attackPack.png");
        } 
        this.x = x;
        this.y = y;
    }

    public double getX() { // Returns tile X
        return x;
    }

    public double getY() { // Returns tile Y
        return y;
    }

    public BufferedImage getImage() {
        return image;
    }

    public String getType() {
        return type;
    }
}