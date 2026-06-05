import { useEffect, useMemo, useState } from 'react';
import {
  Bell,
  Bookmark,
  BookmarkCheck,
  BriefcaseBusiness,
  ChevronLeft,
  CircleUserRound,
  ExternalLink,
  Headphones,
  Home,
  ListFilter,
  Newspaper,
  Pause,
  Play,
  Radio,
  Search,
  Settings,
  ShieldCheck,
  Sparkles,
  UserRoundCog
} from 'lucide-react';
import { adminMetrics, articles, categories, defaultProfile, digests } from './data';
import {
  articleToPlayerItem,
  canCompleteSubscriptions,
  digestToPlayerItem,
  filterArticlesByCategory,
  relativeTime,
  sortHomeArticles,
  toggleFavorite
} from './logic';
import type { AppMode, Article, Digest, PlayerItem, TabKey, UserProfile } from './types';

const tabItems: Array<{ key: TabKey; label: string; icon: typeof Home }> = [
  { key: 'home', label: '首页', icon: Home },
  { key: 'categories', label: '分类', icon: ListFilter },
  { key: 'digests', label: '简报', icon: Newspaper },
  { key: 'profile', label: '我的', icon: CircleUserRound }
];

export function App() {
  const [mode, setMode] = useState<AppMode>('user');
  const [hasEntered, setHasEntered] = useState(false);
  const [activeTab, setActiveTab] = useState<TabKey>('home');
  const [activeCategory, setActiveCategory] = useState('all');
  const [profile, setProfile] = useState<UserProfile>(defaultProfile);
  const [favoriteIds, setFavoriteIds] = useState<number[]>([]);
  const [selectedArticle, setSelectedArticle] = useState<Article | null>(null);
  const [playerItem, setPlayerItem] = useState<PlayerItem | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);

  const sortedArticles = useMemo(
    () => sortHomeArticles(articles, profile.selectedCategories),
    [profile.selectedCategories]
  );
  const visibleArticles = useMemo(
    () => filterArticlesByCategory(sortedArticles, activeCategory),
    [activeCategory, sortedArticles]
  );
  const favoriteArticles = articles.filter((article) => favoriteIds.includes(article.id));

  useEffect(() => {
    const scrollContainer = document.querySelector<HTMLElement>('.screen-content, .admin-screen, .detail-screen, .onboarding');
    if (scrollContainer && typeof scrollContainer.scrollTo === 'function') {
      scrollContainer.scrollTo({ top: 0 });
    } else if (scrollContainer) {
      scrollContainer.scrollTop = 0;
    }

    if (navigator.userAgent.includes('jsdom')) {
      return;
    }

    try {
      window.scrollTo({ top: 0 });
    } catch {
      // jsdom exposes scrollTo but does not implement it.
    }
  }, [activeTab, hasEntered, mode, selectedArticle]);

  function updateSubscription(categoryCode: string) {
    setProfile((current) => {
      const selectedCategories = current.selectedCategories.includes(categoryCode)
        ? current.selectedCategories.filter((code) => code !== categoryCode)
        : [...current.selectedCategories, categoryCode];

      return { ...current, selectedCategories };
    });
  }

  function playArticle(article: Article) {
    setPlayerItem(articleToPlayerItem(article));
    setIsPlaying(true);
  }

  function playDigest(digest: Digest) {
    setPlayerItem(digestToPlayerItem(digest));
    setIsPlaying(true);
  }

  function toggleArticleFavorite(articleId: number) {
    setFavoriteIds((current) => toggleFavorite(current, articleId));
  }

  if (!hasEntered) {
    return (
      <AppFrame mode={mode} onModeChange={setMode}>
        <Onboarding
          profile={profile}
          onToggle={updateSubscription}
          onEnter={() => setHasEntered(true)}
        />
      </AppFrame>
    );
  }

  return (
    <AppFrame mode={mode} onModeChange={setMode}>
      {mode === 'admin' ? (
        <AdminConsole />
      ) : selectedArticle ? (
        <>
          <ArticleDetail
            article={selectedArticle}
            isFavorite={favoriteIds.includes(selectedArticle.id)}
            onBack={() => setSelectedArticle(null)}
            onFavorite={() => toggleArticleFavorite(selectedArticle.id)}
            onPlay={() => playArticle(selectedArticle)}
          />
          {playerItem && (
            <MiniPlayer item={playerItem} isPlaying={isPlaying} onToggle={() => setIsPlaying((value) => !value)} />
          )}
        </>
      ) : (
        <>
          <main className="screen-content">
            {activeTab === 'home' && (
              <HomeScreen
                articles={sortedArticles}
                favoriteIds={favoriteIds}
                selectedCategories={profile.selectedCategories}
                onOpenArticle={setSelectedArticle}
                onFavorite={toggleArticleFavorite}
                onPlay={playArticle}
                onPlayDigest={() => playDigest(digests[0])}
              />
            )}
            {activeTab === 'categories' && (
              <CategoryScreen
                activeCategory={activeCategory}
                articles={visibleArticles}
                favoriteIds={favoriteIds}
                onCategoryChange={setActiveCategory}
                onOpenArticle={setSelectedArticle}
                onFavorite={toggleArticleFavorite}
                onPlay={playArticle}
              />
            )}
            {activeTab === 'digests' && <DigestScreen onPlay={playDigest} />}
            {activeTab === 'profile' && (
              <ProfileScreen
                profile={profile}
                favoriteArticles={favoriteArticles}
                onToggleSubscription={updateSubscription}
                onToggleAutoPlay={() =>
                  setProfile((current) => ({ ...current, autoPlayNext: !current.autoPlayNext }))
                }
                onToggleMorningPush={() =>
                  setProfile((current) => ({ ...current, morningPush: !current.morningPush }))
                }
                onToggleEveningPush={() =>
                  setProfile((current) => ({ ...current, eveningPush: !current.eveningPush }))
                }
              />
            )}
          </main>
          {playerItem && (
            <MiniPlayer item={playerItem} isPlaying={isPlaying} onToggle={() => setIsPlaying((value) => !value)} />
          )}
          <BottomNav activeTab={activeTab} onTabChange={setActiveTab} />
        </>
      )}
    </AppFrame>
  );
}

