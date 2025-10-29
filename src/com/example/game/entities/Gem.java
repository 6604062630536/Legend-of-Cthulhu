// ==================== Gem.java ====================
package com.example.game.entities;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import com.example.game.core.*;

public class Gem extends JComponent {
    private Image[] frames;
    private int frameCount = 0;
    private int frameIndex = 0;
    private Timer timer = null;
    private final javax.sound.sampled.Clip pickClip;

    private int x, y;
    private int w = 96, h = 144;

    // Bobbing animation
    private float bobT = 0f;
    private final float bobSpeed = 0.06f;
    private final int bobAmp = 6;

    private boolean picked = false;
    private boolean vanished = false;

    public Gem(int x, int y) {
        this.x = x;
        this.y = y;
        setOpaque(false);

        pickClip = ResourceLoader.loadClip("/assets/sound/3-down-fast-3-106140.wav");
        SoundManager.setVolume(pickClip, 0.9f);

        loadFrames();

        // Calculate size from first valid frame
        for (int i = 0; i < frameCount; i++) {
            Image f = frames[i];
            if (f != null) {
                w = Math.max(1, f.getWidth(null));
                h = Math.max(1, f.getHeight(null));
                break;
            }
        }

        timer = new Timer(80, e -> {
            if (vanished) {
                timer.stop();
                return;
            }
            frameIndex = (frameIndex + 1) % frameCount;
            bobT += bobSpeed;
            repaint();
        });
        timer.start();
    }

    private void loadFrames() {
        // Try to load gem animation frames
        frames = new Image[12];
        frameCount = 0;

        // Try pattern: gem1.png, gem2.png, ...
        for (int i = 1; i <= 12; i++) {
            String path = String.format("/assets/gem%d.png", i);
            Image img = ResourceLoader.loadImage(path);
            if (img != null && img.getWidth(null) > 0) {
                frames[i - 1] = img;
                frameCount++;
            } else {
                break;
            }
        }

        // Fallback if no frames loaded
        if (frameCount == 0) {
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

    public void setWorldBoundsDimension(int worldW, int worldH) {
        setBounds(0, 0, worldW, worldH);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Rectangle getHitBox() {
        int pad = Math.max(0, Math.min(w, h) / 6);
        return new Rectangle(x + pad, y + pad, w - pad * 2, h - pad * 2);
    }

    public boolean isPicked() {
        return picked;
    }

    public boolean isGone() {
        return vanished;
    }

    public void pick() {
        if (picked) return;
        if (pickClip != null) SoundManager.play(pickClip);
        picked = true;
        vanished = true;
        try {
            timer.stop();
        } catch (Exception ignore) {
        }
        repaint();
    }

    public void disposeTimers() {
        try {
            timer.stop();
        } catch (Exception ignore) {
        }
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