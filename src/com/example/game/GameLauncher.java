package com.example.game;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.*;

public class GameLauncher extends JFrame {

    public GameLauncher() {
        URL bgURL = getClass().getResource("/assets/Background.png");
        Image bgImage = new ImageIcon(bgURL).getImage();
        add(new DrawArea(bgImage));
    }

    class DrawArea extends JPanel {
        // --- Background ---
        Image imgBg;
        int bgWidth, bgHeight;

        // --- Camera ---
        float cameraX = 0f;

        // --- HUD ---
        Image hudIcon;
        final int HUD_X = 12, HUD_Y = 0;
        final int BAR_OFFSET_X = 77;
        final int BAR_OFFSET_Y = 83;
        final int BAR_W = 110;
        final int BAR_H = 14;

        // พื้นร่วม
        final int GROUND_MARGIN = 50;

        // Entities
        Player player;
        Enemy  enemy;
        Cthulu boss;

        // flags ทำดาเมจ
        private int lastPlayerFrame = -1;
        private boolean appliedEnemy = false;
        private boolean appliedBoss  = false;

        Timer t = new Timer(16, new Listener());

        DrawArea(Image img){
            this.imgBg = img;
            this.bgWidth  = img.getWidth(null);
            this.bgHeight = img.getHeight(null);

            setLayout(null);
            setPreferredSize(new Dimension(bgWidth, bgHeight));

            // HUD image
            hudIcon = new ImageIcon(getClass().getResource("/assets/HealthBar.png")).getImage();

            // สร้าง entity
            player = new Player();
            enemy  = new Enemy(600, player);
            boss   = new Cthulu(player);

            // ให้ panel ของแต่ละ entity ครอบเต็ม world (พ่นภาพภายในเอง)
            player.setBounds(0, 0, bgWidth, bgHeight);
            enemy.setBounds(0, 0, bgWidth, bgHeight);
            boss.setBounds(0, 0, bgWidth, bgHeight);

            add(player);
            add(enemy);
            add(boss);

            // ตำแหน่งเริ่มต้น: ให้ทุกตัวอยู่พื้นเดียวกัน
            player.forceSnapToGround(bgHeight, GROUND_MARGIN);
            enemy.snapToGround();
            int bossY = bgHeight - Cthulu.FRAME_H - GROUND_MARGIN;
            // บอสเกิด "ขอบขวาสุด" ของแมพ
            boss.setPosition(bgWidth - Cthulu.FRAME_W, Math.max(0, bossY));

            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) {
                    // อัปเดต bounds ของ panels ให้เท่ากับ world
                    player.setBounds(0, 0, bgWidth, bgHeight);
                    enemy.setBounds(0, 0, bgWidth, bgHeight);
                    boss.setBounds(0, 0, bgWidth, bgHeight);

                    // วางพื้น
                    player.forceSnapToGround(bgHeight, GROUND_MARGIN);
                    enemy.snapToGround();
                    int bossY = bgHeight - Cthulu.FRAME_H - GROUND_MARGIN;
                    // อย่าขยับ X ของบอสเมื่อรีไซส์ (ยังอยู่ขวาสุดของ world อยู่แล้ว)
                    boss.setPosition(boss.getHitBox().x - 30, Math.max(0, bossY+35)); // ใช้ y ใหม่

                    player.requestFocusInWindow();
                }