function AppFrame({
  children,
  mode,
  onModeChange
}: {
  children: React.ReactNode;
  mode: AppMode;
  onModeChange: (mode: AppMode) => void;
}) {
  return (
    <div className="page-shell">
      <div className="phone-shell">
        <header className="app-header">
          <div className="brand-lockup">
            <div className="brand-mark">
              <Radio size={18} />
            </div>
            <div>
              <strong>PulseBrief</strong>
              <span>全球热点简报</span>
            </div>
          </div>
          <div className="mode-switch" aria-label="视图切换">
            <button className={mode === 'user' ? 'active' : ''} onClick={() => onModeChange('user')}>
              APP
            </button>
            <button className={mode === 'admin' ? 'active' : ''} onClick={() => onModeChange('admin')}>
              后台
            </button>
          </div>
        </header>
        {children}
      </div>
    </div>
  );
}

function Onboarding({
  profile,
  onToggle,
  onEnter
}: {
  profile: UserProfile;
  onToggle: (categoryCode: string) => void;
  onEnter: () => void;
}) {
  const canEnter = canCompleteSubscriptions(profile.selectedCategories);

  return (
    <main className="onboarding">
      <section className="intro-panel">
        <div className="intro-art" aria-hidden="true">
          <Sparkles size={30} />
          <div className="signal-bars">
            <span />
            <span />
            <span />
          </div>
        </div>
        <h1>选择你关注的内容</h1>
        <p>默认从全球热点、财经市场和科技趋势开始，你也可以加入 AI、宏观和投行公开观点。</p>
      </section>

      <section className="choice-grid" aria-label="兴趣分类">
        {categories.map((category) => {
          const selected = profile.selectedCategories.includes(category.code);

          return (
            <button
              key={category.code}
              className={`choice-card ${selected ? 'selected' : ''}`}
              onClick={() => onToggle(category.code)}
            >
              <span>{category.name}</span>
              <small>{category.description}</small>
            </button>
          );
        })}
      </section>

      <button className="primary-action" disabled={!canEnter} onClick={onEnter}>
        已选 {profile.selectedCategories.length} 个，进入首页
      </button>
      {!canEnter && <p className="helper-text">至少选择 3 个分类后继续。</p>}
    </main>
  );
}

