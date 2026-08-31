FROM python:3.11-slim

WORKDIR /app
ENV TZ=Asia/Shanghai \
    PYTHONUNBUFFERED=1

# 精简依赖清单（不含 torch / transformers 等未使用的重包，显著缩小镜像）
# 本地开发仍使用完整版 requirements.txt
COPY requirements-docker.txt .
# 清华 PyPI 镜像加速（境外网络可删除 -i 参数）
RUN pip install --no-cache-dir -r requirements-docker.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 注意：不 COPY .env，敏感配置由 docker-compose 的 env_file 注入
COPY app ./app
COPY api ./api
COPY service ./service
COPY rag ./rag
COPY prompt ./prompt
COPY vector ./vector

EXPOSE 8011
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8011"]
