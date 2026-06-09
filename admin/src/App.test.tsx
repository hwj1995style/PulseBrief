import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';

describe('PulseBrief Admin shell', () => {
  it('renders the candidate review workspace', async () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: '候选资讯审核' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: '后台导航' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '待审核' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByText('高盛：AI 基建投资仍将持续')).toBeInTheDocument();

    const detail = screen.getByRole('complementary', { name: '候选详情' });
    expect(within(detail).getByText('AI 摘要草稿')).toBeInTheDocument();
    expect(within(detail).getByRole('button', { name: '发布为文章' })).toBeEnabled();
    expect(within(detail).getByRole('button', { name: '拒绝候选' })).toBeEnabled();
  });

  it('updates local candidate status from review actions', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: '发布为文章' }));
    expect(screen.getByText('已发布')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '已发布' }));
    expect(screen.getByText('高盛：AI 基建投资仍将持续')).toBeInTheDocument();
  });
});
