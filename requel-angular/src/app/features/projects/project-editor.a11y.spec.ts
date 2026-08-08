import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ProjectEditorComponent } from './project-editor';
import { ProjectService } from '../../core/project.service';
import { UserService } from '../../core/user.service';
import { CommandService } from '../../core/command.service';
import { PermissionService } from '../../core/permission.service';
import { TagService } from '../../core/tag.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const flush = () => new Promise(r => setTimeout(r, 0));

const CREATED = {
  id: 9, version: 1, name: 'My New Project', description: null, organizationName: null,
};

// #173: create is a wizard now, so the create surface needs its own axe pass - including with a
// field in the error state, where label/message association problems actually show up.
describe('ProjectEditorComponent - create wizard accessibility', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ProjectEditorComponent;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'new' }));

    TestBed.configureTestingModule({
      imports: [ProjectEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: ProjectService, useValue: {
            getProject: vi.fn().mockResolvedValue(CREATED),
            downloadProjectXml: vi.fn(),
            notifyTreeChanged: vi.fn(),
          } },
        { provide: UserService, useValue: {
            listOrganizations: vi.fn().mockResolvedValue([{ id: 1, version: 0, name: 'Acme' }]),
          } },
        { provide: CommandService, useValue: {
            execute: vi.fn().mockResolvedValue({ success: true, entity: CREATED }),
          } },
        { provide: PermissionService, useValue: {
            loadForProject: vi.fn().mockResolvedValue(undefined),
            canEdit: vi.fn().mockReturnValue(true),
          } },
        { provide: TagService, useValue: {
            listTagsForEntity: vi.fn().mockResolvedValue([]),
            listTagCategories: vi.fn().mockResolvedValue([]),
          } },
        { provide: ConfirmationService, useValue: { confirm: vi.fn() } },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(ProjectEditorComponent);
    comp = fixture.componentInstance;
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

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
    const wizard = fixture.nativeElement.querySelector('[data-testid="project-wizard"]');
    expect(wizard).not.toBeNull();
    return wizard as HTMLElement;
  }

  it('renders the wizard with the step nav labelled and Details current', async () => {
    const wizard = await renderWizard();
    expect(wizard.querySelector('nav')!.getAttribute('aria-label')).toBe('New project steps');
    expect(wizard.querySelector('[aria-current="step"]')!.textContent).toContain('Details');
  });

  it('has no axe-core violations on the Details step', async () => {
    await expectNoAxeViolations(await renderWizard());
  });

  it('has no axe-core violations with the name field in the error state', async () => {
    const wizard = await renderWizard();
    comp.submitted.set(true);
    comp.detailsForm.controls.name.setValue('');
    comp.detailsForm.markAllAsTouched();
    await settle();
    expect(wizard.textContent).toContain('A project needs a name.');
    await expectNoAxeViolations(wizard);
  });

  it('has no axe-core violations on the Tags step', async () => {
    const wizard = await renderWizard();
    comp.detailsForm.controls.name.setValue('My New Project');
    await settle();
    (wizard.querySelector('[data-testid="wizard-continue"] button') as HTMLButtonElement).click();
    await flush();
    await settle();
    expect(comp.wizardStep).toBe('tags');
    await expectNoAxeViolations(wizard);
  });

  // Regression guard for the e2e contract. ProjectsPage addresses these by DOM id -
  // locator('#name'), locator('#description'). The #173 conversion dropped the ids and let
  // app-field generate them, which broke 23 e2e tests while every unit test stayed green.
  it('keeps the stable control ids the e2e page objects address', async () => {
    const wizard = await renderWizard();
    for (const [id, tag] of [['name', 'INPUT'], ['description', 'TEXTAREA']] as const) {
      const el = wizard.querySelector(`#${id}`);
      expect(el, `missing #${id} - e2e page objects locate this control by id`).not.toBeNull();
      expect(el!.tagName).toBe(tag);
      // The label must point at that same id, or the id is present but unassociated.
      expect(wizard.querySelector(`label[for="${id}"]`), `no <label for="${id}">`).not.toBeNull();
    }
  });

});
