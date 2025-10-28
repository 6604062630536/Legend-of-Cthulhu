package com.example.game.entities;

import com.example.game.core.*;
import javax.swing.*;
import java.awt.*;
import javax.sound.sampled.Clip;

public class Enemy extends Entity {
    public static final int FRAME_W = 128;
    public static final int FRAME_H = 128;

    private final Image[] walkFrames = new Image[8];
    private final Image[] hitFrames  = new Image[3];

    private int x, y;
    private int frameIndex = 0, elapsed = 0;
    private static final int WALK_INTERVAL = 100;
    private static final int HIT_INTERVAL  = 80;
    private int interval = WALK_INTERVAL;

    private int attackRange = 120;
    private boolean facingLeft = true;
    private boolean swingUsed = false;

    private final Player target;
    private final Timer timer;

    // sounds
    private final Clip growlClip, deathClip , attackClip;
    private int growlElapsed = 0;
    private boolean attackSfxArmed = false;
    private static final int GROWL_INTERVAL = 2000;

    private enum State { WALK, ATTACK, DEATH }
    private State state = State.WALK;

    public Enemy(int startX, Player target) {
        super(40, 6, 0, 2);
        this.x = startX;
        this.target = target;
        setOpaque(false);

        // โหลด “วิ่ง” (อย่าเขียนทับด้วย idle)
        for (int i = 0; i < walkFrames.length; i++) {
            walkFrames[i] = loadImg("/assets/Enemy/_run_minion_c" + (i + 1) + ".png");
        }
        for (int i = 0; i < hitFrames.length; i++) {
            hitFrames[i] = loadImg("/assets/Enemy/_hit_minion_c" + (i + 1) + ".png");
        }

        growlClip = SoundManager.loadClip("/assets/sound/small-monster-attack-195712.wav");
        deathClip = SoundManager.loadClip("/assets/sound/goblin-scream-87564.wav");
        attackClip = SoundManager.loadClip("/assets/sound/mixkit-weak-fast-blow-2145.wav");
        SoundManager.setVolume(growlClip, 0.7f);
        SoundManager.setVolume(deathClip, 0.9f);
        SoundManager.setVolume(attackClip, 0.7f);

        timer = new Timer(16, e -> update(16));
        timer.start();
    }

    private Image loadImg(String path) {
        var url = getClass().getResource(path);
        if (url == null) throw new IllegalArgumentException("Missing resource: " + path);
        return new ImageIcon(url).getImage().getScaledInstance(FRAME_W, FRAME_H, Image.SCALE_SMOOTH);
    }

    private void setState(State s) {
        if (state != s) {
            state = s;
            frameIndex = 0;
            elapsed = 0;
            interval = (s == State.ATTACK) ? HIT_INTERVAL : WALK_INTERVAL;
            if (s == State.ATTACK) {
                swingUsed = false;
                attackSfxArmed = false;      
            } else {
                attackSfxArmed = false;      
            }
        }
    }

    private void update(int dt) {
        if (vanished) return;

        // เสียงร้องทุก ๆ 5 วิ (ถ้ายังไม่ตาย)
        if (!isDead()) {
            growlElapsed += dt;
            if (growlElapsed >= GROWL_INTERVAL) {
                SoundManager.play(growlClip);
                growlElapsed = 0;
            }
        }

        // ตาย → เล่นท่าตายจนสุด แล้วหาย
        if (isDead()) {
            elapsed += dt;
            if (elapsed >= HIT_INTERVAL) {
                elapsed = 0;
                if (frameIndex < hitFrames.length - 1) frameIndex++;
                else { vanished = true; timer.stop(); }
            }
            repaint();
            return;
        }

        // --- AI (ไม่ยกเลิกท่าโจมตีกลางคัน) ---
        if (state != State.ATTACK && target != null) {
            int dx = target.getXPos() - x;
            facingLeft = dx < 0;
            int dist = Math.abs(dx);
            if (dist < attackRange) setState(State.ATTACK);
            else { setState(State.WALK); x += (dx > 0 ? speed : -speed); }
        }

        // --- อัปเดตเฟรม ---
        elapsed += dt;
        if (elapsed >= interval) {
            elapsed = 0;
            if (state == State.WALK) {
                frameIndex = (frameIndex + 1) % walkFrames.length;
            } else if (state == State.ATTACK) {
                if (frameIndex < hitFrames.length - 1) frameIndex++;
                else setState(State.WALK); // จบท่าโจมตีแล้วกลับไปเดิน
            }
        }
        
        if (state == State.ATTACK) {
            final int HIT_FRAME = 1; // <<-- ปรับให้ตรงกับเฟรมที่คุณใช้ทำดาเมจ
            if (frameIndex == HIT_FRAME && !attackSfxArmed) {
                SoundManager.play(attackClip);
                attackSfxArmed = true;
            }
        }

        repaint();
    }

    @Override
    public void takeDamage(int dmg) {
        super.takeDamage(dmg);
    }

    @Override
    protected void onDeath() {
        SoundManager.play(deathClip);
        setState(State.DEATH);
    }

    /** สร้างดาเมจให้ผู้เล่น: true เมื่อโดนในเฟรมโจมตี */
    public boolean tryHit(Rectangle targetHitBox) {
        if (state != State.ATTACK || swingUsed) return false;
        // เฟรมกลางของท่าโจมตี (ปรับได้)
        int HIT_FRAME = 1;
        if (frameIndex != HIT_FRAME) return false;

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
    
    @Override public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::snapToGround); // ให้ snap หลังรู้ขนาด parent
    }
    public void snapToGround() {
        int h = (getParent() != null) ? getParent().getHeight() : getHeight();
        y = Math.max(0, h - FRAME_H + 30 );
        repaint();
    }

    @Override public Rectangle getHitBox() { return new Rectangle(x, y, FRAME_W, FRAME_H); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (vanished) return;
        Image f = (state == State.WALK ? walkFrames : hitFrames)[Math.min(
            frameIndex, (state == State.WALK ? walkFrames.length : hitFrames.length) - 1)];
        if (facingLeft) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x + FRAME_W, y);
            g2.scale(-1, 1);
            g2.drawImage(f, 0, 0, this);
            g2.dispose();
        } else {
            g.drawImage(f, x, y, this);
        }
    }
}
