# React Admin 候选审核 API 联调设计

## 背景与现状

React Admin 已完成候选资讯审核工作台骨架，当前数据层仍使用 `admin/src/mock/candidates.ts` 本地 mock 状态。Spring Boot 后端已经提供 Admin 候选资讯接口，包括列表、详情、发布、拒绝和编辑接口，并通过 `Authorization: Bearer dev-admin-token` 做开发期 Admin Token 校验。

当前基础联调已经完成：React Admin 可在 mock/API 双模式下查看候选列表、候选详情、发布和拒绝候选；后端也已支持本地 Vite CORS 预检。当前剩余缺口是候选详情侧栏仍只能查看内容，运营人员无法在发布前编辑标题、摘要、分类和来源。标签字段暂未进入后端候选模型，需在后续模型扩展中单独设计。

## 目标与非目标

目标：

1. React Admin 在配置 `VITE_ADMIN_API_BASE_URL` 后调用 Spring Boot Admin API。
2. 未配置后端地址时继续使用 mock 数据，方便纯前端预览和 UI 精修。
3. 候选列表、详情、发布、拒绝操作具备真实 API 调用路径。
4. 后端支持本地 Vite 开发端口的 Admin API CORS 预检。
5. 补充前端 API client 单测和后端 CORS 回归测试。
6. 候选详情页支持编辑标题、摘要、分类和来源，并在保存后刷新详情侧栏与列表。

非目标：

1. 不新增采集源、不改变真实资讯采集策略。
2. 不新增 Admin 登录、角色权限或多用户体系。
3. 不调整 Flutter 用户端接口。
4. 不实现批量审核、全文编辑器或 PDF 在线预览。
5. 本轮不实现候选标签字段，避免前端出现无法持久化的假编辑能力。

## 影响范围

前端影响：

- `admin/src/shared/api/adminApi.ts` 从单纯 mock 改为 mock + HTTP 双模式。
- `admin/src/features/candidates/CandidateReviewPage.tsx` 增加加载、错误、刷新和异步发布/拒绝状态。
- `admin/src/features/candidates/CandidateReviewPage.tsx` 增加编辑表单、保存草稿状态和保存成功提示。
- 增加 API client 测试，覆盖列表映射、详情映射、候选编辑和发布/拒绝调用。

后端影响：

- `/api/admin/**` 增加本地开发 CORS 配置。
- Admin Token Filter 放行 `OPTIONS` 预检请求。

文档影响：

- 更新 Admin 工程 README，说明真实 API 模式的环境变量和启动方式。
- 更新下一阶段任务清单中 Admin 联调状态。

## 数据模型或权限模型

前端内部仍使用 `AdminCandidate`：

- `id`、`rawNewsItemId`、`title`、`summary`、`categoryCode`、`sourceName`、`originalUrl`、`publishedAt`、`status` 来自后端候选字段。
- `categoryName` 由前端根据 `categoryCode` 做展示映射。
- `aiSummary` 当前优先使用详情中的 `rawItem.summary` 或候选 `summary` 兜底，后续若后端提供结构化 AI 摘要字段再替换。
- `fetchedAt` 优先使用详情中的 `rawItem.fetchedAt`，列表场景使用后端 `createdAt` 兜底。
- `reportAssets` 来自详情接口，列表场景默认为空数组。
- 本轮新增 `sourceName` 编辑能力，后端 `candidate_article.source_name` 会随草稿一起更新。
- 标签字段后续如需落地，建议新增独立 `tags: string[]` 或 `tag_names` 字段，并明确发布到 `news_article` 的映射规则。

权限模型保持开发期 Admin Token：

- 前端通过 `VITE_ADMIN_TOKEN` 配置 Bearer Token，默认值 `dev-admin-token`。
- 后端继续使用 `pulsebrief.admin.token` 配置项校验。
- CORS 只解决浏览器联调，不代表正式生产权限方案。

## 后端实现方案

