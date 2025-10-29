// ==================== Entity.java ====================
package com.example.game.core;

import java.awt.*;
import javax.swing.*;

public abstract class Entity extends JPanel {
    protected int hp, maxHp, atk, def, speed;
    protected int x, y;
    protected boolean facingLeft = false;
    protected boolean vanished = false;

    public Entity(int maxHp, int atk, int def, int speed) {
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.atk = atk;
        this.def = def;
        this.speed = speed;
        setOpaque(false);
    }

    public boolean isDead() { return hp <= 0; }
    public boolean isGone() { return vanished; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getAtk() { return atk; }
    public int getXPos() { return x; }
    public int getYPos() { return y; }

    public void takeDamage(int dmg) {
        if (isDead()) return;
        int real = Math.max(1, dmg - def);
        hp = Math.max(0, hp - real);
        if (hp <= 0) onDeath();
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        repaint();
    }

    protected abstract void onDeath();
    public abstract Rectangle getHitBox();
    
    protected void drawFlipped(Graphics g, Image image, int x, int y, boolean flipH) {
        if (flipH) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x + image.getWidth(null), y);
            g2.scale(-1, 1);
            g2.drawImage(image, 0, 0, this);
            g2.dispose();
        } else {
            g.drawImage(image, x, y, this);
        }
    }
}
