# 每日全球热点资讯 APP 产品设计文档

## 1. 文档信息

| 项目 | 内容 |
|---|---|
| 产品名称 | 暂定：Global Brief / 全球热点简报 |
| 产品类型 | 移动端资讯订阅 APP |
| 支持终端 | iOS、Android |
| 技术路线 | Flutter + Spring Boot + MySQL |
| 首版内容来源 | 免费公开渠道、RSS、GDELT、官方公开信息、投行公开观点页 |
| 核心能力 | 全球热点聚合、分类订阅、AI 摘要、语音播报、每日简报、原文跳转 |
| 首版目标 | 低成本上线，验证用户对“全球热点 + 投行观点 + 语音简报”的需求 |

---

## 2. 产品定位

### 2.1 一句话定位

面向关注全球资讯、财经市场、科技趋势和投行观点的用户，提供每日全球热点摘要、分类订阅和语音播报服务。

### 2.2 核心价值

本产品不是传统新闻客户端，也不是新闻全文搬运工具，而是一个：

> 全球热点资讯筛选工具 + AI 摘要工具 + 语音简报工具。

核心价值包括：

1. 帮用户节省筛选全球资讯的时间。
2. 将分散的新闻、公开机构观点、投行公开内容聚合为简明摘要。
3. 支持按兴趣分类订阅。
4. 支持每日早报、晚报和语音播报。
5. 支持跳转原文，避免直接搬运全文带来的版权风险。

---

## 3. 目标用户

### 3.1 核心用户

| 用户类型 | 需求 |
|---|---|
| 投资者 | 快速了解全球市场、科技、AI、宏观、产业动态 |
| 职场人士 | 通勤时通过语音收听每日热点 |
| 科技从业者 | 关注 AI、半导体、互联网、产业趋势 |
| 跨境业务人员 | 关注国际政治、经济、贸易和金融政策 |
| 高信息密度用户 | 希望减少碎片化阅读，提高信息获取效率 |

### 3.2 典型用户场景

#### 场景一：早上通勤

用户打开 APP，进入“每日早报”，点击“语音播报”，收听今日全球热点。

#### 场景二：中午看市场

用户进入“财经市场”分类，查看美股、港股、宏观、投行观点相关摘要。

#### 场景三：晚上复盘

用户进入“晚间复盘”，查看当天热点事件及影响解读。

#### 场景四：关注 AI 产业

用户订阅“AI”“半导体”“云计算”“投行观点”，每日接收相关推送。

---

## 4. 首版产品范围

### 4.1 V1.0 必做功能

| 模块 | 功能 | 是否首版 |
|---|---|---|
| 首页 | 今日全球热点 | 是 |
| 分类 | 按分类浏览资讯 | 是 |
| 订阅 | 用户选择关注分类 | 是 |
| 简报 | 每日早报 / 晚报 | 是 |
| 详情 | 资讯详情、AI 摘要、原文跳转 | 是 |
| 语音 | 单条资讯播报、每日简报播报 | 是 |
| 收藏 | 收藏资讯 | 是 |
| 我的 | 用户信息、订阅设置、播放设置 | 是 |
| 推送 | 每日简报推送 | 是 |
| 后台 | 资讯源、分类、文章、简报、推送管理 | 是 |

### 4.2 V1.0 暂不做功能

| 功能 | 暂不做原因 |
|---|---|
| 社区评论 | 审核成本高，首版不需要 |
| 用户发帖 | 偏社区产品，非首版核心 |
| 复杂个性化推荐 | 需要用户行为积累 |
| 研报 PDF 下载 | 版权风险高 |
| 新闻全文展示 | 版权风险高 |
| 付费会员 | 先验证使用价值 |
| 服务端高质量音频 | 首版先用客户端 TTS |
| Elasticsearch 搜索 | MySQL 基础搜索先够用 |
| Redis 缓存 | 初期用户量不大，可后置 |

---

## 5. 内容策略设计

### 5.1 内容来源原则

首版坚持以下原则：

1. 只接入公开免费渠道。
2. 不抓取付费墙内容。
3. 不搬运完整新闻正文。
4. 不存储完整投行研报 PDF。
5. 展示标题、来源、发布时间、短摘要、AI 解读和原文链接。
6. 用户点击原文时跳转至原网站。

### 5.2 首版内容来源类型

