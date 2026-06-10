package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

public class BoardPickerDialog extends JDialog {
    public interface BoardPickerListener {
        void onBoardSelected(String dbData);
    }

    private final String boardsDataList;
    private final BoardPickerListener listener;
    
    private final Color bg = TutorHubTheme.BACKGROUND;
    private final Color surface = TutorHubTheme.SURFACE;
    private final Color textDark = TutorHubTheme.TEXT_DARK;

    public BoardPickerDialog(Window owner, String boardsDataList, BoardPickerListener listener) {
        super(owner, "Chọn Bảng vẽ", ModalityType.APPLICATION_MODAL);
        this.boardsDataList = boardsDataList;
        this.listener = listener;

        setSize(700, 500);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(bg);
        setLayout(new BorderLayout());

        JLabel header = new JLabel("Kho Bảng vẽ Của Bạn");
        header.setFont(TutorHubTheme.font(Font.BOLD, 18));
        header.setForeground(textDark);
        header.setBorder(new EmptyBorder(16, 20, 10, 20));
        add(header, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new java.awt.GridLayout(0, 3, 16, 16));
        gridPanel.setBackground(bg);
        gridPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        List<BoardItem> items = parseBoards(boardsDataList);
        if (items.isEmpty()) {
            JLabel empty = new JLabel("Không có bảng vẽ nào được lưu.", SwingConstants.CENTER);
            empty.setFont(TutorHubTheme.font(Font.PLAIN, 14));
            add(empty, BorderLayout.CENTER);
        } else {
            for (BoardItem item : items) {
                gridPanel.add(createBoardCard(item));
            }
            JScrollPane scroll = new JScrollPane(gridPanel);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            add(scroll, BorderLayout.CENTER);
        }
    }

    private JPanel createBoardCard(BoardItem item) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(surface);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(TutorHubTheme.BORDER, 1),
            new EmptyBorder(8, 8, 8, 8)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setPreferredSize(new Dimension(180, 120));
        try {
            if (item.thumbnailBase64 != null && item.thumbnailBase64.contains("###")) {
                String imgB64 = item.thumbnailBase64.split("###")[0];
                if (imgB64.startsWith("data:image/png;base64,")) {
                    imgB64 = imgB64.replace("data:image/png;base64,", "");
                }
                byte[] imgBytes = Base64.getDecoder().decode(imgB64);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
                if (img != null) {
                    Image scaled = img.getScaledInstance(180, 120, Image.SCALE_SMOOTH);
                    imgLabel.setIcon(new ImageIcon(scaled));
                }
            } else {
                imgLabel.setText("Không có ảnh");
            }
        } catch (Exception e) {
            imgLabel.setText("Lỗi hiển thị");
        }
        
        JLabel titleLabel = new JLabel(item.title);
        titleLabel.setFont(TutorHubTheme.font(Font.BOLD, 14));
        titleLabel.setForeground(textDark);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        card.add(imgLabel, BorderLayout.CENTER);
        card.add(titleLabel, BorderLayout.SOUTH);
        
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int res = JOptionPane.showConfirmDialog(BoardPickerDialog.this, "Bạn có muốn mở bảng vẽ '" + item.title + "' vào lớp học không? Toàn bộ nét vẽ hiện tại sẽ bị ghi đè.", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    dispose();
                    if (listener != null) listener.onBoardSelected(item.thumbnailBase64);
                }
            }
        });
        return card;
    }

    private List<BoardItem> parseBoards(String data) {
        List<BoardItem> res = new ArrayList<>();
        if (data == null || data.equals("EMPTY")) return res;
        String[] boards = data.split(";;");
        for (String b : boards) {
            String[] parts = b.split("\\|");
            if (parts.length >= 8) {
                BoardItem item = new BoardItem();
                item.boardId = parts[0];
                item.title = parts[1];
                item.className = parts[2];
                item.thumbnailBase64 = parts[7];
                res.add(item);
            }
        }
        return res;
    }

    private static class BoardItem {
        String boardId;
        String title;
        String className;
        String thumbnailBase64;
    }
}
