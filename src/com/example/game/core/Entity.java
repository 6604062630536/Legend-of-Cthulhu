package com.example.game.core;

import java.awt.*;
import javax.swing.*;

public abstract class Entity extends JPanel {
    protected int hp, maxHp, atk, def, speed;
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
    public int getAtk() { return atk; }

    public void takeDamage(int dmg) {
        if (isDead()) return;
        int real = Math.max(1, dmg - def);
        hp = Math.max(0, hp - real);
        if (hp <= 0) onDeath();
    }

    protected abstract void onDeath();
    public abstract Rectangle getHitBox();
}
