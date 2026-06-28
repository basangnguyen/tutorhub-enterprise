package com.mycompany.tutorhub_enterprise.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ExcelEditorDialog extends JDialog {
    private final CefBrowser browser;
    private final CefMessageRouter msgRouter;
    private static File extractAssetsDir = null;

    public ExcelEditorDialog(Frame owner, DefaultTableModel tableModel, boolean isDegree) {
        super(owner, isDegree ? "Quản lý Bằng cấp (Excel)" : "Quản lý Chứng chỉ (Excel)", true);
        setSize(1100, 750);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // 1. Giải nén tài nguyên ra thư mục cục bộ nếu chạy từ file JAR
        File assetsDir = getExtractedAssetsDir();
        String htmlFileName = isDegree ? "deg_excel.html" : "cert_excel.html";
        File htmlFile = new File(assetsDir, htmlFileName);
        String htmlUrl = htmlFile.toURI().toString();

        System.out.println("[EXCEL_EDITOR] Loading UI from: " + htmlUrl);
        browser = JcefManager.getClient().createBrowser(htmlUrl, false, false);

        // 2. Tạo MessageRouter để nhận sự kiện từ JavaScript (cefQuery)
        msgRouter = CefMessageRouter.create();
        msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                if (request.startsWith("SAVE:") || request.startsWith("SAVE_DEG:")) {
                    String json = request.substring(request.indexOf(":") + 1);
                    handleSaveData(json, tableModel, isDegree);
                    callback.success("OK");
                    SwingUtilities.invokeLater(() -> dispose());
                    return true;
                } 
                else if (request.startsWith("EXPORT_XLSX:")) {
                    String base64 = request.substring("EXPORT_XLSX:".length());
                    handleExportXlsx(base64);
                    callback.success("OK");
                    return true;
                }
                return false;
            }
        }, true);
        
        JcefManager.getClient().addMessageRouter(msgRouter);
        add(browser.getUIComponent(), BorderLayout.CENTER);
    }

    private static synchronized File getExtractedAssetsDir() {
        if (extractAssetsDir != null && extractAssetsDir.exists()) {
            return extractAssetsDir;
        }
        
        File destDir = new File(System.getProperty("user.home"), ".tutorhub_excel_assets");
        
        try {
            URL url = ExcelEditorDialog.class.getResource("/cert_excel.html");
            if (url != null && "jar".equals(url.getProtocol())) {
                // Chỉ thực hiện giải nén một lần hoặc ghi đè nếu muốn cập nhật
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }
                
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                JarFile jar = connection.getJarFile();
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith("luckysheet/") || name.startsWith("luckyexcel/") || name.equals("cert_excel.html") || name.equals("deg_excel.html")) {
                        File destFile = new File(destDir, name);
                        if (entry.isDirectory()) {
                            destFile.mkdirs();
                        } else {
                            destFile.getParentFile().mkdirs();
                            try (InputStream is = jar.getInputStream(entry);
                                 FileOutputStream fos = new FileOutputStream(destFile)) {
                                byte[] buffer = new byte[8192];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                }
                            }
                        }
                    }
                }
                System.out.println("[EXCEL_EDITOR] Extracted assets from JAR to: " + destDir.getAbsolutePath());
            } else {
                // Nếu chạy trong IDE (không đóng gói JAR), sử dụng trực tiếp tài nguyên từ thư mục build
                if (url != null) {
                    File file = new File(url.toURI());
                    extractAssetsDir = file.getParentFile();
                    return extractAssetsDir;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        extractAssetsDir = destDir;
        return destDir;
    }

    private void handleSaveData(String json, DefaultTableModel tableModel, boolean isDegree) {
        new Thread(() -> {
            try {
                Gson gson = new Gson();
                List<Map<String, String>> rows = gson.fromJson(json, new TypeToken<List<Map<String, String>>>(){}.getType());
                
                for (Map<String, String> r : rows) {
                    if (isDegree) {
                        String name = r.get("col0") != null ? r.get("col0") : "";
                        String uni = r.get("col1") != null ? r.get("col1") : "";
                        String major = r.get("col2") != null ? r.get("col2") : "";
                        String year = r.get("col3") != null ? r.get("col3") : "";
                        String rank = r.get("col4") != null ? r.get("col4") : "";
                        
                        SwingUtilities.invokeLater(() -> {
                            tableModel.addRow(new Object[]{
                                name + " - " + major, 
                                uni, 
                                year, 
                                rank, 
                                "",            
                                "Chờ duyệt"    
                            });
                        });
                        
                        // Payload: Tên bằng cấp | Chuyên ngành | Trường/Đơn vị | Năm TN | fileName | b64
                        String payload = name + "|" + major + "|" + uni + "|" + year + "|Excel_Data.xlsx|NO_FILE";
                        com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().sendPacket(new com.mycompany.tutorhub_enterprise.models.Packet("ADD_DEGREE", payload));
                    } else {
                        String name = r.get("col0") != null ? r.get("col0") : "";
                        String prov = r.get("col1") != null ? r.get("col1") : "";
                        String issue = r.get("col2") != null ? r.get("col2") : "";
                        String exp = r.get("col3") != null ? r.get("col3") : "";
                        String note = r.get("col4") != null ? r.get("col4") : "";
                        
                        SwingUtilities.invokeLater(() -> {
                            tableModel.addRow(new Object[]{
                                name,
                                prov,
                                issue,
                                exp,
                                "",             
                                "Chờ duyệt"     
                            });
                        });
                        
                        // Payload: Tên chứng chỉ | Đơn vị cấp | Ngày cấp | Hạn SD | fileName | b64
                        String payload = name + "|" + prov + "|" + issue + "|" + exp + "|Excel_Data.xlsx|NO_FILE";
                        com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().sendPacket(new com.mycompany.tutorhub_enterprise.models.Packet("ADD_CERTIFICATE", payload));
                    }
                }
                
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Đã lưu " + rows.size() + (isDegree ? " bằng cấp!" : " chứng chỉ!"), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lỗi phân tích dữ liệu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void handleExportXlsx(String base64) {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Lưu file Excel");
            chooser.setSelectedFile(new File("ThongTin_TutorHub.xlsx"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] data = Base64.getDecoder().decode(base64);
                    fos.write(data);
                    JOptionPane.showMessageDialog(this, "Xuất file thành công: " + file.getAbsolutePath());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Lỗi lưu file: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    @Override
    public void dispose() {
        if (msgRouter != null) {
            JcefManager.getClient().removeMessageRouter(msgRouter);
            msgRouter.dispose();
        }
        if (browser != null) {
            browser.close(true);
        }
        super.dispose();
    }
}
