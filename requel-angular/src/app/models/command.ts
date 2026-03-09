export interface CommandResult<T = unknown> {
  success: boolean;
  commandType: string;
  message: string | null;
  data: T | null;
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
