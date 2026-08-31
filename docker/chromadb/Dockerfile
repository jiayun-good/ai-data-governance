# 定制 ChromaDB 镜像
# - 安装 python3，使 health check 能够执行
# - 修正持久化数据目录为 /data
FROM chromadb/chroma:latest

# 安装 python3（仅用于 health check）
RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 && \
    rm -rf /var/lib/apt/lists/*