function HomeScreen({
  articles,
  favoriteIds,
  selectedCategories,
  onOpenArticle,
  onFavorite,
  onPlay,
  onPlayDigest
}: {
  articles: Article[];
  favoriteIds: number[];
  selectedCategories: string[];
  onOpenArticle: (article: Article) => void;
  onFavorite: (articleId: number) => void;
  onPlay: (article: Article) => void;
  onPlayDigest: () => void;
}) {
  const topArticles = articles.slice(0, 3);
  const subscribedCategories = categories.filter((category) => selectedCategories.includes(category.code));

  return (
    <>
      <section className="home-hero">
        <div>
          <span className="date-line">2026 年 6 月 5 日</span>
          <h1>早上好，今日全球重点已整理完成</h1>
          <p>利率路径、AI 算力投资和亚洲出口链条是今天最值得追踪的三条线。</p>
        </div>
        <button className="icon-button" aria-label="搜索">
          <Search size={19} />
        </button>
      </section>

      <section className="brief-strip">
        <div>
          <strong>今日全球热点 Top 3</strong>
          <span>{topArticles.map((article) => article.categoryName).join(' / ')}</span>
        </div>
        <button className="play-button" onClick={onPlayDigest}>
          <Play size={16} />
          播放
        </button>
      </section>

      <section className="top-stack">
        {topArticles.map((article, index) => (
          <button key={article.id} className="headline-row" onClick={() => onOpenArticle(article)}>
            <span>{index + 1}</span>
            <strong>{article.title}</strong>
          </button>
        ))}
      </section>

      <section className="tab-rail" aria-label="我的订阅">
        <button className="active">全部</button>
        {subscribedCategories.map((category) => (
          <button key={category.code}>{category.name}</button>
        ))}
      </section>

      <ArticleList
        articles={articles}
        favoriteIds={favoriteIds}
        onOpenArticle={onOpenArticle}
        onFavorite={onFavorite}
        onPlay={onPlay}
      />
    </>
  );
}

function CategoryScreen({
  activeCategory,
  articles: visibleArticles,
  favoriteIds,
  onCategoryChange,
  onOpenArticle,
  onFavorite,
  onPlay
}: {
  activeCategory: string;
  articles: Article[];
  favoriteIds: number[];
  onCategoryChange: (categoryCode: string) => void;
  onOpenArticle: (article: Article) => void;
  onFavorite: (articleId: number) => void;
  onPlay: (article: Article) => void;
}) {
  const currentCategory = categories.find((category) => category.code === activeCategory);

  return (
    <>
      <section className="section-heading">
        <h1>分类</h1>
        <p>{currentCategory?.description ?? '按主题浏览全球热点资讯。'}</p>
      </section>
      <section className="tab-rail wrapped" aria-label="分类导航">
        <button className={activeCategory === 'all' ? 'active' : ''} onClick={() => onCategoryChange('all')}>
          全部
        </button>
        {categories.map((category) => (
          <button
            key={category.code}
            className={activeCategory === category.code ? 'active' : ''}
            onClick={() => onCategoryChange(category.code)}
          >
            {category.name}
          </button>
        ))}
      </section>
      <div className="category-summary">
        <strong>今日重点</strong>
        <span>{visibleArticles.length} 条已发布资讯，按热度和时间排序。</span>
      </div>
      <ArticleList
        articles={visibleArticles}
        favoriteIds={favoriteIds}
        onOpenArticle={onOpenArticle}
        onFavorite={onFavorite}
        onPlay={onPlay}
      />
    </>
  );
}

function DigestScreen({ onPlay }: { onPlay: (digest: Digest) => void }) {
  return (
    <>
      <section className="section-heading">
        <h1>每日简报</h1>
        <p>早报、午间快讯和晚间复盘都可以一键收听。</p>
      </section>
      <div className="digest-stack">
        {digests.map((digest) => (
          <article key={digest.id} className="digest-card">
            <div className="digest-meta">
              <span>{digestLabel(digest.type)}</span>
              <time>{relativeTime(digest.publishTime)}</time>
            </div>
            <h2>{digest.title}</h2>
            <p>{digest.summary}</p>
            <button className="secondary-action" onClick={() => onPlay(digest)}>
              <Headphones size={16} />
              播放简报
            </button>
          </article>
        ))}
      </div>
    </>
  );
}

