import { render } from '@testing-library/angular';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import { TagSelectorComponent } from './tag-selector';
import { TagService } from '../core/tag.service';
import { expectNoAxeViolations } from './testing/a11y';

describe('TagSelectorComponent — accessibility (issue #138)', () => {
  function providers() {
    return [
      provideNoopAnimations(),
      { provide: TagService, useValue: {
        getTagsOnEntity: vi.fn().mockResolvedValue([]),
        getTagsForProject: vi.fn().mockResolvedValue([]),
        getCategories: vi.fn().mockResolvedValue([]),
        getTypedCategories: vi.fn().mockResolvedValue([]),
        editTag: vi.fn().mockResolvedValue({ success: true, entity: { id: 5 } }),
        assignTag: vi.fn().mockResolvedValue({ success: true }),
        unassignTag: vi.fn().mockResolvedValue({ success: true }),
      } },
      { provide: MessageService, useValue: { add: vi.fn() } },
    ];
  }

  it('groups the add-row under a fieldset with an accessible name and no axe violations', async () => {
    const { fixture } = await render(TagSelectorComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 1, canEdit: true },
    });
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const fs = el.querySelector('fieldset[data-testid="tag-add-form"]') as HTMLElement;
    expect(fs).not.toBeNull();
    expect(fs.querySelector('legend')?.textContent?.trim()).toBe('Add tag');
    await expectNoAxeViolations(fs);
  });
});
