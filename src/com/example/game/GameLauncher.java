// ==================== GameLauncher.java ====================
package com.example.game;

import com.example.game.entities.*;
import com.example.game.core.*;
import javax.swing.*;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.Clip;
import java.util.*;
import java.util.List;

public class GameLauncher extends JFrame {

    public GameLauncher() {
        setTitle("LEGEND OF CTHULU");
        setSize(928, 396);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setContentPane(new TitleScreenPanel());
        setVisible(true);
    }

    // =============== Title Screen ===============
    class TitleScreenPanel extends JPanel implements ActionListener, KeyListener {
        private Image titleImage;
        private boolean showText = true;
        private Timer blinkTimer;
        private Clip bgmClip;

        TitleScreenPanel() {
            setFocusable(true);
            addKeyListener(this);

            titleImage = ResourceLoader.loadImage("/assets/Title Screen.png");
            bgmClip = ResourceLoader.loadClip("/assets/sound/intense-fantasy-soundtrack-201079.wav");
            
            if (bgmClip != null) {
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                bgmClip.start();
            }

            blinkTimer = new Timer(500, this);
            blinkTimer.start();
        }

        private void stopBGM() {
            SoundManager.close(bgmClip);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showText = !showText;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (titleImage != null) {
                g.drawImage(titleImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            if (showText) {
                g.setFont(new Font("Monospaced", Font.BOLD, 22));
                g.setColor(Color.WHITE);
                g.drawString("Press K Button to Start", getWidth() / 2 - 150, getHeight() / 2 + 80);
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_K) {
                if (blinkTimer != null) blinkTimer.stop();
                stopBGM();

                Image bgImage = ResourceLoader.loadImage("/assets/Background.png");
                DrawArea game = new DrawArea(bgImage);
                GameLauncher.this.setContentPane(game);
                GameLauncher.this.revalidate();
                GameLauncher.this.repaint();
                SwingUtilities.invokeLater(game::requestFocusInWindow);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }
    }

    // =============== Game Area ===============
    class DrawArea extends JPanel implements KeyListener {
        // World
        final Image imgBg;
        final int bgWidth, bgHeight;
        float cameraX = 0f;

        // HUD
        Image hudIcon;
        final int HUD_X = 12, HUD_Y = 0;
        final int BAR_OFFSET_X = 77, BAR_OFFSET_Y = 83, BAR_W = 110, BAR_H = 14;

        // World constants
        final int GROUND_MARGIN = 50;
        final int WORLD_LEFT = 0;
        final int WORLD_HALF_R = 928;
        final int WORLD_RIGHT = 1952;

        // Entities
        Player player;
        List<Enemy> enemies = new ArrayList<>();
        Cthulu boss;
        Gem gem = null;
        boolean gameWon = false;
        boolean gameDefeated = false;

        // Gate system
        boolean leftHalfCleared = false;
        boolean bossActivated = false;

        // Damage tracking
        private int lastPlayerFrame = -1;
        private boolean[] enemyHitApplied;
        private boolean bossHitApplied = false;

        Timer gameTimer = new Timer(16, new GameUpdateListener());

        DrawArea(Image img) {
            this.imgBg = img;
            this.bgWidth = img.getWidth(null);
            this.bgHeight = img.getHeight(null);

            setLayout(null);
            setFocusable(true);
            addKeyListener(this);

            hudIcon = ResourceLoader.loadImage("/assets/HealthBar.png");

            // Create player
            player = new Player();
            player.setBounds(0, 0, bgWidth, bgHeight);
            add(player);

            // Create enemies
            int[] spawnX = {520, 700, 860};
            for (int x : spawnX) {
                Enemy e = new Enemy(x, player);
                e.setBounds(0, 0, bgWidth, bgHeight);
                e.snapToGround();
                enemies.add(e);
                add(e);
            }
            enemyHitApplied = new boolean[enemies.size()];

            // Create boss
            boss = new Cthulu(player);
            boss.setBounds(0, 0, bgWidth, bgHeight);
            boss.setPosition(WORLD_RIGHT - Cthulu.FRAME_W, bgHeight - Cthulu.FRAME_H - GROUND_MARGIN + 35);
            add(boss);

            // Snap entities to ground
            SwingUtilities.invokeLater(() -> {
                player.forceSnapToGround(bgHeight, GROUND_MARGIN);
                for (Enemy e : enemies) {
                    e.snapToGround();
                }
                requestFocusInWindow();
            });

            gameTimer.start();
        }

        class GameUpdateListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGateLogic();
                updatePlayerBounds();
                updateCamera();
                updateDamageTracking();
                handleCombat();
                handleGemSpawn();
                handleGameEnd();
                repaint();
            }

            private void updateGateLogic() {
                if (!leftHalfCleared) {
                    boolean anyAlive = enemies.stream().anyMatch(en -> !en.isGone() && !en.isDead());
                    if (!anyAlive) {
                        leftHalfCleared = true;
                        if (!bossActivated) {
                            boss.setChaseEnabled(true);
                            boss.triggerRoar();
                            bossActivated = true;
                        }
                    }
                }
            }

            private void updatePlayerBounds() {
                int allowedRight = leftHalfCleared ? (WORLD_RIGHT - Player.FRAME_W) : (WORLD_HALF_R - Player.FRAME_W);
                int px = player.getXPos();
                px = Math.max(WORLD_LEFT, Math.min(px, allowedRight));
                
                if (px != player.getXPos()) {
                    player.setPosition(px, player.getYPos());
                }
            }

            private void updateCamera() {
                int playerCenter = player.getXPos() + Player.FRAME_W / 2;
                int halfScreen = Math.max(1, getWidth() / 2);
                cameraX = playerCenter - halfScreen;
                cameraX = Math.max(0, Math.min(cameraX, Math.max(0, bgWidth - getWidth())));
            }

            private void updateDamageTracking() {
                if (player.isAttacking()) {
                    int cur = player.getCurrentFrameIndex();
                    if (cur < lastPlayerFrame) {
                        Arrays.fill(enemyHitApplied, false);
                        bossHitApplied = false;
                    }
                    lastPlayerFrame = cur;
                } else {
                    Arrays.fill(enemyHitApplied, false);
                    bossHitApplied = false;
                    lastPlayerFrame = -1;
                }
            }

            private void handleCombat() {
                // Player attacks enemies
                if (player.isAttacking() && player.isAtHitFrame()) {
                    for (int i = 0; i < enemies.size(); i++) {
                        Enemy en = enemies.get(i);
                        if (!enemyHitApplied[i] && !en.isGone() && !en.isDead()
                                && player.getHitBox().intersects(en.getHitBox())) {
                            en.takeDamage(player.getAtk());
                            enemyHitApplied[i] = true;
                        }
                    }
                }

                // Player attacks boss
                if (player.isAttacking() && player.isAtHitFrame()) {
                    if (!bossHitApplied && boss != null && !boss.isGone() && !boss.isDead()
                            && player.getHitBox().intersects(boss.getHitBox())) {
                        boss.takeDamageFromPlayer(player.getAtk(), player.isInAttack2());
                        bossHitApplied = true;
                    }
                }

                // Enemies attack player
                for (Enemy en : enemies) {
                    if (en.tryHit(player.getHitBox())) {
                        player.takeDamage(en.getAtk());
                    }
                }

                // Boss attacks player
                if (boss.tryHit(player.getHitBox())) {
                    player.takeDamage(boss.getAtk());
                }
            }

            private void handleGemSpawn() {
                if (gem == null && boss.isDead() && boss.isGone()) {
                    int gemX = boss.getHitBox().x + boss.getHitBox().width / 2 - 16;
                    int groundY = bgHeight - GROUND_MARGIN - 16;
                    int gemY = Math.min(groundY, boss.getHitBox().y + boss.getHitBox().height - 64);

                    gem = new Gem(gemX, gemY);
                    gem.setWorldBoundsDimension(bgWidth, bgHeight);
                    gem.setOpaque(false);
                    add(gem);
                }

                if (!gameWon && gem != null && !gem.isPicked()) {
                    if (player.getHitBox().intersects(gem.getHitBox())) {
                        gem.pick();
                        gameWon = true;
                        gameTimer.stop();
                    }
                }
            }

            private void handleGameEnd() {
                if (!gameDefeated && player.isDead() && player.isGone()) {
                    gameDefeated = true;
                    try {
                        gameTimer.stop();
                    } catch (Exception ignore) {
                    }
                    repaint();
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(imgBg, 0, 0, null);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D world = (Graphics2D) g.create();
            world.translate(-cameraX, 0);
            super.paint(world);
            world.dispose();
            drawHUD((Graphics2D) g);
        }

        private void drawHUD(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(hudIcon, HUD_X, HUD_Y, this);

            int hp = player.getHp(), maxHp = player.getMaxHp();
            float pct = Math.max(0f, Math.min(1f, hp / (float) maxHp));
            int x = HUD_X + BAR_OFFSET_X, y = HUD_Y + BAR_OFFSET_Y;

            g2.setColor(new Color(200, 40, 40));
            g2.fillRect(x, y, Math.round(BAR_W * pct), BAR_H);

            g2.setColor(Color.BLACK);
            g2.drawRect(x, y, BAR_W, BAR_H);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            g2.setColor(Color.WHITE);
            g2.drawString(hp + " / " + maxHp, x + BAR_W - 100, y + BAR_H - 1);

            if (gameWon) {
                String msg = "VICTORY!";
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 24f));
                g2.setColor(new Color(255, 255, 255));
                int w = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, (getWidth() - w) / 2, 60);
            }

            if (gameDefeated) {
                String msg = "DEFEAT";
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
                g2.setColor(new Color(220, 60, 60));
                int wStr = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, (getWidth() - wStr) / 2, 60);
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            player.onKeyPressed(e.getKeyCode());
        }

        @Override
        public void keyReleased(KeyEvent e) {
            player.onKeyReleased(e.getKeyCode());
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameLauncher::new);
    }
}