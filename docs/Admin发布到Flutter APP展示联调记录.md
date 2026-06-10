# Admin 发布到 Flutter APP 展示联调记录

## 背景与现状

PulseBrief 已具备真实资讯采集、候选资讯生成、Admin 审核发布 API、React Admin 候选审核页和 Flutter 用户端 API Repository。上一阶段完成了 React Admin 对 `/api/admin/candidates` 的真实 API 接入，本阶段需要验证“后台发布的候选资讯”是否能稳定进入 Flutter APP 读取的用户端接口。

## 目标与非目标

目标：

1. 验证 Admin 发布候选资讯后，用户端 `/api/articles` 可读取该文章卡片。
2. 验证用户端 `/api/articles/{id}` 可读取文章详情、AI 摘要、核心要点和影响分析。
3. 验证 Flutter `ApiPulseRepository` 能把后台发布后的文章详情响应映射成 APP 使用的 `Article` 模型。
4. 将该闭环纳入自动化回归，减少后续 Admin 或移动端改动破坏发布链路的风险。

非目标：

1. 不新增真实外部资讯 Provider。
2. 不改 Flutter UI 页面视觉。
3. 不实现 Admin 候选编辑表单。
4. 不生成每日简报或真实音频。

## 影响范围

后端：

- 增强 `AdminCandidateControllerTest`，从服务层断言升级到用户端 Controller 接口断言。
- 覆盖发布后 `/api/articles` 列表与 `/api/articles/{id}` 详情可读。

Flutter：

- 增强 `mobile/test/repository_test.dart`，覆盖后台发布文章详情响应映射。
- 不改 UI 组件和页面结构。

文档：

- 更新下一阶段任务清单，记录阶段 17 到 APP 展示链路已纳入回归。

## 数据模型或权限模型

数据流保持不变：

```text
raw_news_item
→ candidate_article(PENDING_REVIEW)
→ Admin publish
→ news_article(PUBLISHED)
→ /api/articles、/api/articles/{id}
→ Flutter ApiPulseRepository
→ HomePage / CategoryPage / ArticleDetailPage
```

权限模型保持不变：

- Admin 发布接口需要 `Authorization: Bearer dev-admin-token`。
- 用户端文章列表和详情仍为公开读取接口。
- 登录态下读取详情会记录阅读历史，但非登录读取不受影响。

## 后端验证方案

1. 通过 fixture ingestion 创建一条 pending candidate。
2. 调用 `POST /api/admin/candidates/{id}/publish` 发布候选。
3. 读取 `candidate_article.published_article_id`。
4. 调用 `GET /api/articles?categoryCode=all&page=1&pageSize=50`，断言列表包含该文章 ID 和标题。
5. 调用 `GET /api/articles/{publishedArticleId}`，断言详情标题、AI 摘要、核心要点和影响分析来自 Admin 发布请求。
6. 保留重复发布返回 409 的断言。

## Flutter 验证方案

1. 在 `ApiPulseRepository` 测试 fake transport 中增加 `GET /articles/101` 响应。
2. 响应字段模拟 Admin 发布后的用户端详情结构：
   - `aiSummary`
   - `keyPoints`
   - `impactAnalysis`
   - `relatedArticles`
3. 调用 `repository.getArticleDetail('101')`。
4. 断言 APP 模型中的 `summary`、`keyPoints`、`impact` 和时间格式化正确。

## 测试与回归方案

本阶段执行：

```powershell
cd backend
.\mvnw.cmd test

cd ..\mobile
flutter analyze
flutter test
```

验收标准：

1. 后端测试通过。
2. Flutter analyze 无问题。
3. Flutter test 全部通过。
4. 文档任务清单标记该闭环验证完成。

## 风险与分阶段建议

风险：

1. 当前真实外部 Provider 未接入，本阶段仍使用 fixture candidate 验证发布链路。
2. Flutter UI 层不做截图复验，本阶段主要验证数据链路和模型映射。
3. 每日简报仍未从已发布文章生成，需要下一阶段单独推进。

分阶段建议：

1. 本阶段先固定 Admin 发布到 APP 文章展示链路。
2. 下一阶段推进每日简报生成与审核。
3. 再后续补充 Admin 候选编辑、采集任务后台和真实 Provider。
