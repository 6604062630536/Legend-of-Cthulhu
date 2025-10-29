// ==================== Player.java ====================
package com.example.game.entities;

import com.example.game.core.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class Player extends AnimatedEntity {
    public static final int FRAME_W = 240;
    public static final int FRAME_H = 160;

    private static final String IDLE = "idle";
    private static final String RUN = "run";
    private static final String ATTACK1 = "attack1";
    private static final String ATTACK2 = "attack2";
    private static final String DEATH = "death";

    private static final int HIT_FRAME_ATK1 = 2;
    private static final int HIT_FRAME_ATK2 = 3;
    private static final int COMBO_GRACE_MS = 150;
    
    // Attack speed intervals
    private static final int NORMAL_ATK_INTERVAL = 50;   // ความเร็วปกติ
    private static final int DEBUFF_ATK_INTERVAL = 90;   // ช้าลง 1.8 เท่า
    
    private static final int INVINCIBLE_MS = 800;
    private boolean invincible = false;
    private int invincibleElapsed = 0;
    
    // ✅ Debuff system
    private boolean isDebuffed = false;
    
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean comboQueued = false;
    private int comboGraceRemain = 0;

    public Player() {
        super(100, 20, 3, 8);
        
        // Load animations
        addAnimation(IDLE, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Player", "_Idle_r1_c", 10, FRAME_W, FRAME_H), 120));
        addAnimation(RUN, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Player", "_Run_r1_c", 10, FRAME_W, FRAME_H), 50));
        addAnimation(ATTACK1, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Player", "_Attack_r1_c", 4, FRAME_W, FRAME_H), 
            NORMAL_ATK_INTERVAL, false));
        addAnimation(ATTACK2, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Player", "_Attack2_r1_c", 6, FRAME_W, FRAME_H), 
            NORMAL_ATK_INTERVAL, false));
        addAnimation(DEATH, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Player", "_Death_r1_c", 10, FRAME_W, FRAME_H), 90, false));
        
        addSound("sword", "/assets/sound/sword-sound-2-36274.wav", 0.8f);
        addSound("death", "/assets/sound/death-sound-1-165630.wav", 0.9f);
        
        setState(IDLE);
        startTimer();
    }


    public void setDebuffed(boolean debuffed) {
        if (this.isDebuffed != debuffed) {
            this.isDebuffed = debuffed;
            updateAttackSpeed();
            
            if (debuffed) {
                System.out.println("⚠️ Player entered cursed area - Attack speed decreased!");
            } else {
                System.out.println("✅ Player left cursed area - Attack speed normal");
            }
        }
    }
    
    public boolean isDebuffed() {
        return isDebuffed;
    }
    
    // อัปเดตความเร็วโจมตี
    private void updateAttackSpeed() {
        int interval = isDebuffed ? DEBUFF_ATK_INTERVAL : NORMAL_ATK_INTERVAL;
        
        // อัปเดต ATTACK1 และ ATTACK2 animations
        animations.put(ATTACK1, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Player", "_Attack_r1_c", 4, FRAME_W, FRAME_H), 
            interval, false));
        animations.put(ATTACK2, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Player", "_Attack2_r1_c", 6, FRAME_W, FRAME_H), 
            interval, false));
        
        // ถ้ากำลังโจมตีอยู่ ให้รีเซ็ตแอนิเมชัน
        if (ATTACK1.equals(currentState) || ATTACK2.equals(currentState)) {
            Animation anim = getCurrentAnimation();
            if (anim != null) {
                anim.reset();
            }
        }
    }

    public void onKeyPressed(int keyCode) {
        if (DEATH.equals(currentState)) return;
        
        if (keyCode == KeyEvent.VK_A) { 
            leftPressed = true;  
            facingLeft = true;  
        }
        if (keyCode == KeyEvent.VK_D) { 
            rightPressed = true; 
            facingLeft = false; 
        }
        if (keyCode == KeyEvent.VK_J) { 
            attack(); 
        }
    }

    public void onKeyReleased(int keyCode) {
        if (keyCode == KeyEvent.VK_A) leftPressed = false;
        if (keyCode == KeyEvent.VK_D) rightPressed = false;
    }

    private void attack() {
        if (ATTACK1.equals(currentState)) {
            comboQueued = true;
            return;
        }
        if (comboGraceRemain > 0 && !ATTACK2.equals(currentState)) {
            startAttack(ATTACK2);
            return;
        }
        if (!ATTACK2.equals(currentState)) {
            startAttack(ATTACK1);
        }
    }

    private void startAttack(String attackState) {
        setState(attackState);
        comboQueued = false;
        if (ATTACK2.equals(attackState)) {
            comboGraceRemain = 0;
        }
        playSound("sword");
    }

    @Override
    public void takeDamage(int dmg) {
        if (isDead() || invincible) {
            return;
        }
        
        int real = Math.max(1, dmg - def);
        hp = Math.max(0, hp - real);
        
        invincible = true;
        invincibleElapsed = 0;
        
        System.out.println("Player took " + real + " damage. HP: " + hp + "/" + maxHp);
        
        if (hp <= 0) onDeath();
    }

    @Override
    protected void onDeath() {
        setState(DEATH);
        playSound("death");
    }

    @Override
    protected void update(int dt) {
        if (invincible) {
            invincibleElapsed += dt;
            if (invincibleElapsed >= INVINCIBLE_MS) {
                invincible = false;
                invincibleElapsed = 0;
            }
        }
        
        if (!ATTACK1.equals(currentState) && !ATTACK2.equals(currentState) && !DEATH.equals(currentState)) {
            if (leftPressed ^ rightPressed) {
                x += leftPressed ? -speed : speed;
                setState(RUN);
            } else {
                setState(IDLE);
            }
            clampPosition();
        }

        if (comboGraceRemain > 0) {
            comboGraceRemain = Math.max(0, comboGraceRemain - dt);
        }

        Animation anim = getCurrentAnimation();
        if (anim != null) {
            anim.update(dt);
            
            if (ATTACK1.equals(currentState) && anim.isFinished()) {
                comboGraceRemain = COMBO_GRACE_MS;
                if (comboQueued) {
                    startAttack(ATTACK2);
                } else {
                    backToMoveState();
                }
            } else if (ATTACK2.equals(currentState) && anim.isFinished()) {
                backToMoveState();
            } else if (DEATH.equals(currentState) && anim.isFinished()) {
                vanished = true;
            }
        }

        repaint();
    }

    private void backToMoveState() {
        setState((leftPressed || rightPressed) ? RUN : IDLE);
        comboQueued = false;
        comboGraceRemain = 0;
    }

    private void clampPosition() {
        int maxW = getWidth() > 0 ? getWidth() : (getParent() != null ? getParent().getWidth() : 0);
        int maxH = getHeight() > 0 ? getHeight() : (getParent() != null ? getParent().getHeight() : 0);
        if (maxW <= 0 || maxH <= 0) return;

        x = Math.max(0, Math.min(x, maxW - FRAME_W));
        y = Math.max(0, maxH - FRAME_H - 50);
    }

    public boolean isAttacking() { 
        return ATTACK1.equals(currentState) || ATTACK2.equals(currentState); 
    }
    
    public boolean isInAttack2() { 
        return ATTACK2.equals(currentState); 
    }
    
    public boolean isAtHitFrame() {
        Animation anim = getCurrentAnimation();
        if (anim == null) return false;
        
        int frame = anim.getCurrentIndex();
        return (ATTACK1.equals(currentState) && frame == HIT_FRAME_ATK1)
            || (ATTACK2.equals(currentState) && frame == HIT_FRAME_ATK2);
    }

    public int getCurrentFrameIndex() {
        Animation anim = getCurrentAnimation();
        return anim != null ? anim.getCurrentIndex() : 0;
    }
    
    public boolean isInvulnerable() {
        return invincible || isDead();
    }

    @Override
    public Rectangle getHitBox() {
        return new Rectangle(x, y, FRAME_W - 60, FRAME_H - 20);
    }

    public void forceSnapToGround(int panelH, int groundMargin) {
        if (panelH > 0) {
            y = Math.max(0, panelH - FRAME_H - groundMargin);
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (vanished) return;

        Animation anim = getCurrentAnimation();
        if (anim != null) {
            Image frame = anim.getCurrentFrame();
            
            if (invincible && (invincibleElapsed / 100) % 2 == 0) {
                return;
            }
            
            if (isDebuffed) {

            }
            
            drawFlipped(g, frame, x, y, facingLeft);
        }
    }
}