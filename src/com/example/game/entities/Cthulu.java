// ==================== Cthulu.java ====================
package com.example.game.entities;

import com.example.game.core.*;
import java.awt.*;

public class Cthulu extends AnimatedEntity {
    public static final int FRAME_W = 192 * 2;
    public static final int FRAME_H = 112 * 2;

    private static final String IDLE = "idle";
    private static final String WALK = "walk";
    private static final String ATTACK1 = "attack1";
    private static final String ATTACK2 = "attack2";
    private static final String DEATH = "death";

    private static final int HIT_FRAME_ATK1 = 3;
    private static final int HIT_FRAME_ATK2 = 4;
    private static final int HURT_FREEZE_MS = 200;

    private final Player target;
    private int attackRange = 220;
    private int chaseRange = 700;
    private int chaseSpeed = 2;
    private boolean chaseLocked = true;
    private boolean hasRoared = false;
    
    // ✅ ตัวแปรสำคัญ - ป้องกันดาเมจซ้ำ
    private boolean hitAppliedThisAttack = false;
    
    private boolean attackSfxArmed = false;
    private boolean inHurtFreeze = false;
    private int hurtElapsedMs = 0;

    public Cthulu(Player target) {
        super(100, 18, 5, 2);
        this.target = target;
        this.facingLeft = true;

        // Load animations
        addAnimation(IDLE, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Cthulhu", "idle_", 9, FRAME_W, FRAME_H), 100));
        addAnimation(WALK, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Cthulhu", "walk_", 12, FRAME_W, FRAME_H), 100));
        addAnimation(ATTACK1, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Cthulhu", "1atk_", 7, FRAME_W, FRAME_H), 100, false));
        addAnimation(ATTACK2, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Cthulhu", "2atk_", 9, FRAME_W, FRAME_H), 100, false));
        addAnimation(DEATH, new Animation(
            ResourceLoader.loadAnimationFrames("/assets/Cthulhu", "death_", 9, FRAME_W, FRAME_H), 100, false));

        // Load sounds
        addSound("roar", "/assets/sound/awake-the-beast-106445.wav", 0.8f);
        addSound("death", "/assets/sound/monster-growl-6311.wav", 0.8f);
        addSound("attack", "/assets/sound/mixkit-fast-blow-2144.wav", 0.9f);

        setState(IDLE);
        startTimer();
    }

    @Override
    protected void setState(String newState) {
        super.setState(newState);
        if (ATTACK1.equals(newState) || ATTACK2.equals(newState)) {
            attackSfxArmed = false;
            hitAppliedThisAttack = false;  // ✅ รีเซ็ตทุกครั้งที่เริ่มโจมตีใหม่
        }
    }

    public void setChaseEnabled(boolean enabled) {
        this.chaseLocked = !enabled;
    }

    public void setChaseRange(int range) {
        this.chaseRange = Math.max(0, range);
    }

    public void triggerRoar() {
        if (!hasRoared) {
            playSound("roar");
            hasRoared = true;
        }
    }

    public void takeDamageFromPlayer(int dmg, boolean fromAttack2) {
        super.takeDamage(dmg);
        if (!isDead() && fromAttack2) {
            inHurtFreeze = true;
            hurtElapsedMs = 0;
        }
    }

    public boolean tryHit(Rectangle targetHitBox) {
        // ✅ เช็คว่ากำลังโจมตี และ ยังไม่เคยโดนในท่านี้
        if (!(ATTACK1.equals(currentState) || ATTACK2.equals(currentState))) {
            return false;
        }
        
        if (hitAppliedThisAttack) {
            return false;  // ✅ ท่านี้โดนไปแล้ว ไม่ให้โดนซ้ำ
        }

        Animation anim = getCurrentAnimation();
        if (anim == null) return false;
        
        int currentFrame = anim.getCurrentIndex();
        int hitFrame = ATTACK1.equals(currentState) ? HIT_FRAME_ATK1 : HIT_FRAME_ATK2;
        
        // ✅ ต้องอยู่ในเฟรมที่กำหนดเท่านั้น
        if (currentFrame != hitFrame) return false;

        Rectangle atkBox = new Rectangle(
            facingLeft ? x - 40 : x + FRAME_W - 40,
            y + 20, 60, FRAME_H - 40
        );

        if (atkBox.intersects(targetHitBox)) {
            hitAppliedThisAttack = true;  // ✅ ทำเครื่องหมายว่าโดนแล้ว
            System.out.println("Boss hit player! ATK: " + atk);
            return true;
        }
        return false;
    }

    @Override
    protected void onDeath() {
        playSound("death");
        setState(DEATH);
    }

    @Override
    protected void update(int dt) {
        if (isDead()) {
            Animation anim = getCurrentAnimation();
            if (anim != null) {
                anim.update(dt);
                if (anim.isFinished()) {
                    vanished = true;
                }
            }
            repaint();
            return;
        }

        // Hurt freeze
        if (inHurtFreeze) {
            hurtElapsedMs += dt;
            if (hurtElapsedMs >= HURT_FREEZE_MS) {
                inHurtFreeze = false;
                hurtElapsedMs = 0;
            }
            repaint();
            return;
        }

        // AI
        if (IDLE.equals(currentState) || WALK.equals(currentState)) {
            if (target != null) {
                int dx = target.getXPos() - x;
                facingLeft = dx < 0;
                int dist = Math.abs(dx);

                if (chaseLocked) {
                    setState(IDLE);
                } else {
                    if (dist < attackRange) {
                        setState(ATTACK1);
                    } else if (dist < chaseRange) {
                        setState(WALK);
                        x += (dx > 0 ? chaseSpeed : -chaseSpeed);
                    } else {
                        setState(IDLE);
                    }
                }
            }
        }

        // Update animation
        Animation anim = getCurrentAnimation();
        if (anim != null) {
            anim.update(dt);
            
            int currentFrame = anim.getCurrentIndex();
            
            // Attack sounds
            if (ATTACK1.equals(currentState) && currentFrame == HIT_FRAME_ATK1 && !attackSfxArmed) {
                playSound("attack");
                attackSfxArmed = true;
            } else if (ATTACK2.equals(currentState) && currentFrame == HIT_FRAME_ATK2 && !attackSfxArmed) {
                playSound("attack");
                attackSfxArmed = true;
            }
            
            // Combo transition
            if (ATTACK1.equals(currentState) && anim.isFinished()) {
                setState(ATTACK2);  // ✅ จะรีเซ็ต hitAppliedThisAttack ใน setState
            } else if (ATTACK2.equals(currentState) && anim.isFinished()) {
                setState(IDLE);
            }
        }

        repaint();
    }

    @Override
    public Rectangle getHitBox() {
        return new Rectangle(x + 50, y + 50, FRAME_W - 100, FRAME_H - 50);
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