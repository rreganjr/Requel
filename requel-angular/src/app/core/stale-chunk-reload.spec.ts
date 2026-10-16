import { NavigationError } from '@angular/router';
import {
  isStaleChunkError,
  reloadOnStaleChunk,
  ReloadDeps,
  STALE_CHUNK_RELOAD_KEY,
  STALE_CHUNK_RETRY_WINDOW_MS,
} from './stale-chunk-reload';

/** Issue #400. */
describe('reloadOnStaleChunk', () => {
  let store: Map<string, string>;
  let assigned: string[];
  let clock: number;
  let deps: ReloadDeps;

  beforeEach(() => {
    store = new Map();
    assigned = [];
    clock = 1_000_000;
    deps = {
      assign: url => assigned.push(url),
      storage: {
        getItem: key => store.get(key) ?? null,
        setItem: (key, value) => void store.set(key, value),
      },
      now: () => clock,
    };
  });

  const failure = (url: string, message: string) =>
    new NavigationError(1, url, new TypeError(message));
  const chromeStale = (url: string) =>
    failure(url, 'Failed to fetch dynamically imported module: http://localhost:8080/chunk-DLSXGK3B.js');

  it('recognises the Chrome, Safari and Firefox messages', () => {
    expect(isStaleChunkError(new TypeError('Failed to fetch dynamically imported module: x.js'))).toBe(true);
    expect(isStaleChunkError(new TypeError('Importing a module script failed.'))).toBe(true);
    expect(isStaleChunkError(new TypeError('error loading dynamically imported module: x.js'))).toBe(true);
    expect(isStaleChunkError(new Error('Http failure response: 500'))).toBe(false);
    expect(isStaleChunkError(undefined)).toBe(false);
  });

  it('reloads at the URL being opened', () => {
    expect(reloadOnStaleChunk(chromeStale('/projects/Demo/stakeholders/2'), deps)).toBe(true);
    expect(assigned).toEqual(['/projects/Demo/stakeholders/2']);
    expect(store.has(STALE_CHUNK_RELOAD_KEY)).toBe(true);
  });

  it('reloads only once for the same URL, so a really missing chunk shows the error', () => {
    reloadOnStaleChunk(chromeStale('/projects/Demo'), deps);
    clock += 1_000;
    expect(reloadOnStaleChunk(chromeStale('/projects/Demo'), deps)).toBe(false);
    expect(assigned).toEqual(['/projects/Demo']);
  });

  it('reloads again for another URL, or for the same one after the window', () => {
    reloadOnStaleChunk(chromeStale('/projects/Demo'), deps);
    expect(reloadOnStaleChunk(chromeStale('/users'), deps)).toBe(true);
    clock += STALE_CHUNK_RETRY_WINDOW_MS + 1;
    expect(reloadOnStaleChunk(chromeStale('/users'), deps)).toBe(true);
    expect(assigned).toEqual(['/projects/Demo', '/users', '/users']);
  });

  it('leaves other navigation errors alone', () => {
    expect(reloadOnStaleChunk(failure('/projects', 'Http failure response: 500'), deps)).toBe(false);
    expect(assigned).toEqual([]);
  });

  it('does not reload when it cannot record the attempt', () => {
    expect(reloadOnStaleChunk(chromeStale('/projects'), { ...deps, storage: null })).toBe(false);
    const throwing = { ...deps, storage: { getItem: () => null, setItem: () => { throw new Error('quota'); } } };
    expect(reloadOnStaleChunk(chromeStale('/projects'), throwing)).toBe(false);
    expect(assigned).toEqual([]);
  });
});
