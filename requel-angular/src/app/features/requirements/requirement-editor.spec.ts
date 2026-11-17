import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { EMPTY } from 'rxjs';
import { MessageService } from 'primeng/api';
import { RequirementEditorComponent } from './requirement-editor';
import { RequirementService } from '../../core/requirement.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { AnnouncerService } from '../../core/announcer.service';
import { RequirementDto, RequirementStatus, ReviewDepartment } from '../../models/requirement';

describe('Requirement review entry points', () => {
  function render(status: RequirementStatus, departments: ReviewDepartment[] = ['QUALITY'],
                  canApproveOrReopen = false, allReviewsPassed = false) {
    TestBed.configureTestingModule({
      imports: [RequirementEditorComponent],
      providers: [
        provideRouter([]), provideNoopAnimations(),
        { provide: ActivatedRoute, useValue: { paramMap: EMPTY } },
        { provide: RequirementService, useValue: {} },
        { provide: CommandService, useValue: {} },
        { provide: ProjectService, useValue: {} },
        { provide: PermissionService, useValue: {
          canEdit: () => true, canDelete: () => true, getRoles: () => [],
        } },
        { provide: EventStreamService, useValue: {} },
        { provide: AnnouncerService, useValue: {} },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(RequirementEditorComponent);
    const component = fixture.componentInstance;
    // Exercise the actual detail template without fetching a record or ancillary panels.
    component.isNew.set(false);
    component.loading.set(false);
    component.requirement.set({
      id: 1, version: 0, requirementId: 'REQ-1', shallText: 'The system shall export reports.',
      status, ideaId: null, ideaName: null, jamaItemId: null, createdBy: 'author',
      createdAt: '2026-11-16T12:00:00Z', updatedAt: '2026-11-16T12:00:00Z',
      reviews: [], allReviewsPassed, isApproved: status === 'APPROVED',
      allowedReviewDepartments: departments, canApproveOrReopen,
      isPublished: status === 'PUBLISHED',
    } satisfies RequirementDto);
    fixture.detectChanges();
    return fixture;
  }

  it.each<RequirementStatus>(['DRAFT', 'IN_REVIEW', 'REJECTED'])('allows department review from %s', status => {
    const fixture = render(status);
    expect(fixture.nativeElement.querySelectorAll('p-button[data-testid^="req-review-"]').length).toBe(1);
    const button = fixture.nativeElement.querySelector('[data-testid="req-review-quality"] button') as HTMLButtonElement;
    button.click();
    expect(fixture.componentInstance.reviewDialogVisible()).toBe(true);
    expect(fixture.componentInstance.reviewDialogDept()).toBe('QUALITY');
  });

  it.each<RequirementStatus>(['APPROVED', 'PUBLISHED'])('does not offer new reviews while %s', status => {
    const fixture = render(status);
    expect(fixture.nativeElement.querySelectorAll('p-button[data-testid^="req-review-"]').length).toBe(0);
  });

  it('does not expose review controls without a reviewer role', () => {
    const fixture = render('DRAFT', []);
    expect(fixture.nativeElement.querySelectorAll('p-button[data-testid^="req-review-"]').length).toBe(0);
  });

  it.each<ReviewDepartment>(['FEASIBILITY', 'TESTABILITY'])('offers only the %s review', department => {
    const fixture = render('DRAFT', [department]);
    const buttons = fixture.nativeElement.querySelectorAll('p-button[data-testid^="req-review-"]');
    expect(buttons.length).toBe(1);
    expect(buttons[0].getAttribute('data-testid')).toBe(`req-review-${department.toLowerCase()}`);
  });

  it('offers final approval and zero department reviews to a Product Manager', () => {
    const fixture = render('IN_REVIEW', [], true, true);
    expect(fixture.nativeElement.querySelector('[data-testid="req-approve"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelectorAll('p-button[data-testid^="req-review-"]').length).toBe(0);
  });

  it('offers all three department reviews to a system administrator', () => {
    const fixture = render('DRAFT', ['QUALITY', 'FEASIBILITY', 'TESTABILITY']);
    expect(fixture.nativeElement.querySelectorAll('p-button[data-testid^="req-review-"]').length).toBe(3);
  });

  it('lets a Product Manager reopen a rejected requirement', () => {
    const fixture = render('REJECTED', [], true);
    expect(fixture.nativeElement.querySelector('[data-testid="req-reopen"]')).not.toBeNull();
  });
});
