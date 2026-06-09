import type { CandidateStatus } from '../../shared/types/candidate';

export function candidateStatusText(status: CandidateStatus): string {
  switch (status) {
    case 'PENDING_REVIEW':
      return '待审核';
    case 'PUBLISHED':
      return '已发布';
    case 'REJECTED':
      return '已拒绝';
  }
}

export function candidateStatusTone(status: CandidateStatus): string {
  switch (status) {
    case 'PENDING_REVIEW':
      return 'warning';
    case 'PUBLISHED':
      return 'success';
    case 'REJECTED':
      return 'danger';
  }
}
