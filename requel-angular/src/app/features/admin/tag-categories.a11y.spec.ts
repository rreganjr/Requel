import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import { TagCategoriesComponent } from './tag-categories';
import { TagService } from '../../core/tag.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const MOCK_CATEGORIES = [
  { id: 1, version: 1, projectId: null, name: 'type', exclusive: true, color: null,
    allowedEntityTypes: ['Goal'], values: ['business-rule', 'performance'] }
];
const flush = () => new Promise(r => setTimeout(r, 0));

describe('TagCategoriesComponent — accessibility (issue #138)', () => {
  async function render() {
    TestBed.configureTestingModule({
      imports: [TagCategoriesComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: {
          getTypedCategories: vi.fn().mockResolvedValue(MOCK_CATEGORIES),
          editTagCategory: vi.fn().mockResolvedValue({ success: true, entity: { id: 2 } }),
          deleteTagCategory: vi.fn().mockResolvedValue({ success: true }),
        } },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(TagCategoriesComponent);
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('groups the add-row under a fieldset with an accessible name', async () => {
    const el = await render();
    const fs = el.querySelector('fieldset[data-testid="tag-category-add-form"]');
    expect(fs).not.toBeNull();
    expect(fs!.querySelector('legend')?.textContent?.trim()).toBe('Add tag category');
  });

  it('has no axe-core violations in the add-form group', async () => {
    const el = await render();
    // Scope to the add-form fieldset: the rest of the page (data-table) carries a
    // pre-existing empty-table-header issue that is out of scope for #138.
    const fs = el.querySelector('fieldset[data-testid="tag-category-add-form"]') as HTMLElement;
    await expectNoAxeViolations(fs);
  });
});
