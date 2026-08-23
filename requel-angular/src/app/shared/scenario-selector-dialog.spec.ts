import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { SimpleChange } from '@angular/core';
import { ScenarioSelectorDialogComponent } from './scenario-selector-dialog';
import { ScenarioService } from '../core/scenario.service';
import { CommandService } from '../core/command.service';

const MOCK_SCENARIOS = [
  { id: 1, version: 0, name: 'Main flow', text: null, scenarioType: 'Primary', createdBy: null, steps: null },
  { id: 2, version: 0, name: 'Alt flow', text: null, scenarioType: 'Alternative', createdBy: null, steps: null },
  { id: 3, version: 0, name: 'Error flow', text: null, scenarioType: 'Exception', createdBy: null, steps: null },
];

describe('ScenarioSelectorDialogComponent', () => {
  let scenarioServiceMock: { listScenarios: ReturnType<typeof vi.fn> };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  let comp: ScenarioSelectorDialogComponent;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;

  beforeEach(() => {
    scenarioServiceMock = { listScenarios: vi.fn().mockResolvedValue(MOCK_SCENARIOS) };
    commandServiceMock = {
      execute: vi.fn().mockResolvedValue({
        success: true,
        entity: { id: 99, version: 0, name: 'New Scenario', text: null, scenarioType: 'Primary', createdBy: null, steps: null }
      })
    };

    TestBed.configureTestingModule({
      imports: [ScenarioSelectorDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: ScenarioService, useValue: scenarioServiceMock },
        { provide: CommandService, useValue: commandServiceMock },
      ]
    });
    fixture = TestBed.createComponent(ScenarioSelectorDialogComponent);
    comp = fixture.componentInstance;
  });

  it('does not load scenarios when visible is false', async () => {
    comp.visible = false;
    comp.projectName = 'proj1';
    comp.ngOnChanges({ visible: new SimpleChange(undefined, false, true) });
    await fixture.whenStable();
    expect(scenarioServiceMock.listScenarios).not.toHaveBeenCalled();
  });

  it('loads scenarios when visible becomes true', async () => {
    comp.visible = true;
    comp.projectName = 'proj1';
    comp.ngOnChanges({ visible: new SimpleChange(false, true, false) });
    await fixture.whenStable();
    expect(scenarioServiceMock.listScenarios).toHaveBeenCalledWith('proj1');
    expect(comp.scenarios().length).toBe(3);
  });

  it('filters out excludeIds from loaded scenarios', async () => {
    comp.visible = true;
    comp.projectName = 'proj1';
    comp.excludeIds = [2];
    comp.ngOnChanges({ visible: new SimpleChange(false, true, false) });
    await fixture.whenStable();
    expect(comp.scenarios().length).toBe(2);
    expect(comp.scenarios().some(s => s.id === 2)).toBe(false);
  });

  it('onSelect emits selected scenario ref and closed', () => {
    const selectedSpy = vi.fn();
    const closedSpy = vi.fn();
    comp.selected.subscribe(selectedSpy);
    comp.closed.subscribe(closedSpy);
    comp.onSelect({ data: MOCK_SCENARIOS[0] });
    expect(selectedSpy).toHaveBeenCalledWith({ id: 1, name: 'Main flow', scenarioType: 'Primary' });
    expect(closedSpy).toHaveBeenCalled();
  });

  it('onCreateAndAdd calls commandService.execute with EditScenario command', async () => {
    comp.projectName = 'proj1';
    comp.createForm.controls.name.setValue('New Scenario');
    comp.createForm.controls.scenarioType.setValue('Primary');
    const selectedSpy = vi.fn();
    comp.selected.subscribe(selectedSpy);
    await comp.onCreateAndAdd();
    expect(commandServiceMock.execute).toHaveBeenCalledWith('EditScenario', {
      projectName: 'proj1',
      name: 'New Scenario',
      scenarioTypeName: 'Primary',
      steps: []
    });
    expect(selectedSpy).toHaveBeenCalledWith({ id: 99, name: 'New Scenario', scenarioType: 'Primary' });
  });

  it('onCreateAndAdd sets createError when command fails', async () => {
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Scenario name taken' });
    comp.projectName = 'proj1';
    comp.createForm.controls.name.setValue('Duplicate');
    await comp.onCreateAndAdd();
    expect(comp.createError()).toBe('Scenario name taken');
  });

  it('onCreateAndAdd shows the required error and does not call the command when the name is blank', async () => {
    comp.projectName = 'proj1';
    comp.createForm.controls.name.setValue('   ');
    await comp.onCreateAndAdd();
    expect(commandServiceMock.execute).not.toHaveBeenCalled();
    expect(comp.createForm.controls.name.invalid).toBe(true);
    expect(comp.createForm.controls.name.touched).toBe(true);
  });
});
