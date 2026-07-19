# Admin 账号生命周期与生产安全说明

## 已实现能力

- `ADMIN` 可在 React Admin 的“管理员账号”页面创建账号、调整显示名称、角色和状态。
- 新账号使用临时密码，首次登录只能访问身份、改密和退出接口；完成改密后才能进入运营功能。
- 管理员可主动修改自己的密码。修改或重置密码后，该账号的全部既有会话立即失效。
- 禁用账号会撤销全部会话；系统禁止停用或降级最后一个有效 `ADMIN`，也禁止当前管理员停用或降级自己。
- 密码默认 90 天到期，可通过环境变量调整；到期后按首次登录改密流程处理。
- 过期会话和超过保留期的已撤销会话默认每天 03:15 自动删除。
- API 响应统一设置 CSP、`nosniff`、Referrer Policy 和 Permissions Policy；`admin/nginx.conf` 提供生产静态站点 CSP 示例。

## API

```text
POST /api/admin/auth/password
GET  /api/admin/users
POST /api/admin/users
PUT  /api/admin/users/{id}
POST /api/admin/users/{id}/password-reset
```

账号管理接口全部要求 `ADMIN`；`VIEWER` 和 `EDITOR` 无法读取账号列表。

## 环境配置

```text
PULSEBRIEF_ADMIN_PASSWORD_MAX_AGE_DAYS=90
PULSEBRIEF_ADMIN_SESSION_CLEANUP_ENABLED=true
PULSEBRIEF_ADMIN_SESSION_CLEANUP_RETENTION_DAYS=7
PULSEBRIEF_ADMIN_SESSION_CLEANUP_CRON=0 15 3 * * *
```

生产部署应使用 HTTPS，并由实际反向代理加载与部署域名匹配的 CSP。仓库内 `admin/nginx.conf` 默认允许同源资源及 HTTPS API；如果使用独立 API 域名，应进一步将 `connect-src` 收紧到明确域名。

## 运维建议

1. 至少保留两个独立 `ADMIN` 账号，日常内容操作使用 `EDITOR`。
2. 临时密码通过独立安全渠道传递，不写入工单、日志或 Git。
3. 定期检查禁用账号和异常登录锁定情况。
4. 下一批生产增强可接入 MFA/SSO 与集中 Secret Manager。
