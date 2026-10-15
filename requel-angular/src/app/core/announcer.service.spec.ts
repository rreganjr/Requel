import { TestBed } from '@angular/core/testing';
import { LiveAnnouncer } from '@angular/cdk/a11y';
import { AnnouncerService } from './announcer.service';

describe('AnnouncerService (issue #140)', () => {
  let announce: ReturnType<typeof vi.fn>;
  let svc: AnnouncerService;

  beforeEach(() => {
    announce = vi.fn().mockResolvedValue(undefined);
    TestBed.configureTestingModule({
      providers: [AnnouncerService, { provide: LiveAnnouncer, useValue: { announce } }]
    });
    svc = TestBed.inject(AnnouncerService);
  });

  it('announce delegates to LiveAnnouncer with polite politeness', () => {
    svc.announce('Hello');
    expect(announce).toHaveBeenCalledWith('Hello', 'polite');
  });

  it('announceThrottled announces immediately, then coalesces a burst into one more', () => {
    vi.useFakeTimers();
    svc.announceThrottled('goal:1', 'first', 1000);
    expect(announce).toHaveBeenCalledTimes(1);
    expect(announce).toHaveBeenLastCalledWith('first', 'polite');

    svc.announceThrottled('goal:1', 'second', 1000);
    svc.announceThrottled('goal:1', 'third', 1000);
    expect(announce).toHaveBeenCalledTimes(1); // still within the window

    vi.advanceTimersByTime(1000);
    expect(announce).toHaveBeenCalledTimes(2);
    expect(announce).toHaveBeenLastCalledWith('third', 'polite');
    vi.useRealTimers();
  });

  it('a single throttled call announces once and nothing extra fires at window end', () => {
    vi.useFakeTimers();
    svc.announceThrottled('k', 'only', 1000);
    vi.advanceTimersByTime(1000);
    expect(announce).toHaveBeenCalledTimes(1);
    vi.useRealTimers();
  });

  it('different keys announce independently', () => {
    vi.useFakeTimers();
    svc.announceThrottled('a', 'A', 1000);
    svc.announceThrottled('b', 'B', 1000);
    expect(announce).toHaveBeenCalledTimes(2);
    vi.useRealTimers();
  });
});
