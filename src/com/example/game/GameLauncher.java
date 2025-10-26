package com.example.game;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.sound.sampled.*;
import javax.swing.*;
import java.io.*;

public class GameLauncher extends JFrame {

    private Clip bgmClip;     // เพลงประกอบ title screen

    public GameLauncher() {
        setTitle("LEGEND OF CTHULU");
        setSize(928, 396);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // เริ่มที่ Title Screen ก่อน
        setContentPane(new TitleScreenPanel());
        setVisible(true);
    }

    // -------------------------------------------------
    // 🔷 Title Screen Panel
    // -------------------------------------------------
    class TitleScreenPanel extends JPanel implements ActionListener, KeyListener {
        private Image titleImage;
        private boolean showText = true;
        private Timer blinkTimer;
        private boolean started = false;

        TitleScreenPanel() {
            setFocusable(true);
            setBackground(Color.BLACK);
            addKeyListener(this);

            // โหลดภาพ Title
            try {
                URL titleURL = getClass().getResource("/assets/Title Screen.png");
                titleImage = new ImageIcon(titleURL).getImage();
            } catch (Exception e) {
                System.err.println("ไม่พบ TitleScreen.png");
            }

            // โหลดเพลงประกอบ
            playBGM("/assets/sound/intense-fantasy-soundtrack-201079.wav");

            // กระพริบข้อความทุก 500ms
            blinkTimer = new Timer(500, this);
            blinkTimer.start();
        }

        private void playBGM(String path) {
            try (InputStream audioSrc = getClass().getResourceAsStream(path)) {
                if (audioSrc == null) return;
                InputStream bufferedIn = new BufferedInputStream(audioSrc);
                AudioInputStream ais = AudioSystem.getAudioInputStream(bufferedIn);
                bgmClip = AudioSystem.getClip();
                bgmClip.open(ais);
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                bgmClip.start();
            } catch (Exception e) {
                System.err.println("โหลด soundtrack.wav ไม่ได้: " + e.getMessage());
            }
        }