| 来源类型 | 示例 | 用途 |
|---|---|---|
| 全球新闻索引 | GDELT | 全球热点发现 |
| RSS | 官方媒体 RSS、公开博客 RSS | 标题和摘要获取 |
| 官方公告 | 美联储、SEC、联合国、政府公告 | 宏观和政策信息 |
| 投行公开观点 | 高盛、大摩、野村公开 Insights 页面 | 投行观点摘要 |
| 科技公司博客 | OpenAI、Google、Microsoft、NVIDIA 等公开博客 | 科技和 AI 动态 |
| 财经公开数据源 | 官方交易所公告、上市公司 IR 页面 | 财经事件补充 |

### 5.3 内容展示边界

APP 内允许展示：

1. 标题。
2. 来源。
3. 发布时间。
4. 原文链接。
5. 机器生成的简短摘要。
6. 自有结构化解读。
7. 多来源事件聚合。

APP 内不建议展示：

1. 原站完整正文。
2. 完整研报 PDF。
3. 需要登录才能访问的内容。
4. 付费订阅内容。
5. 未授权的长篇翻译内容。

---

## 6. 内容分类设计

### 6.1 首版一级分类

| 分类名称 | 分类编码 | 说明 |
|---|---|---|
| 全球热点 | global | 默认综合热点 |
| 财经市场 | finance | 股市、债券、汇率、大宗商品 |
| 科技趋势 | technology | 科技公司、产品、互联网 |
| AI 前沿 | ai | 大模型、算力、芯片、AI 应用 |
| 宏观政策 | macro | 利率、通胀、央行、财政政策 |
| 投行观点 | ib_research | 高盛、大摩、野村等公开观点 |
| 中美动态 | china_us | 中美关系、贸易、产业政策 |
| 产业观察 | industry | 半导体、新能源、医药、消费等 |
| 公司动态 | company | 重点公司新闻和公告 |
| 每日简报 | digest | 早报、晚报、专题简报 |

### 6.2 分类订阅规则

1. 用户首次进入 APP 时，系统引导用户选择关注分类。
2. 用户至少选择 3 个分类。
3. 默认订阅：全球热点、财经市场、科技趋势。
4. 用户可在“我的 - 订阅管理”中修改。
5. 首页优先展示用户已订阅分类内容。

---

## 7. APP 信息架构

### 7.1 底部导航设计

首版采用 4 个底部 Tab：

```text
首页
分类
简报
我的
```

### 7.2 页面结构

```text
APP
├── 首页
│   ├── 今日热点
│   ├── 我的订阅
│   ├── 投行观点
│   ├── AI 前沿
│   └── 热点列表
│
├── 分类
│   ├── 全球热点
│   ├── 财经市场
│   ├── 科技趋势
│   ├── AI 前沿
│   ├── 宏观政策
│   ├── 投行观点
│   └── 产业观察
│
├── 简报
│   ├── 每日早报
│   ├── 午间快讯
│   ├── 晚间复盘
│   └── 专题简报
│
└── 我的
    ├── 订阅管理
    ├── 收藏
    ├── 阅读历史
    ├── 播放历史
    ├── 播报设置
    ├── 推送设置
    └── 关于我们
```

---

## 8. 核心页面设计

### 8.1 启动页

#### 页面目标

展示品牌，完成初始化加载。

#### 页面元素

1. 产品 Logo。
2. 产品名称。
3. 加载动画。
4. 版本号。

#### 交互规则

1. 已登录用户直接进入首页。
2. 未登录用户进入登录页。
3. 首次登录用户进入兴趣选择页。

---

### 8.2 登录页

#### 支持方式

首版建议支持：

1. 手机号验证码登录。
2. 邮箱验证码登录。
3. Apple 登录，iOS 必备。
4. 游客模式，可选。

#### 页面字段

| 字段 | 说明 |
|---|---|
| 手机号 / 邮箱 | 用户输入 |
| 验证码 | 6 位验证码 |
| 登录按钮 | 验证成功后登录 |
| 用户协议 | 必须勾选 |
| 隐私政策 | 必须勾选 |

#### 规则

1. 未勾选协议不能登录。
2. 验证码有效期 5 分钟。
3. 同一手机号 60 秒内不能重复发送。
4. 登录成功后写入用户 token。

---

### 8.3 兴趣选择页

#### 页面目标

让用户完成初始订阅。

#### 页面元素

1. 标题：选择你关注的内容。
2. 分类标签。
3. 已选数量提示。
4. 完成按钮。

#### 分类标签

```text
全球热点
财经市场
科技趋势
AI 前沿
宏观政策
投行观点
中美动态
产业观察
公司动态
```

#### 规则

