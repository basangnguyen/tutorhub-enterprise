#!/bin/sh

echo "[STARTUP] Bắt đầu TutorServer..."
# Chạy ngầm Java Server
# Hugging Face Spaces thiết lập các biến môi trường cho Secret/Variable. 
# PORT cho TutorServer nội bộ là 9000
export PORT=9000

java -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false -cp /app/TutorServer.jar com.mycompany.tutorhub_enterprise.server.TutorServer &

echo "[STARTUP] Chờ 3 giây để Java khởi động..."
sleep 3

echo "[STARTUP] Bắt đầu Nginx..."
# Chạy Nginx foreground để giữ container sống
nginx -c /etc/nginx/nginx.conf