1. 新增 Admin CORS 配置：
   - 路径：`/api/admin/**`
   - 允许 Origin Pattern：`http://localhost:*`、`http://127.0.0.1:*`
   - 允许方法：`GET`、`POST`、`PUT`、`DELETE`、`OPTIONS`
   - 允许请求头：`Authorization`、`Content-Type`
2. `AdminTokenFilter` 对 `OPTIONS` 请求直接放行，让 Spring MVC CORS 处理预检响应。
3. 保持已有 Admin 接口响应结构不变。

## 前端实现方案

1. 抽象 `createAdminApiClient(config)`：
   - 有 `apiBaseUrl` 时创建 HTTP client。
   - 无 `apiBaseUrl` 时创建 mock client。
2. HTTP client：
   - 列表：`GET /api/admin/candidates?status=...&page=1&pageSize=50`
   - 详情：`GET /api/admin/candidates/{id}`
   - 发布：`POST /api/admin/candidates/{id}/publish`
   - 拒绝：`POST /api/admin/candidates/{id}/reject`
3. 编辑：`PUT /api/admin/candidates/{id}`，请求体包含 `title`、`summary`、`categoryCode`、`sourceName`。
4. 列表页启动时加载候选池，筛选在前端内存完成；刷新按钮重新拉取。
5. 点击候选行时优先加载详情，补齐 PDF 资产与抓取时间，并将可编辑字段同步到表单。
6. 保存成功后更新本地候选列表和详情侧栏，展示“候选内容已保存”提示。
7. 发布/拒绝成功后更新本地候选状态并保留选中项。
8. 出错时展示可读错误提示，不阻塞 mock 模式使用。

## 测试与回归方案

前端：

- `adminApi` 单测覆盖 HTTP 列表字段映射。
- `adminApi` 单测覆盖详情字段与 PDF 资产映射。
- `adminApi` 单测覆盖候选编辑请求路径、方法、Authorization Header 和 JSON 请求体。
- `adminApi` 单测覆盖发布、拒绝请求路径、方法和 Authorization Header。
- 页面测试覆盖编辑标题、摘要、分类、来源并保存后刷新详情。
- 保留现有页面测试，验证发布操作仍可在 mock 模式下完成。
- 运行 `npm test -- --run`、`npm run lint`、`npm run build`。

后端：

- 新增 CORS 预检测试，验证本地 Vite Origin 可以获得 `Access-Control-Allow-Origin`。
- 保留 Admin Token 保护测试，验证非预检请求仍需要 Token。
- 运行 `.\mvnw.cmd test`。

## 风险与分阶段落地建议

风险：

1. 当前列表接口不返回全文、结构化 AI 摘要和 PDF 资产，Admin 详情栏需要通过详情接口补齐。
2. 开发期 Token 放在前端环境变量中，仅适合本地或内网开发，不适合生产。
3. 后续生产部署需要将 CORS Origin 收紧到正式 Admin 域名。

分阶段建议：

1. 本轮完成 mock + HTTP 双模式和本地联调闭环。
2. 下一轮增加 Admin 登录或后端签发短期管理 Token。
3. 再下一轮扩展候选编辑、批量发布、审核记录和 PDF 资产预览能力。

## 阶段实现记录

已完成：

1. React Admin 候选详情页支持编辑标题、摘要、分类和来源。
2. Admin API client 新增 `updateCandidate(id, input)`，mock/API 双模式均支持保存候选草稿。
3. 后端 `PUT /api/admin/candidates/{id}` 扩展 `sourceName` 字段，保存后会更新 `candidate_article.source_name`。
4. 页面保存成功后会刷新候选列表和详情侧栏，并展示“候选内容已保存”提示。
5. 已补充后端 Controller 测试、Admin API client 测试和页面交互测试。

仍待后续：

1. 候选标签字段设计与持久化。
2. 批量审核。
3. 审核操作日志。
4. PDF 资产合规状态编辑。
