export type StreamConnectionState = 'idle' | 'connecting' | 'open' | 'closed' | 'error';

export type StreamEventType = 'Session' | 'Data' | 'TargetDeleted' | 'SESSION_EXPIRED';

export interface StreamEventEnvelope {
  eventType: StreamEventType;
  targetType?: string;
  targetId?: number;
  payload: unknown;
}
