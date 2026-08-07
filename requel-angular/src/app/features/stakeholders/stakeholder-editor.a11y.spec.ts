import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { MessageService } from 'primeng/api';
import { StakeholderEditorComponent } from './stakeholder-editor';
import { StakeholderService } from '../../core/stakeholder.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { UserService } from '../../core/user.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const flush = () => new Promise(r => setTimeout(r, 0));

const PERMISSIONS = [
  { entityType: 'Goal', permissionKey: 'edit_goal', permissionType: 'Edit' },
  { entityType: 'Goal', permissionKey: 'delete_goal', permissionType: 'Delete' },
];

const CREATED = {
  id: 51, version: 1, name: 'FASB', type: 'non-user', goals: [],
  userDetails: null, nonUserDetails: { text: 'Financial authority' },
};

// #173: create is a wizard now, and the permission matrix is the part most likely to have
// accessibility problems - a grid of checkboxes whose only visible labels are column headers.
describe('StakeholderEditorComponent - create wizard accessibility', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: StakeholderEditorComponent;

  function setup(routeId: string): void {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', stakeholderId: routeId }));

    TestBed.configureTestingModule({
      imports: [StakeholderEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: StakeholderService, useValue: {
            getStakeholder: vi.fn().mockResolvedValue(CREATED),
            getAvailablePermissions: vi.fn().mockResolvedValue(PERMISSIONS),
          } },
        { provide: CommandService, useValue: {
            execute: vi.fn().mockResolvedValue({ success: true, entity: CREATED }),
          } },
        { provide: ProjectService, useValue: { notifyTreeChanged: vi.fn() } },
        { provide: UserService, useValue: {
            listUsers: vi.fn().mockResolvedValue([
              { id: 1, version: 0, username: 'alice', name: 'Alice', emailAddress: null,
                phoneNumber: null, organizationName: null, roles: [], permissions: [],
                permissionsByRole: null },
            ]),
          } },
        { provide: PermissionService, useValue: {
            loadForProject: vi.fn().mockResolvedValue(undefined),
            canEdit: vi.fn().mockReturnValue(true),
            canDelete: vi.fn().mockReturnValue(true),
          } },
        { provide: EventStreamService, useValue: {
            events$: EMPTY,
            addSubscription: vi.fn().mockResolvedValue(undefined),
            removeSubscription: vi.fn().mockResolvedValue(undefined),
          } },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(StakeholderEditorComponent);
    comp = fixture.componentInstance;
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  }

  afterEach(() => {
    fixture.destroy();
  });

  async function settle(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function renderWizard(): Promise<HTMLElement> {
    fixture.detectChanges();
    await flush();
    await settle();
    const wizard = fixture.nativeElement.querySelector('[data-testid="stakeholder-wizard"]');
    expect(wizard).not.toBeNull();
    return wizard as HTMLElement;
  }

  it('renders the wizard with the step nav labelled and Details current', async () => {
    setup('new-nonuser');
    const wizard = await renderWizard();
    expect(wizard.querySelector('nav')!.getAttribute('aria-label')).toBe('New stakeholder steps');
    expect(wizard.querySelector('[aria-current="step"]')!.textContent).toContain('Details');
  });

  it('has no axe-core violations on the non-user Details step', async () => {
    setup('new-nonuser');
    await expectNoAxeViolations(await renderWizard());
  });

  it('has no axe-core violations with the name field in the error state', async () => {
    setup('new-nonuser');
    const wizard = await renderWizard();
    comp.submitted.set(true);
    comp.detailsForm.controls.name.setValue('');
    comp.detailsForm.markAllAsTouched();
    await settle();
    expect(wizard.textContent).toContain('A stakeholder needs a name.');
    await expectNoAxeViolations(wizard);
  });

  it('has no axe-core violations on the user Details step, permission matrix included', async () => {
    setup('new-user');
    const wizard = await renderWizard();
    // The matrix must actually be rendered or this asserts nothing.
    expect(wizard.querySelector('[data-testid="stakeholder-perm-edit_goal"]')).not.toBeNull();
    await expectNoAxeViolations(wizard);
  });

  it('gives every permission checkbox its own accessible name', async () => {
    setup('new-user');
    const wizard = await renderWizard();
    // The column header alone is not an accessible name - each box carries a hidden label.
    for (const key of ['edit_goal', 'delete_goal']) {
      const label = wizard.querySelector(`label[for="perm-${key}"]`);
      expect(label, `missing label for ${key}`).not.toBeNull();
      expect(label!.textContent!.trim().length).toBeGreaterThan(0);
    }
  });

  it('has no axe-core violations on the Goals step', async () => {
    setup('new-nonuser');
    const wizard = await renderWizard();
    comp.detailsForm.controls.name.setValue('FASB');
    await settle();
    (wizard.querySelector('[data-testid="wizard-continue"] button') as HTMLButtonElement).click();
    await flush();
    await settle();
    expect(comp.wizardStep).toBe('goals');
    await expectNoAxeViolations(wizard);
  });
});
