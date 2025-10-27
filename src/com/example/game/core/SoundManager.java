package com.example.game.core;

import javax.sound.sampled.*;
import java.io.*;

public class SoundManager {
    public static Clip loadClip(String path) {
        try (InputStream audioSrc = SoundManager.class.getResourceAsStream(path)) {
            if (audioSrc == null) return null;
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

    public static void play(Clip clip) {
        if (clip == null) return;
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    public static void setVolume(Clip clip, float scale) {
        if (clip == null) return;
        try {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float range = gain.getMaximum() - gain.getMinimum();
            float gainValue = (range * scale) + gain.getMinimum();
            gain.setValue(Math.min(gainValue, gain.getMaximum()));
        } catch (Exception e) {
            System.err.println("⚠️ ปรับเสียงไม่ได้: " + e.getMessage());
        }
    }
}
