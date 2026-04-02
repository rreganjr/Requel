export interface ReportGeneratorDto {
  id: number;
  version: number;
  name: string;
  /** XSLT stylesheet — included in detail view, null in list view */
  text: string | null;
  createdBy: string | null;
}