function ProfileScreen({
  profile,
  favoriteArticles,
  onToggleSubscription,
  onToggleAutoPlay,
  onToggleMorningPush,
  onToggleEveningPush
}: {
  profile: UserProfile;
  favoriteArticles: Article[];
  onToggleSubscription: (categoryCode: string) => void;
  onToggleAutoPlay: () => void;
  onToggleMorningPush: () => void;
  onToggleEveningPush: () => void;
}) {
  return (
    <>
      <section className="profile-card">
        <div className="avatar">PR</div>
        <div>
          <h1>{profile.nickname}</h1>
          <p>已订阅 {profile.selectedCategories.length} 个分类，收藏 {favoriteArticles.length} 条资讯。</p>
        </div>
      </section>

      <section className="settings-panel">
        <h2>我的订阅</h2>
        <div className="subscription-grid">
          {categories.map((category) => (
            <button
              key={category.code}
              className={profile.selectedCategories.includes(category.code) ? 'selected' : ''}
              onClick={() => onToggleSubscription(category.code)}
            >
              {category.name}
            </button>
          ))}
        </div>
      </section>

      <section className="settings-panel">
        <h2>播报设置</h2>
        <SettingRow icon={Headphones} label="播报语言" value={profile.language} />
        <SettingRow icon={Settings} label="播放倍速" value={profile.playbackSpeed} />
        <ToggleRow label="自动播放下一条" checked={profile.autoPlayNext} onChange={onToggleAutoPlay} />
      </section>

      <section className="settings-panel">
        <h2>推送设置</h2>
        <ToggleRow label="每日早报推送" checked={profile.morningPush} onChange={onToggleMorningPush} />
        <ToggleRow label="晚间复盘推送" checked={profile.eveningPush} onChange={onToggleEveningPush} />
        <ToggleRow label="突发热点推送" checked={profile.breakingPush} onChange={() => undefined} />
      </section>
    </>
  );
}

function ArticleDetail({
  article,
  isFavorite,
  onBack,
  onFavorite,
  onPlay
}: {
  article: Article;
  isFavorite: boolean;
  onBack: () => void;
  onFavorite: () => void;
  onPlay: () => void;
}) {
  return (
    <main className="detail-screen">
      <button className="back-button" onClick={onBack}>
        <ChevronLeft size={18} />
        返回
      </button>
      <article className="detail-article">
        <div className="article-labels">
          <span>{article.categoryName}</span>
          {article.isBreaking && <span className="breaking">突发</span>}
        </div>
        <h1>{article.title}</h1>
        <p className="source-line">
          {article.sourceName} / {relativeTime(article.publishTime)}
        </p>
        <section>
          <h2>AI 摘要</h2>
          <p>{article.aiSummary}</p>
        </section>
        <section>
          <h2>核心要点</h2>
          <ul>
            {article.keyPoints.map((point) => (
              <li key={point}>{point}</li>
            ))}
          </ul>
        </section>
        <section>
          <h2>可能影响</h2>
          <p>{article.impactAnalysis}</p>
        </section>
        <div className="detail-actions">
          <button className="secondary-action" onClick={onPlay}>
            <Play size={16} />
            语音播报
          </button>
          <button className="secondary-action" onClick={onFavorite}>
            {isFavorite ? <BookmarkCheck size={16} /> : <Bookmark size={16} />}
            {isFavorite ? '已收藏' : '收藏'}
          </button>
          <a className="primary-link" href={article.originalUrl} target="_blank" rel="noreferrer">
            阅读原文
            <ExternalLink size={15} />
          </a>
        </div>
      </article>
    </main>
  );
}

function ArticleList({
  articles: list,
  favoriteIds,
  onOpenArticle,
  onFavorite,
  onPlay
}: {
  articles: Article[];
  favoriteIds: number[];
  onOpenArticle: (article: Article) => void;
  onFavorite: (articleId: number) => void;
  onPlay: (article: Article) => void;
}) {
  return (
    <section className="article-list" aria-label="资讯列表">
      {list.map((article) => (
        <ArticleCard
          key={article.id}
          article={article}
          isFavorite={favoriteIds.includes(article.id)}
          onOpen={() => onOpenArticle(article)}
          onFavorite={() => onFavorite(article.id)}
          onPlay={() => onPlay(article)}
        />
      ))}
    </section>
  );
}

