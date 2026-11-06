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

  function setUp(): void {
    scenarioService = { getStep: vi.fn().mockResolvedValue(STEP) };
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

  it('loads the step, lists the scenarios using it, and places the annotations', async () => {
    setUp();
    const el = await load();
    expect(scenarioService.getStep).toHaveBeenCalledWith('proj1', 9);
    expect(comp.form.getRawValue()).toEqual({ name: 'Member signs in', scenarioType: 'Primary', text: 'with a card' });
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
      scenarioTypeName: 'Primary', text: 'with a card',
    });
    expect(comp.hasUnsavedChanges()).toBe(false);
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

  it('Back goes to the only scenario using the step', async () => {
    setUp();
    scenarioService.getStep.mockResolvedValue({ ...STEP, usedIn: [STEP.usedIn[0]] });
    await load();
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    comp.onBack();
    expect(navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'scenarios', 3]);
  });

  it('shows the error state when the step cannot be loaded', async () => {
    setUp();
    scenarioService.getStep.mockRejectedValue(new Error('404'));
    const el = await load();
    expect(el.querySelector('[data-testid="step-editor-load-error"]')).not.toBeNull();
  });
});
