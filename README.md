# PulseBrief

PulseBrief 是一个“全球热点简报 + AI 摘要 + 语音播报”产品的首版工程基线。仓库中的原始产品设计文档规划了 Flutter + Spring Boot + MySQL 的完整 V1.0，本次交付先提供一个可运行的 React/Vite 移动端 PWA MVP 和 Node mock API，方便产品走查与后续后端/Flutter 迁移。

## 已落地范围

用户端：

1. 模拟登录与兴趣选择。
2. 首页热点、订阅分类、资讯列表。
3. 分类浏览与资讯详情。
4. 每日早报、午间快讯、晚间复盘。
5. 收藏、播放入口、底部迷你播放器。
6. 我的订阅、收藏、播放设置、推送设置。

后台端：

1. 数据看板。
2. 资讯源、文章、简报、推送任务列表。
3. 用户端/后台端视图切换。

工程侧：

1. React + Vite + TypeScript 前端。
2. Node 原生 http mock API。
3. Vitest + Testing Library 测试。
4. 工程落地设计与测试方案。

## 本地运行

安装依赖：

```bash
npm install
```

启动前端：

```bash
npm run dev
```

启动 mock API：

```bash
npm run api
```

验证：

```bash
npm test
npm run build
```

## 目录结构

```text
apps/web              React/Vite PWA
services/mock-api     本地 mock API
docs                  工程落地设计与测试方案
每日全球热点资讯APP产品设计文档.md  原始产品规格
```

## 后续迁移路径

1. 以 `services/mock-api` 响应结构作为 Spring Boot DTO 初稿。
2. 以产品设计文档第 11 章 SQL 作为 MySQL schema 初稿。
3. 先替换只读接口，再接入用户收藏、订阅、播放历史写接口。
4. 最后迁移 Flutter APP、真实资讯采集、AI 摘要和推送。

