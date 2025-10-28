package com.example.game.entities;

import com.example.game.core.*;
import javax.swing.*;
import java.awt.*;
import javax.sound.sampled.Clip;

public class Cthulu extends Entity {

    public static final int FRAME_W = 192 * 2;
    public static final int FRAME_H = 112 * 2;

    // --- Anim sets ---
    private final Image[] idle   = new Image[9];
    private final Image[] walk   = new Image[12];
    private final Image[] atk1   = new Image[7];
    private final Image[] atk2   = new Image[9];
    private final Image[] death  = new Image[9];

    // --- Pos / AI ---
    private int x, y;
    private boolean facingLeft = true;
    private Player target;

    // --- Audio ---
    private final Clip roarClip;
    private final Clip deathClip;
    private final Clip attackClip; 
    private boolean hasRoared = false;
    private boolean attackSfxArmed = false;

    // --- Loop ---
    private final javax.swing.Timer timer;
    private int frameIndex = 0, elapsed = 0;
    private int interval = 100;

    // --- States ---
    private enum State { IDLE, WALK, ATTACK1, ATTACK2, DEATH }
    private State state = State.IDLE;

    // --- AI params ---
    private boolean chaseLocked = true;   // เริ่มเกมล็อกไว้
    private final int attackRange = 220;  // ระยะเริ่มคอมโบ
    private final int chaseSpeed  = 2;
    private int chaseRange = 700;

    // --- Freeze เมื่อโดน Attack2 ---
    private boolean inHurtFreeze = false;
    private int hurtElapsedMs = 0;
    private static final int HURT_FREEZE_MS = 200;

    // --- Hit window ของแต่ละท่า ---
    private static final int HIT_FRAME_ATK1 = 3;
    private static final int HIT_FRAME_ATK2 = 4;
    private boolean swingHitUsed = false;

    public Cthulu(Player target) {
        super(100, 18, 5, 2);
        this.target = target;

        // โหลดภาพจาก /assets/Cthulhu/
        for (int i = 0; i < idle.length;  i++) idle[i]  = loadFromBoss("idle_",  i + 1);
        for (int i = 0; i < walk.length;  i++) walk[i]  = loadFromBoss("walk_",  i + 1);   // ✅ เพิ่มส่วนนี้
        for (int i = 0; i < atk1.length;  i++) atk1[i]  = loadFromBoss("1atk_",  i + 1);
        for (int i = 0; i < atk2.length;  i++) atk2[i]  = loadFromBoss("2atk_",  i + 1);
        for (int i = 0; i < death.length; i++) death[i] = loadFromBoss("death_", i + 1);

        roarClip  = SoundManager.loadClip("/assets/sound/awake-the-beast-106445.wav");
        deathClip = SoundManager.loadClip("/assets/sound/monster-growl-6311.wav");
        attackClip = SoundManager.loadClip("/assets/sound/mixkit-fast-blow-2144.wav");   
        SoundManager.setVolume(roarClip, 0.8f);
        SoundManager.setVolume(deathClip, 0.8f);
        SoundManager.setVolume(attackClip,0.9f); 

        timer = new javax.swing.Timer(16, e -> update(16));
        timer.start();

        setOpaque(false);
    }

    private Image loadFromBoss(String prefix, int i) {
        String path = "/assets/Cthulhu/" + prefix + i + ".png";
        return new ImageIcon(getClass().getResource(path))
                .getImage().getScaledInstance(FRAME_W, FRAME_H, Image.SCALE_SMOOTH);
    }

    // ---------- API ----------
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void setChaseEnabled(boolean enabled) { this.chaseLocked = !enabled; }
    public void setChaseRange(int range) { this.chaseRange = Math.max(0, range); }

    public void triggerRoar() {
        if (!hasRoared) { SoundManager.play(roarClip); hasRoared = true; }
    }

    public void takeDamageFromPlayer(int dmg, boolean fromAttack2) {
        super.takeDamage(dmg);
        if (!isDead() && fromAttack2) {
            inHurtFreeze = true;
            hurtElapsedMs = 0;
        }
    }

