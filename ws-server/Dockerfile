# Sử dụng Node.js bản nhẹ
FROM node:18-alpine

# Thư mục làm việc
WORKDIR /app

# Copy package.json và cài đặt thư viện
COPY package.json .
RUN npm install

# Copy toàn bộ mã nguồn server.js
COPY . .

# Mở cổng mặc định của Hugging Face
EXPOSE 7860
ENV PORT=7860

# Khởi động server
CMD ["node", "server.js"]