        private void stopBGM() {
            if (bgmClip != null && bgmClip.isRunning()) {
                bgmClip.stop();
                bgmClip.close();
            }
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // วาดพื้นหลัง
            if (titleImage != null) {
                g2.drawImage(titleImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }


            // วาดข้อความกระพริบ
            if (showText) {
                g2.setFont(new Font("Monospaced", Font.BOLD, 22));
                g2.setColor(Color.WHITE);
                g2.drawString("Press K Button to Start", getWidth()/2 - 150, getHeight()/2 + 80);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showText = !showText;
            repaint();
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (!started && e.getKeyCode() == KeyEvent.VK_K) {
                started = true;

                // หยุดกระพริบ/เพลงของหน้า Title
                if (blinkTimer != null) blinkTimer.stop();
                stopBGM();

                // โหลดฉากเกม
                URL bgURL = getClass().getResource("/assets/Background.png");
                Image bgImage = new ImageIcon(bgURL).getImage();
                DrawArea game = new DrawArea(bgImage);

                // ❗️สลับคอนเทนต์ "บนเฟรม" ไม่ใช่บน JPanel
                GameLauncher.this.setContentPane(game);
                GameLauncher.this.revalidate();
                GameLauncher.this.repaint();

                // โอนโฟกัสให้เกมรับคีย์ได้ทันที
                SwingUtilities.invokeLater(game::requestFocusInWindow);
            }
        }

        @Override public void keyReleased(KeyEvent e) {}
        @Override public void keyTyped(KeyEvent e) {}
    }

    // -------------------------------------------------
    // 🔷 DrawArea (จากเกมเดิม)
    // -------------------------------------------------
    class DrawArea extends JPanel implements KeyListener {
        Image imgBg;
        int bgWidth, bgHeight;
        float cameraX = 0f;

        Image hudIcon;
        final int HUD_X = 12, HUD_Y = 0;
        final int BAR_OFFSET_X = 77;
        final int BAR_OFFSET_Y = 83;
        final int BAR_W = 110;
        final int BAR_H = 14;

        final int GROUND_MARGIN = 50;

        Player player;
        Enemy enemy;
        Cthulu boss;

        private int lastPlayerFrame = -1;
        private boolean appliedEnemy = false;
        private boolean appliedBoss = false;

        Timer t = new Timer(16, new Listener());

        DrawArea(Image img){
            this.imgBg = img;
            this.bgWidth  = img.getWidth(null);
            this.bgHeight = img.getHeight(null);

            setLayout(null);
            setFocusable(true);
            addKeyListener(this);
            requestFocusInWindow();
            setPreferredSize(new Dimension(bgWidth, bgHeight));
            hudIcon = new ImageIcon(getClass().getResource("/assets/HealthBar.png")).getImage();

            player = new Player();
            enemy  = new Enemy(600, player);
            boss   = new Cthulu(player);

            player.setBounds(0, 0, bgWidth, bgHeight);
            enemy.setBounds(0, 0, bgWidth, bgHeight);
            boss.setBounds(0, 0, bgWidth, bgHeight);

            add(player);
            add(enemy);
            add(boss);

            player.forceSnapToGround(bgHeight, GROUND_MARGIN);
            enemy.snapToGround();
            int bossY = bgHeight - Cthulu.FRAME_H - GROUND_MARGIN;
            boss.setPosition(bgWidth - Cthulu.FRAME_W, Math.max(0, bossY + 35));

            SwingUtilities.invokeLater(player::requestFocusInWindow);
            t.start();
        }

        class Listener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                int playerCenter = player.getXPos() + Player.FRAME_W / 2;
                int halfScreen = Math.max(1, getWidth() / 2);
                cameraX = playerCenter - halfScreen;
                cameraX = Math.max(0, Math.min(cameraX, Math.max(0, bgWidth - getWidth())));

                if (player.isAttacking()) {
                    int cur = player.getCurrentFrameIndex();
                    if (cur < lastPlayerFrame) {
                        appliedEnemy = false;
                        appliedBoss  = false;
                    }
                    lastPlayerFrame = cur;
                } else {
                    appliedEnemy = false;
                    appliedBoss  = false;
                    lastPlayerFrame = -1;
                }

                // player → enemy
                if (player.isAttacking() && player.isAtHitFrame() && !enemy.isGone() && !appliedEnemy) {
                    if (player.getHitBox().intersects(enemy.getHitBox()) && !enemy.isDead()) {
                        enemy.takeDamage(player.getAtk());
                        appliedEnemy = true;
                    }
                }

                // player → boss
                if (player.isAttacking() && player.isAtHitFrame() && !boss.isGone() && !appliedBoss) {
                    if (player.getHitBox().intersects(boss.getHitBox()) && !boss.isDead()) {
                        boss.takeDamage(player.getAtk(), player.isInAttack2());
                        appliedBoss = true;
                    }
                }

                // boss/enemy → player
                if (!player.isInvulnerable() && boss.tryHit(player.getHitBox())) {
                    player.takeDamage(boss.getAtk());
                }
                if (!player.isInvulnerable() && enemy.tryHit(player.getHitBox())) {
                    player.takeDamage(enemy.getAtk());
                }

                repaint();
            }
        }

        private void drawHUD(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(hudIcon, HUD_X, HUD_Y, this);
            int hp = player.getHp();
            int maxHp = player.getMaxHp();
            float percent = Math.max(0f, Math.min(1f, hp / (float) maxHp));
            int barX = HUD_X + BAR_OFFSET_X;
            int barY = HUD_Y + BAR_OFFSET_Y;
            g2.setColor(new Color(200, 40, 40));
            g2.fillRect(barX, barY, Math.round(BAR_W * percent), BAR_H);
            g2.setColor(Color.BLACK);
            g2.drawRect(barX, barY, BAR_W, BAR_H);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            g2.setColor(Color.WHITE);
            g2.drawString(hp + " / " + maxHp, barX + BAR_W - 100, barY + BAR_H - 1);
        }

        @Override
        protected void paintComponent(Graphics g){
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

		@Override
		public void keyTyped(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void keyPressed(KeyEvent e) {
		    int key = e.getKeyCode();
		    if (key == KeyEvent.VK_A) {
		        player.walk('A');
		    } else if (key == KeyEvent.VK_D) {
		        player.walk('D');
		    } else if (key == KeyEvent.VK_J) {
		        player.attack();
		    }
			
		}

		@Override
		public void keyReleased(KeyEvent e) {
		    int key = e.getKeyCode();
		    if (key == KeyEvent.VK_A) {
		        player.stopWalking('A');
		    } else if (key == KeyEvent.VK_D) {
		        player.stopWalking('D');
		    }
		}

    }

    // -------------------------------------------------
    // 🔷 Main
    // -------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameLauncher::new);
    }
}
