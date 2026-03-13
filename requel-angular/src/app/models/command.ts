export interface CommandResult<T = unknown> {
  success: boolean;
  entityType: string | null;
  entity: T | null;
  error: string | null;
  violations: FieldViolation[] | null;
}

export interface FieldViolation {
  field: string | null;
  message: string;
}

export interface ErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}
