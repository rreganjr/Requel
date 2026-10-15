import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot } from '@angular/router';
import { RequelTitleStrategy } from './requel-title-strategy';

describe('RequelTitleStrategy (#142)', () => {
  let strategy: RequelTitleStrategy;
  let title: Title;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    title = TestBed.inject(Title);
    strategy = TestBed.inject(RequelTitleStrategy);
  });

  it('formats the route title as "<title> · Requel"', () => {
    vi.spyOn(strategy, 'buildTitle').mockReturnValue('Goals');
    strategy.updateTitle({} as RouterStateSnapshot);
    expect(title.getTitle()).toBe('Goals · Requel');
  });

  it('falls back to the bare app name when the route has no title', () => {
    vi.spyOn(strategy, 'buildTitle').mockReturnValue(undefined);
    strategy.updateTitle({} as RouterStateSnapshot);
    expect(title.getTitle()).toBe('Requel');
  });

  it('prefers an entity name resolved onto the deepest route (#154)', () => {
    // buildTitle would return the static type label; the resolved name wins.
    vi.spyOn(strategy, 'buildTitle').mockReturnValue('Goal');
    const snapshot = {
      root: { firstChild: { firstChild: null, data: { entityName: 'Login flow' } } },
    } as unknown as RouterStateSnapshot;
    strategy.updateTitle(snapshot);
    expect(title.getTitle()).toBe('Login flow · Requel');
  });

  it('uses the static title when no entity name was resolved', () => {
    vi.spyOn(strategy, 'buildTitle').mockReturnValue('Goals');
    const snapshot = { root: { firstChild: null, data: {} } } as unknown as RouterStateSnapshot;
    strategy.updateTitle(snapshot);
    expect(title.getTitle()).toBe('Goals · Requel');
  });
});
