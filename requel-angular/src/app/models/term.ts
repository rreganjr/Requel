import { EntityReferenceDto } from './entity-reference';

export interface GlossaryTermDto {
  id: number;
  version: number;
  name: string;
  text: string | null;
  createdBy: string | null;
  canonicalTermId: number | null;
  canonicalTermName: string | null;
  alternateTerms: EntityReferenceDto[] | null;
  referers: EntityReferenceDto[] | null;
}
