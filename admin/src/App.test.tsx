import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';
import { resetAdminApiMock } from './shared/api/adminApi';

describe('PulseBrief Admin shell', () => {
  beforeEach(() => {
    resetAdminApiMock();
    window.location.hash = '';
  });

  it('renders the candidate review workspace', async () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: '候选资讯审核' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: '后台导航' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '待审核' })).toHaveAttribute('aria-pressed', 'true');
    expect(await screen.findByText('高盛：AI 基建投资仍将持续')).toBeInTheDocument();

    const detail = screen.getByRole('complementary', { name: '候选详情' });
    expect(within(detail).getByText('AI 摘要草稿')).toBeInTheDocument();
    expect(within(detail).getByRole('button', { name: '发布为文章' })).toBeEnabled();
    expect(within(detail).getByRole('button', { name: '拒绝候选' })).toBeEnabled();
  });

  it('updates local candidate status from review actions', async () => {
    const user = userEvent.setup();
    render(<App />);

    expect(await screen.findByText('高盛：AI 基建投资仍将持续')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '发布为文章' }));
    expect(screen.getByText('已发布')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '已发布' }));
    expect(screen.getByText('高盛：AI 基建投资仍将持续')).toBeInTheDocument();
  });

  it('edits candidate review fields before publishing', async () => {
    const user = userEvent.setup();
    render(<App />);

    expect(await screen.findByText('高盛：AI 基建投资仍将持续')).toBeInTheDocument();

    await user.clear(screen.getByLabelText('候选标题'));
    await user.type(screen.getByLabelText('候选标题'), '运营修订后的 AI 基建标题');
    await user.clear(screen.getByLabelText('候选摘要'));
    await user.type(screen.getByLabelText('候选摘要'), '运营修订后的摘要内容');
    await user.selectOptions(screen.getByLabelText('候选分类'), 'ai');
    await user.clear(screen.getByLabelText('候选来源'));
    await user.type(screen.getByLabelText('候选来源'), 'Updated Source');
    await user.clear(screen.getByLabelText('候选标签'));
    await user.type(screen.getByLabelText('候选标签'), 'AI 基建，算力，AI 基建');
    await user.click(screen.getByRole('button', { name: '保存候选内容' }));

    expect(await screen.findByText('候选内容已保存')).toBeInTheDocument();
    const detail = screen.getByRole('complementary', { name: '候选详情' });
    expect(within(detail).getByRole('heading', { name: '运营修订后的 AI 基建标题 · 详情' })).toBeInTheDocument();
    expect(within(detail).getByText(/Updated Source · AI 前沿/)).toBeInTheDocument();
    expect(within(detail).getAllByText('运营修订后的摘要内容')).toHaveLength(2);
    expect(within(detail).getByText('AI 基建')).toBeInTheDocument();
    expect(within(detail).getByText('算力')).toBeInTheDocument();
  });

  it('creates and publishes a daily digest from selected articles', async () => {
    const user = userEvent.setup();
    window.location.hash = '#/digests';
    render(<App />);

    expect(await screen.findByRole('heading', { name: '简报管理' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /选择 高盛：AI 基建投资仍将持续/ }));
    expect(screen.getByText('已选 1 条')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '创建草稿' }));
    expect(await screen.findByText('草稿已创建，可发布到 APP')).toBeInTheDocument();

    const detail = screen.getByRole('complementary', { name: '简报详情' });
    expect(within(detail).getByText('今日全球早报')).toBeInTheDocument();

    await user.click(within(detail).getByRole('button', { name: '发布简报' }));
    expect(await screen.findByText('已发布到 APP')).toBeInTheDocument();
  });

  it('edits draft digest and offlines published digest', async () => {
    const user = userEvent.setup();
    window.location.hash = '#/digests';
    render(<App />);

    expect(await screen.findByRole('heading', { name: '简报管理' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /选择 高盛：AI 基建投资仍将持续/ }));
    await user.click(screen.getByRole('button', { name: '创建草稿' }));
    expect(await screen.findByText('草稿已创建，可发布到 APP')).toBeInTheDocument();

    await user.clear(screen.getByLabelText('简报标题'));
    await user.type(screen.getByLabelText('简报标题'), '更新后的每日早报');
    await user.clear(screen.getByLabelText('简报摘要'));
    await user.type(screen.getByLabelText('简报摘要'), '运营更新后的摘要');
    await user.click(screen.getByRole('button', { name: '保存草稿' }));
    expect(await screen.findByText('草稿已保存')).toBeInTheDocument();

    const detail = screen.getByRole('complementary', { name: '简报详情' });
    expect(within(detail).getByText('更新后的每日早报')).toBeInTheDocument();
    expect(within(detail).getByText('运营更新后的摘要')).toBeInTheDocument();

    await user.click(within(detail).getByRole('button', { name: '发布简报' }));
    expect(await screen.findByText('已发布到 APP')).toBeInTheDocument();

    await user.click(within(detail).getByRole('button', { name: '下线简报' }));
    expect(await screen.findByText('已下线，用户端不可见')).toBeInTheDocument();
    expect(within(detail).getByText('已下线')).toBeInTheDocument();
  });

  it('renders ingestion monitor metrics and failure logs', async () => {
    window.location.hash = '#/ingestion';
    render(<App />);

    expect(await screen.findByRole('heading', { name: '采集任务' })).toBeInTheDocument();
    expect(screen.getByText('今日采集')).toBeInTheDocument();
    expect(screen.getAllByText('失败任务').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Provider timeout')).toBeInTheDocument();
    expect(screen.getByText('Fixture Global')).toBeInTheDocument();
    expect(screen.getByText('SUMMARY_ONLY · 最新 24 小时')).toBeInTheDocument();
  });
});
