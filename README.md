# PasteHub

匿名、临时的跨设备文本与图片传送站。

## 项目结构

- `frontend/`：Vue 3 + TypeScript + Vite
- `backend/`：Spring Boot + Java 17 基线（可升级至 Java 21）
- `infra/`：本地 MySQL、Redis、SeaweedFS
- `产品方案书.md` / `技术方案.md`：产品与技术设计

## 本地启动

```bash
# 终端 1：基础设施
docker compose -f infra/docker-compose.yml up -d

# 终端 2：后端
cd backend && ./mvnw spring-boot:run

# 终端 3：前端
cd frontend && npm install && npm run dev
```

前端开发服务器会把 `/api` 代理到 `http://localhost:8080`。可用 `GET /api/v1/status` 检查后端状态。

## 生产运行配置

生产环境使用 MySQL 保存投递元数据，并使用 Redis 对匿名写入接口限流。启动后端时设置：

```bash
SPRING_PROFILES_ACTIVE=prod \
PASTEHUB_DB_URL='jdbc:mysql://mysql:3306/pastehub' \
PASTEHUB_DB_USERNAME=pastehub \
PASTEHUB_DB_PASSWORD='请使用安全密码' \
PASTEHUB_REDIS_HOST=redis \
PASTEHUB_RATE_LIMIT_ENABLED=true \
./mvnw spring-boot:run
```

默认固定窗口为 60 秒：文本创建 10 次、图片初始化 10 次、图片发布 20 次、删除 20 次（均按 IP）。可用 `PASTEHUB_RATE_LIMIT_*` 覆盖阈值和窗口。超限返回 `429 RATE_LIMITED` 与 `Retry-After`。

默认不信任 `X-Forwarded-For`。只有反向代理的直连地址配置到 `PASTEHUB_TRUSTED_PROXIES`（逗号分隔）后，系统才会使用其转发的客户端 IP。

## 当前状态

已完成文本与图片临时投递 MVP；当前正在按 OpenSpec 完成 MySQL 持久化与 Redis IP 限流变更。
