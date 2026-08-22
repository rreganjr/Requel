import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { SimpleChange } from '@angular/core';
import { EntitySelectorDialogComponent } from './entity-selector-dialog';
import { GoalService } from '../core/goal.service';
import { StoryService } from '../core/story.service';
import { ActorService } from '../core/actor.service';
import { ScenarioService } from '../core/scenario.service';

const MOCK_GOALS = [
  { id: 1, name: 'Goal A' },
  { id: 2, name: 'Goal B' },
  { id: 3, name: 'Goal C' },
];

describe('EntitySelectorDialogComponent', () => {
  let goalServiceMock: { listGoals: ReturnType<typeof vi.fn> };
  let storyServiceMock: { listStories: ReturnType<typeof vi.fn> };
  let actorServiceMock: { listActors: ReturnType<typeof vi.fn> };
  let scenarioServiceMock: { listScenarios: ReturnType<typeof vi.fn> };
  let comp: EntitySelectorDialogComponent;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;

  beforeEach(() => {
    goalServiceMock = { listGoals: vi.fn().mockResolvedValue(MOCK_GOALS) };
    storyServiceMock = { listStories: vi.fn().mockResolvedValue([]) };
    actorServiceMock = { listActors: vi.fn().mockResolvedValue([]) };
    scenarioServiceMock = { listScenarios: vi.fn().mockResolvedValue([]) };

    TestBed.configureTestingModule({
      imports: [EntitySelectorDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: GoalService, useValue: goalServiceMock },
        { provide: StoryService, useValue: storyServiceMock },
        { provide: ActorService, useValue: actorServiceMock },
        { provide: ScenarioService, useValue: scenarioServiceMock },
      ]
    });
    fixture = TestBed.createComponent(EntitySelectorDialogComponent);
    comp = fixture.componentInstance;
  });

  it('does not load entities when visible is false', async () => {
    comp.visible = false;
    comp.projectName = 'proj1';
    comp.entityType = 'Goal';
    comp.ngOnChanges({ visible: new SimpleChange(undefined, false, true) });
    await fixture.whenStable();
    expect(goalServiceMock.listGoals).not.toHaveBeenCalled();
  });

  it('loads goals when visible becomes true', async () => {
    comp.visible = true;
    comp.projectName = 'proj1';
    comp.entityType = 'Goal';
    comp.ngOnChanges({ visible: new SimpleChange(false, true, false) });
    await fixture.whenStable();
    expect(goalServiceMock.listGoals).toHaveBeenCalledWith('proj1');
    expect(comp.entities().length).toBe(3);
  });

  it('gives the search box an entity-specific accessible name (#138)', async () => {
    comp.visible = true;
    comp.projectName = 'proj1';
    comp.entityType = 'Goal';
    comp.ngOnChanges({ visible: new SimpleChange(false, true, false) });
    await fixture.whenStable();
    fixture.detectChanges();
    // p-dialog uses appendTo="body", so the search input renders in the document.
    const search = document.querySelector('[data-testid="entity-selector-search"]');
    expect(search?.getAttribute('aria-label')).toBe('Search goals');
  });

  it('filters out excludeIds from loaded entities', async () => {
    comp.visible = true;
    comp.projectName = 'proj1';
    comp.entityType = 'Goal';
    comp.excludeIds = [2];
    comp.ngOnChanges({ visible: new SimpleChange(false, true, false) });
    await fixture.whenStable();
    expect(comp.entities().length).toBe(2);
    expect(comp.entities().some(e => e.id === 2)).toBe(false);
  });

  it('displayedEntities filters by typeFilter', async () => {
    scenarioServiceMock.listScenarios.mockResolvedValue([
      { id: 1, name: 'S1', scenarioType: 'Primary' },
      { id: 2, name: 'S2', scenarioType: 'Optional' },
    ]);

    comp.visible = true;
    comp.projectName = 'proj1';
    comp.entityType = 'Scenario';
    comp.ngOnChanges({ visible: new SimpleChange(false, true, false) });
    await fixture.whenStable();

    expect(comp.displayedEntities().length).toBe(2);
    comp.typeFilter.set('Primary');
    expect(comp.displayedEntities().length).toBe(1);
    expect(comp.displayedEntities()[0].name).toBe('S1');
  });

  it('onSelect emits selected entity and closed', () => {
    const selectedSpy = vi.fn();
    const closedSpy = vi.fn();
    comp.selected.subscribe(selectedSpy);
    comp.closed.subscribe(closedSpy);
    const entity = { entityType: 'Goal', id: 1, name: 'Goal A' };
    comp.onSelect({ data: entity });
    expect(selectedSpy).toHaveBeenCalledWith(entity);
    expect(closedSpy).toHaveBeenCalled();
  });
});
