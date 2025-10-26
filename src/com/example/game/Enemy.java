package com.example.game;

import javax.swing.*;
import java.awt.*;

public class Enemy extends JPanel {

    public static final int FRAME_W = 64 * 2;
    public static final int FRAME_H = 64 * 2;

    private static final int WALK_FRAMES = 8; 
    private static final int HIT_FRAMES  = 3; 

    private static final int WALK_INTERVAL = 100;
    private static final int HIT_INTERVAL  = 80;

    private Image[] walkFrames = new Image[WALK_FRAMES];
    private Image[] hitFrames  = new Image[HIT_FRAMES];

    private int x, y;
    private int hp = 60;
    private int atk = 6;            // ดาเมจน้อยกว่าบอส
    private int speed = 2;
    private int attackRange = 110;
    private boolean facingLeft = true;
    private boolean vanished = false;

    private int frameIndex = 0, elapsed = 0;

    private final Timer timer;

    private enum State { WALK, ATTACK, DEATH }
    private State state = State.WALK;

    private Player target;

    // โจมตี: hit-frame + กันตีซ้ำ
    private static final int HIT_FRAME = 2;
    private boolean swingHitUsed = false;

    public Enemy(int startX) {
        this(startX, null);
    }
    public Enemy(int startX, Player target) {
        this.target = target;
        setOpaque(false);

        for (int i = 0; i < WALK_FRAMES; i++) {
            String p = String.format("/assets/_run_minion_c%d.png", i + 1);
            walkFrames[i] = new ImageIcon(getClass().getResource(p))
                    .getImage().getScaledInstance(FRAME_W, FRAME_H, Image.SCALE_SMOOTH);
        }
        for (int i = 0; i < HIT_FRAMES; i++) {
            String p = String.format("/assets/_hit_minion_c%d.png", i + 1);
            hitFrames[i] = new ImageIcon(getClass().getResource(p))
                    .getImage().getScaledInstance(FRAME_W, FRAME_H, Image.SCALE_SMOOTH);
        }

        this.x = startX;
        this.y = 0;
        timer = new Timer(16, e -> tick(16));
        timer.start();
    }

    public Rectangle getHitBox() {
        return vanished ? new Rectangle(0,0,0,0) : new Rectangle(x, y, FRAME_W, FRAME_H);
    }
    public boolean isDead() { return state == State.DEATH; }
    public boolean isGone() { return vanished; }
    public boolean isAttacking(){ return state == State.ATTACK; }
    public int getCurrentFrameIndex(){ return frameIndex; }
    public int getAtk(){ return atk; }


    public void snapToGround() {
        int h = getHeight() > 0 ? getHeight()
                : (getParent()!=null ? getParent().getHeight() : 0);
        if (h > 0) {
            int groundMargin = 12;
            y = h - FRAME_H - groundMargin;
            repaint();
        }
    }

    public void takeDamage(int dmg) {
        if (vanished) return;
        hp -= Math.max(1, dmg);
        if (hp <= 0) {
            hp = 0; state = State.DEATH; frameIndex = 0; elapsed = 0;
        }
        repaint();
    }

    private void tick(int dt) {
        if (vanished) return;
        elapsed += dt;

        // AI: ไม่ยกเลิกอนิเมชันโจมตี
        if (state == State.WALK && target != null) {
            int dx = target.getXPos() - x;
            facingLeft = dx < 0;
            int dist = Math.abs(dx);
            if (dist < attackRange) {
                state = State.ATTACK; frameIndex = 0; elapsed = 0; swingHitUsed = false;
            } else {
                x += (dx > 0 ? speed : -speed);
            }
        }

        // animation
        int interval = switch (state) {
            case WALK    -> WALK_INTERVAL;
            case ATTACK  -> HIT_INTERVAL;
            case DEATH   -> 100;
        };

        if (elapsed >= interval) {
            frameIndex++; elapsed = 0;

            if (state == State.ATTACK && frameIndex == HIT_FRAME) swingHitUsed = false;

            int max = switch (state) {
                case WALK   -> WALK_FRAMES;
                case ATTACK -> HIT_FRAMES;
                case DEATH  -> HIT_FRAMES; // reuse length
            };
            if (frameIndex >= max) {
                if (state == State.ATTACK) { state = State.WALK; frameIndex = 0; }
                else if (state == State.DEATH) { vanished = true; timer.stop(); }
                else frameIndex = 0;
            }
        }

        repaint();
    }

    /** ลองตี Player: คืน true เมื่อทำดาเมจได้สำเร็จ 1 ครั้งใน swing */
    public boolean tryHit(Rectangle targetHitBox) {
        if (!isAttacking() || swingHitUsed) return false;
        if (frameIndex != HIT_FRAME) return false;

        Rectangle atkBox = new Rectangle(
                facingLeft ? x - 25 : x + FRAME_W - 35,
                y + 10,
                40, FRAME_H - 20
        );
        if (atkBox.intersects(targetHitBox)) {
            swingHitUsed = true; return true;
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (vanished) return;
        Image frame = switch (state) {
            case WALK   -> walkFrames[frameIndex];
            case ATTACK -> hitFrames[frameIndex];
            case DEATH  -> hitFrames[Math.min(frameIndex, HIT_FRAMES-1)];
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
