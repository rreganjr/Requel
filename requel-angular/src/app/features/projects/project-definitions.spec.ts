import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ProjectDefinitionsComponent } from './project-definitions';
import { AssistantDefinitionsService } from '../../core/assistant-definitions.service';
import { PermissionService } from '../../core/permission.service';
import { ProjectDefinitionDto } from '../../models/assistant-definition';

function definition(over: Partial<ProjectDefinitionDto>): ProjectDefinitionDto {
  return {
    key: 'ai-review-goal', displayName: 'Goal review', kind: 'REVIEW',
    taskType: 'REQUIREMENTS_REVIEW', scope: ['Goal'], contextProviders: ['entity'],
    contextBudgets: {}, instructions: 'Review it.', vocabulary: [], localOnly: false,
    source: 'BUNDLED', version: 3, forkedFromVersion: null, bundledVersion: 3, lockVersion: 0,
    inEffectFor: ['Goal'], enabled: true, ...over,
  };
}

const BUNDLED = definition({});
const FORK = definition({ key: 'ai-review-story', displayName: 'Our story review',
  source: 'PROJECT', version: 2, forkedFromVersion: 1, bundledVersion: 2, lockVersion: 4,
  inEffectFor: ['Story'] });
const OWN = definition({ key: 'house-style', displayName: 'House style', kind: 'POLICY',
  taskType: 'POLICY_REVIEW', scope: [], source: 'PROJECT', version: 1, bundledVersion: null,
  lockVersion: 1, inEffectFor: ['Goal', 'Story'], enabled: false });
const flush = () => new Promise(r => setTimeout(r, 0));

describe('ProjectDefinitionsComponent (#264)', () => {
  let service: Record<string, ReturnType<typeof vi.fn>>;
  let messages: { add: ReturnType<typeof vi.fn> };

  async function render(canManage: boolean) {
    service = {
      list: vi.fn().mockResolvedValue([BUNDLED, FORK, OWN]),
      fork: vi.fn().mockResolvedValue({ success: true, entity: FORK }),
      revert: vi.fn().mockResolvedValue({ success: true }),
      delete: vi.fn().mockResolvedValue({ success: true }),
    };
    messages = { add: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ProjectDefinitionsComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ name: 'Proj A' })) } },
        { provide: AssistantDefinitionsService, useValue: service },
        { provide: PermissionService, useValue: {
          loadForProject: vi.fn().mockResolvedValue(undefined),
          canEdit: (type: string) => canManage && type === 'AssistantDefinition',
        } },
        { provide: MessageService, useValue: messages },
      ],
    });
    const fixture = TestBed.createComponent(ProjectDefinitionsComponent);
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    await flush();
    await flush();
    fixture.detectChanges();
    return { fixture, comp: fixture.componentInstance, el: fixture.nativeElement as HTMLElement,
      router };
  }

  it('lists the definitions by kind with where each came from', async () => {
    const { el, comp } = await render(true);
    expect(service['list']).toHaveBeenCalledWith('Proj A');
    expect(comp.groups().map(g => g.label)).toEqual(['AI review', 'Policies']);
    expect(el.querySelector('[data-testid="definition-source-ai-review-goal"]')?.textContent)
      .toContain('Bundled');
    expect(el.querySelector('[data-testid="definition-source-ai-review-story"]')?.textContent)
      .toContain('Customized');
    expect(el.querySelector('[data-testid="definition-source-house-style"]')?.textContent)
      .toContain('off');
    expect(el.querySelector('[data-testid="definition-newer-ai-review-story"]')?.textContent)
      .toContain('Bundled v2 is newer');
    expect(el.querySelector('[data-testid="definition-newer-ai-review-goal"]')).toBeNull();
    expect(el.querySelector('[data-testid="definitions-risk"]')?.textContent).toContain('misleading');
  });

  it('offers customize, revert or delete according to the source', async () => {
    const { el } = await render(true);
    expect(el.querySelector('[data-testid="definition-fork-ai-review-goal"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="definition-revert-ai-review-story"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="definition-delete-house-style"]')).not.toBeNull();
  });

  it('customizing copies the bundled definition and opens the copy', async () => {
    const { comp, router } = await render(true);
    await comp.fork(BUNDLED);
    expect(service['fork']).toHaveBeenCalledWith('Proj A', 'ai-review-goal');
    expect(router.navigate).toHaveBeenCalledWith(
      ['/projects', 'Proj A', 'definitions', 'ai-review-goal']);
  });

  it('reverts and deletes with the lock version it read, then reloads', async () => {
    const { comp } = await render(true);
    await comp.revert(FORK);
    await comp.remove(OWN);
    expect(service['revert']).toHaveBeenCalledWith('Proj A', 'ai-review-story', 4);
    expect(service['delete']).toHaveBeenCalledWith('Proj A', 'house-style', 1);
    expect(service['list']).toHaveBeenCalledTimes(3);
    expect(messages.add).toHaveBeenCalledTimes(2);
  });

  it('says when someone else changed a definition', async () => {
    const { comp } = await render(true);
    service['revert'].mockResolvedValue({ success: false, error: 'Conflict', status: 409 });
    await comp.revert(FORK);
    expect(comp.errorMessage()).toContain('Someone else changed');
    service['delete'].mockResolvedValue({ success: false, error: 'nope' });
    await comp.remove(OWN);
    expect(comp.errorMessage()).toBe('nope');
  });

  it('goes to a new definition', async () => {
    const { comp, router } = await render(true);
    comp.create();
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'Proj A', 'definitions', 'new']);
  });

  it('shows a retryable error when the list fails', async () => {
    const { comp } = await render(true);
    service['list'].mockRejectedValue(new Error('down'));
    await comp.load();
    expect(comp.loadFailed()).toBe(true);
    expect(comp.errorMessage()).toBe('Failed to load the AI definitions.');
    service['list'].mockResolvedValue([]);
    await comp.load();
    expect(comp.loadFailed()).toBe(false);
  });

  it('is denied without AssistantDefinition[Edit]', async () => {
    const { el } = await render(false);
    expect(el.querySelector('[data-testid="definitions-forbidden"]')).not.toBeNull();
    expect(service['list']).not.toHaveBeenCalled();
  });
});
