// ==================== AnimatedEntity.java ====================
package com.example.game.core;

import javax.sound.sampled.Clip;
import javax.swing.Timer;
import java.util.HashMap;
import java.util.Map;

public abstract class AnimatedEntity extends Entity {
    protected Map<String, Animation> animations = new HashMap<>();
    protected String currentState;
    protected Timer gameTimer;
    protected Map<String, Clip> sounds = new HashMap<>();
    
    public AnimatedEntity(int maxHp, int atk, int def, int speed) {
        super(maxHp, atk, def, speed);
        gameTimer = new Timer(16, e -> update(16));
    }
    
    protected void addAnimation(String state, Animation anim) {
        animations.put(state, anim);
    }
    
    protected void addSound(String name, String path, float volume) {
        Clip clip = ResourceLoader.loadClip(path);
        if (clip != null) {
            SoundManager.setVolume(clip, volume);
            sounds.put(name, clip);
        }
    }
    
    protected void playSound(String name) {
        Clip clip = sounds.get(name);
        if (clip != null) {
            SoundManager.play(clip);
        }
    }
    
    protected void setState(String newState) {
        if (!newState.equals(currentState)) {
            currentState = newState;
            Animation anim = animations.get(currentState);
            if (anim != null) {
                anim.reset();
            }
        }
    }
    
    protected Animation getCurrentAnimation() {
        return animations.get(currentState);
    }
    
    public void startTimer() {
        if (!gameTimer.isRunning()) {
            gameTimer.start();
        }
    }
    
    public void stopTimer() {
        if (gameTimer.isRunning()) {
            gameTimer.stop();
        }
    }
    
    protected abstract void update(int dt);
}