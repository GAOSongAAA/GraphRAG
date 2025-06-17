# Graph RAG Solution Dockerfile

# 使用 OpenJDK 17 作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制项目文件
COPY . /app

# 安装必要的系统依赖
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    maven \
    && rm -rf /var/lib/apt/lists/*

# 设置 JAVA_HOME 环境变量
ENV JAVA_HOME=/usr/local/openjdk-17
ENV PATH=$JAVA_HOME/bin:$PATH

# 创建日志目录
RUN mkdir -p /app/logs

CMD ["sleep", "infinity"]