1. 最少选择 3 个。
2. 默认选中全球热点、财经市场、科技趋势。
3. 点击完成后进入首页。
4. 用户选择结果写入 user_subscription 表。

---

### 8.4 首页

#### 页面目标

展示用户最关心的今日热点。

#### 页面布局

```text
顶部区域
├── 日期
├── 问候语
├── 搜索入口
└── 播放今日简报按钮

热点卡片区
├── 今日全球热点 Top 3
├── 一键播放
└── 查看全部

订阅分类横向 Tab
├── 全部
├── 财经市场
├── 科技趋势
├── AI 前沿
├── 投行观点
└── 更多

资讯列表
├── 资讯卡片 1
├── 资讯卡片 2
└── 资讯卡片 N
```

#### 资讯卡片字段

| 字段 | 说明 |
|---|---|
| 标题 | 资讯标题 |
| 来源 | 来源名称 |
| 发布时间 | 相对时间，如 2 小时前 |
| 分类标签 | 如 AI、财经 |
| 摘要 | 1-2 行 |
| 热度标识 | 热点、突发、深度 |
| 播放按钮 | 播放单条资讯 |
| 收藏按钮 | 收藏资讯 |

#### 首页排序规则

默认排序逻辑：

```text
用户订阅分类优先
+
hot_score 热度分
+
publish_time 发布时间
```

排序优先级：

1. 置顶简报。
2. 突发热点。
3. 用户订阅分类。
4. 高热度文章。
5. 最新文章。

---

### 8.5 分类页

#### 页面目标

用户按主题浏览资讯。

#### 页面结构

```text
顶部分类导航
├── 全球热点
├── 财经市场
├── 科技趋势
├── AI 前沿
├── 宏观政策
├── 投行观点
└── 产业观察

当前分类内容
├── 分类简介
├── 今日重点
├── 资讯列表
└── 加载更多
```

#### 交互规则

1. 点击分类切换列表。
2. 下拉刷新当前分类。
3. 上滑分页加载。
4. 点击资讯进入详情页。
5. 点击播放按钮直接播报当前资讯摘要。

---

### 8.6 资讯详情页

#### 页面目标

展示单条资讯的结构化摘要和原文入口。

#### 页面结构

```text
标题
来源 / 发布时间 / 分类
AI 摘要
核心要点
可能影响
相关资讯
操作区
├── 语音播报
├── 收藏
├── 分享
└── 阅读原文
```

#### 字段说明

| 区域 | 内容 |
|---|---|
| 标题 | 原始标题或规范化标题 |
| 来源 | 原始来源名称 |
| 发布时间 | 原始发布时间 |
| AI 摘要 | 100-300 字 |
| 核心要点 | 3-5 条 bullet |
| 可能影响 | 对市场、行业、政策等的简短解读 |
| 相关资讯 | 同事件其他来源 |
| 阅读原文 | 跳转外部浏览器或内置 WebView |

#### 合规展示规则

1. 不展示原文全文。
2. 摘要不超过合理长度。
3. 明确展示来源和原文链接。
4. 用户点击“阅读原文”跳转原站。
5. 投行公开观点只做短摘要，不展示完整报告。

---

### 8.7 简报页

#### 页面目标

承载每日早报、午间快讯、晚间复盘和专题简报。

#### 页面结构

```text
简报页
├── 今日早报
│   ├── 标题
│   ├── 发布时间
│   ├── 播放按钮
│   └── 简报内容
│
├── 午间快讯
├── 晚间复盘
└── 专题简报
```

#### 简报类型

| 类型 | 时间 | 内容 |
|---|---|---|
| 每日早报 | 07:00 | 过去 24 小时全球重点 |
| 午间快讯 | 12:00 | 亚洲和欧洲时段重点 |
| 晚间复盘 | 18:00 | 市场和产业复盘 |
| 专题简报 | 不固定 | AI、半导体、投行观点等专题 |

#### 简报生成规则

1. 系统定时从 news_article 中筛选高热度内容。
2. 按分类聚合。
3. 生成简报标题、摘要和播报文案。
4. 写入 daily_digest 表。
5. APP 端读取后展示和播报。

---

### 8.8 语音播报设计

#### 首版实现方式

首版采用客户端系统 TTS。

#### 播报入口

1. 首页顶部：播放今日简报。
2. 资讯卡片：播放单条资讯。
3. 资讯详情：播放当前资讯。
4. 简报页：播放整篇简报。
5. 播放历史：继续播放。

#### 播放器样式

底部悬浮迷你播放器：

