import { mockCandidates } from '../../mock/candidates';
import type { AdminCandidate, CandidateStatus } from '../types/candidate';

const apiBaseUrl = import.meta.env.VITE_ADMIN_API_BASE_URL;
const adminToken = import.meta.env.VITE_ADMIN_TOKEN ?? 'dev-admin-token';

let localCandidates = mockCandidates.map((candidate) => ({ ...candidate }));

export const adminApiConfig = {
  apiBaseUrl,
  adminToken,
  mode: apiBaseUrl ? 'api' : 'mock'
};

export function getInitialCandidates(): AdminCandidate[] {
  return localCandidates;
}

export async function listCandidates(status?: CandidateStatus | 'ALL'): Promise<AdminCandidate[]> {
  if (!status || status === 'ALL') {
    return localCandidates;
  }
  return localCandidates.filter((candidate) => candidate.status === status);
}

export async function updateCandidateStatus(id: number, status: CandidateStatus): Promise<AdminCandidate> {
  localCandidates = localCandidates.map((candidate) =>
    candidate.id === id ? { ...candidate, status } : candidate
  );
  const updated = localCandidates.find((candidate) => candidate.id === id);
  if (!updated) {
    throw new Error('Candidate not found');
  }
  return updated;
}
