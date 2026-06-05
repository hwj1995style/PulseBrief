import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from './App';

describe('App', () => {
  it('renders onboarding with default subscription count', () => {
    render(<App />);

    expect(screen.getByText('选择你关注的内容')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /已选 3 个/ })).toBeEnabled();
  });

  it('renders the admin switch entry', () => {
    render(<App />);

    expect(screen.getByRole('button', { name: '后台' })).toBeInTheDocument();
  });
});

