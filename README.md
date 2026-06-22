# CC Survey - 智能问卷系统

一个现代化的问卷/调查系统，支持动态表单创建、自动评分、文件上传、AI 智能助手等功能。

## 技术栈

### 后端
- **Spring Boot 3.2.x** - Web框架
- **MyBatis-Plus 3.5.x** - ORM框架
- **MySQL 8.0** - 数据库
- **Redis** - 缓存、限流、分布式锁
- **RabbitMQ** - 异步消息处理
- **LangChain4j** - AI Agent 框架
- **BCrypt** - 密码加密
- **JWT** - 身份认证 (HttpOnly Cookie)

### 前端
- **Vue 3** - 前端框架 (Composition API)
- **Pinia** - 状态管理
- **Element Plus** - UI组件库
- **Vite** - 构建工具
- **Axios** - HTTP客户端

## 功能特性

### 核心功能
- 动态表单创建与渲染
- 多种题型支持 (单选、多选、填空、上传等)
- 自动评分系统
- 文件上传与管理
- 问卷分发与收集

### AI 智能助手
- **自然语言创建问卷**: 通过对话描述需求，AI 自动生成问卷
- **智能意图识别**: 自动识别问卷创建、数据分析、日志查询等意图
- **多 Agent 协作**: Survey Agent、Data Agent、Log Agent 分工协作
- **流式对话**: SSE 实时响应，流畅的对话体验

### 用户功能
- 用户注册与邮箱验证
- 密码重置
- 问卷填写与提交
- 查看提交历史

### 管理功能
- 问卷管理 (创建、编辑、发布、归档)
- 用户管理
- 答卷管理
- 操作日志
- 统计仪表盘

### 技术特性
- Redis缓存优化性能
- RabbitMQ异步处理
- 分布式锁防重复提交
- 接口限流保护
- 高并发测试接口

## 快速开始

### 环境要求
- JDK 17+
- Node.js 20+
- MySQL 8.0+
- Docker & Docker Compose (用于 Redis 和 RabbitMQ)

### 1. 启动基础服务 (Redis + RabbitMQ)

```bash
# 在 cc_survey 目录下执行
docker-compose up -d

# 查看服务状态
docker-compose ps

# RabbitMQ 管理界面: http://localhost:15672
# 用户名: cc_survey  密码: cc_survey123
```

### 2. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE cc_survey_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 执行迁移脚本
mysql -u root -p cc_survey_db < cc_survey_backend/sql/V1__init_schema.sql
mysql -u root -p cc_survey_db < cc_survey_backend/sql/V2__add_constraints.sql
mysql -u root -p cc_survey_db < cc_survey_backend/sql/V3__add_indexes.sql
```

### 3. 配置环境变量

```bash
# 复制配置模板
cp cc_survey_backend/src/main/resources/application-dev.yml.template cc_survey_backend/src/main/resources/application-dev.yml

# 编辑配置文件，填入实际值
```

或使用环境变量：

```bash
export DB_PASSWORD=your_db_password
export JWT_SECRET=your_jwt_secret
export XFYUN_API_KEY=your_api_key
```

### 4. 后端启动

```bash
cd cc_survey_backend

# 开发模式
./mvnw spring-boot:run

# 或使用 Maven
mvn spring-boot:run
```

### 5. 前端启动

```bash
cd cc_survey_frontend

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

### 访问系统
- 前端: http://localhost:5173
- 后端: http://localhost:8080
- 默认管理员: admin / admin123

## 项目结构

