package com.example.game;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.sound.sampled.*;
import java.io.*;

public class Player extends JPanel implements KeyListener {

    // --- Stats ---
    private int maxHp = 100;
    private int hp = 100;
    private int atkdmg = 10;
    private int atksp = 3;
    private int speed  = 5;
    private int defense= 3;

    // --- Position/Direction ---
    private int x = 100;
    private int y = 100;
    private boolean facingLeft = false;

    // --- Input flags ---
    private boolean leftPressed  = false;
    private boolean rightPressed = false;

    // --- Animation constants ---
    public static final int FRAME_W = 120 * 2;
    public static final int FRAME_H = 80  * 2;

    private static final int IDLE_FRAMES = 10;
    private static final int RUN_FRAMES  = 10;
    private static final int ATK1_FRAMES = 4;
    private static final int ATK2_FRAMES = 6;

    private Image[] idleFrames = new Image[IDLE_FRAMES];
    private Image[] runFrames  = new Image[RUN_FRAMES];
    private Image[] atk1Frames = new Image[ATK1_FRAMES];
    private Image[] atk2Frames = new Image[ATK2_FRAMES];

    public enum State { IDLE, RUN, ATTACK1, ATTACK2 }
    private State state = State.IDLE;

    private int frameIndex = 0;
    private Timer gameTimer;
    private int animElapsed = 0;
    private int attackElapsed = 0;

    private static final int IDLE_INTERVAL = 120;
    private static final int RUN_INTERVAL  = 50;
    private static final int ATK_INTERVAL  = 50;

    // combo
    private boolean comboQueued = false;
    private int comboGraceRemain = 0;
    private static final int COMBO_GRACE_MS = 150;

    // hurt / I-frames
    private int iFrameRemainMs = 0;              // ช่วงล่องหนจากดาเมจ (ms)
    private static final int IFRAME_MS = 300;

    public Player() {
        setOpaque(false);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(this);

        for (int i = 0; i < IDLE_FRAMES; i++) {
            String path = String.format("/assets/_idle_r1_c%d.png", i + 1);
            idleFrames[i] = new ImageIcon(getClass().getResource(path))
                    .getImage().getScaledInstance(FRAME_W, FRAME_H, Image.SCALE_SMOOTH);
        }
        for (int i = 0; i < RUN_FRAMES; i++) {
            String path = String.format("/assets/_Run_r1_c%d.png", i + 1);
            runFrames[i] = new ImageIcon(getClass().getResource(path))
                    .getImage().getScaledInstance(FRAME_W, FRAME_H, Image.SCALE_SMOOTH);
        }
        for (int i = 0; i < ATK1_FRAMES; i++) {
            String path = String.format("/assets/_Attack_r1_c%d.png", i + 1);
            atk1Frames[i] = new ImageIcon(getClass().getResource(path))
                    .getImage().getScaledInstance(FRAME_W, FRAME_H, Image.SCALE_SMOOTH);
        }
        for (int i = 0; i < ATK2_FRAMES; i++) {
            try {
                String path = String.format("/assets/_Attack2_r1_c%d.png", i + 1);
                Image img = new ImageIcon(getClass().getResource(path)).getImage();
                atk2Frames[i] = img.getScaledInstance(FRAME_W, FRAME_H, Image.SCALE_SMOOTH);
            } catch (Exception ex) {
                atk2Frames[i] = atk1Frames[Math.min(ATK1_FRAMES - 1, i % ATK1_FRAMES)];
            }
        }

        gameTimer = new Timer(16, e -> update(16));
        gameTimer.start();
    }

    // --- Public getters / combat ---
    public int  getHp()         { return hp; }
    public int  getMaxHp()      { return maxHp; }
    public int  getDef()        { return defense; }
    public int  getSpeed()      { return speed; }
    public int  getAtkSpeed()   { return atksp; }
    public int  getAtk()        { return atkdmg; }
    public boolean isAttacking(){ return state == State.ATTACK1 || state == State.ATTACK2; }
    public boolean isInAttack2() { return state == State.ATTACK2; }
    public Rectangle getHitBox(){ return new Rectangle(x, y, FRAME_W - 50, FRAME_H); }
    public int  getCurrentFrameIndex(){ return frameIndex; }
    public int  getXPos()       { return x; }
    public int  getYPos()       { return y; }
    public boolean isInvulnerable(){ return iFrameRemainMs > 0; }

 // ให้ดาเมจเฉพาะ hit frame: ATTACK1 เฟรม 1, ATTACK2 เฟรม 2 (ปรับเลขได้)
    public boolean isAtHitFrame() {
     return (state == State.ATTACK1 && frameIndex == 2)
         || (state == State.ATTACK2 && frameIndex == 3);
    }
    public Image getImageFrame() {
        return switch (state) {
            case IDLE    -> idleFrames[frameIndex];
            case RUN     -> runFrames[frameIndex];
            case ATTACK1 -> atk1Frames[frameIndex];
            case ATTACK2 -> atk2Frames[frameIndex];
        };
    }

    public boolean isFacingLeft() { return facingLeft; }


    public void takeDamage(int dmg) {
        if (isInvulnerable()) return;
        int real = Math.max(1, dmg - defense);
        hp = Math.max(0, hp - real);
        iFrameRemainMs = IFRAME_MS;
        repaint();
    }

    // ให้ GameLauncher สั่ง snap พื้นเมื่อเริ่ม/รีไซส์
    public void forceSnapToGround(int panelH, int groundMargin) {
        if (panelH > 0) {
            y = Math.max(0, panelH - FRAME_H - groundMargin);
            repaint();
        }
    }

