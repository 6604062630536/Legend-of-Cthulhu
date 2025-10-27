package com.example.game.core;

import java.awt.*;

public class Animation {
    private final Image[] frames;
    private final int interval;
    private int index = 0;
    private int elapsed = 0;

    public Animation(Image[] frames, int interval) {
        this.frames = frames;
        this.interval = interval;
    }

    public void update(int dt) {
        elapsed += dt;
        if (elapsed >= interval) {
            elapsed = 0;
            index = (index + 1) % frames.length;
        }
    }

    public Image getCurrentFrame() {
        return frames[index];
    }

    public void reset() {
        index = 0; elapsed = 0;
    }
}
