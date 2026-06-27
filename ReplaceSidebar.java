import java.nio.file.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class ReplaceSidebar {
    public static void main(String[] args) throws Exception {
        Path p = Paths.get("src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java");
        List<String> lines = Files.readAllLines(p);
        
        int start = 225; // 0-indexed line 226
        int end = 547; // 0-indexed line 548
        
        List<String> newLines = new ArrayList<>();
        for (int i = 0; i < start; i++) {
            newLines.add(lines.get(i));
        }
        
        String replacement = 
            "    private JPanel createSidebar() {\n" +
            "        sidebarPanel = new JPanel(new BorderLayout());\n" +
            "        sidebarPanel.setBackground(Color.decode(\"#F6F7FB\"));\n" +
            "        sidebarPanel.setBorder(new EmptyBorder(16, 16, 16, 12));\n\n" +
            "        JPanel cardPanel = new JPanel(new BorderLayout()) {\n" +
            "            @Override protected void paintComponent(Graphics g) {\n" +
            "                Graphics2D g2 = (Graphics2D)g.create();\n" +
            "                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);\n" +
            "                g2.setColor(SIDEBAR_BG);\n" +
            "                g2.fillRoundRect(0,0,getWidth(),getHeight(), 20, 20);\n" +
            "                g2.setColor(Color.decode(\"#E2E8F0\"));\n" +
            "                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1, 20, 20);\n" +
            "                g2.dispose();\n" +
            "            }\n" +
            "        };\n" +
            "        cardPanel.setOpaque(false);\n\n" +
            "        JPanel topWrapper = new JPanel(new BorderLayout());\n" +
            "        topWrapper.setOpaque(false);\n\n" +
            "        JPanel logoPanel = new JPanel();\n" +
            "        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.X_AXIS));\n" +
            "        logoPanel.setOpaque(false);\n" +
            "        logoPanel.setBorder(new EmptyBorder(30, 16, 20, 0));\n\n" +
            "        JLabel lblLogoIcon = new JLabel();\n" +
            "        try {\n" +
            "            java.net.URL logoUrl = getClass().getResource(\"/images/logomoi.png\");\n" +
            "            if (logoUrl != null) {\n" +
            "                java.awt.Image img = javax.imageio.ImageIO.read(logoUrl);\n" +
            "                if (img != null) {\n" +
            "                    lblLogoIcon.setIcon(new javax.swing.ImageIcon(img.getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH)));\n" +
            "                    this.setIconImage(img);\n" +
            "                }\n" +
            "            }\n" +
            "        } catch (Exception ex) {\n" +
            "            com.formdev.flatlaf.extras.FlatSVGIcon fb = new com.formdev.flatlaf.extras.FlatSVGIcon(\"images/icon/home.svg\", 32, 32);\n" +
            "            fb.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(c -> PRIMARY));\n" +
            "            lblLogoIcon.setIcon(fb);\n" +
            "        }\n\n" +
            "        JPanel textPanel = new JPanel();\n" +
            "        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));\n" +
            "        textPanel.setOpaque(false);\n\n" +
            "        lblLogoText = new JLabel(\"TutorHub\");\n" +
            "        lblLogoText.setFont(new Font(\"Segoe UI\", Font.BOLD, 17));\n" +
            "        lblLogoText.setForeground(PRIMARY);\n" +
            "        lblLogoText.setAlignmentX(Component.LEFT_ALIGNMENT);\n\n" +
            "        JLabel lblEnterprise = new JLabel(\"Enterprise\");\n" +
            "        lblEnterprise.setFont(new Font(\"Segoe UI\", Font.PLAIN, 11));\n" +
            "        lblEnterprise.setForeground(TEXT_MUTED);\n" +
            "        lblEnterprise.setAlignmentX(Component.LEFT_ALIGNMENT);\n\n" +
            "        textPanel.add(lblLogoText);\n" +
            "        textPanel.add(lblEnterprise);\n\n" +
            "        logoPanel.add(lblLogoIcon);\n" +
            "        logoPanel.add(Box.createRigidArea(new Dimension(8, 0)));\n" +
            "        logoPanel.add(textPanel);\n\n" +
            "        topWrapper.add(logoPanel, BorderLayout.WEST);\n\n" +
            "        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));\n" +
            "        togglePanel.setOpaque(false);\n" +
            "        togglePanel.setBorder(new EmptyBorder(36, 0, 0, 16));\n" +
            "        JLabel lblToggleIcon = new JLabel(\"\\u00ab\");\n" + 
            "        lblToggleIcon.setFont(new Font(\"Segoe UI\", Font.BOLD, 18));\n" +
            "        lblToggleIcon.setForeground(TEXT_MUTED);\n" +
            "        lblToggleIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));\n" +
            "        \n" +
            "        lblToggleIcon.addMouseListener(new MouseAdapter() {\n" +
            "            @Override\n" +
            "            public void mouseClicked(MouseEvent e) {\n" +
            "                isSidebarExpanded = !isSidebarExpanded;\n" +
            "                if(isSidebarExpanded) {\n" +
            "                    sidebarPanel.setMinimumSize(new Dimension(240, 0));\n" +
            "                    sidebarPanel.setPreferredSize(new Dimension(240, 0));\n" +
            "                    textPanel.setVisible(true);\n" +
            "                    lblLogoIcon.setVisible(true);\n" +
            "                    for(SidebarMenuItem item : menuItems) {\n" +
            "                        item.setExpanded(true);\n" +
            "                    }\n" +
            "                    lblToggleIcon.setText(\"\\u00ab\");\n" +
            "                } else {\n" +
            "                    sidebarPanel.setMinimumSize(new Dimension(88, 0));\n" +
            "                    sidebarPanel.setPreferredSize(new Dimension(88, 0));\n" +
            "                    textPanel.setVisible(false);\n" +
            "                    lblLogoIcon.setVisible(true);\n" +
            "                    for(SidebarMenuItem item : menuItems) {\n" +
            "                        item.setExpanded(false);\n" +
            "                    }\n" +
            "                    lblToggleIcon.setText(\"\\u00bb\");\n" +
            "                }\n" +
            "                revalidate();\n" +
            "                repaint();\n" +
            "            }\n" +
            "        });\n" +
            "        togglePanel.add(lblToggleIcon);\n" +
            "        topWrapper.add(togglePanel, BorderLayout.EAST);\n\n" +
            "        cardPanel.add(topWrapper, BorderLayout.NORTH);\n\n" +
            "        JPanel menuPanel = new JPanel();\n" +
            "        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));\n" +
            "        menuPanel.setOpaque(false);\n" +
            "        menuPanel.setBorder(new EmptyBorder(16, 12, 0, 12));\n\n" +
            "        menuPanel.add(createMenuItem(\"Bảng tin\", \"home\", \"Home\", 0));\n" +
            "        menuPanel.add(createMenuItem(\"Reels\", \"reels\", \"Reels\", 0));\n" +
            "        menuPanel.add(createMenuItem(\"Tin nhắn\", \"message\", \"Chat\", 1));\n\n" +
            "        menuPanel.add(Box.createVerticalStrut(8));\n" +
            "        menuPanel.add(createDivider());\n" +
            "        menuPanel.add(Box.createVerticalStrut(8));\n\n" +
            "        menuPanel.add(createMenuItem(\"Lớp học\", \"my-class\", \"Saved\", 0));\n" +
            "        menuPanel.add(createMenuItem(\"Đã nhận\", \"accepted-class\", \"Taken\", 0));\n" +
            "        menuPanel.add(createMenuItem(\"Lịch\", \"calendar\", \"Schedule\", 0));\n\n" +
            "        menuPanel.add(Box.createVerticalStrut(8));\n" +
            "        menuPanel.add(createDivider());\n" +
            "        menuPanel.add(Box.createVerticalStrut(8));\n\n" +
            "        menuPanel.add(createMenuItem(\"Thi\", \"exam\", \"Exam\", 0));\n" +
            "        menuPanel.add(createMenuItem(\"Nhiệm vụ\", \"task\", \"Todo\", 0));\n" +
            "        \n" +
            "        if (\"TUTOR\".equalsIgnoreCase(currentUserRole) || \"ADMIN\".equalsIgnoreCase(currentUserRole)) {\n" +
            "            menuPanel.add(createMenuItem(\"Đề thi\", \"document\", \"ExamPaper\", 0));\n" +
            "            menuPanel.add(createMenuItem(\"Câu hỏi\", \"profile\", \"QuestionBank\", 0));\n" +
            "        }\n" +
            "        \n" +
            "        menuPanel.add(createMenuItem(\"Tài liệu\", \"document\", \"Docs\", 0));\n" +
            "        menuPanel.add(createMenuItem(\"Bảng vẽ\", \"drawing\", \"Blackboard\", 0));\n" +
            "        menuPanel.add(createMenuItem(\"Hồ sơ\", \"profile\", \"Profile\", 0));\n\n" +
            "        JScrollPane scrollPane = new JScrollPane(menuPanel);\n" +
            "        scrollPane.setBorder(null);\n" +
            "        scrollPane.setOpaque(false);\n" +
            "        scrollPane.getViewport().setOpaque(false);\n" +
            "        scrollPane.getVerticalScrollBar().setUnitIncrement(16);\n" +
            "        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));\n" +
            "        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);\n\n" +
            "        cardPanel.add(scrollPane, BorderLayout.CENTER);\n\n" +
            "        bottomPanel = new JPanel(new BorderLayout());\n" +
            "        bottomPanel.setOpaque(false);\n" +
            "        bottomPanel.setBorder(new EmptyBorder(16, 16, 24, 16));\n" +
            "        bottomPanel.add(new PremiumCard(), BorderLayout.CENTER);\n" +
            "        \n" +
            "        cardPanel.add(bottomPanel, BorderLayout.SOUTH);\n\n" +
            "        sidebarPanel.add(cardPanel, BorderLayout.CENTER);\n\n" +
            "        return sidebarPanel;\n" +
            "    }\n\n" +
            "    private JPanel createDivider() {\n" +
            "        JPanel div = new JPanel() {\n" +
            "            @Override protected void paintComponent(Graphics g) {\n" +
            "                g.setColor(Color.decode(\"#EEF0F6\"));\n" +
            "                g.fillRect(12, 0, getWidth() - 24, 1);\n" +
            "            }\n" +
            "        };\n" +
            "        div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));\n" +
            "        div.setPreferredSize(new Dimension(0, 1));\n" +
            "        div.setOpaque(false);\n" +
            "        return div;\n" +
            "    }\n\n" +
            "    private JLabel createGroupLabel(String text) {\n" +
            "        JLabel lbl = new JLabel(text);\n" +
            "        lbl.setFont(new Font(\"Segoe UI\", Font.BOLD, 11));\n" +
            "        lbl.setForeground(TEXT_HEADING);\n" +
            "        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);\n" +
            "        lbl.setBorder(new EmptyBorder(0, 12, 10, 0));\n" +
            "        groupLabels.add(lbl);\n" +
            "        return lbl;\n" +
            "    }\n\n" +
            "    class SidebarMenuItem extends JPanel {\n" +
            "        private String cardName;\n" +
            "        private boolean isActive = false;\n" +
            "        private boolean isHovered = false;\n" +
            "        private JLabel lblIcon, lblText;\n" +
            "        private String iconName;\n" +
            "        private JPanel badgeWrapper;\n\n" +
            "        public SidebarMenuItem(String title, String iconName, String cardName, int badgeCount) {\n" +
            "            this.cardName = cardName;\n" +
            "            this.iconName = iconName;\n\n" +
            "            setLayout(new BorderLayout(12, 0));\n" +
            "            setOpaque(false);\n" +
            "            setBorder(new EmptyBorder(0, 12, 0, 12));\n" +
            "            setMaximumSize(new Dimension(999, 44));\n" +
            "            setPreferredSize(new Dimension(228, 44));\n" +
            "            setCursor(new Cursor(Cursor.HAND_CURSOR));\n" +
            "            setAlignmentX(Component.LEFT_ALIGNMENT);\n\n" +
            "            lblIcon = new JLabel();\n" +
            "            \n" +
            "            lblText = new JLabel(title);\n" +
            "            lblText.setFont(new Font(\"Segoe UI\", Font.PLAIN, 13));\n" +
            "            lblText.setForeground(TEXT_MUTED);\n\n" +
            "            add(lblIcon, BorderLayout.WEST);\n" +
            "            add(lblText, BorderLayout.CENTER);\n\n" +
            "            if (badgeCount > 0) {\n" +
            "                JPanel badgePanel = new JPanel(new BorderLayout()) {\n" +
            "                    @Override\n" +
            "                    protected void paintComponent(Graphics g) {\n" +
            "                        Graphics2D g2 = (Graphics2D) g.create();\n" +
            "                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);\n" +
            "                        g2.setColor(Color.decode(\"#EF4444\"));\n" +
            "                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getWidth(), getHeight());\n" +
            "                        g2.dispose();\n" +
            "                    }\n" +
            "                };\n" +
            "                badgePanel.setOpaque(false);\n" +
            "                badgePanel.setBorder(new EmptyBorder(2, 6, 2, 6));\n" +
            "                JLabel lblBadge = new JLabel(badgeCount > 99 ? \"99+\" : String.valueOf(badgeCount));\n" +
            "                lblBadge.setFont(new Font(\"Segoe UI\", Font.BOLD, 10));\n" +
            "                lblBadge.setForeground(Color.WHITE);\n" +
            "                badgePanel.add(lblBadge, BorderLayout.CENTER);\n\n" +
            "                badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));\n" +
            "                badgeWrapper.setOpaque(false);\n" +
            "                badgeWrapper.add(badgePanel);\n" +
            "                add(badgeWrapper, BorderLayout.EAST);\n" +
            "            }\n\n" +
            "            addMouseListener(new MouseAdapter() {\n" +
            "                @Override\n" +
            "                public void mouseEntered(MouseEvent e) {\n" +
            "                    if (!isActive) {\n" +
            "                        isHovered = true;\n" +
            "                        repaint();\n" +
            "                    }\n" +
            "                }\n\n" +
            "                @Override\n" +
            "                public void mouseExited(MouseEvent e) {\n" +
            "                    if (!isActive) {\n" +
            "                        isHovered = false;\n" +
            "                        repaint();\n" +
            "                    }\n" +
            "                }\n\n" +
            "                @Override\n" +
            "                public void mouseClicked(MouseEvent e) {\n" +
            "                    switchTab(SidebarMenuItem.this, cardName);\n" +
            "                }\n" +
            "            });\n" +
            "            \n" +
            "            setActive(false);\n" +
            "        }\n\n" +
            "        @Override\n" +
            "        protected void paintComponent(Graphics g) {\n" +
            "            super.paintComponent(g);\n" +
            "            if (isActive) {\n" +
            "                Graphics2D g2 = (Graphics2D) g.create();\n" +
            "                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);\n" +
            "                g2.setColor(Color.decode(\"#F3E8FF\"));\n" +
            "                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);\n" +
            "                g2.dispose();\n" +
            "            } else if (isHovered) {\n" +
            "                Graphics2D g2 = (Graphics2D) g.create();\n" +
            "                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);\n" +
            "                g2.setColor(Color.decode(\"#F1F5F9\"));\n" +
            "                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);\n" +
            "                g2.dispose();\n" +
            "            }\n" +
            "        }\n\n" +
            "        public void setExpanded(boolean expanded) {\n" +
            "            lblText.setVisible(expanded);\n" +
            "            if (badgeWrapper != null)\n" +
            "                badgeWrapper.setVisible(expanded);\n" +
            "            if (expanded) {\n" +
            "                setPreferredSize(new Dimension(228, 44));\n" +
            "                setMaximumSize(new Dimension(999, 44));\n" +
            "            } else {\n" +
            "                setPreferredSize(new Dimension(44, 44));\n" +
            "                setMaximumSize(new Dimension(44, 44));\n" +
            "            }\n" +
            "            revalidate();\n" +
            "            repaint();\n" +
            "        }\n\n" +
            "        public void setActive(boolean active) {\n" +
            "            this.isActive = active;\n" +
            "            this.isHovered = false;\n" +
            "            \n" +
            "            if (active) {\n" +
            "                lblText.setForeground(PRIMARY);\n" +
            "                lblText.setFont(new Font(\"Segoe UI\", Font.BOLD, 13));\n" +
            "            } else {\n" +
            "                lblText.setForeground(TEXT_MUTED);\n" +
            "                lblText.setFont(new Font(\"Segoe UI\", Font.PLAIN, 13));\n" +
            "            }\n" +
            "            \n" +
            "            try {\n" +
            "                com.formdev.flatlaf.extras.FlatSVGIcon svgIc = new com.formdev.flatlaf.extras.FlatSVGIcon(\n" +
            "                        \"images/tab-icons/\" + iconName + \".svg\", 20, 20);\n" +
            "                lblIcon.setIcon(svgIc);\n" +
            "            } catch (Exception ex) {\n" +
            "                try {\n" +
            "                    com.formdev.flatlaf.extras.FlatSVGIcon svgIc2 = new com.formdev.flatlaf.extras.FlatSVGIcon(\n" +
            "                            \"images/icon_svg/\" + iconName + \".svg\", 20, 20);\n" +
            "                    lblIcon.setIcon(svgIc2);\n" +
            "                } catch (Exception ex2) {\n" +
            "                    try {\n" +
            "                        com.formdev.flatlaf.extras.FlatSVGIcon svgIc3 = new com.formdev.flatlaf.extras.FlatSVGIcon(\n" +
            "                                \"images/icon/\" + iconName + \".svg\", 20, 20);\n" +
            "                        lblIcon.setIcon(svgIc3);\n" +
            "                    } catch (Exception ex3) {\n" +
            "                        String activeStr = active ? \"fluency\" : \"fluency-systems-regular\";\n" +
            "                        String colorStr = active ? \"\" : \"73708A/\";\n" +
            "                        setNetworkIcon(lblIcon,\n" +
            "                                \"https://img.icons8.com/\" + activeStr + \"/48/\" + colorStr + iconName + \".png\", 20, 20);\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "            repaint();\n" +
            "        }\n" +
            "    }\n";
        
        String[] repLines = replacement.split("\n");
        for (String l : repLines) {
            newLines.add(l);
        }
        
        for (int i = end; i < lines.size(); i++) {
            newLines.add(lines.get(i));
        }
        
        Files.write(p, newLines, StandardCharsets.UTF_8);
    }
}