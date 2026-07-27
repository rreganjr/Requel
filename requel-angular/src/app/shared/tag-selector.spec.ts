import { render, screen } from '@testing-library/angular';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import { TagSelectorComponent } from './tag-selector';
import { TagService } from '../core/tag.service';

describe('TagSelectorComponent', () => {
  let tagServiceMock: {
    getTagsOnEntity: ReturnType<typeof vi.fn>;
    getTagsForProject: ReturnType<typeof vi.fn>;
    getCategories: ReturnType<typeof vi.fn>;
    editTag: ReturnType<typeof vi.fn>;
    assignTag: ReturnType<typeof vi.fn>;
    unassignTag: ReturnType<typeof vi.fn>;
  };
  let messageServiceMock: { add: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    tagServiceMock = {
      getTagsOnEntity: vi.fn().mockResolvedValue([]),
      getTagsForProject: vi.fn().mockResolvedValue([]),
      getCategories: vi.fn().mockResolvedValue([]),
      editTag: vi.fn().mockResolvedValue({ success: true, entity: { id: 5 } }),
      assignTag: vi.fn().mockResolvedValue({ success: true }),
      unassignTag: vi.fn().mockResolvedValue({ success: true }),
    };
    messageServiceMock = { add: vi.fn() };
  });

  function providers() {
    return [
      provideNoopAnimations(),
      { provide: TagService, useValue: tagServiceMock },
      { provide: MessageService, useValue: messageServiceMock },
    ];
  }

  it('shows "No tags." when the entity has none', async () => {
    const { fixture } = await render(TagSelectorComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 1, canEdit: false }
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(screen.getByText('No tags.')).toBeInTheDocument();
  });

  it('loads assigned tags when entityId is provided', async () => {
    const { fixture } = await render(TagSelectorComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 42, canEdit: false }
    });
    await fixture.whenStable();
    expect(tagServiceMock.getTagsOnEntity).toHaveBeenCalledWith('Goal', 42);
  });

  it('does not load when entityId is null', async () => {
    const { fixture } = await render(TagSelectorComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: null, canEdit: false }
    });
    await fixture.whenStable();
    expect(tagServiceMock.getTagsOnEntity).not.toHaveBeenCalled();
  });

  it('hides the Add Tag control when canEdit is false', async () => {
    const { fixture } = await render(TagSelectorComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 1, canEdit: false }
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(screen.queryByText('Add Tag')).not.toBeInTheDocument();
  });

  it('shows the Add Tag control when canEdit is true', async () => {
    const { fixture } = await render(TagSelectorComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 1, canEdit: true }
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(screen.getByText('Add Tag')).toBeInTheDocument();
  });

  it('renders a chip labeled "category=value" for an assigned namespaced tag', async () => {
    tagServiceMock.getTagsOnEntity.mockResolvedValue([
      { id: 1, version: 1, category: 'type', value: 'business-rule', projectId: 9,
        color: null, createdBy: 'admin' }
    ]);
    const { fixture } = await render(TagSelectorComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 1, canEdit: true }
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(screen.getByText('type=business-rule')).toBeInTheDocument();
  });

  it('addTag() creates then assigns a tag and clears the inputs', async () => {
    const { fixture } = await render(TagSelectorComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 1, canEdit: true }
    });
    await fixture.whenStable();
    const comp = fixture.componentInstance as TagSelectorComponent;
    comp.newCategory = 'type';
    comp.newValue = 'performance';
    await comp.addTag();
    expect(tagServiceMock.editTag).toHaveBeenCalledWith('proj1', 'type', 'performance');
    expect(tagServiceMock.assignTag).toHaveBeenCalledWith(5, 'Goal', 1);
    expect(comp.newValue).toBe('');
    expect(comp.newCategory).toBe('');
  });
});
