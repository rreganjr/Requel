import { TestBed } from '@angular/core/testing';
import { ProvenanceService } from '../../core/provenance.service';
import { ProjectSourcesDto } from '../../models/provenance';
import { ProjectReferencesComponent } from './project-references';

const SOURCES: ProjectSourcesDto = {
  sources: [
    { source: { id: 1, system: 'doc', externalId: 'docs/guide.pdf', locatorType: 'PATH',
        locator: 'docs/guide.pdf', title: 'Production Guide', contentHash: null,
        lastIngestedAt: null, kind: 'guide', note: 'Operator procedures' },
      derivedCount: 4, citedByCount: 0,
      defersTo: [{ system: 'doc', externalId: 'RUNBOOK.md', title: 'Runbook' }], outranks: [] },
    { source: { id: 2, system: 'doc', externalId: 'RUNBOOK.md', locatorType: 'URL',
        locator: 'https://github.example.com/roundtable/RUNBOOK.md', title: 'Runbook',
        contentHash: null, lastIngestedAt: null, kind: 'runbook', note: null },
      derivedCount: 0, citedByCount: 2, defersTo: [],
      outranks: [{ system: 'doc', externalId: 'docs/guide.pdf', title: 'Production Guide' }] },
    { source: { id: 3, system: 'doc', externalId: 'zoom-matrix', locatorType: null, locator: null,
        title: null, contentHash: null, lastIngestedAt: null, kind: null, note: null },
      derivedCount: 0, citedByCount: 0, defersTo: [], outranks: [] },
  ],
  authority: [{ subordinate: { system: 'doc', externalId: 'docs/guide.pdf', title: 'Production Guide' },
    superior: { system: 'doc', externalId: 'RUNBOOK.md', title: 'Runbook' }, note: null }],
};
const flush = () => new Promise(r => setTimeout(r, 0));

describe('ProjectReferencesComponent (#273)', () => {
  async function render(result: Promise<ProjectSourcesDto>) {
    const getProjectSources = vi.fn().mockReturnValue(result);
    TestBed.configureTestingModule({
      imports: [ProjectReferencesComponent],
      providers: [{ provide: ProvenanceService, useValue: { getProjectSources } }],
    });
    const fixture = TestBed.createComponent(ProjectReferencesComponent);
    fixture.componentRef.setInput('projectName', 'Acme');
    fixture.componentInstance.ngOnChanges();
    await flush();
    fixture.detectChanges();
    return { getProjectSources, el: fixture.nativeElement as HTMLElement };
  }

  it('asks for the project\'s sources', async () => {
    const { getProjectSources } = await render(Promise.resolve(SOURCES));
    expect(getProjectSources).toHaveBeenCalledWith('Acme');
  });

  it('is hidden when the project has no sources', async () => {
    const { el } = await render(Promise.resolve({ sources: [], authority: [] }));
    expect(el.querySelector('[data-testid="workspace-references"]')).toBeNull();
  });

  it('lists every source by title, falling back to its external id', async () => {
    const { el } = await render(Promise.resolve(SOURCES));
    const titles = Array.from(el.querySelectorAll('[data-testid="reference-title"]'))
      .map(e => e.textContent?.trim());
    expect(titles).toEqual(['Production Guide', 'Runbook', 'zoom-matrix']);
    expect(el.querySelector('[data-testid="reference-note"]')?.textContent).toContain('Operator procedures');
  });

  it('opens a URL in a new tab and shows a path as text', async () => {
    const { el } = await render(Promise.resolve(SOURCES));
    const links = el.querySelectorAll('a[data-testid="reference-link"]');
    expect(links.length).toBe(1);
    const a = links[0] as HTMLAnchorElement;
    expect(a.getAttribute('href')).toBe('https://github.example.com/roundtable/RUNBOOK.md');
    expect(a.getAttribute('target')).toBe('_blank');
    expect(a.getAttribute('rel')).toBe('noopener noreferrer');
    expect(el.querySelector('[data-testid="reference-path"]')?.textContent).toContain('docs/guide.pdf');
  });

  it('shows precedence both ways and the link counts', async () => {
    const { el } = await render(Promise.resolve(SOURCES));
    expect(el.querySelector('[data-testid="reference-defers-to"]')?.textContent).toContain('Defers to Runbook');
    expect(el.querySelector('[data-testid="reference-outranks"]')?.textContent).toContain('Outranks Production Guide');
    const counts = Array.from(el.querySelectorAll('[data-testid="reference-counts"]'))
      .map(e => e.textContent?.trim());
    expect(counts).toEqual(['4 derived from it', 'cited by 2', 'Not yet cited']);
  });

  it('says so when the references cannot be loaded', async () => {
    const { el } = await render(Promise.reject(new Error('boom')));
    expect(el.querySelector('[data-testid="workspace-references"]')).toBeNull();
    expect(el.querySelector('[data-testid="references-load-error"]')).not.toBeNull();
  });
});
