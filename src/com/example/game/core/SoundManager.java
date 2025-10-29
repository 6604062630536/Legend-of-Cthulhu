// ==================== SoundManager.java ====================
package com.example.game.core;

import javax.sound.sampled.*;

public class SoundManager {
    
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
    
    public static void stop(Clip clip) {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }
    
    public static void close(Clip clip) {
        if (clip != null) {
            stop(clip);
            clip.close();
        }
    }
}
