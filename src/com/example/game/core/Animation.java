// ==================== Animation.java ====================
package com.example.game.core;

import java.awt.*;

public class Animation {
    private final Image[] frames;
    private final int interval;
    private int index = 0;
    private int elapsed = 0;
    private boolean loop;
    private boolean finished = false;

    public Animation(Image[] frames, int interval) {
        this(frames, interval, true);
    }
    
    public Animation(Image[] frames, int interval, boolean loop) {
        this.frames = frames;
        this.interval = interval;
        this.loop = loop;
    }

    public void update(int dt) {
        if (finished && !loop) return;
        
        elapsed += dt;
        if (elapsed >= interval) {
            elapsed = 0;
            index++;
            
            if (index >= frames.length) {
                if (loop) {
                    index = 0;
                } else {
                    index = frames.length - 1;
                    finished = true;
                }
            }
        }
    }

    public Image getCurrentFrame() {
        return frames[Math.min(index, frames.length - 1)];
    }
    
    public int getCurrentIndex() {
        return index;
    }
    
    public boolean isFinished() {
        return finished;
    }

    public void reset() {
        index = 0; 
        elapsed = 0;
        finished = false;
    }
}
