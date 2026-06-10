# React Admin 每日简报管理 API 联调设计

## 背景与现状

PulseBrief 已完成阶段 18 第一批后端闭环：Admin 可通过 `/api/admin/digests` 查询已发布文章池、创建每日简报草稿并发布为 `daily_digest`，发布后 Flutter 用户端复用 `/api/digests/today` 和 `/api/digests/{id}` 读取简报。

当前 React Admin 中“简报管理”仍是占位页面，运营人员无法在后台完成“选择已发布文章 → 生成热点清单 → 创建草稿 → 发布到 APP”的操作。

## 目标与非目标

目标：

1. 将 React Admin 的“简报管理”占位页替换为可用工作台。
2. 支持 mock/API 双模式，未配置后端时仍可本地预览。
3. 支持查看简报列表、选择已发布文章、创建草稿、发布草稿。
4. 页面文案和交互保持后台工具风格，和现有候选审核页一致。

非目标：

1. 不实现简报编辑和下线，因为后端接口尚未完成。
2. 不接入真实 TTS 音频生成。
3. 不实现 AI 自动生成摘要。
4. 不改变 Flutter 用户端页面结构。

## 影响范围

前端：

1. `admin/src/App.tsx`：将 `/digests` 路由指向真实页面。
2. `admin/src/shared/api/adminApi.ts`：补充 Digest 相关类型和客户端方法。
3. `admin/src/features/digests/*`：新增简报管理页面和辅助逻辑。
4. `admin/src/mock/*`：新增简报和文章候选 mock 数据。
5. `admin/src/styles.css`：复用现有后台视觉系统，补充表单、选择列表和摘要栏样式。

后端：

1. 本阶段不改后端。
2. 依赖已完成的 `/api/admin/digests/article-candidates`、`GET /api/admin/digests`、`POST /api/admin/digests`、`POST /api/admin/digests/{id}/publish`。

## 数据模型或权限模型

前端新增类型：

```ts
type AdminDigestStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE';

interface AdminDigest {
  id: number;
  digestDate: string;
  digestType: string;
  categoryCode: string;
  title: string;
  summary: string;
  content: string;
  audioText: string;
  status: AdminDigestStatus;
  publishTime: string | null;
  articleCount: number;
  articles: AdminDigestArticle[];
  availableActions: string[];
}

interface AdminDigestArticleCandidate {
  id: number;
  title: string;
  sourceName: string;
  publishTime: string;
  categoryName: string;
  summary: string;
}
```

权限模型：

1. 继续使用开发期 `Authorization: Bearer dev-admin-token`。
2. 缺少 Token 时由后端返回 `401`，前端展示错误提示。

## 后端实现方案

本阶段不新增后端接口，仅消费已有接口：

1. `GET /api/admin/digests?status=DRAFT&page=1&pageSize=50`
2. `GET /api/admin/digests?status=PUBLISHED&page=1&pageSize=50`
3. `GET /api/admin/digests/article-candidates?keyword=&page=1&pageSize=50`
4. `POST /api/admin/digests`
5. `POST /api/admin/digests/{id}/publish`

## 前端影响

页面结构：

1. 顶部标题、说明和刷新按钮。
2. 指标区：草稿数、已发布数、已选文章数、候选文章数。
3. 左侧主区域：简报列表、创建草稿表单、文章候选池。
4. 右侧详情栏：当前草稿/已发布简报详情、热点清单、发布按钮。

基础交互：

1. 进入 `/digests` 后加载简报列表和文章候选池。
2. 点击文章候选可加入或移出当前草稿选择。
3. 创建草稿时根据选中文章自动生成热点清单和播报文案。
4. 发布草稿后更新本地简报列表状态。
5. API 模式失败时展示错误，不阻断 mock 模式本地预览。

## 测试与回归方案

React Admin 测试：

1. `adminApi.test.ts`：覆盖 Digest 列表、文章候选、创建草稿、发布草稿的 HTTP 映射。
2. `App.test.tsx`：覆盖进入简报管理页、选择文章、创建草稿、发布草稿。

验证命令：

```powershell
cd admin
npm test -- --run
npm run lint
npm run build
```

## 风险与分阶段落地建议

风险：

1. 后端暂未实现编辑和下线，页面不能展示完整运营生命周期。
2. 文章候选池当前后端按 `findAll()` 过滤，数据量增大后需要分页和搜索优化。
3. Mock 与 API 响应结构若后续分化，可能导致页面状态不一致。

分阶段建议：

1. 第一批实现创建和发布闭环。
2. 第二批补后端编辑/下线接口，并在 React Admin 增加编辑态。
3. 第三批补操作日志、发布前校验和 OpenAPI 示例。