```text
左侧：当前播报标题
中间：播放 / 暂停
右侧：下一条
```

点击迷你播放器进入完整播放器。

#### 完整播放器功能

1. 播放 / 暂停。
2. 上一条 / 下一条。
3. 播放进度。
4. 播放倍速。
5. 播报语言。
6. 停止播放。
7. 查看原文。

#### 播放规则

1. 播放单条资讯时，播报 AI 摘要和核心要点。
2. 播放简报时，播报 daily_digest.content。
3. 用户切换页面时，底部播放器保持。
4. 用户关闭 APP 后，首版可先不支持后台继续播放。
5. 播放记录写入 user_play_history。

---

### 8.9 我的页面

#### 页面模块

```text
用户信息
├── 头像
├── 昵称
└── 登录状态

内容管理
├── 我的订阅
├── 我的收藏
├── 阅读历史
└── 播放历史

设置
├── 播报设置
├── 推送设置
├── 主题设置
├── 清除缓存
└── 关于我们
```

#### 订阅管理

用户可以：

1. 新增分类订阅。
2. 取消分类订阅。
3. 调整订阅排序。
4. 恢复默认订阅。

#### 播报设置

| 设置项 | 默认值 |
|---|---|
| 播报语言 | 中文 |
| 播放倍速 | 1.0x |
| 是否自动播放下一条 | 是 |
| 是否播报来源 | 是 |
| 是否播报时间 | 否 |

#### 推送设置

| 设置项 | 默认值 |
|---|---|
| 每日早报推送 | 开启 |
| 晚间复盘推送 | 开启 |
| 突发热点推送 | 关闭 |
| 投行观点推送 | 关闭 |
| 推送时间 | 07:30 |

---

## 9. 后台管理系统设计

### 9.1 后台目标

后台用于管理资讯来源、分类、文章、简报、推送任务和基础运营数据。

### 9.2 后台菜单

```text
后台管理系统
├── 数据看板
├── 资讯源管理
├── 分类管理
├── 文章管理
├── 简报管理
├── 推送管理
├── 用户管理
├── 订阅统计
└── 系统配置
```

---

### 9.3 数据看板

#### 指标

| 指标 | 说明 |
|---|---|
| 今日采集文章数 | 当天入库文章总数 |
| 今日有效文章数 | 通过过滤和去重后的文章数 |
| 今日简报数 | 已生成简报数量 |
| 今日活跃用户 | 打开 APP 的用户数 |
| 今日播放次数 | TTS 播放次数 |
| 今日收藏次数 | 收藏行为次数 |
| 推送发送数 | 今日推送数量 |
| 推送点击率 | 推送点击 / 推送发送 |

---

### 9.4 资讯源管理

#### 功能

1. 新增资讯源。
2. 编辑资讯源。
3. 启用 / 停用资讯源。
4. 配置来源类型。
5. 配置风险等级。
6. 配置采集频率。
7. 查看最近采集状态。

#### 字段

| 字段 | 说明 |
|---|---|
| 来源名称 | 如 Goldman Sachs Research |
| 来源类型 | GDELT / RSS / OFFICIAL / IB_PUBLIC |
| 来源地址 | URL |
| 默认分类 | 如投行观点 |
| 语言 | en / zh / ja 等 |
| 国家地区 | US / CN / JP 等 |
| 是否允许商业展示 | 首版默认谨慎 |
| 是否允许展示全文 | 默认否 |
| 风险等级 | LOW / MEDIUM / HIGH |
| 采集频率 | 10 分钟 / 30 分钟 / 1 小时 |
| 状态 | 启用 / 停用 |

---

### 9.5 文章管理

#### 功能

1. 查看文章列表。
2. 按来源筛选。
3. 按分类筛选。
4. 按状态筛选。
5. 查看文章详情。
6. 修改分类。
7. 调整热度分。
8. 设置置顶。
9. 下架文章。

#### 文章状态

| 状态 | 说明 |
|---|---|
| 待处理 | 刚采集入库 |
| 已分类 | 已完成分类 |
| 已摘要 | 已生成摘要 |
| 已发布 | APP 可见 |
| 已下架 | APP 不可见 |
| 采集异常 | 数据不完整或不合规 |

---

### 9.6 简报管理

#### 功能

1. 查看每日简报。
2. 手动生成简报。
3. 编辑简报标题。
4. 编辑简报内容。
5. 设置发布时间。
6. 设置推送状态。
7. 预览播报文案。
8. 发布 / 下架简报。

#### 简报状态

