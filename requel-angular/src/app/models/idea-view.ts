/** What `POST /api/ideas/new-project` returns. */
export interface StartedIdea {
  projectName: string;
  ideaId: string;
}

export type IdeaStatus = 'PENDING' | 'GENERATING' | 'DRAFTED' | 'FAILED' | 'SKIPPED';

export type DraftKind = 'GOAL' | 'ACTOR' | 'STORY' | 'USE_CASE' | 'GLOSSARY_TERM';

export interface DraftReference {
  kind: DraftKind;
  name: string;
  existing: boolean;
}

/** One proposed requirement. Its position in `IdeaView.drafts` is what `accept` takes. */
export interface IdeaDraft {
  kind: DraftKind;
  name: string;
  text: string;
  note: string | null;
  assumption: boolean;
  evidence: string[];
  evidenceUnverified: boolean;
  confidence: number | null;
  references: DraftReference[];
}

export interface IdeaProblem {
  subject: string;
  dropped: boolean;
  message: string;
}

/** An idea and, once generated, its drafts (`GET /api/projects/{name}/ideas/{id}`). */
export interface IdeaView {
  id: string;
  projectId: number;
  ideaText: string;
  status: IdeaStatus;
  errorSummary: string | null;
  summary: string | null;
  attempts: number;
  retryable: boolean;
  messages: string[];
  problems: IdeaProblem[];
  drafts: IdeaDraft[];
}

export interface AcceptedIdea {
  projectName: string;
  created: Record<string, number>;
  notes: string[];
}
