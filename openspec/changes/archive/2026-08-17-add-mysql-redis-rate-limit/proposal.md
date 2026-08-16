## Why

当前开发环境仍以 H2 作为默认数据库，匿名投递接口也没有跨实例共享的访问频率控制；这不适合作为公开服务的持久化与防滥用基础。现在补齐 MySQL 容器和基于 Redis 的 IP 限流，才能让文本、图片投递在重启和多实例场景下保持可靠，并降低匿名接口被批量滥用的风险。

## What Changes

- 在本地 Docker Compose 中提供可持久化的 MySQL 与 Redis，并使应用能通过环境变量使用它们。
- 将生产运行时的投递数据持久化目标明确为 MySQL；保留 H2 仅作为自动化测试和轻量本地开发的显式选择。
- 为创建文本投递、初始化图片上传、完成图片发布和删除投递接口增加基于客户端 IP 的 Redis 限流。
- 限流阈值、窗口和可信代理配置必须可配置；被限流时返回一致的 `429 Too Many Requests` 响应，且不创建或修改投递。
- 增加运行说明、自动化测试和容器级验证。

## Capabilities

### New Capabilities

- `anonymous-api-rate-limit`: 对匿名写入接口实施可配置、跨实例共享的 IP 访问频率限制。
- `runtime-data-services`: 提供 MySQL 和 Redis 的本地/生产运行时配置与持久化约束。

### Modified Capabilities

- `text-transfer`: 文本投递创建与删除在超过 IP 限流时必须不产生状态变更。
- `image-transfer`: 图片初始化、发布与删除在超过 IP 限流时必须不产生状态变更。

## Impact

- 影响 `infra/docker-compose.yml`、Spring Boot 数据源/Redis 配置、请求过滤器或拦截器、错误响应及运行文档。
- 新增 Spring Data Redis 客户端依赖；应用运行需要可访问的 Redis，生产环境需要 MySQL。
- 前端无需改变正常流程，但需能向用户展示 API 返回的限流错误。
