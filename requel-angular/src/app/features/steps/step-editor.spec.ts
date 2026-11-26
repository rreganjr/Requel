import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { MessageService } from 'primeng/api';
import { Component, DebugElement, Input } from '@angular/core';
import { StepEditorComponent } from './step-editor';
import { ScenarioService } from '../../core/scenario.service';
import { CommandService } from '../../core/command.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { AnnotationsSectionComponent } from '../../shared/annotations-section';
import { SourcesSectionComponent } from '../../shared/sources-section';
import { ActorService } from '../../core/actor.service';
import { By } from '@angular/platform-browser';
import { TermsUsedComponent } from '../../shared/terms-used';

// The real sections load their own data; the step page only has to place them.
@Component({ selector: 'app-annotations-section', standalone: true, template: '' })
class AnnotationsStub {
  @Input() projectName = '';
  @Input() entityType = '';
  @Input() entityId: number | null = null;
}
@Component({ selector: 'app-sources-section', standalone: true, template: '' })
class SourcesStub {
  @Input() projectName = '';
  @Input() entityType = '';
  @Input() entityId: number | null = null;
}

const STEP = {
  id: 9, version: 2, name: 'Member signs in', text: 'with a card', scenarioType: 'Primary',
  createdBy: 'admin',
  usedIn: [{ entityType: 'Scenario', id: 3, name: 'Borrow' }, { entityType: 'Scenario', id: 4, name: 'Return' }],
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('StepEditorComponent (#476)', () => {
  let scenarioService: { getStep: ReturnType<typeof vi.fn> };
  let commandService: { execute: ReturnType<typeof vi.fn> };
  let canEdit: ReturnType<typeof vi.fn>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: StepEditorComponent;

  function setUp(step: object = STEP): void {
    scenarioService = { getStep: vi.fn().mockResolvedValue(step) };
    commandService = { execute: vi.fn().mockResolvedValue({ success: true, entity: STEP }) };
    canEdit = vi.fn().mockReturnValue(true);
    TestBed.configureTestingModule({
      imports: [StepEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: new BehaviorSubject(convertToParamMap({ name: 'proj1', stepId: '9' })) } },
        { provide: ScenarioService, useValue: scenarioService },
        { provide: CommandService, useValue: commandService },
        { provide: PermissionService, useValue: { loadForProject: vi.fn().mockResolvedValue(undefined), canEdit, canDelete: vi.fn() } },
        { provide: EventStreamService, useValue: { events$: EMPTY, addSubscription: vi.fn().mockResolvedValue(undefined) } },
        { provide: MessageService, useValue: { add: vi.fn() } },
        { provide: ActorService, useValue: { listActors: vi.fn().mockResolvedValue([{ id: 1, name: 'Clerk' }, { id: 2, name: 'Member' }]) } },
      ],
    });
    TestBed.overrideComponent(StepEditorComponent, {
      remove: { imports: [AnnotationsSectionComponent, SourcesSectionComponent] },
      add: { imports: [AnnotationsStub, SourcesStub] },
    });
    fixture = TestBed.createComponent(StepEditorComponent);
    comp = fixture.componentInstance;
  }

  async function load(): Promise<HTMLElement> {
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('feeds the Terms used row the name and text as typed (#506)', async () => {
    setUp();
    await load();
    comp.form.controls.name.setValue('Speed up the Host');
    const text = fixture.nativeElement.querySelector('[data-testid="step-text"]') as HTMLTextAreaElement;
    text.value = 'The Host ends the video room';
    text.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    const row = fixture.debugElement.query(By.directive(TermsUsedComponent)).componentInstance as TermsUsedComponent;
    expect(row.projectName()).toBe('proj1');
    expect(row.name()).toBe('Speed up the Host');
    expect(row.text()).toBe('The Host ends the video room');
  });

  it('loads the step, lists the scenarios using it, and places the annotations', async () => {
    setUp();
    const el = await load();
    expect(scenarioService.getStep).toHaveBeenCalledWith('proj1', 9);
    expect(comp.form.getRawValue()).toEqual({ who: '', name: 'Member signs in', scenarioType: 'Primary', text: 'with a card' });
    const links = el.querySelectorAll('[data-testid="step-used-in-link"]');
    expect(Array.from(links).map(a => a.textContent?.trim())).toEqual(['Borrow', 'Return']);
    expect(el.querySelector('[data-testid="step-shared-hint"]')?.textContent).toContain('2 scenarios');
    const annotations = fixture.debugElement.query((d: DebugElement) => d.componentInstance instanceof AnnotationsStub);
    expect(annotations.componentInstance.entityType).toBe('Step');
    expect(annotations.componentInstance.entityId).toBe(9);
  });

  it('Save sends EditScenarioStep with the version, and is off while pristine', async () => {
    setUp();
    await load();
    expect(comp.hasUnsavedChanges()).toBe(false);
    comp.form.controls.name.setValue('Member signs in with a card');
    comp.form.controls.name.markAsDirty();
    expect(comp.hasUnsavedChanges()).toBe(true);
    await comp.onSave();
    expect(commandService.execute).toHaveBeenCalledWith('EditScenarioStep', {
      projectName: 'proj1', stepId: 9, version: 2, name: 'Member signs in with a card',
      actorRole: 'NONE', scenarioTypeName: 'Primary', text: 'with a card',
    });
    expect(comp.hasUnsavedChanges()).toBe(false);
  });

  it('#468: loads the role, shows how it reads in each use case, and saves it', async () => {
    setUp({
      ...STEP, name: 'signs in', actorRole: 'PRIMARY_ACTOR', displayName: 'Primary actor signs in',
      readsIn: [
        { useCaseId: 5, useCaseName: 'Borrow a tool', primaryActorName: 'Member', readsAs: 'Member signs in' },
        { useCaseId: 6, useCaseName: 'Run the desk', primaryActorName: 'Clerk', readsAs: 'Clerk signs in' },
      ],
    });
    const el = await load();
    expect(comp.form.controls.who.value).toBe('PRIMARY_ACTOR');
    expect(comp.stepName()).toBe('Primary actor signs in');
    expect(comp.roleChoices().map(c => c.label))
      .toEqual(['In the wording', 'Primary actor', 'The system', 'Clerk', 'Member']);
    const rows = () => Array.from(el.querySelectorAll('[data-testid="step-reads-as-row"]'))
      .map(r => r.textContent?.replace(/\s+/g, ' ').trim());
    expect(rows()).toEqual(['Member signs in in Borrow a tool', 'Clerk signs in in Run the desk']);

    comp.form.controls.who.setValue('NAMED:Clerk');
    comp.form.controls.who.markAsDirty();
    fixture.detectChanges();
    expect(rows()).toEqual(['Clerk signs in in Borrow a tool', 'Clerk signs in in Run the desk']);
    await comp.onSave();
    expect(commandService.execute).toHaveBeenCalledWith('EditScenarioStep', expect.objectContaining({
      name: 'signs in', actorRole: 'NAMED', actorName: 'Clerk',
    }));
  });

  it('#468: no role, no reads-as lines; the field is labelled Action once a role is picked', async () => {
    setUp();
    const el = await load();
    expect(el.querySelector('[data-testid="step-reads-as"]')).toBeNull();
    expect(el.querySelector('label[for="stepName"]')?.textContent?.trim()).toMatch(/^Name\b/);
    comp.form.controls.who.setValue('SYSTEM');
    fixture.detectChanges();
    expect(el.querySelector('label[for="stepName"]')?.textContent?.trim()).toMatch(/^Action\b/);
    expect(el.querySelector('[data-testid="step-reads-as-row"]')?.textContent).toContain('The system Member signs in');
  });

  it('a refused save shows the server message and keeps the edit', async () => {
    setUp();
    await load();
    commandService.execute.mockResolvedValue({ success: false, error: 'The name is taken.', violations: null });
    comp.form.controls.name.setValue('Taken');
    comp.form.controls.name.markAsDirty();
    await comp.onSave();
    expect(comp.errorMessage()).toBe('The name is taken.');
    expect(comp.hasUnsavedChanges()).toBe(true);
  });

  it('is read-only without Scenario edit permission', async () => {
    setUp();
    canEdit.mockReturnValue(false);
    const el = await load();
    expect(el.querySelector('[data-testid="step-save"]')).toBeNull();
    expect((el.querySelector('[data-testid="step-name"]') as HTMLInputElement).readOnly).toBe(true);
  });

  it('reloads the step after an issue on it is resolved, e.g. a rewrite renamed it (#486)', async () => {
    setUp();
    await load();
    scenarioService.getStep.mockResolvedValue({ ...STEP, version: 3, name: 'Member signs in' });
    comp.onIssueResolved();
    await flush();
    expect(scenarioService.getStep).toHaveBeenCalledTimes(2);
    expect(comp.form.controls.name.value).toBe('Member signs in');
    expect(comp.step()?.version).toBe(3);
  });

  it('Back goes to the only scenario using the step', async () => {
    setUp();
    scenarioService.getStep.mockResolvedValue({ ...STEP, usedIn: [STEP.usedIn[0]] });
    await load();
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    comp.onBack();
    expect(navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'scenarios', 3]);
  });

  it('Back returns to the steps list when opened from it (#505)', async () => {
    setUp();
    scenarioService.getStep.mockResolvedValue({ ...STEP, usedIn: [STEP.usedIn[0]] });
    await load();
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const back = vi.spyOn(history, 'back').mockImplementation(() => undefined);
    vi.spyOn(history, 'state', 'get').mockReturnValue({ fromStepList: true, navigationId: 2 });
    comp.onBack();
    expect(back).toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
    vi.restoreAllMocks();
  });

  it('shows the error state when the step cannot be loaded', async () => {
    setUp();
    scenarioService.getStep.mockRejectedValue(new Error('404'));
    const el = await load();
    expect(el.querySelector('[data-testid="step-editor-load-error"]')).not.toBeNull();
  });
});
