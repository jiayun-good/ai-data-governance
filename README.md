# 数据治理平台（ai-data-governance）使用说明

一个集 **前端（Vue 3）+ 后端（Spring Boot）+ AI 服务（FastAPI / LangChain / RAG）** 于一体的数据质量治理平台，支持数据源管理、数据质量规则定义与执行、AI 智能生成质量规则（带知识库 RAG 增强）等功能。

---

## 目录

1. [系统架构](#一系统架构)
2. [技术栈](#二技术栈)
3. [环境要求](#三环境要求)
4. [端口规划](#四端口规划)
5. [基础设施启动（MySQL / Redis / ChromaDB）](#五基础设施启动mysql--redis--chromadb)
6. [AI 服务启动（重点）](#六ai-服务启动重点)
7. [后端服务启动](#七后端服务启动)
8. [前端服务启动](#八前端服务启动)
9. [推荐启动顺序](#九推荐启动顺序)
10. [核心接口清单](#十核心接口清单)
11. [常见问题排查](#十一常见问题排查)

---

## 一、系统架构

```
┌─────────────┐   /api 代理    ┌──────────────┐   HTTP 透传   ┌──────────────────┐
│  前端 Vue 3  │ ────────────▶ │ 后端 Spring  │ ────────────▶ │  AI 服务 FastAPI  │
│  Vite :5173  │               │  Boot :8081  │               │      :8000        │
└─────────────┘               └──────┬───────┘               └───────┬──────────┘
                                     │                               │
                              ┌──────┴──────┐              ┌─────────┴─────────┐
                              │  MySQL :3306 │              │ ChromaDB :8001    │
                              │  Redis :6379 │              │ （向量数据库RAG）  │
                              └─────────────┘              └───────────────────┘
                                                                       │
                                                             ┌─────────┴─────────┐
                                                             │ 通义千问 DashScope │
                                                             │ （LLM + Embedding）│
                                                             └───────────────────┘
```

**调用链路说明：**

- 前端（5173）通过 Vite 代理将 `/api` 请求转发到后端（8081）；
- 后端（8081，context-path 为 `/api`）负责业务逻辑、鉴权（JWT）、规则执行；
- 后端通过 `RestTemplate` **硬编码调用 AI 服务 `http://localhost:8000`**（见 `AiRuleServiceImpl.java`、`KnowledgeServiceImpl.java`），因此 **AI 服务必须监听 8000 端口，不可随意更改**；
- AI 服务负责智能选表、规则生成（调用通义千问），并通过 ChromaDB 做 RAG 知识检索增强；
- ChromaDB 是独立的向量数据库进程，端口由 `ai-service/.env` 中的 `CHROMA_PORT` 指定（默认 8000，但会与 FastAPI 冲突，**推荐改为 8001**，详见下文）。

---

## 二、技术栈

| 模块 | 技术 |
|------|------|
| 前端 | Vue 3.5（Composition API）、Vite 6、Element Plus 2.9、Pinia、Vue Router 4、Axios |
| 后端 | Spring Boot 4.0.7、Java 17、MyBatis-Plus 3.5.17、MySQL、Redis、JWT（jjwt 0.12.6）、SpringDoc（Swagger）3.0.0 |
| AI 服务 | Python、FastAPI 0.141、LangChain 1.x、LangGraph、通义千问（DashScope）、ChromaDB 1.5.9、Sentence-Transformers、Uvicorn |
| 其他 | Maven、XXL-Job（当前已注释禁用）、Docker（可选，用于基础设施） |

---

## 三、环境要求

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 后端编译运行（`pom.xml` 中 `<java.version>17</java.version>`） |
| Maven | 3.6+ | 后端构建 |
| Node.js | 18+ | 前端构建（Vite 6 要求） |
| Python | 3.10 ~ 3.12 | AI 服务（依赖 `torch`、`transformers` 等，建议 3.10+） |
| MySQL | 8.x | 业务数据库 |
| Redis | 任意稳定版 | 会话 / 缓存（使用 **DB 1**） |
| Docker | 可选 | 用于快速启动 MySQL、Redis、ChromaDB |

---

## 四、端口规划

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 Vite Dev Server | 5173 | `npm run dev` 启动 |
| 后端 Spring Boot | 8081 | 接口前缀 `/api`，Swagger 文档同端口 |
| **AI 服务 FastAPI** | **8000** | **后端硬编码调用，不可更改** |
| ChromaDB 向量库 | 8001（推荐） | 由 `.env` 中 `CHROMA_PORT` 决定，须与 AI 服务端口区分 |
| MySQL | 3306 | 库名 `data_governance`，账号 `root / 123456`（可改配置） |
| Redis | 6379 | 使用 DB 1（`application.yml` 中 `database: 1`） |

> ⚠️ **端口冲突提示**：`ai-service/.env` 中 `CHROMA_PORT` 默认值为 `8000`，与 AI 服务（FastAPI）端口相同，两者不能同时占用 8000。**请将 ChromaDB 端口改为 8001（或其他空闲端口），并保持 `.env` 与 Docker 映射一致。**

---

## 五、基础设施启动（MySQL / Redis / ChromaDB）

以下均以 Docker 方式启动（也可使用本机安装的 MySQL / Redis 替代，只需保证连接配置一致）。

### 5.1 MySQL

```powershell
docker run -d --name mysql8 -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=123456 `
  -e MYSQL_DATABASE=data_governance `
  mysql:8
```

> 若使用已有 MySQL，请手动创建数据库并保持与 [application.yml](backend/src/main/resources/application.yml) 中的账号密码一致：
>
> ```sql
> CREATE DATABASE IF NOT EXISTS data_governance DEFAULT CHARACTER SET utf8mb4;
> ```

### 5.2 Redis

```powershell
docker run -d --name redis -p 6379:6379 redis
```

后端配置使用 Redis **DB 1**（`application.yml` 中 `spring.data.redis.database: 1`），无需额外处理。

### 5.3 ChromaDB（向量数据库）

```powershell
docker run -d -p 8001:8000 --name chromadb chromadb/chroma
```

启动后**同步修改** `ai-service/.env` 中的端口配置：

```ini
CHROMA_HOST="localhost"
CHROMA_PORT="8001"
```

> 验证：浏览器访问 `http://localhost:8001/api/v2/` 应返回 Chroma 相关信息。

---

## 六、AI 服务启动（重点）

AI 服务为 Python FastAPI 应用，位于 [ai-service](ai-service) 目录，负责智能选表、规则生成、知识库管理与 RAG 检索。

### 6.1 配置环境变量

编辑 [ai-service/.env](ai-service/.env)：

```ini
# 大模型配置（通义千问 DashScope 兼容模式）
MODEL="qwen3.7-plus"
API_KEY="sk-xxxxxxxxxxxxxxxx"          # 替换为你的 DashScope API Key
BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"

# Embedding 模型（用于 RAG 向量化）
EMBEDDING_MODEL="qwen3.7-text-embedding"

# ChromaDB 配置（端口须与 AI 服务端口区分）
CHROMA_HOST="localhost"
CHROMA_PORT="8001"

# 文本切片参数
CHUNK_SIZE=500
CHUNK_OVERLAP=100
```

### 6.2 安装依赖

在 PowerShell 中执行（建议使用虚拟环境）：

```powershell
cd d:\self_study\ai\ai-data-governance\ai-service

# 创建并激活虚拟环境
python -m venv .venv
.venv\Scripts\Activate.ps1

# 安装依赖（依赖较多，包含 torch / chromadb 等，耗时较长）
pip install -r requirements.txt
```

### 6.3 启动 AI 服务

```powershell
# 方式一：开发模式（推荐，带热重载）
uvicorn app.main:app --host 0.0.0.0 --port 8011 --reload

# 方式二：生产模式（无热重载）
uvicorn app.main:app --host 0.0.0.0 --port 8011
```

> 若已使用虚拟环境，确保 `Activate.ps1` 已激活后再执行；否则使用 `python -m uvicorn app.main:app ...`。

### 6.4 验证服务

- 健康检查：`http://localhost:8000/` 应返回 `{"msg":"AI service running"}`
- 在线接口文档（Swagger UI）：`http://localhost:8000/docs`
- 嵌入式测试：`http://localhost:8000/test/embedding?text=用户表名称不能为空`（返回向量信息）

### 6.5 首次使用：初始化 RAG 知识库

项目预置了 5 份数据质量治理领域知识文档（位于 [ai-service/rag/knowledge](ai-service/rag/knowledge)）。首次启动后需将其加载进 ChromaDB，否则 AI 生成规则时无知识参考：

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8000/ai/knowledge/load-dir"
```

或使用 curl：

```bash
curl -X POST http://localhost:8000/ai/knowledge/load-dir
```

返回示例：

```json
{"success": true, "loaded": 5, "documents": ["规则类型详解.md", "..."]}
```

加载后可通过 `GET http://localhost:8000/ai/knowledge` 查看知识库中的文档列表。

---

## 七、后端服务启动

### 7.1 准备数据库

1. 创建数据库 `data_governance`（见 5.1）；
2. 创建业务表（共 6 张）：

   | 表名 | 用途 | 建表脚本 |
   |------|------|----------|
   | `sys_user` | 系统用户（登录） | 手动创建 |
   | `data_source` | 数据源信息 | 手动创建 |
   | `data_quality_rule` | 数据质量规则 | 手动创建 |
   | `data_quality_check_record` | 质量检查记录 | 手动创建 |
   | `data_quality_error` | 质量异常数据 | 手动创建 |
   | `chat_session` | AI 聊天会话索引 | ✅ 已提供 [chat_session.sql](backend/src/main/resources/sql/chat_session.sql) |

3. 插入默认登录用户（登录逻辑为**明文密码比对**，可直接插入明文）：

   ```sql
   INSERT INTO sys_user (username, password, nickname, status)
   VALUES ('admin', '123456', '管理员', 1);
   ```

   > `sys_user` 表字段：`id`、`username`、`password`、`nickname`、`status`、`create_time`、`update_time`。

### 7.2 修改配置（如与本地环境不符）

[application.yml](backend/src/main/resources/application.yml) 关键配置：

```yaml
server:
  port: 8081
  servlet:
    context-path: /api          # 接口统一前缀

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/data_governance?...
    username: root
    password: 123456
  data:
    redis:
      host: localhost
      port: 6379
      database: 1               # 注意使用 DB 1
```

> XXL-Job 已在 [XxlJobConfig.java](backend/src/main/java/com/mp/config/XxlJobConfig.java) 中注释禁用（`//@Configuration  //先不启用xxljon`），**无需启动 XXL-Job 调度中心**。

### 7.3 启动后端

```powershell
cd d:\self_study\ai\ai-data-governance\backend

# 方式一：Maven 直接运行（开发常用）
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

### 7.4 验证服务

- Swagger 接口文档：`http://localhost:8081/api/swagger-ui/index.html`
- 登录接口：`POST http://localhost:8081/api/sys-user/login`

```json
// 请求体
{ "username": "admin", "password": "123456" }
// 响应：{"code":200,"data":{"token":"eyJhbGciOi..."}}
```

> 除 `/sys-user/login` 外，所有接口均需携带 `Authorization: Bearer <token>` 请求头（JWT 拦截器保护）。

---

## 八、前端服务启动

### 8.1 安装依赖

```powershell
cd d:\self_study\ai\ai-data-governance\frontend
npm install
```

### 8.2 启动开发服务器

```powershell
npm run dev
```

启动后访问 **http://localhost:5173**（使用项目默认登录账号即可登录）。

> Vite 已配置代理：`/api` → `http://localhost:8081`（见 [vite.config.js](frontend/vite.config.js)），前端开发时无需处理跨域。

### 8.3 生产构建

```powershell
npm run build      # 构建产物输出到 dist/
npm run preview    # 本地预览构建产物
```

---

## 九、推荐启动顺序

| 步骤 | 服务 | 命令/方式 | 验证 |
|------|------|-----------|------|
| 1 | MySQL | Docker 或本机 | `mysql -uroot -p123456` 可连接 |
| 2 | Redis | Docker 或本机 | `redis-cli ping` 返回 PONG |
| 3 | ChromaDB | `docker run -d -p 8001:8000 --name chromadb chromadb/chroma` | 访问 `http://localhost:8001` |
| 4 | **AI 服务** | `uvicorn app.main:app --host 0.0.0.0 --port 8000` | 访问 `http://localhost:8000/` 返回 `AI service running`；执行 `/ai/knowledge/load-dir` 初始化知识库 |
| 5 | 后端 | `mvn spring-boot:run`（8081） | 访问 Swagger 文档 |
| 6 | 前端 | `npm run dev`（5173） | 浏览器打开 `http://localhost:5173` 登录使用 |

---

## 十、核心接口清单

### 10.1 后端接口（前缀 `/api`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/sys-user/login` | 登录（无需 Token） |
| GET | `/sys-user/info` | 当前登录用户信息 |
| GET/POST/PUT/DELETE | `/data-source` | 数据源 CRUD |
| POST | `/data-source/{id}/test` | 测试数据源连接 |
| GET | `/data-source/{id}/tables`、`/tables/{table}/columns` | 查询表 / 字段 |
| GET/POST/PUT/DELETE | `/quality` | 质量规则 CRUD |
| POST | `/quality/{id}/execute` | 执行规则（结果写入检查记录） |
| GET | `/quality/check-record` | 检查记录查询 |
| GET | `/check/errors` | 异常数据查询 |
| POST | `/rule/ai/preview` | **AI 生成规则（预览，不入库）** |
| POST | `/rule/ai/save` | **确认保存 AI 生成的规则** |
| GET | `/rule/ai/sessions` | AI 聊天会话列表 |
| GET/DELETE | `/rule/ai/session/{sessionId}` | 会话详情 / 删除 |
| GET/POST/PUT/DELETE | `/knowledge` | 知识库管理（透传 AI 服务） |
| GET | `/knowledge/search` | 知识检索 |
| POST | `/knowledge/load-dir` | 批量加载知识目录 |

### 10.2 AI 服务接口（`http://localhost:8000`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 健康检查 |
| POST | `/ai/rule/analyze-table` | AI 智能选表（带上下文切换检测） |
| POST | `/ai/rule/generate` | 根据表名 + 字段元数据生成质量规则（含 RAG 检索） |
| POST | `/ai/knowledge` | 添加知识（切片 → Embedding → 写入 Chroma） |
| GET | `/ai/knowledge` | 列出所有知识文档 |
| GET | `/ai/knowledge/{doc_id}` | 获取单个文档内容 |
| GET | `/ai/knowledge/search?query=&k=` | 知识检索 |
| PUT/DELETE | `/ai/knowledge/{doc_id}` | 更新 / 删除知识文档 |
| POST | `/ai/knowledge/load-dir` | 批量加载 `rag/knowledge/` 下的 `.md` 文件 |
| GET | `/test/embedding` | 测试 Embedding 向量化 |
| POST | `/add`、GET `/search` | 规则向量存储测试接口 |

### 10.3 核心业务流程：AI 生成规则（预览 → 保存）

1. 前端选择数据源 → 获取表列表 → 用户输入自然语言需求（如"用户表的手机号字段要校验格式"）；
2. 后端调用 AI 服务 `/ai/rule/analyze-table` 智能匹配目标表；
3. 后端获取该表字段元数据 → 调用 `/ai/rule/generate`（AI 服务先从 ChromaDB 检索相关知识片段注入 Prompt，再调用通义千问生成规则 JSON，附带 `knowledgeRefs` 引用）；
4. 前端展示规则预览卡片（可修改）→ 用户确认 → 调用后端 `/rule/ai/save` 入库；
5. 规则可在规则管理页执行，结果写入检查记录与异常数据表。

---

## 十一、常见问题排查

| 现象 | 原因 | 解决办法 |
|------|------|----------|
| 接口返回 `{"detail":"Not Found"}` | 请求打到了 FastAPI（8000）而非 Spring Boot（8081），或反之 | 确认端口：业务接口在 `localhost:8081/api/...`，AI 接口在 `localhost:8000/ai/...` |
| AI 服务启动后 ChromaDB 连不上 | `CHROMA_PORT` 与 Docker 映射端口不一致，或与 FastAPI 端口冲突 | 统一 `.env` 中 `CHROMA_PORT` 与 `docker run -p` 映射（推荐 8001） |
| AI 生成规则无知识参考 | 知识库尚未初始化 | 调用 `POST /ai/knowledge/load-dir` 加载预置知识文档 |
| 登录返回 401 / 未授权 | Token 缺失或过期 | 重新调用 `/sys-user/login` 获取新 Token；前端自动跳转登录页 |
| AI 生成报错 / 返回空 | `API_KEY` 无效、余额不足或网络不通 | 检查 `ai-service/.env` 配置，用 `python test_llm.py` 验证配置读取 |
| 后端启动失败 | MySQL / Redis 未启动或账号密码不符 | 依次启动基础设施，核对 `application.yml` 连接配置 |
| `pip install` 安装失败 | Python 版本过高（3.13+）或网络问题 | 使用 Python 3.10~3.12；可切换 pip 国内镜像源 |
| PowerShell 中命令报错 | PowerShell 不支持 `&&` 连接符 | 使用 `;` 分隔命令，或逐条执行 |
| 前端页面打不开 | 未启动前端服务或端口被占用 | `npm run dev` 后访问 5173；检查 5173 端口占用 |

---

## 附：项目目录结构

```
ai-data-governance/
├── frontend/          # Vue 3 前端（Vite + Element Plus + Pinia）
├── backend/           # Spring Boot 后端（Java 17 + MyBatis-Plus + MySQL + Redis）
│   └── src/main/resources/sql/chat_session.sql   # 会话表建表脚本
└── ai-service/        # AI 服务（FastAPI + LangChain + 通义千问 + ChromaDB RAG）
    ├── app/           # FastAPI 应用入口与配置（config.py 读取 .env）
    ├── api/           # /ai/rule 与 /ai/knowledge 路由
    ├── service/       # LLM 调用、规则生成、知识库 CRUD 业务
    ├── prompt/        # Prompt 模板
    ├── rag/           # RAG：chunker 切片、retriever 检索、knowledge 预置知识库
    ├── vector/        # ChromaDB 客户端与向量集合
    ├── .env           # AI 服务环境变量（API Key / 模型 / Chroma 端口）
    └── requirements.txt
```
