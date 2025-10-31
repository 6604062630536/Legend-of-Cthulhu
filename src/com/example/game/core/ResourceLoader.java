package com.example.game.core;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;

public class ResourceLoader {
    
    public static Image loadImage(String path) {
        URL url = ResourceLoader.class.getResource(path);
        if (url == null) {
            System.err.println("ไม่พบไฟล์รูป: " + path);
            return createFallbackImage(64, 64, Color.MAGENTA);
        }
        return new ImageIcon(url).getImage();
    }
    
    public static Image loadImage(String path, int width, int height) {
        Image img = loadImage(path);
        if (img == null) return createFallbackImage(width, height, Color.MAGENTA);
        return img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }
    
    public static Image[] loadAnimationFrames(String folder, String prefix, int count, int width, int height) {
        Image[] frames = new Image[count];
        for (int i = 0; i < count; i++) {
            String path = String.format("%s/%s%d.png", folder, prefix, i + 1);
            frames[i] = loadImage(path, width, height);
        }
        return frames;
    }
    
    public static Clip loadClip(String path) {
        try (InputStream audioSrc = ResourceLoader.class.getResourceAsStream(path)) {
            if (audioSrc == null) {
                System.err.println("ไม่พบไฟล์เสียง: " + path);
                return null;
            }
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream ais = AudioSystem.getAudioInputStream(bufferedIn);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            System.err.println("โหลดเสียงไม่สำเร็จ: " + path + " → " + e.getMessage());
            return null;
        }
    }
    
    private static Image createFallbackImage(int width, int height, Color color) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
            width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, width - 1, height - 1);
        g.dispose();
        return img;
    }
}