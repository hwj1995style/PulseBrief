# PulseBrief Admin

PulseBrief 运营后台，使用 React + Vite + TypeScript 构建。

当前阶段已完成独立工程骨架、候选资讯审核页面和每日简报管理页面，不是旧的用户端 React/Vite PWA。页面支持未配置后端时使用 mock 数据，也支持配置 Spring Boot Admin API 后进入真实联调模式。

## 功能范围

- 专业后台应用壳：侧边栏、顶部栏、内容区。
- 候选资讯审核页：状态筛选、候选列表、右侧详情、发布和拒绝操作。
- 简报管理页：简报列表、已发布文章池、创建草稿、发布到 APP。
- Admin API client 支持 mock / Spring Boot `/api/admin/**` 双模式。
- 导航入口：仪表盘、采集任务、候选资讯、文章管理、分类管理、简报管理。

## 环境变量

接真实后端时使用：

```text
VITE_ADMIN_API_BASE_URL=http://localhost:8080
VITE_ADMIN_TOKEN=dev-admin-token
```

当前未配置 `VITE_ADMIN_API_BASE_URL` 时默认使用 mock 数据。

本地真实 API 联调建议：

```powershell
cd ..\backend
$env:JAVA_HOME='D:\Dev\jdk\jdk-17'
$env:Path='D:\Dev\jdk\jdk-17\bin;' + $env:Path
.\mvnw.cmd spring-boot:run

cd ..\admin
$env:VITE_ADMIN_API_BASE_URL='http://localhost:8080'
$env:VITE_ADMIN_TOKEN='dev-admin-token'
npm run dev -- --port 5188 --strictPort
```

后端 `/api/admin/**` 已支持本地 Vite 端口 CORS 预检；真实 GET/POST 请求仍需要 Admin Token。

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

已完成第一批候选审核后台骨架，并接入真实 `/api/admin/candidates` 列表、详情、发布和拒绝接口；已完成第一批简报管理页，支持 mock/API 双模式创建每日简报草稿并发布到 APP。下一阶段建议补后端简报编辑/下线接口，再扩展 React Admin 编辑态、操作日志和采集任务管理页。