| 状态 | 说明 |
|---|---|
| 草稿 | 已生成但未发布 |
| 已发布 | APP 可见 |
| 已推送 | 已发送推送 |
| 已下架 | 不再展示 |

---

### 9.7 推送管理

#### 功能

1. 创建推送任务。
2. 选择推送对象。
3. 设置推送时间。
4. 预览推送内容。
5. 查看发送状态。
6. 查看点击数据。

#### 推送对象

| 类型 | 说明 |
|---|---|
| 全部用户 | 所有用户 |
| 分类订阅用户 | 订阅某分类的用户 |
| 活跃用户 | 最近 7 天活跃 |
| 指定用户 | 后台指定用户 |

---

## 10. 主要业务流程

### 10.1 用户首次使用流程

```text
打开 APP
↓
启动页
↓
登录 / 游客进入
↓
选择兴趣分类
↓
进入首页
↓
查看今日热点
↓
点击语音播报
```

### 10.2 资讯采集流程

```text
定时任务触发
↓
读取启用的资讯源
↓
按来源类型调用采集适配器
↓
解析标题、摘要、链接、时间、来源
↓
生成 article_hash
↓
MySQL 去重
↓
写入 news_article
↓
进入待处理状态
```

### 10.3 资讯处理流程

```text
读取待处理文章
↓
规则过滤
↓
分类识别
↓
生成 AI 摘要
↓
计算热度分
↓
更新为已发布状态
```

### 10.4 每日简报生成流程

```text
定时任务触发
↓
查询过去 24 小时高热度文章
↓
按分类聚合
↓
生成简报大纲
↓
生成播报文案
↓
写入 daily_digest
↓
后台可编辑
↓
发布到 APP
```

### 10.5 语音播报流程

```text
用户点击播放
↓
APP 请求播报文本
↓
Flutter 调用系统 TTS
↓
展示底部播放器
↓
记录播放历史
```

---

## 11. 数据库表设计

### 11.1 用户表 app_user

```sql
CREATE TABLE app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mobile VARCHAR(30),
    email VARCHAR(100),
    nickname VARCHAR(100),
    avatar_url VARCHAR(500),
    user_status TINYINT DEFAULT 1 COMMENT '1正常 0禁用',
    last_login_time DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_mobile (mobile),
    UNIQUE KEY uk_email (email)
);
```

### 11.2 分类表 news_category

```sql
CREATE TABLE news_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_no INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_category_code (category_code)
);
```

### 11.3 资讯源表 news_source

```sql
CREATE TABLE news_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_name VARCHAR(100) NOT NULL,
    source_type VARCHAR(30) NOT NULL COMMENT 'GDELT/RSS/OFFICIAL/IB_PUBLIC/API',
    source_url VARCHAR(500),
    country VARCHAR(50),
    language VARCHAR(20),
    default_category_code VARCHAR(50),
    usage_policy VARCHAR(50) COMMENT 'FREE/PERSONAL_ONLY/DEV_ONLY/COMMERCIAL_ALLOWED/UNKNOWN',
    allow_title TINYINT DEFAULT 1,
    allow_summary TINYINT DEFAULT 1,
    allow_fulltext TINYINT DEFAULT 0,
    allow_commercial TINYINT DEFAULT 0,
    risk_level VARCHAR(20) DEFAULT 'MEDIUM',
    fetch_interval_minutes INT DEFAULT 60,
    last_fetch_time DATETIME,
    last_fetch_status VARCHAR(30),
    status TINYINT DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME
);
```

### 11.4 资讯文章表 news_article

```sql
CREATE TABLE news_article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(500) NOT NULL,
    normalized_title VARCHAR(500),
    summary TEXT,
    ai_summary TEXT,
    key_points TEXT,
    impact_analysis TEXT,
    source_id BIGINT,
    source_name VARCHAR(100),
    original_url VARCHAR(1000),
    category_id BIGINT,
    category_code VARCHAR(50),
    language VARCHAR(20),
    country VARCHAR(50),
    publish_time DATETIME,
    hot_score DECIMAL(10,2) DEFAULT 0,
    article_hash VARCHAR(64) NOT NULL,
    article_status VARCHAR(30) DEFAULT 'PENDING',
    is_top TINYINT DEFAULT 0,
    view_count INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    play_count INT DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_article_hash (article_hash),
    KEY idx_category_publish_time (category_code, publish_time),
    KEY idx_hot_score (hot_score),
    KEY idx_publish_time (publish_time),
    KEY idx_source_id (source_id)
);
```

### 11.5 用户订阅表 user_subscription

