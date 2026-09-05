import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, BehaviorSubject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { ProjectWorkspaceComponent } from './project-workspace';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { ProjectDto } from '../../models/project';

function makeProject(over: Partial<ProjectDto> = {}): ProjectDto {
  return {
    id: 1, version: 0, name: 'Acme', description: null, organizationName: null,
    createdBy: null, status: null,
    stakeholderCount: 0, goalCount: 0, storyCount: 0, actorCount: 0,
    scenarioCount: 0, useCaseCount: 0, glossaryTermCount: 0, reportGeneratorCount: 0,
    ...over,
  } as ProjectDto;
}

describe('ProjectWorkspaceComponent (#154)', () => {
  let getProject: ReturnType<typeof vi.fn>;
  let httpGet: ReturnType<typeof vi.fn>;
  let canDeleteFn: ReturnType<typeof vi.fn>;
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  function setup() {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'Acme' }));
    TestBed.configureTestingModule({
      imports: [ProjectWorkspaceComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: ProjectService, useValue: { getProject } },
        { provide: HttpClient, useValue: { get: httpGet } },
        { provide: PermissionService, useValue: {
          loadForProject: vi.fn().mockResolvedValue(undefined),
          canDelete: canDeleteFn,
        } },
      ],
    });
    const fixture = TestBed.createComponent(ProjectWorkspaceComponent);
    return fixture;
  }

  const flush = () => new Promise(r => setTimeout(r, 0));

  beforeEach(() => {
    getProject = vi.fn().mockResolvedValue(makeProject({ goalCount: 3, stakeholderCount: 2 }));
    httpGet = vi.fn().mockReturnValue(of([]));
    canDeleteFn = vi.fn().mockReturnValue(false);
  });

  it('renders a count card per artifact type with the project counts', async () => {
    const fixture = setup();
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    const goals = el.querySelector('[data-testid="count-goals"]') as HTMLAnchorElement;
    expect(goals?.textContent).toContain('3');
    expect(goals?.getAttribute('href')).toContain('/goals');
    expect(el.querySelectorAll('.count-card').length).toBe(8);
    // sanity on the model too
    expect(fixture.componentInstance.counts().find(c => c.segment === 'goals')?.count).toBe(3);
  });

  it('summarizes open issues with a must-resolve count', async () => {
    httpGet = vi.fn().mockReturnValue(of([
      { mustBeResolved: true }, { mustBeResolved: false }, { mustBeResolved: true },
    ]));
    const fixture = setup();
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    expect(fixture.componentInstance.openIssueCount()).toBe(3);
    expect(fixture.componentInstance.mustResolveCount()).toBe(2);
    const panel = fixture.nativeElement.querySelector('[data-testid="workspace-open-issues"]');
    expect(panel?.textContent).toContain('3 open');
  });

  it('derives next actions: resolve blockers, add first goal/stakeholder', async () => {
    getProject = vi.fn().mockResolvedValue(makeProject({ goalCount: 0, stakeholderCount: 0 }));
    httpGet = vi.fn().mockReturnValue(of([{ mustBeResolved: true }]));
    const fixture = setup();
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    const labels = fixture.componentInstance.nextActions().map(a => a.label);
    expect(labels).toContain('Resolve 1 blocking issue');
    expect(labels).toContain('Add your first goal');
    expect(labels).toContain('Add a stakeholder');
  });

  it('has an Edit action pointing at the editor route', async () => {
    const fixture = setup();
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    const edit = fixture.nativeElement.querySelector('[data-testid="workspace-edit"]') as HTMLAnchorElement;
    expect(edit).not.toBeNull();
    expect(edit.getAttribute('href')).toContain('/edit');
  });

  it('shows an error surface when the project fails to load', async () => {
    getProject = vi.fn().mockRejectedValue(new Error('boom'));
    const fixture = setup();
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    expect(fixture.componentInstance.errorMessage()).toBe('Failed to load the project workspace.');
  });

  it('hides the Delete action when the user lacks Project[Delete]', async () => {
    canDeleteFn = vi.fn().mockReturnValue(false);
    const fixture = setup();
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    expect(fixture.componentInstance.canDelete()).toBe(false);
    expect(fixture.nativeElement.querySelector('[data-testid="workspace-delete"]')).toBeNull();
  });

  it('shows the Delete action and opens the dialog when the user holds Project[Delete]', async () => {
    canDeleteFn = vi.fn().mockReturnValue(true);
    const fixture = setup();
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    expect(canDeleteFn).toHaveBeenCalledWith('Project');
    const del = fixture.nativeElement.querySelector('[data-testid="workspace-delete"]');
    expect(del).not.toBeNull();

    // Assert the state onDeleteProject sets, without re-rendering (the dialog
    // child pulls CommandService/EventStreamService, out of scope for this spec).
    fixture.componentInstance.onDeleteProject();
    expect(fixture.componentInstance.deleteVisible()).toBe(true);
    expect(fixture.componentInstance.deleteTarget()).toEqual({ name: 'Acme', version: 0 });
  });
});
