import { TestBed } from '@angular/core/testing';
import { dirtyCheckGuard, DirtyCheckable } from './dirty-check.guard';

const cleanComponent: DirtyCheckable = { hasUnsavedChanges: () => false };
const dirtyComponent: DirtyCheckable = { hasUnsavedChanges: () => true };

describe('dirtyCheckGuard', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('returns true immediately when the component has no unsaved changes', () => {
    const result = TestBed.runInInjectionContext(() =>
      dirtyCheckGuard(cleanComponent, {} as any, {} as any, {} as any)
    );
    expect(result).toBe(true);
  });

  it('returns the confirm() result (true) when the user acknowledges leaving', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const result = TestBed.runInInjectionContext(() =>
      dirtyCheckGuard(dirtyComponent, {} as any, {} as any, {} as any)
    );
    expect(result).toBe(true);
    expect(window.confirm).toHaveBeenCalled();
    vi.restoreAllMocks();
  });

  it('returns false when the user cancels the confirm dialog', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const result = TestBed.runInInjectionContext(() =>
      dirtyCheckGuard(dirtyComponent, {} as any, {} as any, {} as any)
    );
    expect(result).toBe(false);
    vi.restoreAllMocks();
  });
});
