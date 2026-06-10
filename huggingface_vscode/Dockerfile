FROM ubuntu:22.04

# Cài đặt các công cụ cơ bản, Node.js, Python, Java, C++
RUN apt-get update && apt-get install -y \
    curl \
    git \
    wget \
    sudo \
    python3 \
    python3-pip \
    default-jdk \
    g++ \
    cpulimit \
    && rm -rf /var/lib/apt/lists/*

# Cài đặt Node.js 18
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs

# Cài đặt code-server
RUN curl -fsSL https://code-server.dev/install.sh | sh

# Tạo user không có quyền root (Hugging Face khuyến nghị)
RUN useradd -m -s /bin/bash coder && \
    echo "coder ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# Thiết lập thư mục làm việc và cấp quyền
RUN mkdir -p /home/coder/project /workspace && \
    chown -R coder:coder /home/coder /workspace
WORKDIR /home/coder/project
ENV WORKSPACE_DIR=/workspace

# Copy file của server proxy vào
COPY --chown=coder:coder package.json server.js ./

# Chuyển sang user coder
USER coder

# Cài đặt thư viện Node.js cho Proxy Server
RUN npm install

# Mở cổng 7860 cho Hugging Face Spaces
EXPOSE 7860

# Lệnh khởi động: 
# 1. Chạy code-server dưới nền (port 8080), tắt xác thực (auth none)
# 2. Chạy Proxy Server Node.js ở foreground (port 7860)
CMD ["sh", "-c", "code-server --bind-addr 127.0.0.1:8080 --auth none --disable-telemetry /workspace & node server.js"]
