# Hướng dẫn thiết lập Google Apps Script Email Relay cho TutorHub

Tài liệu này hướng dẫn cách cấu hình gửi email tự động từ ứng dụng TutorHub (chạy trên Hugging Face) thông qua một Web App của Google Apps Script (Sử dụng Gmail cá nhân/Workspace miễn phí). Giải pháp này giúp vượt qua giới hạn chặn cổng SMTP (port 587) trên Hugging Face Spaces.

## Bước 1: Tạo Google Apps Script mới

1. Truy cập [Google Apps Script](https://script.google.com/).
2. Đăng nhập bằng tài khoản Gmail bạn muốn dùng để gửi email.
3. Nhấp vào nút **Dự án mới (New Project)**.
4. Ở góc trên cùng bên trái, đổi tên dự án thành `TutorHub Email Relay`.

## Bước 2: Thêm Code Xử Lý

Xóa toàn bộ code có sẵn trong màn hình soạn thảo và dán đoạn code sau vào:

```javascript
function doPost(e) {
  try {
    const props = PropertiesService.getScriptProperties();
    const secret = props.getProperty('TUTORHUB_EMAIL_RELAY_SHARED_SECRET');

    const raw = e && e.postData && e.postData.contents ? e.postData.contents : '{}';
    const body = JSON.parse(raw);

    if (!secret || body.secret !== secret) {
      return jsonResponse({
        success: false,
        message: 'Unauthorized'
      });
    }

    const to = String(body.to || '').trim();
    const subject = String(body.subject || 'TutorHub verification code').trim();
    const text = String(body.text || '').trim();
    const html = String(body.html || '').trim();

    if (!to || !subject || (!text && !html)) {
      return jsonResponse({
        success: false,
        message: 'Bad request'
      });
    }

    const remainingQuota = MailApp.getRemainingDailyQuota();

    if (remainingQuota <= 0) {
      return jsonResponse({
        success: false,
        message: 'Daily quota exceeded'
      });
    }

    MailApp.sendEmail({
      to: to,
      subject: subject,
      body: text || 'Your TutorHub verification code is included in this email.',
      htmlBody: html || undefined,
      name: 'TutorHub Enterprise'
    });

    return jsonResponse({
      success: true,
      remainingQuota: Math.max(remainingQuota - 1, 0)
    });

  } catch (err) {
    return jsonResponse({
      success: false,
      message: 'Send failed'
    });
  }
}

function jsonResponse(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
```
*Lưu ý: Không thay đổi mã này trừ khi bạn hiểu rõ.*

5. Nhấn **Save (Ctrl + S)** để lưu mã.

## Bước 3: Cấu hình Secret (Script Properties)

Để đảm bảo bảo mật, ứng dụng sẽ yêu cầu một "Mật khẩu chia sẻ" (Shared Secret) để tránh người lạ gọi API gửi mail spam.

1. Nhìn sang menu thanh bên trái, chọn biểu tượng bánh răng **Cài đặt dự án (Project Settings)**.
2. Cuộn xuống phần **Thuộc tính tập lệnh (Script Properties)**.
3. Nhấp vào **Thêm thuộc tính tập lệnh (Add script property)**.
4. Ở ô **Thuộc tính (Property)**, nhập chính xác:
   `TUTORHUB_EMAIL_RELAY_SHARED_SECRET`
5. Ở ô **Giá trị (Value)**, tạo một mật khẩu bí mật bất kỳ của bạn (Ví dụ: `mat-khau-bi-mat-1234`). Mật khẩu này sẽ cần được khai báo lại trên máy chủ TutorHub. **LƯU Ý:** Tuyệt đối không chia sẻ mật khẩu này ở nơi công cộng.
6. Nhấp **Lưu các thuộc tính tập lệnh (Save script properties)**.

## Bước 4: Deploy (Triển khai) Web App

1. Ở góc trên cùng bên phải, nhấp vào nút **Triển khai (Deploy)** màu xanh dương -> Chọn **Triển khai mới (New deployment)**.
2. Ở cửa sổ bật lên, nhấp vào biểu tượng bánh răng (⚙️) bên cạnh dòng chữ "Chọn loại (Select type)" -> Tích chọn **Ứng dụng web (Web app)**.
3. Điền cấu hình theo đúng thiết lập sau:
   - Mô tả (Description): `TutorHub Relay v1`
   - Thực thi với tư cách (Execute as): **Tôi (Me) / Tài khoản Gmail của bạn**.
   - Ai có quyền truy cập (Who has access): **Bất kỳ ai (Anyone)**.
4. Nhấp nút **Triển khai (Deploy)**.
5. Lần đầu tiên, Google sẽ yêu cầu "Cấp quyền truy cập" (Authorize Access). 
   - Nhấp vào **Cấp quyền truy cập**.
   - Chọn tài khoản Google của bạn.
   - Nhấp vào **Nâng cao (Advanced)** -> Chọn **Đi tới TutorHub Email Relay (không an toàn)**.
   - Kéo xuống và nhấp **Cho phép (Allow)**.
6. Khi quá trình hoàn tất, Google sẽ cung cấp cho bạn một đường dẫn (Web App URL) có dạng: `https://script.google.com/macros/s/.../exec`.
   -> **Hãy Copy lại đường dẫn Web App URL này.**

## Bước 5: Cấu hình Variables (Secrets) trên Hugging Face

Truy cập trang cài đặt (Settings) Space trên Hugging Face của dự án TutorHub. Tại phần **Variables and secrets**, hãy thêm hoặc chỉnh sửa các cấu hình sau:

**Variable/Secret mới cần bắt buộc có:**
1. `TUTORHUB_EMAIL_DELIVERY_MODE` = `apps_script`
2. `TUTORHUB_EMAIL_RELAY_URL` = `https://script.google.com/macros/s/.../exec` (Dán URL ở Bước 4 vào đây)
3. `TUTORHUB_EMAIL_RELAY_SHARED_SECRET` = Mật khẩu bí mật bạn đã tạo ở Bước 3.

**Các cấu hình SMTP cũ:** 
(Tùy chọn) Vẫn có thể giữ nguyên cấu hình cũ (TUTORHUB_SMTP_HOST, v.v.) như một bản dự phòng. Hệ thống sẽ bỏ qua SMTP nếu DELIVERY_MODE là `apps_script`.

## Bước 6: Khởi động lại và Kiểm tra
1. Khởi động lại máy chủ TutorHub trên Hugging Face (Factory Rebuild/Restart Space).
2. Tại Client App, sử dụng chức năng **Quên Mật Khẩu (Forgot Password)**.
3. Xác minh Email đã có thể nhận OTP thành công. Hệ thống Server sẽ không tiết lộ bí mật (Secret), hay OTP ra màn hình.

> [!WARNING]
> **Giới hạn số lượng Email:**
> Google cá nhân miễn phí cho phép gửi **khoảng 100 email / ngày**. Nếu dùng Google Workspace, giới hạn là 1500 email. Đừng sử dụng cho mục đích Spam để tránh bị khóa tính năng gửi mail.
