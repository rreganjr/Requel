import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ActorEditorComponent } from './actor-editor';
import { ActorService } from '../../core/actor.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const flush = () => new Promise(r => setTimeout(r, 0));

const CREATED = {
  id: 5, version: 1, name: 'Customer', text: 'End user of the system.',
  goals: [], referencedByUseCases: [], referencedByStories: [],
};

// #173: create is now an app-form-wizard, so the create surface needs its own axe pass -
// including with a field in the error state, which is where a missing or duplicated label and
// an unassociated error message actually show up.
describe('ActorEditorComponent - create wizard accessibility', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: ActorEditorComponent;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', actorId: 'new' }));

    TestBed.configureTestingModule({
      imports: [ActorEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: ActorService, useValue: { getActor: vi.fn().mockResolvedValue(CREATED) } },
        { provide: CommandService, useValue: {
            execute: vi.fn().mockResolvedValue({ success: true, entity: CREATED }),
          } },
        { provide: ProjectService, useValue: { notifyTreeChanged: vi.fn() } },
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
    fixture = TestBed.createComponent(ActorEditorComponent);
    comp = fixture.componentInstance;
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
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
    const wizard = fixture.nativeElement.querySelector('[data-testid="actor-wizard"]');
    expect(wizard).not.toBeNull();
    return wizard as HTMLElement;
  }

  it('renders the wizard on create, with the step nav labelled and Details current', async () => {
    const wizard = await renderWizard();
    expect(wizard.querySelector('nav')!.getAttribute('aria-label')).toBe('New actor steps');
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
    // The message has to be on screen or this asserts nothing.
    expect(wizard.textContent).toContain('An actor needs a name.');
    await expectNoAxeViolations(wizard);
  });

  it('has no axe-core violations on the Goals step', async () => {
    const wizard = await renderWizard();

    // Advance the way a user does. Assigning comp.wizardStep directly mutates the value the
    // [(activeKey)] binding was checked with and throws NG0100 - and it would skip the step-1
    // commit, so Goals would render its "save the details first" placeholder instead of the
    // table this test is meant to cover.
    comp.detailsForm.controls.name.setValue('Customer');
    await settle();
    (wizard.querySelector('[data-testid="wizard-continue"] button') as HTMLButtonElement).click();
    await flush();
    await settle();
    expect(comp.wizardStep).toBe('goals');

    await expectNoAxeViolations(wizard);
  });
});
