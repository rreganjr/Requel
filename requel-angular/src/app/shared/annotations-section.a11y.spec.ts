import { render } from '@testing-library/angular';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import { AnnotationsSectionComponent } from './annotations-section';
import { AnnotationService } from '../core/annotation.service';
import { expectNoAxeViolations } from './testing/a11y';

describe('AnnotationsSectionComponent — accessibility (issue #138)', () => {
  function providers() {
    return [
      provideNoopAnimations(),
      { provide: AnnotationService, useValue: {
        getAnnotations: vi.fn().mockResolvedValue({ notes: [], issues: [] }),
        addNote: vi.fn().mockResolvedValue({ success: true }),
        addIssue: vi.fn().mockResolvedValue({ success: true }),
        resolveIssue: vi.fn().mockResolvedValue({ success: true }),
      } },
      { provide: MessageService, useValue: { add: vi.fn() } },
    ];
  }

  it('groups the add-note / add-issue forms under labelled fieldsets, no axe violations', async () => {
    const { fixture } = await render(AnnotationsSectionComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 1, canEdit: true },
    });
    await fixture.whenStable();
    fixture.componentInstance.showNoteForm.set(true);
    fixture.componentInstance.showIssueForm.set(true);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    const noteFs = el.querySelector('fieldset[data-testid="annotation-note-form"]') as HTMLElement;
    const issueFs = el.querySelector('fieldset[data-testid="annotation-issue-form"]') as HTMLElement;
    expect(noteFs).not.toBeNull();
    expect(issueFs).not.toBeNull();
    expect(noteFs.querySelector('legend')?.textContent?.trim()).toBe('Add note');
    expect(issueFs.querySelector('legend')?.textContent?.trim()).toBe('Add issue');
    await expectNoAxeViolations(noteFs);
    await expectNoAxeViolations(issueFs);
  });

  it('keeps the note / issue forms accessible when the required error is shown', async () => {
    const { fixture } = await render(AnnotationsSectionComponent, {
      providers: providers(),
      inputs: { projectName: 'proj1', entityType: 'Goal', entityId: 1, canEdit: true },
    });
    await fixture.whenStable();
    const comp = fixture.componentInstance;
    comp.showNoteForm.set(true);
    comp.showIssueForm.set(true);
    fixture.detectChanges();
    await comp.saveNote();
    await comp.saveIssue();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    const noteFs = el.querySelector('fieldset[data-testid="annotation-note-form"]') as HTMLElement;
    const issueFs = el.querySelector('fieldset[data-testid="annotation-issue-form"]') as HTMLElement;
    expect(noteFs.querySelector('[data-testid="annotation-note-error"]')).not.toBeNull();
    expect(issueFs.querySelector('[data-testid="annotation-issue-error"]')).not.toBeNull();
    await expectNoAxeViolations(noteFs);
    await expectNoAxeViolations(issueFs);
  });
});
