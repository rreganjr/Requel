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
    return fixture;
  }

  it('groups the add-row under a fieldset with an accessible name', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    const fs = el.querySelector('fieldset[data-testid="tag-category-add-form"]');
    expect(fs).not.toBeNull();
    expect(fs!.querySelector('legend')?.textContent?.trim()).toBe('Add tag category');
  });

  it('has no axe-core violations in the add-form group', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    // Scope to the add-form fieldset: the rest of the page (data-table) carries a
    // pre-existing empty-table-header issue that is out of scope for #138.
    const fs = el.querySelector('fieldset[data-testid="tag-category-add-form"]') as HTMLElement;
    await expectNoAxeViolations(fs);
  });
  it('keeps the add-row accessible when the required message is shown', async () => {
    const fixture = await render();
    fixture.componentInstance.addForm.controls.name.setValue('  ');
    await fixture.componentInstance.addCategory();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const fs = el.querySelector('fieldset[data-testid="tag-category-add-form"]') as HTMLElement;
    expect(fs.querySelector('.rq-field-error')).not.toBeNull();
    await expectNoAxeViolations(fs);
  });

  /**
   * The slug hint (#255) is the kind of helper text that used to be added as a bare paragraph a
   * screen-reader user never heard. Both text inputs whose content gets slugged point at it, and
   * the name input's error joins that list rather than replacing it — losing the hint the moment
   * the field is invalid would hide it exactly when it is most likely to explain the problem.
   */
  it('describes both slugged inputs by the normalization hint', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    const hint = el.querySelector('#tag-category-slug-hint');
    expect(hint?.textContent).toContain('con-3685');

    const name = el.querySelector<HTMLInputElement>('[data-testid="tag-category-name"]')!;
    const values = el.querySelector<HTMLInputElement>('[data-testid="tag-category-values"]')!;
    expect(name.getAttribute('aria-describedby')).toBe('tag-category-slug-hint');
    expect(values.getAttribute('aria-describedby')).toBe('tag-category-slug-hint');
  });

  it('keeps the hint described alongside the error when the name is invalid', async () => {
    const fixture = await render();
    fixture.componentInstance.addForm.controls.name.setValue('  ');
    await fixture.componentInstance.addCategory();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    const name = el.querySelector<HTMLInputElement>('[data-testid="tag-category-name"]')!;
    const ids = (name.getAttribute('aria-describedby') ?? '').split(/\s+/).filter(Boolean);
    expect(ids).toContain('tag-category-slug-hint');
    expect(ids).toContain('tag-category-name-error');
    expect(ids).toHaveLength(2);
  });
});