```sql
CREATE TABLE user_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    category_code VARCHAR(50),
    sort_no INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_user_category (user_id, category_id)
);
```

### 11.6 收藏表 user_favorite

```sql
CREATE TABLE user_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    created_at DATETIME,
    UNIQUE KEY uk_user_article (user_id, article_id),
    KEY idx_user_id (user_id)
);
```

### 11.7 阅读历史表 user_read_history

```sql
CREATE TABLE user_read_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    read_time DATETIME,
    KEY idx_user_time (user_id, read_time)
);
```

### 11.8 播放历史表 user_play_history

```sql
CREATE TABLE user_play_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    article_id BIGINT,
    digest_id BIGINT,
    play_type VARCHAR(30) COMMENT 'ARTICLE/DIGEST',
    play_title VARCHAR(500),
    play_time DATETIME,
    duration_seconds INT DEFAULT 0,
    KEY idx_user_time (user_id, play_time)
);
```

### 11.9 每日简报表 daily_digest

```sql
CREATE TABLE daily_digest (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    digest_date DATE NOT NULL,
    digest_type VARCHAR(30) COMMENT 'MORNING/NOON/EVENING/TOPIC',
    category_code VARCHAR(50),
    title VARCHAR(300),
    summary TEXT,
    content TEXT,
    audio_text TEXT,
    digest_status VARCHAR(30) DEFAULT 'DRAFT',
    publish_time DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    KEY idx_digest_date_type (digest_date, digest_type)
);
```

### 11.10 推送任务表 push_task

```sql
CREATE TABLE push_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type VARCHAR(30),
    title VARCHAR(200),
    content VARCHAR(500),
    target_type VARCHAR(30),
    target_category_code VARCHAR(50),
    related_digest_id BIGINT,
    related_article_id BIGINT,
    scheduled_time DATETIME,
    send_status VARCHAR(30) DEFAULT 'PENDING',
    sent_count INT DEFAULT 0,
    click_count INT DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME,
    KEY idx_scheduled_time (scheduled_time)
);
```

---

## 12. API 接口清单

### 12.1 用户接口

| 接口 | 方法 | 说明 |
|---|---|---|
| /api/auth/send-code | POST | 发送验证码 |
| /api/auth/login | POST | 登录 |
| /api/user/profile | GET | 获取用户信息 |
| /api/user/profile | PUT | 修改用户信息 |

### 12.2 分类接口

| 接口 | 方法 | 说明 |
|---|---|---|
| /api/categories | GET | 获取分类列表 |
| /api/user/subscriptions | GET | 获取用户订阅 |
| /api/user/subscriptions | POST | 保存用户订阅 |

### 12.3 资讯接口

| 接口 | 方法 | 说明 |
|---|---|---|
| /api/articles/home | GET | 首页资讯流 |
| /api/articles | GET | 分类资讯列表 |
| /api/articles/{id} | GET | 资讯详情 |
| /api/articles/{id}/favorite | POST | 收藏 |
| /api/articles/{id}/favorite | DELETE | 取消收藏 |
| /api/articles/{id}/read | POST | 记录阅读 |
| /api/articles/{id}/play-text | GET | 获取播报文本 |

### 12.4 简报接口

| 接口 | 方法 | 说明 |
|---|---|---|
| /api/digests/today | GET | 获取今日简报 |
| /api/digests/{id} | GET | 简报详情 |
| /api/digests/{id}/play-text | GET | 获取简报播报文本 |

### 12.5 历史记录接口

| 接口 | 方法 | 说明 |
|---|---|---|
| /api/user/favorites | GET | 我的收藏 |
| /api/user/read-history | GET | 阅读历史 |
| /api/user/play-history | GET | 播放历史 |
| /api/play-history | POST | 新增播放历史 |

### 12.6 后台接口

| 接口 | 方法 | 说明 |
|---|---|---|
| /admin/sources | GET | 资讯源列表 |
| /admin/sources | POST | 新增资讯源 |
| /admin/sources/{id} | PUT | 修改资讯源 |
| /admin/articles | GET | 文章列表 |
| /admin/articles/{id} | PUT | 编辑文章 |
| /admin/digests | GET | 简报列表 |
| /admin/digests/generate | POST | 手动生成简报 |
| /admin/push-tasks | GET | 推送任务列表 |
| /admin/push-tasks | POST | 创建推送任务 |

---

## 13. 热度分规则

### 13.1 hot_score 计算因素

