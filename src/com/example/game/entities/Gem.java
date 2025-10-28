package com.example.game.entities;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import com.example.game.core.SoundManager;
import javax.sound.sampled.Clip;

public class Gem extends JComponent {

    private Image[] frames;         // จะเซ็ตตามจำนวนที่โหลดจริง
    private int frameCount = 0;
    private int frameIndex = 0;
    private Timer timer = null;
    private final Clip pickClip;    

    // world position
    private int x, y;
    private int w = 48 * 2, h = 48 * 3;

    // bobbing
    private float bobT = 0f;
    private final float bobSpeed = 0.06f;
    private final int bobAmp = 6;

    private boolean picked = false;
    private boolean vanished = false;

    public Gem(int x, int y) {
        this.x = x;
        this.y = y;
        setOpaque(false);
        pickClip = SoundManager.loadClip("/assets/sound/3-down-fast-3-106140.wav");
        SoundManager.setVolume(pickClip, 0.9f);
        
        // 1) โหลดเฟรมให้เรียบร้อยก่อน
        loadFrames();

        // 2) คำนวณขนาดจากเฟรมแรกที่ไม่ใช่ null
        for (int i = 0; i < frameCount; i++) {
            Image f = frames[i];
            if (f != null) {
                w = Math.max(1, f.getWidth(null));
                h = Math.max(1, f.getHeight(null));
                break;
            }
        }

        // 3) เริ่มแอนิเมชันหลังมั่นใจว่ามีเฟรมให้เล่น
        timer = new javax.swing.Timer(80, e -> {
            if (vanished) { timer.stop(); return; }
            frameIndex = (frameIndex + 1) % frameCount;
            bobT += bobSpeed;
            repaint();
        });
        timer.start();
    }

    private void loadFrames() {
        // รองรับได้หลายชื่อไฟล์: gem_1.png, gem_01.png, gem1.png, gem01.png
        String[][] patterns = new String[][] {
            {"/assets/gem%d.png","1", "12"},
        };

        frames = new Image[12];
        frameCount = 0;

        for (String[] pat : patterns) {
            int loaded = tryLoadPattern(pat[0]);
            if (loaded > 0) { frameCount = loaded; break; }
        }

        if (frameCount == 0) {
            // fallback — เผื่อพาธผิดจะยังเห็นเป็นวงกลม
            frames = new Image[12];
            for (int i = 0; i < 12; i++) {
                BufferedImage bi = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = bi.createGraphics();
                g2.setColor(new Color(70, 255, 170));
                g2.fillOval(0, 0, 32, 32);
                g2.dispose();
                frames[i] = bi;
            }
            frameCount = 12;
            System.err.println("[Gem] WARNING: cannot find sprite files, using fallback circle.");
        }
    }

    private int tryLoadPattern(String pattern) {
        int count = 0;
        for (int i = 1; i <= 12; i++) {
            String path = String.format(pattern, i);
            URL u = getClass().getResource(path);
            if (u != null) {
                frames[i - 1] = new ImageIcon(u).getImage();
                count++;
            } else {
                // ถ้าหาไม่ได้บางเฟรม ให้หยุดที่จำนวนที่โหลดสำเร็จ
                // และจะเล่นเฉพาะจำนวนที่มีจริง
                break;
            }
        }
        // ถ้าโหลดได้ 0 เฟรม ให้ล้างค่า (จะลองแพทเทิร์นถัดไป)
        if (count == 0) {
            for (int i = 0; i < 12; i++) frames[i] = null;
        }
        return count;
    }

    public void setWorldBoundsDimension(int worldW, int worldH) {
        setBounds(0, 0, worldW, worldH);
    }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    public Rectangle getHitBox() {
        int pad = Math.max(0, Math.min(w, h) / 6);
        return new Rectangle(x + pad, y + pad, w - pad * 2, h - pad * 2);
    }

    public boolean isPicked() { return picked; }
    public boolean isGone()   { return vanished; }

    public void pick() {
        if (picked) return;                
        if (pickClip != null) SoundManager.play(pickClip); 
        picked = true;
        vanished = true;
        try { timer.stop(); } catch (Exception ignore) {}
        repaint();
    }

    public void disposeTimers() {
        try { timer.stop(); } catch (Exception ignore) {}
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (vanished || frameCount == 0) return;
        Image frame = frames[frameIndex];
        if (frame == null) return;
        int bob = (int) Math.round(Math.sin(bobT) * bobAmp);
        g.drawImage(frame, x, y + bob, null);
    }
}
