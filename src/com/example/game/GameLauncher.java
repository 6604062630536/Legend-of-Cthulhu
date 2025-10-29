package com.example.game;

import com.example.game.entities.*; // Player, Enemy, Cthulu
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import javax.swing.Timer;

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

            // รูป Title
            titleImage = loadImage("/assets/Title Screen.png");

            bgmClip = loadClip("/assets/sound/intense-fantasy-soundtrack-201079.wav");
            if (bgmClip != null) {
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                bgmClip.start();
            }

            // กระพริบข้อความ
            blinkTimer = new Timer(500, this);
            blinkTimer.start();
        }

        private Image loadImage(String path) {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("ไม่พบไฟล์รูป: " + path);
                return null;
            }
            return new ImageIcon(url).getImage();
        }

        private Clip loadClip(String path) {
            try (InputStream in = getClass().getResourceAsStream(path)) {
                if (in == null) {
                    System.err.println("ไม่พบไฟล์เสียง: " + path);
                    return null;
                }
                AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(in));
                Clip c = AudioSystem.getClip();
                c.open(ais);
                return c;
            } catch (Exception e) {
                System.err.println("โหลดเสียงล้มเหลว: " + path + " -> " + e.getMessage());
                return null;
            }
        }

        private void stopBGM() {
            try {
                if (bgmClip != null) {
                    if (bgmClip.isRunning())
                        bgmClip.stop();
                    bgmClip.close();
                }
            } catch (Exception ignore) {
            }
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
                if (blinkTimer != null)
                    blinkTimer.stop();
                stopBGM();

                // โหลดพื้นหลังเวิลด์ (อยู่ที่ /assets/Background.png)
                URL bgURL = getClass().getResource("/assets/Background.png");
                Image bgImage = new ImageIcon(bgURL).getImage();

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
        // --- Background / world ---
        final Image imgBg;
        final int bgWidth, bgHeight;
        float cameraX = 0f;

        // --- HUD ---
        Image hudIcon;
        final int HUD_X = 12, HUD_Y = 0;
        final int BAR_OFFSET_X = 77, BAR_OFFSET_Y = 83, BAR_W = 110, BAR_H = 14;

        // --- World constants ---
        final int GROUND_MARGIN = 50;
        final int WORLD_LEFT = 0;
        final int WORLD_HALF_R = 928; // ขอบเขตสิทธิ์เข้า หลังเคลียร์ศัตรู
        final int WORLD_RIGHT = 1952; // พื้นหลังของคุณกว้าง 1952

        // --- Entities ---
        Player player;
        java.util.List<Enemy> enemies = new java.util.ArrayList<>();
        Cthulu boss;
        Gem gem = null;              
        boolean gameWon = false;
        boolean gameDefeated = false;  

        // gate
        boolean leftHalfCleared = false;
        boolean bossActivated = false;

        // damage flags
        private int lastPlayerFrame = -1;
        private boolean[] enemyHitApplied; // ต่อศัตรูหลายตัว
        private boolean bossHitApplied = false;

        javax.swing.Timer t = new javax.swing.Timer(16, new Listener());

        DrawArea(Image img) {
            this.imgBg = img;
            this.bgWidth = img.getWidth(null);
            this.bgHeight = img.getHeight(null);

            setLayout(null);
            setFocusable(true);
            addKeyListener(this);

            hudIcon = new ImageIcon(getClass().getResource("/assets/HealthBar.png")).getImage();

            // --- Create entities ---
            player = new Player();
            player.setBounds(0, 0, bgWidth, bgHeight);
            add(player);

            // 👉 สร้างศัตรู “ในครึ่งซ้าย” และวาง ให้ติดพื้น
            int[] spawnX = { 520, 700, 860 }; // ปรับตำแหน่งตามต้องการ (ไม่เกิน 928)
            for (int x : spawnX) {
                Enemy e = new Enemy(x, player);
                e.setBounds(0, 0, bgWidth, bgHeight);
                e.snapToGround();
                enemies.add(e);
                add(e);
            }
            enemyHitApplied = new boolean[enemies.size()];

            // บอสยังเกิดขวาสุดไว้ก่อน (จะสู้เมื่อเดินไปถึงครึ่งขวา)
            boss = new Cthulu(player);
            boss.setBounds(0, 0, bgWidth, bgHeight);
            boss.setPosition(WORLD_RIGHT - Cthulu.FRAME_W, bgHeight - Cthulu.FRAME_H - GROUND_MARGIN + 35);
            add(boss);

            // snap ให้ทุกตัว “ติดพื้น” หลังได้ขนาด panel จริง
            SwingUtilities.invokeLater(() -> {
                player.forceSnapToGround(bgHeight, GROUND_MARGIN);
                for (Enemy e : enemies)
                    e.snapToGround();
                requestFocusInWindow();
            });

            t.start();
        }

        class Listener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                // --- Gate: จำกัดขอบขวาตามสถานะเคลียร์ ---
                if (!leftHalfCleared) {
                    // ถ้าฆ่าศัตรูครบแล้ว เปิดประตู
                    boolean anyAlive = enemies.stream().anyMatch(en -> !en.isGone() && !en.isDead());
                    if (!anyAlive) {
                        leftHalfCleared = true;
                        // ✅ ปลดล็อคบอส + ให้ร้อง 1 ครั้ง
                        if (!bossActivated) {
                            boss.setChaseEnabled(true); // ปลด chaseLocked
                            boss.triggerRoar(); // ร้องครั้งแรก
                            bossActivated = true;
                        }
                    }
                }

                // --- ปรับ X ของ player ไม่ให้ข้ามประตูก่อนเวลา ---
                int allowedRight = leftHalfCleared ? (WORLD_RIGHT - Player.FRAME_W) : (WORLD_HALF_R - Player.FRAME_W);
                // clamp แบบ manual (เพราะ player.walk ใช้ความกว้าง panel ซึ่งเป็น world อยู่แล้ว)
                int px = player.getXPos();
                if (px < WORLD_LEFT)
                    px = WORLD_LEFT;
                if (px > allowedRight)
                    px = allowedRight;
                // set กลับ (ให้เดินชนกำแพงนิ่ม ๆ)
                // เคล็ดลับ: ใช้ method เดิมไม่ได้ ก็เลื่อนด้วย walk เพื่ออยู่ใน state RUN; ที่นี่ตั้งตรง ๆ ก็พอ
                // (สมมุติ Player มี setter X; ถ้าไม่มี ให้เพิ่มสั้น ๆ)
                try {
                    java.lang.reflect.Field fx = Player.class.getDeclaredField("x");
                    fx.setAccessible(true);
                    fx.setInt(player, px);
                } catch (Exception ignore) {
                }

                // --- Camera follow ---
                int playerCenter = player.getXPos() + Player.FRAME_W / 2;
                int halfScreen = Math.max(1, getWidth() / 2);
                cameraX = playerCenter - halfScreen;
                cameraX = Math.max(0, Math.min(cameraX, Math.max(0, bgWidth - getWidth())));

                // --- รีเซ็ตธง hit ต่อ “แต่ละศัตรู” เมื่อเริ่มแอนิเมชันโจมตีชุดใหม่ ---
                if (player.isAttacking()) {
                    int cur = player.getCurrentFrameIndex();
                    if (cur < lastPlayerFrame) {
                        java.util.Arrays.fill(enemyHitApplied, false);
                        bossHitApplied = false;
                    }
                    lastPlayerFrame = cur;
                } else {
                    java.util.Arrays.fill(enemyHitApplied, false);
                    bossHitApplied = false;
                    lastPlayerFrame = -1;
                }

                // --- Player → Enemies ---
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

                // ✅ --- Player → Boss ---
                if (player.isAttacking() && player.isAtHitFrame()) {
                    if (!bossHitApplied && boss != null && !boss.isGone() && !boss.isDead()
                            && player.getHitBox().intersects(boss.getHitBox())) {
                        // โจมตีครั้งที่ 2 ให้บอสติด freeze
                        if (player.isInAttack2()) {
                            boss.takeDamageFromPlayer(player.getAtk(), true);
                        } else {
                            boss.takeDamageFromPlayer(player.getAtk(), false);
                        }
                        bossHitApplied = true;
                    }
                }

                // --- Enemies/Boss → Player ---
                if (!player.isInvulnerable()) {
                    for (Enemy en : enemies) {
                        if (en.tryHit(player.getHitBox()))
                            player.takeDamage(en.getAtk());
                    }
                    if (boss.tryHit(player.getHitBox()))
                        player.takeDamage(boss.getAtk());
                }
                
                if (gem == null && boss.isDead() && boss.isGone()) {
                    // ให้วางเจมเหนือพื้นเล็กน้อย
                    int gemX = boss.getHitBox().x + boss.getHitBox().width / 2 - 16;
                    int groundY = bgHeight - GROUND_MARGIN - 16;
                    int gemY = Math.min(groundY, boss.getHitBox().y + boss.getHitBox().height - 64);

                    gem = new Gem(gemX, gemY);
                    gem.setWorldBoundsDimension(bgWidth, bgHeight);
                    gem.setOpaque(false);
                    add(gem);        
                }
             // ----- เก็บเจมเพื่อจบเกม -----
                if (!gameWon && gem != null && !gem.isPicked()) {
                    if (player.getHitBox().intersects(gem.getHitBox())) {
                        gem.pick();
                        gameWon = true;
                        t.stop();  
                    }
                }
                if (!gameDefeated && player.isDead() && player.isGone()) {
                    gameDefeated = true;

                    try { t.stop(); } catch (Exception ignore) {}
                    repaint();
                    return; 
                }


                repaint();
            }
        }

        // --- Painting ---
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(imgBg, 0, 0, null);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D world = (Graphics2D) g.create();
            world.translate(-cameraX, 0); // กล้อง
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
                String msg = "VICTORY! ";
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

        // --- KeyListener: ส่งต่อให้ Player คุมการเคลื่อนที่ทันที ---
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