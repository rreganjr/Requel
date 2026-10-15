import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { IgnoredFindingDto } from '../models/dictionary';
import { IgnoredFindingListComponent } from './ignored-finding-list';

const FINDINGS: IgnoredFindingDto[] = [
  { id: 7, subject: 'groal', findingType: 'unknown-word', entityType: 'Goal', entityId: 3,
    entityName: 'groal intake', propertyName: 'Name', createdBy: 'project', dateCreated: null },
  { id: 8, subject: 'fast', findingType: 'vague-word', entityType: 'Step', entityId: 9,
    entityName: null, propertyName: 'Text', createdBy: 'project', dateCreated: null },
];
const flush = () => new Promise(r => setTimeout(r, 0));

describe('IgnoredFindingListComponent (#320)', () => {
  async function render(inputs: Partial<IgnoredFindingListComponent> = {}) {
    TestBed.configureTestingModule({
      imports: [IgnoredFindingListComponent],
      providers: [provideNoopAnimations(), provideRouter([])],
    });
    const fixture = TestBed.createComponent(IgnoredFindingListComponent);
    const comp = fixture.componentInstance;
    comp.findings = FINDINGS;
    comp.projectName = 'Proj A';
    comp.canRemove = true;
    comp.testid = 'ign';
    Object.assign(comp, inputs);
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return { fixture, comp, el: fixture.nativeElement as HTMLElement };
  }

  it('renders one row per ignored finding with a readable type', async () => {
    const { el, comp } = await render();
    const subjects = Array.from(el.querySelectorAll('[data-testid="ign-subject"]')).map(e => e.textContent);
    expect(subjects).toEqual(['groal', 'fast']);
    expect(comp.findingTypeLabel('unknown-word')).toBe('Spelling');
    expect(comp.findingTypeLabel('something-new')).toBe('something-new');
  });

  it('links the entity to its page, and a step (no page) is plain text', async () => {
    const { el, comp } = await render();
    expect(comp.entityLink(FINDINGS[0])).toEqual(['/projects', 'Proj A', 'goals', '3']);
    expect(comp.entityLink(FINDINGS[1])).toBeNull();
    const entities = Array.from(el.querySelectorAll('[data-testid="ign-entity"]'));
    expect(entities[0].tagName).toBe('A');
    expect(entities[0].textContent).toContain('groal intake');
    expect(entities[1].tagName).toBe('SPAN');
    expect(entities[1].textContent).toContain('Step 9');
  });

  it('emits remove only after the user confirms', async () => {
    const { fixture, comp } = await render();
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    let accept = false;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => { if (accept) conf.accept?.(); return cs; });
    const emitted: IgnoredFindingDto[] = [];
    comp.remove.subscribe(f => emitted.push(f));
    comp.confirmRemove(FINDINGS[0]);
    expect(emitted).toEqual([]);
    accept = true;
    comp.confirmRemove(FINDINGS[0]);
    expect(emitted).toEqual([FINDINGS[0]]);
  });

  it('hides remove buttons without canRemove', async () => {
    const { el } = await render({ canRemove: false });
    expect(el.querySelector('[data-testid="ign-remove"]')).toBeNull();
  });

  it('shows remove buttons with canRemove', async () => {
    const { el } = await render();
    expect(el.querySelectorAll('[data-testid="ign-remove"]').length).toBe(2);
  });
});