```text
hot_score =
来源权重
+ 时间新鲜度
+ 同事件报道数量
+ 用户阅读量
+ 用户收藏量
+ 用户播放量
+ 后台人工加权
```

### 13.2 初版简化规则

```text
hot_score =
source_weight * 10
+ recency_score
+ view_count * 0.1
+ favorite_count * 0.5
+ play_count * 0.3
+ manual_boost
```

### 13.3 来源权重示例

| 来源类型 | 权重 |
|---|---|
| 官方公告 | 10 |
| 投行公开观点 | 9 |
| 主流公开媒体 RSS | 8 |
| GDELT 聚合 | 7 |
| 普通博客 | 5 |

---

## 14. 推送设计

### 14.1 推送类型

| 推送类型 | 说明 |
|---|---|
| 每日早报 | 每天早上固定推送 |
| 晚间复盘 | 每天晚上固定推送 |
| 突发热点 | 重大事件推送 |
| 分类订阅 | 根据用户订阅分类推送 |

### 14.2 首版推送策略

1. 默认开启每日早报。
2. 默认开启晚间复盘。
3. 默认关闭突发热点，避免打扰。
4. 投行观点推送由用户手动开启。
5. 每天最多 3 条推送。

### 14.3 推送文案示例

```text
今日全球早报已生成：美股、AI、宏观政策和投行观点 10 条重点，一键收听。
```

---

## 15. 非功能需求

### 15.1 性能要求

| 场景 | 要求 |
|---|---|
| 首页加载 | 2 秒内返回首屏数据 |
| 分类切换 | 1 秒内响应 |
| 资讯详情 | 1.5 秒内打开 |
| 简报播放 | 点击后 1 秒内开始播报 |
| 列表分页 | 每页 20 条 |

### 15.2 可用性要求

1. APP 崩溃率低于 1%。
2. 接口异常时展示友好提示。
3. 资讯加载失败支持重试。
4. 首页可缓存最近一次数据。
5. 无网络时提示用户检查网络。

### 15.3 安全要求

1. 用户登录使用 token。
2. 后台接口必须鉴权。
3. 密码、验证码等敏感数据不明文存储。
4. 所有外部链接跳转前提示来源。
5. 管理员操作保留日志。

### 15.4 合规要求

1. APP 内提供用户协议。
2. APP 内提供隐私政策。
3. 明确说明资讯来源。
4. 不展示未授权完整正文。
5. 支持用户注销账号。
6. 支持用户删除个人数据。

---

## 16. 埋点设计

### 16.1 用户行为埋点

| 事件 | 说明 |
|---|---|
| app_open | 打开 APP |
| home_view | 进入首页 |
| category_click | 点击分类 |
| article_view | 查看资讯 |
| article_favorite | 收藏资讯 |
| article_share | 分享资讯 |
| article_play | 播放资讯 |
| digest_view | 查看简报 |
| digest_play | 播放简报 |
| push_click | 点击推送 |
| subscription_update | 修改订阅 |

### 16.2 关键指标

| 指标 | 说明 |
|---|---|
| DAU | 日活用户 |
| 次日留存 | 第二天再次打开比例 |
| 简报播放率 | 播放简报用户 / 活跃用户 |
| 收藏率 | 收藏次数 / 文章浏览次数 |
| 推送点击率 | 点击推送 / 推送发送 |
| 分类订阅分布 | 各分类订阅人数 |
| 平均阅读时长 | 用户阅读内容的停留时间 |

---

## 17. 首版验收标准

### 17.1 用户端验收

| 功能 | 验收标准 |
|---|---|
| 登录 | 用户可以通过手机号或邮箱登录 |
| 分类订阅 | 用户可以选择、修改订阅分类 |
| 首页 | 首页能展示今日热点列表 |
| 分类 | 分类页能按分类展示资讯 |
| 详情 | 详情页能展示摘要、要点、原文链接 |
| 收藏 | 用户可以收藏和取消收藏 |
| 简报 | 用户可以查看每日早报和晚报 |
| 语音 | 用户可以播放单条资讯和简报 |
| 我的 | 用户可以查看收藏、历史、设置 |
| 推送 | 用户可以收到每日简报推送 |

### 17.2 后台验收

| 功能 | 验收标准 |
|---|---|
| 资讯源管理 | 可以新增、编辑、停用资讯源 |
| 分类管理 | 可以维护分类 |
| 文章管理 | 可以查看、编辑、发布、下架文章 |
| 简报管理 | 可以生成、编辑、发布简报 |
| 推送管理 | 可以创建和查看推送任务 |
| 数据看板 | 可以查看基础运营指标 |

