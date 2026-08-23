import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import { GlobalTagsComponent } from './global-tags';
import { TagService } from '../../core/tag.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const MOCK_GLOBAL_TAGS = [
  { id: 1, version: 1, category: 'projectKind', value: 'product', projectId: null,
    color: null, createdBy: 'admin' }
];
const flush = () => new Promise(r => setTimeout(r, 0));

describe('GlobalTagsComponent — accessibility (issue #138)', () => {
  async function render() {
    TestBed.configureTestingModule({
      imports: [GlobalTagsComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: {
          getTagsForProject: vi.fn().mockResolvedValue(MOCK_GLOBAL_TAGS),
          editTag: vi.fn().mockResolvedValue({ success: true, entity: { id: 2 } }),
          deleteTag: vi.fn().mockResolvedValue({ success: true }),
        } },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(GlobalTagsComponent);
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture;
  }

  it('groups the add-row under a fieldset with an accessible name', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    const fs = el.querySelector('fieldset[data-testid="global-tag-add-form"]');
    expect(fs).not.toBeNull();
    expect(fs!.querySelector('legend')?.textContent?.trim()).toBe('Add global tag');
  });

  it('has no axe-core violations in the add-form group', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    // Scope to the add-form fieldset: the rest of the page (data-table) carries a
    // pre-existing empty-table-header issue that is out of scope for #138.
    const fs = el.querySelector('fieldset[data-testid="global-tag-add-form"]') as HTMLElement;
    await expectNoAxeViolations(fs);
  });
  it('keeps the add-row accessible when the required message is shown', async () => {
    const fixture = await render();
    fixture.componentInstance.addForm.controls.value.setValue('  ');
    await fixture.componentInstance.addTag();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const fs = el.querySelector('fieldset[data-testid="global-tag-add-form"]') as HTMLElement;
    expect(fs.querySelector('.rq-field-error')).not.toBeNull();
    await expectNoAxeViolations(fs);
  });
});
