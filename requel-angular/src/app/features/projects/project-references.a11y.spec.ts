import { render } from '@testing-library/angular';
import { ProvenanceService } from '../../core/provenance.service';
import { ProjectSourcesDto } from '../../models/provenance';
import { ProjectReferencesComponent } from './project-references';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

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
  ],
  authority: [],
};

describe('ProjectReferencesComponent — accessibility (#273)', () => {
  it('has no axe violations with a link, a path and precedence', async () => {
    const { fixture } = await render(ProjectReferencesComponent, {
      providers: [{ provide: ProvenanceService,
        useValue: { getProjectSources: vi.fn().mockResolvedValue(SOURCES) } }],
      inputs: { projectName: 'Acme' },
    });
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="workspace-references"]')).not.toBeNull();
    await expectNoAxeViolations(el);
  });
});
