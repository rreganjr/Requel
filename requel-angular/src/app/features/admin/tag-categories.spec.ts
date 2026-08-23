import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import { TagCategoriesComponent } from './tag-categories';
import { TagService } from '../../core/tag.service';

const MOCK_CATEGORIES = [
  { id: 1, version: 1, projectId: null, name: 'type', exclusive: true, color: null,
    allowedEntityTypes: ['Goal'], values: ['business-rule', 'performance'] }
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('TagCategoriesComponent', () => {
  let tagServiceMock: {
    getTypedCategories: ReturnType<typeof vi.fn>;
    editTagCategory: ReturnType<typeof vi.fn>;
    deleteTagCategory: ReturnType<typeof vi.fn>;
  };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: TagCategoriesComponent;

  beforeEach(() => {
    tagServiceMock = {
      getTypedCategories: vi.fn().mockResolvedValue(MOCK_CATEGORIES),
      editTagCategory: vi.fn().mockResolvedValue({ success: true, entity: { id: 2 } }),
      deleteTagCategory: vi.fn().mockResolvedValue({ success: true })
    };

    TestBed.configureTestingModule({
      imports: [TagCategoriesComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: tagServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(TagCategoriesComponent);
    comp = fixture.componentInstance;
  });

  it('loads global categories on init (no projectName)', async () => {
    fixture.detectChanges();
    await flush();
    expect(tagServiceMock.getTypedCategories).toHaveBeenCalledWith();
    expect(comp.categories().length).toBe(1);
  });

  it('addCategory() dispatches EditTagCategory with split lists and reloads', async () => {
    fixture.detectChanges();
    await flush();
    comp.addForm.setValue({
      name: 'projectKind',
      exclusive: true,
      allowedTypes: 'Project',
      values: 'product, feature',
      color: '#1d4ed8',
    });
    await comp.addCategory();
    expect(tagServiceMock.editTagCategory).toHaveBeenCalledWith({
      projectName: null,
      name: 'projectKind',
      exclusive: true,
      color: '#1d4ed8',
      allowedEntityTypes: ['Project'],
      values: ['product', 'feature']
    });
    expect(comp.addForm.controls.name.value).toBe('');
    expect(tagServiceMock.getTypedCategories).toHaveBeenCalledTimes(2);
  });

  it('addCategory() shows the required message and calls nothing when the name is blank', async () => {
    fixture.detectChanges();
    await flush();
    comp.addForm.controls.name.setValue('   ');
    await comp.addCategory();
    fixture.detectChanges();
    expect(tagServiceMock.editTagCategory).not.toHaveBeenCalled();
    const el = fixture.nativeElement as HTMLElement;
    const input = el.querySelector('[data-testid="tag-category-name"]') as HTMLElement;
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(input.getAttribute('aria-describedby')).toBe('tag-category-name-error');
    const err = el.querySelector('[data-testid="tag-category-name-error"]') as HTMLElement;
    expect(err.textContent).toContain('Name is required.');
    expect(err.getAttribute('role')).toBe('alert');
  });

  it('deleteCategory() deletes and reloads', async () => {
    fixture.detectChanges();
    await flush();
    await comp.deleteCategory(MOCK_CATEGORIES[0]);
    expect(tagServiceMock.deleteTagCategory).toHaveBeenCalledWith(1);
    expect(tagServiceMock.getTypedCategories).toHaveBeenCalledTimes(2);
  });
});
