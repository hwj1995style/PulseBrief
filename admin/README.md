# PulseBrief Admin

PulseBrief 运营后台，使用 React + Vite + TypeScript 构建。

当前阶段已完成独立工程骨架和候选资讯审核 mock 页面，不是旧的用户端 React/Vite PWA。

## 功能范围

- 专业后台应用壳：侧边栏、顶部栏、内容区。
- 候选资讯审核页：状态筛选、候选列表、右侧详情、发布和拒绝操作。
- Mock Admin API client，后续可切换 Spring Boot `/api/admin/**`。
- 导航入口：仪表盘、采集任务、候选资讯、文章管理、分类管理、简报管理。

## 环境变量

后续接真实后端时使用：

```text
VITE_ADMIN_API_BASE_URL=http://localhost:8080
VITE_ADMIN_TOKEN=dev-admin-token
```

当前未配置 `VITE_ADMIN_API_BASE_URL` 时默认使用 mock 数据。

## 本地启动

```powershell
npm install
npm run dev
```

如果默认端口被占用，可以指定端口：

```powershell
npm run dev -- --port 5188 --strictPort
```

## 验证

```powershell
npm test -- --run
npm run lint
npm run build
```

## 当前状态

已完成第一批候选审核后台骨架。下一阶段建议接入真实 `/api/admin/candidates`，把 mock 发布/拒绝替换为 Spring Boot Admin API 调用。
