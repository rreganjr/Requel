import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import { GlobalTagsComponent } from './global-tags';
import { TagService } from '../../core/tag.service';

const MOCK_GLOBAL_TAGS = [
  { id: 1, version: 1, category: 'projectKind', value: 'product', projectId: null,
    description: null, color: null, createdBy: 'admin' }
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('GlobalTagsComponent', () => {
  let tagServiceMock: {
    getTagsForProject: ReturnType<typeof vi.fn>;
    editTag: ReturnType<typeof vi.fn>;
    deleteTag: ReturnType<typeof vi.fn>;
  };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: GlobalTagsComponent;

  beforeEach(() => {
    tagServiceMock = {
      getTagsForProject: vi.fn().mockResolvedValue(MOCK_GLOBAL_TAGS),
      editTag: vi.fn().mockResolvedValue({ success: true, entity: { id: 2 } }),
      deleteTag: vi.fn().mockResolvedValue({ success: true })
    };

    TestBed.configureTestingModule({
      imports: [GlobalTagsComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TagService, useValue: tagServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } }
      ]
    });
    fixture = TestBed.createComponent(GlobalTagsComponent);
    comp = fixture.componentInstance;
  });

  it('loads global tags on init (no projectName argument)', async () => {
    fixture.detectChanges();
    await flush();
    expect(tagServiceMock.getTagsForProject).toHaveBeenCalledWith();
    expect(comp.tags().length).toBe(1);
    expect(comp.loading()).toBe(false);
  });

  it('addTag() creates a global tag with null projectName and reloads', async () => {
    fixture.detectChanges();
    await flush();
    comp.newCategory = 'projectKind';
    comp.newValue = 'feature';
    await comp.addTag();
    expect(tagServiceMock.editTag).toHaveBeenCalledWith(null, 'projectKind', 'feature');
    expect(comp.newValue).toBe('');
    expect(tagServiceMock.getTagsForProject).toHaveBeenCalledTimes(2);
  });

  it('addTag() does nothing when value is blank', async () => {
    fixture.detectChanges();
    await flush();
    comp.newValue = '   ';
    await comp.addTag();
    expect(tagServiceMock.editTag).not.toHaveBeenCalled();
  });

  it('deleteTag() deletes and reloads', async () => {
    fixture.detectChanges();
    await flush();
    await comp.deleteTag(MOCK_GLOBAL_TAGS[0]);
    expect(tagServiceMock.deleteTag).toHaveBeenCalledWith(1);
    expect(tagServiceMock.getTagsForProject).toHaveBeenCalledTimes(2);
  });

  it('errorMessage set when load fails', async () => {
    tagServiceMock.getTagsForProject.mockRejectedValue(new Error('boom'));
    fixture.detectChanges();
    await flush();
    expect(comp.errorMessage()).toBe('Failed to load global tags.');
  });
});