    // Actions
    public void walk(char dir) {
        if (dir == 'A' || dir == 'a') { facingLeft = true;  x -= speed; }
        else if (dir == 'D' || dir == 'd') { facingLeft = false; x += speed; }
        clampInsidePanel();
        repaint();
    }

    public void attack() {
        if (state == State.ATTACK1) { comboQueued = true; return; }
        if (comboGraceRemain > 0 && state != State.ATTACK2) { startAttack2(); return; }
        if (state != State.ATTACK2) startAttack1();
    }

    private void startAttack1() {
        state = State.ATTACK1;
        frameIndex = 0; animElapsed = 0; attackElapsed = 0;
        comboQueued = false;
        repaint();
    }
    private void startAttack2() {
        state = State.ATTACK2;
        frameIndex = 0; animElapsed = 0; attackElapsed = 0;
        comboQueued = false; comboGraceRemain = 0;
        repaint();
    }

    private Clip swordClip;
    private void loadSwordSound() {
        try (InputStream audioSrc = getClass().getResourceAsStream("/assets/sound/sword-sound-2-36274.wav")) {
            if (audioSrc == null) return;
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioStream = javax.sound.sampled.AudioSystem.getAudioInputStream(bufferedIn);
            swordClip = javax.sound.sampled.AudioSystem.getClip();
            swordClip.open(audioStream);
        } catch (Exception e) { System.err.println("โหลดเสียงไม่สำเร็จ: " + e.getMessage()); }
    }
    private void playAttackSound() {
        try {
            if (swordClip == null) loadSwordSound();
            if (swordClip == null) return;
            if (swordClip.isRunning()) swordClip.stop();
            swordClip.setFramePosition(0);
            swordClip.start();
        } catch (Exception e) { System.err.println("เล่นเสียงไม่ได้: " + e.getMessage()); }
    }

    private void update(int dt) {
        if (iFrameRemainMs > 0) iFrameRemainMs = Math.max(0, iFrameRemainMs - dt);
        if (comboGraceRemain > 0) comboGraceRemain = Math.max(0, comboGraceRemain - dt);

        if (!isAttacking()) {
            if (leftPressed ^ rightPressed) {
                if (leftPressed)  { facingLeft = true;  x -= speed; }
                else              { facingLeft = false; x += speed; }
            }
            clampInsidePanel();
            if (leftPressed || rightPressed) setState(State.RUN);
            else setState(State.IDLE);
        }

        animElapsed += dt;
        switch (state) {
            case IDLE -> {
                if (animElapsed >= IDLE_INTERVAL) {
                    frameIndex = (frameIndex + 1) % IDLE_FRAMES;
                    animElapsed -= IDLE_INTERVAL;
                }
            }
            case RUN -> {
                if (animElapsed >= RUN_INTERVAL) {
                    frameIndex = (frameIndex + 1) % RUN_FRAMES;
                    animElapsed -= RUN_INTERVAL;
                }
            }
            case ATTACK1 -> {
                attackElapsed += dt;
                if (animElapsed >= ATK_INTERVAL) {
                    int old = frameIndex;
                    frameIndex = Math.min(frameIndex + 1, ATK1_FRAMES - 1);
                    animElapsed -= ATK_INTERVAL;
                    if (frameIndex == 1 && old == 0) playAttackSound();
                }
                if (attackElapsed >= ATK1_FRAMES * ATK_INTERVAL) {
                    comboGraceRemain = COMBO_GRACE_MS;
                    if (comboQueued) startAttack2(); else endAttackToMoveState();
                }
            }
            case ATTACK2 -> {
                attackElapsed += dt;
                if (animElapsed >= ATK_INTERVAL) {
                    int old = frameIndex;
                    frameIndex = Math.min(frameIndex + 1, ATK2_FRAMES - 1);
                    animElapsed -= ATK_INTERVAL;
                    if (frameIndex == 2 && old == 1) playAttackSound();
                }
                if (attackElapsed >= ATK2_FRAMES * ATK_INTERVAL) endAttackToMoveState();
            }
        }
        repaint();
    }

    private void endAttackToMoveState() {
        if (leftPressed || rightPressed) setState(State.RUN);
        else setState(State.IDLE);
        frameIndex = 0; animElapsed = 0; attackElapsed = 0;
        comboQueued = false; comboGraceRemain = 0;
    }

    private void setState(State s) {
        if (this.state != s) { this.state = s; frameIndex = 0; animElapsed = 0; }
    }

    private void clampInsidePanel() {
        int maxW = getWidth()  > 0 ? getWidth()  : (getParent()!=null ? getParent().getWidth()  : 0);
        int maxH = getHeight() > 0 ? getHeight() : (getParent()!=null ? getParent().getHeight() : 0);
        if (maxW <= 0 || maxH <= 0) return;
        x = Math.max(0, Math.min(x, maxW - FRAME_W));
        int groundMargin = 50;
        int floorY = maxH - FRAME_H - groundMargin;
        y = floorY;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Image frame = switch (state) {
            case IDLE    -> idleFrames[frameIndex];
            case RUN     -> runFrames[frameIndex];
            case ATTACK1 -> atk1Frames[frameIndex];
            case ATTACK2 -> atk2Frames[frameIndex];
        };
        // กระพริบเล็กน้อยตอนมี i-frames
        if (isInvulnerable() && (System.currentTimeMillis()/80)%2==0) return;

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

    // KeyListener
    @Override public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_A)  { leftPressed = true;  facingLeft = true;  walk('A'); }
        if (k == KeyEvent.VK_D)  { rightPressed = true; facingLeft = false; walk('D'); }
        if (k == KeyEvent.VK_J)  { attack(); }
    }
    @Override public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_A)  leftPressed = false;
        if (k == KeyEvent.VK_D)  rightPressed = false;
    }
    @Override public void keyTyped(KeyEvent e) {}
}
