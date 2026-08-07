import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { Location } from '@angular/common';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { MessageService } from 'primeng/api';
import { UseCaseEditorComponent } from './use-case-editor';
import { UseCaseService } from '../../core/use-case.service';
import { ActorService } from '../../core/actor.service';
import { ScenarioService } from '../../core/scenario.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const flush = () => new Promise(r => setTimeout(r, 0));

const CREATED = {
  id: 30, version: 1, name: 'Place order', text: '', primaryActorName: null,
  scenarioId: null, scenarioName: null, scenarioStepCount: 0,
  goals: [], stories: [], actors: [], additionalScenarios: [],
};

// #173: create is a four-step wizard here, and each step hosts its own tables. Every one gets an
// axe pass, since the association tables are where the empty-header and unlabelled-action
// problems live.
describe('UseCaseEditorComponent - create wizard accessibility', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: UseCaseEditorComponent;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', useCaseId: 'new' }));

    TestBed.configureTestingModule({
      imports: [UseCaseEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: Location, useValue: { back: vi.fn() } },
        { provide: UseCaseService, useValue: { getUseCase: vi.fn().mockResolvedValue(CREATED) } },
        { provide: ActorService, useValue: { listActors: vi.fn().mockResolvedValue([]) } },
        { provide: ScenarioService, useValue: { getScenario: vi.fn().mockResolvedValue(null) } },
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
    fixture = TestBed.createComponent(UseCaseEditorComponent);
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
    const wizard = fixture.nativeElement.querySelector('[data-testid="use-case-wizard"]');
    expect(wizard).not.toBeNull();
    return wizard as HTMLElement;
  }

  /**
   * Clicks Continue once. Assigning comp.wizardStep directly mutates the value the
   * [(activeKey)] binding was checked with and throws NG0100, so every step change goes
   * through the real control.
   */
  async function clickContinue(wizard: HTMLElement): Promise<void> {
    (wizard.querySelector('[data-testid="wizard-continue"] button') as HTMLButtonElement).click();
    await flush();
    await settle();
  }

  /** Commits Details, which is what unlocks every later step. */
  async function advancePastDetails(wizard: HTMLElement): Promise<void> {
    comp.detailsForm.controls.name.setValue('Place order');
    await settle();
    await clickContinue(wizard);
  }

  it('renders four steps with the nav labelled and Details current', async () => {
    const wizard = await renderWizard();
    expect(wizard.querySelector('nav')!.getAttribute('aria-label')).toBe('New use case steps');
    expect(wizard.querySelectorAll('.app-wizard-step').length).toBe(4);
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
    expect(wizard.textContent).toContain('A use case needs a name.');
    await expectNoAxeViolations(wizard);
  });

  it('has no axe-core violations on the Scenarios step', async () => {
    const wizard = await renderWizard();
    await advancePastDetails(wizard);
    expect(comp.wizardStep).toBe('scenarios');
    await expectNoAxeViolations(wizard);
  });

  it('has no axe-core violations on the Goals & Stories step, tables included', async () => {
    const wizard = await renderWizard();
    await advancePastDetails(wizard);
    await clickContinue(wizard);
    expect(comp.wizardStep).toBe('goals-stories');
    // Both tables must be rendered or the axe pass proves nothing about them.
    expect(wizard.querySelector('[data-testid="use-case-goals-table"]')).not.toBeNull();
    expect(wizard.querySelector('[data-testid="use-case-stories-table"]')).not.toBeNull();
    await expectNoAxeViolations(wizard);
  });

  it('has no axe-core violations on the Actors step', async () => {
    const wizard = await renderWizard();
    await advancePastDetails(wizard);
    await clickContinue(wizard);
    await clickContinue(wizard);
    expect(comp.wizardStep).toBe('actors');
    expect(wizard.querySelector('[data-testid="use-case-actors-table"]')).not.toBeNull();
    await expectNoAxeViolations(wizard);
  });
});
