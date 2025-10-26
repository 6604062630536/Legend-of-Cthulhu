package com.example.game;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Cthulu extends JPanel {

    public static final int FRAME_W = 192 * 2;
    public static final int FRAME_H = 112 * 2;

    private int x, y;
    private boolean facingLeft = true;
    private boolean vanished = false;

    private final int maxHp = 400;
    private int hp = maxHp; 
    private int atk = 18;         
    private int speed = 2;
    private int attackRange = 220;
    private int chaseRange = 700;

    private int frameIndex = 0;
    private int elapsed = 0;

    private final Player target;
    private final Timer timer;

    private enum State { IDLE, WALK, ATTACK1, ATTACK2, HURT, DEATH }
    private State state = State.IDLE;

    private final Map<State, Image[]> frames = new HashMap<>();
    private final Map<State, Integer> frameDelay = new HashMap<>();

    // Hurt freeze (ms)
    private int hurtFreeze = 200, hurtElapsed = 0;
    private boolean inHurtFreeze = false;

    // โจมตี: เฟรมที่นับเป็น hit + ธงกันตีซ้ำ
    private static final int ATTACK1_HIT_FRAME = 3;
    private static final int ATTACK2_HIT_FRAME = 4;
    private boolean swingHitUsed = false;

    public Cthulu(Player target) {
        this.target = target;
        setOpaque(false);
        loadAnimations();
        timer = new Timer(16, e -> update(16));
        timer.start();
    }

    private void loadAnimations() {
        frames.put(State.IDLE,    loadSet("idle_",   15));
        frames.put(State.WALK,    loadSet("walk_",   12));
        frames.put(State.ATTACK1, loadSet("1atk_",    7));
        frames.put(State.ATTACK2, loadSet("2atk_",    9));
        frames.put(State.HURT,    loadSet("hurt_",    5));
        frames.put(State.DEATH,   loadSet("death_",  11));

        frameDelay.put(State.IDLE,    120);
        frameDelay.put(State.WALK,     80);
        frameDelay.put(State.ATTACK1, 100);
        frameDelay.put(State.ATTACK2, 100);
        frameDelay.put(State.HURT,     80);
        frameDelay.put(State.DEATH,   120);
    }

    private Image[] loadSet(String prefix, int count) {
        Image[] imgs = new Image[count];
        for (int i = 0; i < count; i++) {
            String path = String.format("/assets/%s%d.png", prefix, i + 1);
            imgs[i] = new ImageIcon(getClass().getResource(path))
                    .getImage().getScaledInstance(FRAME_W, FRAME_H, Image.SCALE_SMOOTH);
        }
        return imgs;
    }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public Rectangle getHitBox() {
        return vanished ? new Rectangle(0,0,0,0)
                        : new Rectangle(x + 30, y + 30, FRAME_W - 60, FRAME_H - 60);
    }
    public boolean isDead() { return state == State.DEATH; }
    public boolean isGone() { return vanished; }
    public boolean isAttacking() { return state == State.ATTACK1 || state == State.ATTACK2; }
    public int getCurrentFrameIndex(){ return frameIndex; }
    public int getAtk(){ return atk; }


    public boolean isFacingLeft() { return facingLeft; }

    public void takeDamage(int dmg) {          // เผื่อที่อื่นเรียกแบบเดิมอยู่
        takeDamage(dmg, false);
    }

    // โดนจากผู้เล่นพร้อมบอกว่าเป็น Attack2 หรือไม่
    public void takeDamage(int dmg, boolean fromAttack2) {
        if (state == State.DEATH || inHurtFreeze) return;
        hp -= Math.max(1, dmg);
        if (hp <= 0) {
            hp = 0;
            setState(State.DEATH);
        } else {
            // ❗ freeze เฉพาะโดน ATK2 เท่านั้น
            if (fromAttack2) {
                setState(State.HURT);
                inHurtFreeze = true;
                hurtElapsed = 0;
            }
            // ถ้าไม่ใช่ ATK2 จะไม่เข้า HURT (ยังทำอนิเมชันเดิมต่อ)
        }
    }

    private void update(int dt) {
        if (vanished) return;

        elapsed += dt;
        if (inHurtFreeze) hurtElapsed += dt;

        // DEATH
        if (state == State.DEATH) {
            if (elapsed >= frameDelay.get(State.DEATH)) {
                frameIndex++; elapsed = 0;
                if (frameIndex >= frames.get(State.DEATH).length) {
                    vanished = true; timer.stop();
                }
            }
            repaint(); return;
        }

        // HURT freeze
        if (inHurtFreeze) {
            if (hurtElapsed >= hurtFreeze) { inHurtFreeze = false; setState(State.IDLE); }
            repaint(); return;
        }

        // ---- AI: ทำเฉพาะตอน IDLE/WALK เท่านั้น → ไม่แคนเซิลอนิเมชันโจมตี ----
        if (state == State.IDLE || state == State.WALK) {
            if (target != null) {
                int playerX = target.getXPos();
                int dx = playerX - x;
                facingLeft = dx < 0;
                int dist = Math.abs(dx);

                if (dist < attackRange) setState(State.ATTACK1);
                else if (dist < chaseRange) {
                    setState(State.WALK);
                    x += (dx > 0 ? speed : -speed);
                } else setState(State.IDLE);
            }
        }

        // ---- Animation step ----
        int delay = frameDelay.get(state);
        if (elapsed >= delay) {
            frameIndex++; elapsed = 0;

            // เปิดหน้าต่างโจมตีของ swing
            if (state == State.ATTACK1 && frameIndex == ATTACK1_HIT_FRAME) swingHitUsed = false;
            if (state == State.ATTACK2 && frameIndex == ATTACK2_HIT_FRAME) swingHitUsed = false;

            if (state == State.ATTACK1 && frameIndex >= frames.get(State.ATTACK1).length) {
                frameIndex = 0; setState(State.ATTACK2); // combo ต่อเอง
            } else if (state == State.ATTACK2 && frameIndex >= frames.get(State.ATTACK2).length) {
                frameIndex = 0; setState(State.IDLE);
            } else if (frameIndex >= frames.get(state).length) {
                frameIndex = 0;
            }
        }

        repaint();
    }

    private void setState(State s) {
        if (state != s) {
            state = s; frameIndex = 0; elapsed = 0;
            // reset ธงตีเมื่อเริ่มท่าโจมตี
            if (s == State.ATTACK1 || s == State.ATTACK2) swingHitUsed = false;
        }
    }

    /** ลองตีเป้าหมาย: คืน true เมื่อทำดาเมจได้สำเร็จ 1 ครั้งใน swing นี้ */
    public boolean tryHit(Rectangle targetHitBox) {
        if (!isAttacking() || swingHitUsed) return false;

        // ใช้ hit-frame เจาะจง
        int need = (state == State.ATTACK1 ? ATTACK1_HIT_FRAME : ATTACK2_HIT_FRAME);
        if (frameIndex != need) return false;

        // hitbox โจมตี (ยื่นไปด้านที่หัน)
        Rectangle atkBox = new Rectangle(
                facingLeft ? x - 40 : x + FRAME_W - 40,
                y + 20,
                60, FRAME_H - 40
        );

        if (atkBox.intersects(targetHitBox)) {
            swingHitUsed = true;
            return true;
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (vanished) return;

        Image frame = frames.get(state)[frameIndex % frames.get(state).length];
        if (facingLeft) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x + FRAME_W, y);
            g2.scale(-1, 1);
            g2.drawImage(frame, 0, 0, this);
            g2.dispose();
        } else {
            g.drawImage(frame, x, y, this);
        }

        drawHPBar((Graphics2D) g);
    }

    private void drawHPBar(Graphics2D g2) {
        int barW = 160, barH = 10;
        int bx = x + (FRAME_W - barW) / 2;
        int by = y - 14;
        float pct = Math.max(0f, hp / (float) maxHp);
        g2.setColor(new Color(60,0,0,160));
        g2.fillRect(bx, by, barW, barH);
        g2.setColor(Color.RED);
        g2.fillRect(bx, by, (int)(barW * pct), barH);
        g2.setColor(Color.BLACK);
        g2.drawRect(bx, by, barW, barH);
    }
}
