# PulseBrief React Admin 工程骨架设计

## 背景与现状

PulseBrief 已完成 Flutter 用户端主线、Spring Boot V1 用户端 API、真实资讯采集基础链路、候选资讯生成、授权 PDF 元数据模型，以及第一批 Admin 候选审核发布 API。

当前仓库中的 `admin/` 只有占位 README。下一步需要建立独立 React Admin 工程，让运营人员可以在浏览器中查看候选资讯、预览详情、拒绝或发布内容。该后台只服务运营审核，不替代 Flutter 用户端，也不恢复已裁剪的 React/Vite PWA MVP。

## 目标与非目标

目标：

1. 创建独立 `admin/` React + Vite + TypeScript 工程。
2. 建立专业运营后台布局：侧边栏、顶部栏、内容区、右侧详情面板。
3. 建立候选资讯审核主页面，包括筛选、列表、详情、拒绝和发布操作入口。
4. 建立 Admin API client 边界，默认使用 mock 数据，后续可切换 Spring Boot `/api/admin/**`。
5. 建立基础测试、类型检查和构建脚本。
6. 更新 README 和任务清单，说明启动方式和当前状态。

非目标：

1. 本阶段不接真实登录、SSO 或复杂 RBAC。
2. 本阶段不实现完整采集任务、文章管理、分类管理和简报管理页面，只保留导航入口。
3. 本阶段不接真实外部资讯源。
4. 本阶段不实现真实 AI 摘要生成。
5. 本阶段不下载或展示真实 PDF 文件正文。
6. 本阶段不做用户端 React/PWA。

## 影响范围

前端：

1. 新增 `admin/` 独立工程。
2. 使用 React + Vite + TypeScript。
3. 使用本地 mock 数据展示候选审核流程。
4. 预留 `VITE_ADMIN_API_BASE_URL` 和 `VITE_ADMIN_TOKEN` 环境变量。

后端：

1. 不新增后端接口。
2. 使用上一阶段已完成的 `/api/admin/candidates` API 作为后续联调目标。

Flutter：

1. 不改动 Flutter 页面和数据层。
2. 用户端继续只读取 `PUBLISHED` 文章。

## 前端结构方案

建议目录：

```text
admin/
├── index.html
├── package.json
├── tsconfig.json
├── tsconfig.node.json
├── vite.config.ts
├── vitest.config.ts
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── styles.css
    ├── app/
    │   └── navigation.ts
    ├── shared/
    │   ├── api/
    │   │   └── adminApi.ts
    │   ├── theme/
    │   │   └── tokens.ts
    │   └── types/
    │       └── candidate.ts
    ├── mock/
    │   └── candidates.ts
    └── features/
        └── candidates/
            ├── CandidateReviewPage.tsx
            └── candidateUtils.ts
```

## UI 设计方向

后台采用专业运营工具风格：

1. 白色和浅灰背景。
2. 深蓝作为主色。
3. 青绿色只用于成功或新鲜状态点缀。
4. 8px 左右圆角，细描边和轻阴影。
5. 高密度信息优先，使用表格和详情面板，不使用营销式 Hero。
6. 左侧固定导航，顶部显示环境和 Token 状态。
7. 候选审核页面以列表为主，右侧详情用于快速审核。
8. 状态使用明确芯片：待审核、已发布、已拒绝。

首批可用页面：

1. 仪表盘：暂展示关键指标。
2. 候选资讯：本阶段主页面。
3. 采集任务、文章管理、分类管理、简报管理：保留导航和空状态。

## API 对接方案

默认使用 mock 数据，后续切换真实 API：

```text
VITE_ADMIN_API_BASE_URL=http://localhost:8080
VITE_ADMIN_TOKEN=dev-admin-token
```

API client 预留方法：

1. `listCandidates(status?: string)`
2. `getCandidate(id: number)`
3. `rejectCandidate(id: number, reviewNote: string)`
4. `publishCandidate(id: number, payload)`

当前阶段：

1. mock 模式下所有操作只更新前端本地状态。
2. 真实 API 模式待下一阶段联调时开启。

## 测试与回归方案

Admin：

1. `npm test -- --run`
2. `npm run build`
3. `npm run lint`

后端：

1. 保持 `mvnw test` 通过。

手工验收：

1. 后台能独立启动。
2. 候选资讯页面能看到列表、详情和状态筛选。
3. 点击拒绝后状态变为已拒绝。
4. 点击发布后状态变为已发布。
5. 页面不出现移动端底部导航或 PWA 痕迹。

## 风险与分阶段落地建议

风险：

1. 后台 UI 若过早接真实接口，容易和后端接口演进互相阻塞。
2. Admin Token 只是开发态方案，生产必须替换为真实登录和权限。
3. 候选审核只是运营闭环第一步，后续还需要采集任务、文章管理和简报管理。

分阶段：

1. 本阶段先完成 React Admin 工程骨架和候选审核 mock 页面。
2. 下一阶段接入真实 `/api/admin/candidates`。
3. 再补采集任务、文章管理和每日简报审核。
4. 最后补登录、权限和操作审计。
