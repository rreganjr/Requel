import { render } from '@testing-library/angular';
import { ProvenanceService } from '../core/provenance.service';
import { EntitySourceLinkDto } from '../models/provenance';
import { SourcesSectionComponent } from './sources-section';
import { expectNoAxeViolations } from './testing/a11y';

const LINKS: EntitySourceLinkDto[] = [
  { id: 1, relation: 'DERIVED_FROM', entityType: 'Goal', entityId: 3, entityName: 'Rooms end',
    fragment: 'AC-4', ingestedAt: '2026-09-29T12:00:00Z', notInLatestSource: true,
    editedSinceIngest: false,
    source: { id: 10, system: 'jira', externalId: 'CON-3685', locatorType: 'URL',
      locator: 'https://tracker.example.com/browse/CON-3685', title: null, contentHash: null,
      lastIngestedAt: null, kind: null, note: null } },
];

describe('SourcesSectionComponent — accessibility (#272)', () => {
  it('has no axe violations with a link and a stale flag', async () => {
    const { fixture } = await render(SourcesSectionComponent, {
      providers: [{ provide: ProvenanceService,
        useValue: { getEntitySources: vi.fn().mockResolvedValue(LINKS) } }],
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 3 },
    });
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="sources-section"]')).not.toBeNull();
    await expectNoAxeViolations(el);
  });
});
