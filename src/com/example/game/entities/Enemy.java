
package com.example.game.entities;

import com.example.game.core.*;
import javax.swing.*;
import java.awt.*;

public class Enemy extends AnimatedEntity {
    public static final int FRAME_W = 128;
    public static final int FRAME_H = 128;

    private static final String WALK = "walk";
    private static final String ATTACK = "attack";
    private static final String DEATH = "death";
    
    private static final int HIT_FRAME = 1;
    private static final int ATTACK_RANGE = 120;
    private static final int GROWL_INTERVAL = 2000;
    
    private final Player target;
    private boolean swingUsed = false;
    private int growlElapsed = 0;
    private boolean attackSfxArmed = false;

    public Enemy(int startX, Player target) {
        super(40, 6, 0, 2);
        this.x = startX;
        this.target = target;
        
        // Load animations
        addAnimation(WALK, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Enemy", "_run_minion_c", 8, FRAME_W, FRAME_H), 100));
        addAnimation(ATTACK, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Enemy", "_hit_minion_c", 3, FRAME_W, FRAME_H), 80, false));
        
        addAnimation(DEATH, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Enemy", "_die_minion_c", 3, FRAME_W, FRAME_H), 80, false));
        
        // Load sounds
        addSound("growl", "/assets/sound/small-monster-attack-195712.wav", 0.7f);
        addSound("death", "/assets/sound/goblin-scream-87564.wav", 0.9f);
        addSound("attack", "/assets/sound/mixkit-weak-fast-blow-2145.wav", 0.7f);
        
        setState(WALK);
        startTimer();
    }

    @Override
    protected void setState(String newState) {
        super.setState(newState);
        if (ATTACK.equals(newState)) {
            swingUsed = false;
            attackSfxArmed = false;
        }
    }

    @Override
    protected void update(int dt) {
        if (vanished) return;

        // Periodic growl
        if (!isDead()) {
            growlElapsed += dt;
            if (growlElapsed >= GROWL_INTERVAL) {
                playSound("growl");
                growlElapsed = 0;
            }
        }

        // Death animation
        if (isDead()) {
            Animation anim = getCurrentAnimation();
            if (anim != null) {
                anim.update(dt);
                if (anim.isFinished()) {
                    vanished = true;
                    stopTimer();
                }
            }
            repaint();
            return;
        }


        if (!ATTACK.equals(currentState) && target != null) {
            int dx = target.getXPos() - x;
            facingLeft = dx < 0;
            int dist = Math.abs(dx);
            
            if (dist < ATTACK_RANGE) {
                setState(ATTACK);
            } else {
                setState(WALK);
                x += (dx > 0 ? speed : -speed);
            }
        }

        // Update animation
        Animation anim = getCurrentAnimation();
        if (anim != null) {
            anim.update(dt);
            
            // Attack sound
            if (ATTACK.equals(currentState) && anim.getCurrentIndex() == HIT_FRAME && !attackSfxArmed) {
                playSound("attack");
                attackSfxArmed = true;
            }
            
            // Return to walk after attack
            if (ATTACK.equals(currentState) && anim.isFinished()) {
                setState(WALK);
            }
        }

        repaint();
    }

    @Override
    protected void onDeath() {
        playSound("death");
        setState(DEATH);
    }

    public boolean tryHit(Rectangle targetHitBox) {
        if (!ATTACK.equals(currentState) || swingUsed) return false;
        
        Animation anim = getCurrentAnimation();
        if (anim == null || anim.getCurrentIndex() != HIT_FRAME) return false;

        Rectangle atkBox = new Rectangle(
            facingLeft ? x - 20 : x + FRAME_W - 30,
            y + 20, 40, 60
        );
        
        if (atkBox.intersects(targetHitBox)) {
            swingUsed = true;
            return true;
        }
        return false;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::snapToGround);
    }

    public void snapToGround() {
        int h = (getParent() != null) ? getParent().getHeight() : getHeight();
        y = Math.max(0, h - FRAME_H + 30);
        repaint();
    }

    @Override
    public Rectangle getHitBox() {
        return new Rectangle(x, y, FRAME_W, FRAME_H);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (vanished) return;

        Animation anim = getCurrentAnimation();
        if (anim != null) {
            drawFlipped(g, anim.getCurrentFrame(), x, y, facingLeft);
        }
    }
}
