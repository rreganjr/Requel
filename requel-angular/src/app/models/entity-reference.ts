/**
 * Lightweight cross-entity pointer reused wherever entities reference
 * other entities — goals, actors, stories, containers, referers, etc.
 */
export interface EntityReferenceDto {
  entityType: string;
  id: number | null;
  name: string;
  typeName?: string;
}
