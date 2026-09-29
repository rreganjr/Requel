import { TestBed } from '@angular/core/testing';
import { SimpleChange } from '@angular/core';
import { ProvenanceService } from '../core/provenance.service';
import { EntitySourceLinkDto } from '../models/provenance';
import { SourcesSectionComponent } from './sources-section';

const LINKS: EntitySourceLinkDto[] = [
  { id: 1, relation: 'DERIVED_FROM', entityType: 'Goal', entityId: 3, entityName: 'Rooms end',
    fragment: 'AC-4', ingestedAt: '2026-09-29T12:00:00Z', notInLatestSource: false,
    editedSinceIngest: false,
    source: { id: 10, system: 'jira', externalId: 'CON-3685', locatorType: 'URL',
      locator: 'https://tracker.example.com/browse/CON-3685', title: null, contentHash: 'v2',
      lastIngestedAt: null } },
  { id: 2, relation: 'DERIVED_FROM', entityType: 'Goal', entityId: 3, entityName: 'Rooms end',
    fragment: 'p.12', ingestedAt: null, notInLatestSource: true, editedSinceIngest: true,
    source: { id: 11, system: 'doc', externalId: 'guide', locatorType: 'PATH',
      locator: 'docs/Roundtable-Production-Guide.pdf', title: null, contentHash: null,
      lastIngestedAt: null } },
];
const flush = () => new Promise(r => setTimeout(r, 0));

describe('SourcesSectionComponent (#272)', () => {
  async function render(result: Promise<EntitySourceLinkDto[]>) {
    const getEntitySources = vi.fn().mockReturnValue(result);
    TestBed.configureTestingModule({
      imports: [SourcesSectionComponent],
      providers: [{ provide: ProvenanceService, useValue: { getEntitySources } }],
    });
    const fixture = TestBed.createComponent(SourcesSectionComponent);
    const comp = fixture.componentInstance;
    comp.projectName = 'Proj A';
    comp.entityType = 'Goal';
    comp.entityId = 3;
    comp.ngOnChanges({ entityId: new SimpleChange(null, 3, true) });
    await flush();
    fixture.detectChanges();
    return { fixture, comp, getEntitySources, el: fixture.nativeElement as HTMLElement };
  }

  it('asks for the entity\'s sources', async () => {
    const { getEntitySources } = await render(Promise.resolve(LINKS));
    expect(getEntitySources).toHaveBeenCalledWith('Proj A', 'Goal', 3);
  });

  it('is hidden when the entity has no sources', async () => {
    const { el } = await render(Promise.resolve([]));
    expect(el.querySelector('[data-testid="sources-section"]')).toBeNull();
  });

  it('renders one row per link with its fragment', async () => {
    const { el } = await render(Promise.resolve(LINKS));
    expect(el.querySelectorAll('[data-testid="source-row"]').length).toBe(2);
    const fragments = Array.from(el.querySelectorAll('[data-testid="source-fragment"]'))
      .map(e => e.textContent?.trim());
    expect(fragments).toEqual(['AC-4', 'p.12']);
  });

  it('opens a URL in a new tab and never links a path', async () => {
    const { el } = await render(Promise.resolve(LINKS));
    const links = el.querySelectorAll('a[data-testid="source-link"]');
    expect(links.length).toBe(1);
    const a = links[0] as HTMLAnchorElement;
    expect(a.getAttribute('href')).toBe('https://tracker.example.com/browse/CON-3685');
    expect(a.getAttribute('target')).toBe('_blank');
    expect(a.getAttribute('rel')).toBe('noopener noreferrer');
    expect(el.querySelector('[data-testid="source-path"]')?.textContent)
      .toContain('docs/Roundtable-Production-Guide.pdf');
    expect(el.querySelector('[data-testid="source-id"]')?.textContent?.trim()).toBe('guide');
  });

  it('flags a fragment missing from the latest source', async () => {
    const { el } = await render(Promise.resolve(LINKS));
    expect(el.querySelectorAll('[data-testid="source-stale"]').length).toBe(1);
  });

  it('says so when the sources cannot be loaded', async () => {
    const { el } = await render(Promise.reject(new Error('boom')));
    expect(el.querySelector('[data-testid="sources-section"]')).toBeNull();
    expect(el.querySelector('[data-testid="sources-error"]')?.textContent)
      .toContain('could not be loaded');
  });
});