```
cc_survey/
├── cc_survey_backend/           # 后端项目
│   ├── sql/                     # 数据库脚本
│   ├── docs/                    # 文档
│   │   ├── AGENT_MODULE.md      # Agent 模块文档
│   │   └── troubleshooting/     # 问题排查文档
│   └── src/main/java/com/ccsurvey/
│       ├── common/              # 通用模块
│       │   ├── config/          # 配置类
│       │   ├── exception/       # 异常处理
│       │   ├── response/        # 响应封装
│       │   └── util/            # 工具类
│       └── modules/             # 业务模块
│           ├── auth/            # 认证
│           ├── user/            # 用户
│           ├── survey/          # 问卷
│           ├── submission/      # 提交
│           ├── agent/           # AI Agent
│           └── test/            # 测试接口
│
├── cc_survey_frontend/          # 前端项目
│   └── src/
│       ├── api/                 # API服务
│       ├── stores/              # 状态管理
│       ├── router/              # 路由
│       └── components/          # 组件
│
└── docs/                        # 文档
```

## API文档

### 认证接口
| 方法 | 路径 | 描述 |
|-----|------|------|
| POST | /api/auth/login | 登录 |
| POST | /api/auth/register | 注册 |
| POST | /api/auth/logout | 登出 |
| GET | /api/auth/me | 获取当前用户 |

### 问卷接口
| 方法 | 路径 | 描述 |
|-----|------|------|
| GET | /api/surveys/list | 获取问卷列表 |
| GET | /api/surveys/{uuid} | 获取问卷详情 |
| POST | /api/submit | 提交问卷 |

### AI Agent 接口
| 方法 | 路径 | 描述 |
|-----|------|------|
| POST | /api/agent/stream | 流式对话 (SSE) |
| POST | /api/agent/chat | 非流式对话 |
| GET | /api/agent/conversations | 获取对话列表 |
| GET | /api/agent/conversations/{id}/messages | 获取对话历史 |
| PUT | /api/agent/conversations/{id}/title | 更新对话标题 |
| DELETE | /api/agent/conversations/{id} | 删除对话 |

### 管理接口
| 方法 | 路径 | 描述 |
|-----|------|------|
| GET | /api/admin/surveys | 获取问卷列表 |
| POST | /api/admin/surveys | 创建问卷 |
| PUT | /api/admin/surveys/{uuid} | 更新问卷 |
| DELETE | /api/admin/surveys/{uuid} | 删除问卷 |

## 配置说明

### 后端配置

复制配置模板并修改：

```bash
cp src/main/resources/application-dev.yml.template src/main/resources/application-dev.yml
```

主要配置项：

```yaml
# 数据库
spring.datasource.url: jdbc:mysql://localhost:3306/cc_survey_db
spring.datasource.username: root
spring.datasource.password: ${DB_PASSWORD}

# Redis
spring.data.redis.host: localhost
spring.data.redis.port: 6379

# RabbitMQ
spring.rabbitmq.host: localhost
spring.rabbitmq.port: 5672

# JWT
jwt.secret: ${JWT_SECRET}

# AI Agent
agent.llm.provider: xfyun
agent.llm.xfyun.api-key: ${XFYUN_API_KEY}
```

### 环境变量

| 变量名 | 描述 | 必填 |
|-------|------|-----|
| DB_PASSWORD | 数据库密码 | 是 |
| JWT_SECRET | JWT 密钥 | 是 |
| XFYUN_API_KEY | 讯飞星火 API Key | 是 (使用 AI 功能) |
| REDIS_HOST | Redis 主机 | 否 (默认 localhost) |
| RABBITMQ_HOST | RabbitMQ 主机 | 否 (默认 localhost) |

## AI Agent 模块

详细文档请参阅 [Agent 模块文档](cc_survey_backend/docs/AGENT_MODULE.md)

### 支持的 LLM 提供商
- 讯飞星火 (xfyun)
- DeepSeek
- 阿里云通义千问 (aliyun)

### Agent 类型
- **Survey Agent**: 问卷创建、编辑
- **Data Agent**: 数据分析、统计
- **Log Agent**: 日志查询

## 安全特性

- BCrypt密码加密 (cost factor 12)
- JWT存储在HttpOnly Cookie中
- 接口限流保护
- 分布式锁防重复提交
- 输入验证
- CORS白名单
- 敏感配置通过环境变量注入

## 许可证

MIT License
