package com.example.game.entities;

import com.example.game.core.SoundManager;
import com.example.game.core.Entity;

import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class Player extends Entity {

    // ------------------- Config / Const -------------------
    public static final int FRAME_W = 120 * 2;   // 240
    public static final int FRAME_H = 80  * 2;   // 160

    private static final int IDLE_FRAMES  = 10;
    private static final int RUN_FRAMES   = 10;
    private static final int ATK1_FRAMES  = 4;
    private static final int ATK2_FRAMES  = 6;
    private static final int DEATH_FRAMES = 10;

    private static final int IDLE_INTERVAL = 120;
    private static final int RUN_INTERVAL  = 50;
    private static final int ATK_INTERVAL  = 50;
    private static final int DEATH_INTERVAL= 90;

    // hit-frame ของแต่ละท่า (ปรับให้เข้าจังหวะภาพจริงของคุณ)
    private static final int HIT_FRAME_ATK1 = 2;
    private static final int HIT_FRAME_ATK2 = 3;

    // หน้าต่างกดคอมโบหลังจบ ATTACK1 (ms)
    private static final int COMBO_GRACE_MS = 150;

    // ------------------- Animation frames -------------------
    private final Image[] idle  = new Image[IDLE_FRAMES];
    private final Image[] run   = new Image[RUN_FRAMES];
    private final Image[] atk1  = new Image[ATK1_FRAMES];
    private final Image[] atk2  = new Image[ATK2_FRAMES];
    private final Image[] death = new Image[DEATH_FRAMES];

    // ------------------- State -------------------
    private enum State { IDLE, RUN, ATTACK1, ATTACK2, DEATH }
    private State state = State.IDLE;

    private int x = 100, y = 100;
    private int frameIndex = 0;
    private int animElapsed = 0;
    private int attackElapsed = 0;
    private int deathElapsed  = 0;

    // การควบคุมคีย์ (รับจาก DrawArea → onKeyPressed/onKeyReleased)
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    // คอมโบ
    private boolean comboQueued = false;
    private int comboGraceRemain = 0;

    // เสียง
    private final Clip swordClip;
    private final Clip deathClip;

    // ลูปเกม
    private final Timer timer;
	

    // ------------------- ctor -------------------
    // super(maxHp, atk, atkSpd, moveSpeed)
    public Player() {
        super(100, 10, 3, 8);  // maxHP=100, atk=10, atkSpd=3, speed=5
        setOpaque(false);

        // โหลดภาพจากโฟลเดอร์ resources: /assets.Player/....
        for (int i = 0; i < IDLE_FRAMES;  i++) idle[i]  = loadFromPlayer("_Idle_r1_c",    i+1);
        for (int i = 0; i < RUN_FRAMES;   i++) run[i]   = loadFromPlayer("_Run_r1_c",     i+1);
        for (int i = 0; i < ATK1_FRAMES;  i++) atk1[i]  = loadFromPlayer("_Attack_r1_c",  i+1);
        for (int i = 0; i < ATK2_FRAMES;  i++) atk2[i]  = loadFromPlayer("_Attack2_r1_c", i+1);
        for (int i = 0; i < DEATH_FRAMES; i++) death[i] = loadFromPlayer("_Death_r1_c",   i+1);

        // โหลดเสียง
        swordClip = SoundManager.loadClip("/assets/sound/sword-sound-2-36274.wav");
        deathClip = SoundManager.loadClip("/assets/sound/death-sound-1-165630.wav");

        // ลูปเกม ~60FPS
        timer = new Timer(16, e -> update(16));
        timer.start();
    }

    private Image loadFromPlayer(String prefix, int i) {
        // โครงสร้างไฟล์: src/main/resources/assets.Player/_Idle_r1_c1.png เป็นต้น
        String path = "/assets/Player/" + prefix + i + ".png";
        Image img = new ImageIcon(getClass().getResource(path)).getImage();
        return img.getScaledInstance(FRAME_W, FRAME_H, Image.SCALE_SMOOTH);
    }

    // ------------------- Input API (เรียกจาก DrawArea) -------------------
    public void onKeyPressed(int keyCode) {
        if (state == State.DEATH) return;
        if (keyCode == KeyEvent.VK_A) { leftPressed = true;  facingLeft = true;  }
        if (keyCode == KeyEvent.VK_D) { rightPressed = true; facingLeft = false; }
        if (keyCode == KeyEvent.VK_J) { attack(); }
    }

    public void onKeyReleased(int keyCode) {
        if (keyCode == KeyEvent.VK_A) leftPressed = false;
        if (keyCode == KeyEvent.VK_D) rightPressed = false;
    }

    // ------------------- Combat -------------------
    public void attack() {
        if (state == State.ATTACK1) {           // ระหว่างตีชุดแรก → จองคอมโบ
            comboQueued = true;
            return;
        }
        if (comboGraceRemain > 0 && state != State.ATTACK2) {
            startAttack2();                      // เพิ่งจบ ATTACK1 แล้วยังอยู่ในหน้าต่าง
            return;
        }
        if (state != State.ATTACK2) startAttack1();
    }

    private void startAttack1() {
        state = State.ATTACK1;
        frameIndex = 0; animElapsed = 0; attackElapsed = 0;
        comboQueued = false;
        // เปิดเสียงตอนเข้าท่า แล้วคิวอีกจังหวะตอนเฟรมกำหนดใน update()
        SoundManager.play(swordClip);
    }

    private void startAttack2() {
        state = State.ATTACK2;
        frameIndex = 0; animElapsed = 0; attackElapsed = 0;
        comboQueued = false;
        comboGraceRemain = 0;
        SoundManager.play(swordClip);
    }

    @Override
    protected void onDeath() {
        state = State.DEATH;
        frameIndex = 0; animElapsed = 0; attackElapsed = 0; deathElapsed = 0;
        SoundManager.play(deathClip);
    }

    // ------------------- Update Loop -------------------
    private void update(int dt) {
        // เดิน/เปลี่ยนสถานะเฉพาะตอน “ไม่ได้โจมตี”
        if (state != State.ATTACK1 && state != State.ATTACK2 && state != State.DEATH) {
            if (leftPressed ^ rightPressed) {
                if (leftPressed)  x -= speed;
                if (rightPressed) x += speed;
                state = State.RUN;
            } else {
                state = State.IDLE;
            }
            clampInsidePanel();
        }

        // combo grace window นับถอยหลัง
        if (comboGraceRemain > 0) comboGraceRemain = Math.max(0, comboGraceRemain - dt);

        // อัปเดตเฟรมตามสถานะ
        switch (state) {
            case IDLE -> tickAnim(dt, IDLE_INTERVAL, IDLE_FRAMES);
            case RUN  -> tickAnim(dt, RUN_INTERVAL,  RUN_FRAMES);

            case ATTACK1 -> {
                attackElapsed += dt;
                animElapsed += dt;
                if (animElapsed >= ATK_INTERVAL) {
                    animElapsed -= ATK_INTERVAL;
                    frameIndex = Math.min(frameIndex + 1, ATK1_FRAMES - 1);
                }
                // จบชุดแรก
                if (attackElapsed >= ATK1_FRAMES * ATK_INTERVAL) {
                    comboGraceRemain = COMBO_GRACE_MS;           // เปิดหน้าต่างต่อคอมโบ
                    if (comboQueued) startAttack2();
                    else backToMoveState();
                }
            }

            case ATTACK2 -> {
                attackElapsed += dt;
                animElapsed += dt;
                if (animElapsed >= ATK_INTERVAL) {
                    animElapsed -= ATK_INTERVAL;
                    frameIndex = Math.min(frameIndex + 1, ATK2_FRAMES - 1);
                }
                // จบชุดสอง
                if (attackElapsed >= ATK2_FRAMES * ATK_INTERVAL) {
                    backToMoveState();
                }
            }

            case DEATH -> {
                deathElapsed += dt;
                if (deathElapsed >= DEATH_INTERVAL) {
                    deathElapsed = 0;
                    frameIndex++;
                    if (frameIndex >= DEATH_FRAMES) {
                        vanished = true; // ให้ Game ลบออกเองถ้าต้องการ
                    }
                }
            }
        }

        repaint();
    }

    private void tickAnim(int dt, int interval, int frames) {
        animElapsed += dt;
        if (animElapsed >= interval) {
            animElapsed -= interval;
            frameIndex = (frameIndex + 1) % frames;
        }
    }

    private void backToMoveState() {
        if (leftPressed || rightPressed) state = State.RUN;
        else state = State.IDLE;
        frameIndex = 0; animElapsed = 0; attackElapsed = 0;
        comboQueued = false; comboGraceRemain = 0;
    }

    private void clampInsidePanel() {
        int maxW = getWidth()  > 0 ? getWidth()  : (getParent()!=null ? getParent().getWidth()  : 0);
        int maxH = getHeight() > 0 ? getHeight() : (getParent()!=null ? getParent().getHeight() : 0);
        if (maxW <= 0 || maxH <= 0) return;

        x = Math.max(0, Math.min(x, maxW - FRAME_W));
        int floorY = Math.max(0, maxH - FRAME_H - 50);
        y = floorY;
    }

    // ------------------- Public API for GameLauncher -------------------
    public boolean isAttacking() { return state == State.ATTACK1 || state == State.ATTACK2; }
    public boolean isInAttack2() { return state == State.ATTACK2; }
    public boolean isAtHitFrame() {
        return (state == State.ATTACK1 && frameIndex == HIT_FRAME_ATK1)
            || (state == State.ATTACK2 && frameIndex == HIT_FRAME_ATK2);
    }
 

    public int getCurrentFrameIndex() { return frameIndex; }
    public int getXPos() { return x; }
    public int getYPos() { return y; }

    // ถ้า HUD เรียกใช้อยู่
    public int getMaxHp() { return 100; }          // ให้สอดคล้องกับค่าใน super()
    public boolean isInvulnerable() { return false; } // ไม่มีเอฟเฟกต์กระพริบอีกแล้ว

    @Override
    public Rectangle getHitBox() {
        return new Rectangle(x, y, FRAME_W - 60, FRAME_H - 20);
    }

    // ------------------- Render -------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (vanished) return;

        Image frame = switch (state) {
            case IDLE    -> idle[frameIndex];
            case RUN     -> run[frameIndex];
            case ATTACK1 -> atk1[Math.min(frameIndex, ATK1_FRAMES-1)];
            case ATTACK2 -> atk2[Math.min(frameIndex, ATK2_FRAMES-1)];
            case DEATH   -> death[Math.min(frameIndex, DEATH_FRAMES-1)];
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

    // ------------------- Utilities -------------------
    /** ให้ DrawArea เรียกตอนเริ่ม/resize เพื่อให้ยืนติดพื้น */
    public void forceSnapToGround(int panelH, int groundMargin) {
        if (panelH > 0) {
            y = Math.max(0, panelH - FRAME_H - groundMargin);
            repaint();
        }
    }
}