                @Override public void componentShown(ComponentEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        player.forceSnapToGround(bgHeight, GROUND_MARGIN);
                        enemy.snapToGround();
                        int bY = bgHeight - Cthulu.FRAME_H - GROUND_MARGIN;
                        boss.setPosition(bgWidth - Cthulu.FRAME_W, Math.max(0, bY));
                        player.requestFocusInWindow();
                    });
                }
            });

            SwingUtilities.invokeLater(player::requestFocusInWindow);
            t.start();
        }

        class Listener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                // --- Camera follow (center on player) ---
                int playerCenter = player.getXPos() + Player.FRAME_W / 2;
                int halfScreen = Math.max(1, getWidth() / 2);
                cameraX = playerCenter - halfScreen;

                // Clamp camera within world
                cameraX = Math.max(0, Math.min(cameraX, Math.max(0, bgWidth - getWidth())));

                // --- รีเซ็ตธงเมื่อเริ่มอนิเมชันใหม่ (เช่นจาก Attack1 → Attack2) ---
                if (player.isAttacking()) {
                    int cur = player.getCurrentFrameIndex();
                    if (cur < lastPlayerFrame) { // เฟรมวนใหม่
                        appliedEnemy = false;
                        appliedBoss  = false;
                    }
                    lastPlayerFrame = cur;
                } else {
                    appliedEnemy = false;
                    appliedBoss  = false;
                    lastPlayerFrame = -1;
                }

                // --- ผู้เล่น → มินเนียน ---
                if (player.isAttacking() && player.isAtHitFrame() && !enemy.isGone() && !appliedEnemy) {
                    if (player.getHitBox().intersects(enemy.getHitBox()) && !enemy.isDead()) {
                        enemy.takeDamage(player.getAtk());
                        appliedEnemy = true;
                    }
                }

                // --- ผู้เล่น → บอส ---
                if (player.isAttacking() && player.isAtHitFrame() && !boss.isGone() && !appliedBoss) {
                    if (player.getHitBox().intersects(boss.getHitBox()) && !boss.isDead()) {
                        boss.takeDamage(player.getAtk(), player.isInAttack2());
                        appliedBoss = true;
                    }
                }

                // --- บอส/มินเนียน → ผู้เล่น ---
                if (!player.isInvulnerable() && boss.tryHit(player.getHitBox())) {
                    player.takeDamage(boss.getAtk());
                }
                if (!player.isInvulnerable() && enemy.tryHit(player.getHitBox())) {
                    player.takeDamage(enemy.getAtk());
                }

                repaint();
            }
        }

        // --- HUD ---
        private void drawHUD(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            g2.drawImage(hudIcon, HUD_X, HUD_Y, this);

            int hp = player.getHp();
            int maxHp = player.getMaxHp();
            float percent = Math.max(0f, Math.min(1f, hp / (float) maxHp));
            int barX = HUD_X + BAR_OFFSET_X;
            int barY = HUD_Y + BAR_OFFSET_Y;
            drawRec(g2, barX, barY, BAR_W, BAR_H, percent);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            g2.setColor(Color.WHITE);
            g2.drawString(hp + " / " + maxHp, barX + BAR_W - 100, barY + BAR_H - 1);
        }

        private void drawRec(Graphics2D g, int x, int y, int w, int h, float percent) {
            int len = Math.round(w * percent);
            g.setColor(new Color(200, 40, 40));
            g.fillRect(x, y, len, h);
            g.setColor(new Color(50, 30, 30));
            g.drawRect(x, y, w, h);
        }

        // --- Painting ---
        @Override
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            // วาดพื้นหลังแบบ world-space (ไม่ยืดภาพ)
            // การขยับจะทำใน paint() ด้วยการ translate(-cameraX, 0)
            g.drawImage(imgBg, 0, 0, null);
        }

        @Override
        public void paint(Graphics g) {
            // 1) world-space: กล้องเลื่อนทั้งฉากและ children
            Graphics2D world = (Graphics2D) g.create();
            world.translate(-cameraX, 0);
            super.paint(world); // เรียก paintComponent + paintChildren ด้วย transform นี้
            world.dispose();

            // 2) screen-space: HUD ไม่เลื่อนตามกล้อง
            drawHUD((Graphics2D) g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameLauncher frame = new GameLauncher();
            frame.setTitle("LEGEND OF CTHULU");
            frame.setSize(928, 396); // หน้าจอมาตรฐาน; world = 1952x396
            frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