    public boolean tryHit(Rectangle targetHitBox) {
        if (!(state == State.ATTACK1 || state == State.ATTACK2) || swingHitUsed) return false;
        int need = (state == State.ATTACK1 ? HIT_FRAME_ATK1 : HIT_FRAME_ATK2);
        if (frameIndex != need) return false;

        Rectangle atkBox = new Rectangle(
                facingLeft ? x - 40 : x + FRAME_W - 40,
                y + 20, 60, FRAME_H - 40
        );
        if (atkBox.intersects(targetHitBox)) {
            swingHitUsed = true;
            return true;
        }
        return false;
    }

    // ---------- Update ----------
    private void update(int dt) {
        if (isDead()) {
            elapsed += dt;
            if (elapsed >= interval) {
                elapsed = 0; frameIndex++;
                if (frameIndex >= death.length) vanished = true;
            }
            repaint(); return;
        }

        if (inHurtFreeze) {
            hurtElapsedMs += dt;
            if (hurtElapsedMs >= HURT_FREEZE_MS) {
                inHurtFreeze = false;
                hurtElapsedMs = 0;
            }
            repaint(); return;
        }

        // ----- AI -----
        if (state == State.IDLE || state == State.WALK) {
            if (target != null) {
                int dx = target.getXPos() - x;
                facingLeft = dx < 0;
                int dist = Math.abs(dx);

                if (chaseLocked) {
                    setState(State.IDLE);
                } else {
                    if (dist < attackRange) {
                        setState(State.ATTACK1);
                        swingHitUsed = false;
                    } else if (dist < chaseRange) {
                        setState(State.WALK);
                        x += (dx > 0 ? chaseSpeed : -chaseSpeed);
                    } else {
                        setState(State.IDLE);
                    }
                }
            }
        }

        // ----- Animation -----
        elapsed += dt;
        if (elapsed >= interval) {
            elapsed = 0;
            frameIndex++;

            switch (state) {
                case IDLE -> frameIndex %= idle.length;
                case WALK -> frameIndex %= walk.length; // ✅ ใช้ walk animation จริง
                case ATTACK1 -> {
                    if (frameIndex == HIT_FRAME_ATK1) swingHitUsed = false;
                    if (frameIndex == HIT_FRAME_ATK1 && !attackSfxArmed) {
                        SoundManager.play(attackClip);
                        attackSfxArmed = true;
                    }
                    if (frameIndex >= atk1.length) {
                        setState(State.ATTACK2);
                        swingHitUsed = false;
                    }
                }
                case ATTACK2 -> {
                    if (frameIndex == HIT_FRAME_ATK2) swingHitUsed = false;
                    if (frameIndex == HIT_FRAME_ATK2 && !attackSfxArmed) {
                        SoundManager.play(attackClip);
                        attackSfxArmed = true;
                    }
                    if (frameIndex >= atk2.length) {
                        setState(State.IDLE);
                    }
                }
                case DEATH -> {
                    if (frameIndex >= death.length) vanished = true;
                }
            }
        }

        repaint();
    }

    private void setState(State s) {
        if (state != s) {
            state = s;
            frameIndex = 0;
            elapsed = 0;
            if (state == State.ATTACK1 || state == State.ATTACK2) {
                attackSfxArmed = false;
            } else if (state == State.IDLE || state == State.WALK) {
                attackSfxArmed = false;
            }
        }
    }

    @Override
    protected void onDeath() {
        SoundManager.play(deathClip);
        setState(State.DEATH);
    }

    @Override
    public Rectangle getHitBox() {
        return new Rectangle(x + 50, y + 50, FRAME_W - 100, FRAME_H - 50);
    }

    // ---------- Render ----------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (vanished) return;

        Image frame = switch (state) {
            case IDLE   -> idle[Math.min(frameIndex, idle.length  - 1)];
            case WALK   -> walk[Math.min(frameIndex, walk.length  - 1)]; 
            case ATTACK1-> atk1[Math.min(frameIndex, atk1.length  - 1)];
            case ATTACK2-> atk2[Math.min(frameIndex, atk2.length  - 1)];
            case DEATH  -> death[Math.min(frameIndex, death.length - 1)];
        };

        if (facingLeft) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x + FRAME_W, y);
            g2.scale(-1, 1);
            g2.drawImage(frame, 0, 0, this);
            g2.dispose();
        } else {
            g.drawImage(frame, x, y, this);
        }
    }
}
