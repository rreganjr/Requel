export interface Idea {
  id?: number;
  text?: string;
  fileContent?: string;
  owner?: string;
  status?: 'SUBMITTED' | 'IN_PROGRESS' | 'ANALYZED';
  openQuestions?: ClarificationQuestion[];
  projectId?: number;
}

export interface ClarificationQuestion {
  id?: number;
  questionText: string;
  answer?: string;
  answered?: boolean;
  createdDate?: string;
}
