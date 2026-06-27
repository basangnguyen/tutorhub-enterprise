# Sử dụng môi trường Java 21 Debian-based
FROM eclipse-temurin:21-jre

# Thư mục làm việc trong Docker
WORKDIR /app

# Copy file update.jar làm Server
COPY update.jar /app/TutorServer.jar

# Copy file update.jar và version.json để Nginx phục vụ tĩnh
RUN mkdir -p /var/www/html
COPY update.jar /var/www/html/update.jar
COPY version.json /var/www/html/version.json

# Biến môi trường PORT (Cloud sẽ đè lên nếu cần, nhưng ta để 9000 cho TutorServer)
ENV PORT=9000

# Cài đặt Nginx và các chứng chỉ bảo mật
RUN apt-get update && apt-get install -y nginx ca-certificates curl openssl tzdata && rm -rf /var/lib/apt/lists/*

# Copy cấu hình Nginx và script khởi động
COPY nginx.conf /etc/nginx/nginx.conf
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

# Cổng public của Nginx
EXPOSE 7860

# Lệnh khởi động (chạy qua script)
CMD ["/app/start.sh"]
