export interface ArgumentDto {
  id: number;
  version: number;
  text: string;
  supportLevel: string;
  createdBy: string | null;
}

export interface PositionDto {
  id: number;
  version: number;
  text: string;
  createdBy: string | null;
  positionType: string;
  arguments: ArgumentDto[];
}

export interface NoteDto {
  id: number;
  version: number;
  text: string;
  createdBy: string | null;
}

export interface IssueDto {
  id: number;
  version: number;
  text: string;
  mustBeResolved: boolean;
  resolved: boolean;
  resolvedBy: string | null;
  resolvedByPosition: string | null;
  createdBy: string | null;
  positions: PositionDto[];
}

export interface AnnotationsDto {
  notes: NoteDto[];
  issues: IssueDto[];
}

export const SUPPORT_LEVEL_OPTIONS = [
  { label: 'Strongly For', value: 'StronglyFor' },
  { label: 'For', value: 'For' },
  { label: 'Neutral', value: 'Neutral' },
  { label: 'Against', value: 'Against' },
  { label: 'Strongly Against', value: 'StronglyAgainst' },
];
