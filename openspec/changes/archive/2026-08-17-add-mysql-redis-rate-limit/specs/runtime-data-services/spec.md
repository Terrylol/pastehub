## Purpose

为 PasteHub 提供可持久化、可容器化的 MySQL 与 Redis 运行时服务，使公开部署不依赖进程内数据库或单实例内存状态。

## ADDED Requirements

### Requirement: 提供持久化运行时服务
系统 SHALL 提供可启动的 MySQL 和 Redis 容器配置，并为 MySQL 数据提供持久化存储。应用 MUST 能通过环境配置连接这些服务，而不得将连接凭据硬编码在代码中。

#### Scenario: 启动本地基础设施
- **WHEN** 维护者启动项目的基础设施编排
- **THEN** MySQL、Redis 和对象存储服务均可被应用容器或本地应用访问

### Requirement: 生产投递数据使用 MySQL
系统 SHALL 在生产运行环境将文本与图片投递元数据持久化到 MySQL。H2 仅可作为测试或显式开发配置，不得作为生产默认数据存储。

#### Scenario: 应用重启后读取投递
- **WHEN** 使用 MySQL 的应用创建未过期投递后重启
- **THEN** 重启后的应用仍可按标识读取该投递