### 17.3 内容处理验收

| 功能 | 验收标准 |
|---|---|
| 采集 | 能从配置的免费源采集数据 |
| 去重 | 相同链接或相同标题不重复入库 |
| 分类 | 文章能自动归入分类 |
| 摘要 | 文章能生成中文摘要 |
| 热度 | 首页能按热度和时间排序 |
| 简报 | 每日能自动生成简报草稿 |

---

## 18. 研发拆分建议

### 18.1 第一阶段：基础框架

周期建议：1-2 周。

任务：

1. Flutter APP 工程初始化。
2. Spring Boot 后端工程初始化。
3. MySQL 表结构初始化。
4. 用户登录接口。
5. 分类和订阅接口。
6. APP 首页基础页面。
7. 后台基础框架。

### 18.2 第二阶段：资讯内容链路

周期建议：2-3 周。

任务：

1. 资讯源管理。
2. RSS 采集适配器。
3. GDELT 采集适配器。
4. 投行公开观点源适配器。
5. 文章入库。
6. 去重逻辑。
7. 分类逻辑。
8. 摘要生成逻辑。
9. 文章管理后台。

### 18.3 第三阶段：用户端核心体验

周期建议：2-3 周。

任务：

1. 首页资讯流。
2. 分类资讯列表。
3. 资讯详情页。
4. 收藏功能。
5. 阅读历史。
6. 播放历史。
7. 客户端 TTS 播报。
8. 底部播放器。
9. 简报页。

### 18.4 第四阶段：简报和推送

周期建议：1-2 周。

任务：

1. 每日简报生成。
2. 简报后台编辑。
3. 简报发布。
4. 推送任务管理。
5. iOS APNs 接入。
6. Android 推送接入。
7. 推送点击跳转。

### 18.5 第五阶段：测试和上架准备

周期建议：1-2 周。

任务：

1. 功能测试。
2. 兼容性测试。
3. 性能测试。
4. 隐私政策。
5. 用户协议。
6. App Store 上架材料。
7. Google Play 上架材料。
8. 内容来源说明。
9. 合规风险检查。

---

## 19. V1.0 发布范围

### 19.1 V1.0 用户端

1. 登录。
2. 兴趣选择。
3. 首页热点。
4. 分类浏览。
5. 资讯详情。
6. AI 摘要。
7. 语音播报。
8. 每日简报。
9. 收藏。
10. 阅读历史。
11. 播放历史。
12. 推送设置。
13. 我的页面。

### 19.2 V1.0 后台端

1. 数据看板。
2. 资讯源管理。
3. 分类管理。
4. 文章管理。
5. 简报管理。
6. 推送管理。
7. 用户管理。

### 19.3 V1.0 内容端

1. 免费资讯源采集。
2. 官方公开源采集。
3. 投行公开观点采集。
4. 去重。
5. 分类。
6. 摘要。
7. 热度排序。
8. 简报生成。

---

## 20. 后续版本规划

### V1.1 体验增强

1. 首页缓存。
2. 深色模式。
3. 关键词订阅。
4. 多语言摘要。
5. 搜索功能。
6. 资讯来源可信度标识。
7. 更多投行公开观点源。

### V1.5 内容增强

1. 专题追踪。
2. 事件时间线。
3. 同事件多来源对比。
4. AI 影响分析。
5. AI 财经解读。
6. 行业专题简报。
7. 服务端高质量 TTS 音频。

### V2.0 商业化

1. 会员订阅。
2. 专属财经早报。
3. 专属投行观点摘要。
4. 无广告模式。
5. 企业简报。
6. Web 管理端企业版。
7. 内容源付费授权接入。

---

## 21. 产品落地结论

首版建议按以下原则执行：

1. 产品定位不要做“新闻搬运”，而是做“全球热点简报”。
2. 内容来源优先使用免费公开渠道。
3. APP 内不展示完整新闻正文和研报全文。
4. 移动端采用 Flutter，保证 iOS 和 Android 体验统一。
5. 后端采用 Spring Boot，数据层首版只用 MySQL。
6. 语音播报首版使用客户端系统 TTS。
7. 后台必须保留资讯源管理、文章管理、简报管理和推送管理。
8. 首版重点打磨首页、分类订阅、每日简报和语音播报四个核心体验。

首版核心闭环：

```text
免费公开资讯源
↓
采集入库
↓
去重分类
↓
AI 摘要
↓
生成每日简报
↓
Flutter APP 展示
↓
用户订阅和语音播报
```