function ArticleCard({
  article,
  isFavorite,
  onOpen,
  onFavorite,
  onPlay
}: {
  article: Article;
  isFavorite: boolean;
  onOpen: () => void;
  onFavorite: () => void;
  onPlay: () => void;
}) {
  return (
    <article className="article-card">
      <button className="article-main" onClick={onOpen}>
        <div className="article-labels">
          <span>{article.categoryName}</span>
          {article.isBreaking ? <span className="breaking">突发</span> : <span>热度 {article.hotScore}</span>}
        </div>
        <h2>{article.title}</h2>
        <p>{article.summary}</p>
        <small>
          {article.sourceName} / {relativeTime(article.publishTime)}
        </small>
      </button>
      <div className="card-actions">
        <button className="icon-button" aria-label="播放资讯" onClick={onPlay}>
          <Play size={16} />
        </button>
        <button className="icon-button" aria-label="收藏资讯" onClick={onFavorite}>
          {isFavorite ? <BookmarkCheck size={16} /> : <Bookmark size={16} />}
        </button>
      </div>
    </article>
  );
}

function AdminConsole() {
  return (
    <main className="admin-screen">
      <section className="section-heading">
        <h1>后台管理</h1>
        <p>运营数据、资讯源、文章、简报和推送任务的首版管理入口。</p>
      </section>

      <section className="metric-grid">
        {adminMetrics.map((metric) => (
          <article key={metric.label} className="metric-card">
            <span>{metric.label}</span>
            <strong>{metric.value}</strong>
            <small>{metric.trend}</small>
          </article>
        ))}
      </section>

      <AdminPanel
        icon={BriefcaseBusiness}
        title="资讯源管理"
        rows={['Goldman Sachs Insights / IB_PUBLIC / 60 分钟', 'GDELT Event Index / GDELT / 30 分钟', 'Company Blogs / RSS / 60 分钟']}
      />
      <AdminPanel
        icon={Newspaper}
        title="文章管理"
        rows={articles.slice(0, 4).map((article) => `${article.categoryName} / 已发布 / ${article.title}`)}
      />
      <AdminPanel
        icon={Bell}
        title="简报与推送"
        rows={digests.map((digest) => `${digestLabel(digest.type)} / 已发布 / ${digest.title}`)}
      />
      <AdminPanel icon={ShieldCheck} title="合规边界" rows={['不展示原文全文', '保留来源和原文链接', '投行观点仅做短摘要']} />
    </main>
  );
}

function AdminPanel({ icon: Icon, title, rows }: { icon: typeof Home; title: string; rows: string[] }) {
  return (
    <section className="admin-panel">
      <h2>
        <Icon size={18} />
        {title}
      </h2>
      {rows.map((row) => (
        <div key={row} className="admin-row">
          {row}
        </div>
      ))}
    </section>
  );
}

function BottomNav({ activeTab, onTabChange }: { activeTab: TabKey; onTabChange: (tab: TabKey) => void }) {
  return (
    <nav className="bottom-nav" aria-label="底部导航">
      {tabItems.map((item) => {
        const Icon = item.icon;
        return (
          <button key={item.key} className={activeTab === item.key ? 'active' : ''} onClick={() => onTabChange(item.key)}>
            <Icon size={19} />
            <span>{item.label}</span>
          </button>
        );
      })}
    </nav>
  );
}

function MiniPlayer({ item, isPlaying, onToggle }: { item: PlayerItem; isPlaying: boolean; onToggle: () => void }) {
  return (
    <aside className="mini-player" aria-label="迷你播放器">
      <div>
        <strong>{item.title}</strong>
        <span>{item.sourceLabel}</span>
      </div>
      <button className="icon-button filled" onClick={onToggle} aria-label={isPlaying ? '暂停' : '播放'}>
        {isPlaying ? <Pause size={16} /> : <Play size={16} />}
      </button>
    </aside>
  );
}

function SettingRow({ icon: Icon, label, value }: { icon: typeof Home; label: string; value: string }) {
  return (
    <div className="setting-row">
      <Icon size={17} />
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function ToggleRow({ label, checked, onChange }: { label: string; checked: boolean; onChange: () => void }) {
  return (
    <button className="toggle-row" onClick={onChange}>
      <span>{label}</span>
      <span className={`switch ${checked ? 'checked' : ''}`} />
    </button>
  );
}

function digestLabel(type: Digest['type']) {
  const labels: Record<Digest['type'], string> = {
    MORNING: '每日早报',
    NOON: '午间快讯',
    EVENING: '晚间复盘',
    TOPIC: '专题简报'
  };

  return labels[type];
}
