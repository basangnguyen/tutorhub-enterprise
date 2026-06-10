package com.mycompany.tutorhub_enterprise.server;

import com.mycompany.tutorhub_enterprise.server.dao.ClassDAO;

public class ClassDispatcher {
    private static ClassDispatcher instance;

    private ClassDispatcher() {}

    public static synchronized ClassDispatcher getInstance() {
        if (instance == null) {
            instance = new ClassDispatcher();
        }
        return instance;
    }

    /**
     * Hàm này được đặt synchronized(classCode). 
     * Nghĩa là khóa cứng cái Mã Lớp đó lại. Người số 1 đang chốt đơn thì người số 2 phải đứng chờ.
     */
    public boolean processAcceptClass(String classCode, int tutorId) {
        // String.intern() đảm bảo việc khóa chính xác dựa trên giá trị chuỗi
        synchronized (classCode.intern()) {
            
            // Gọi lệnh Update Database. 
            // Cấu trúc SQL của DAO đã có "AND status = 'AVAILABLE'".
            // Nếu người số 1 update xong, status thành 'TAKEN'. 
            // Người số 2 chạy vào, điều kiện = AVAILABLE bị sai -> Update thất bại trả về false.
            boolean isUpdated = ClassDAO.updateClassStatus(classCode, tutorId);
            
            if (isUpdated) {
                System.out.println("✅ [CHỐT ĐƠN] Lớp " + classCode + " đã được Tutor_ID " + tutorId + " giành được.");
                return true;
            } else {
                System.out.println("❌ [TRƯỢT ĐƠN] Tutor_ID " + tutorId + " chậm tay với lớp " + classCode);
                return false;
            }
        }
    }
}