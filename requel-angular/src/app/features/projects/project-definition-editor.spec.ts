import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ProjectDefinitionEditorComponent } from './project-definition-editor';
import { AssistantDefinitionsService } from '../../core/assistant-definitions.service';
import { PermissionService } from '../../core/permission.service';
import { ProjectDefinitionDto } from '../../models/assistant-definition';

function definition(over: Partial<ProjectDefinitionDto>): ProjectDefinitionDto {
  return {
    key: 'ai-review-goal', displayName: 'Goal review', kind: 'REVIEW',
    taskType: 'REQUIREMENTS_REVIEW', scope: ['Goal'], contextProviders: ['entity'],
    contextBudgets: {}, instructions: 'Review it.',
    vocabulary: [{ type: 'AMBIGUOUS', description: 'unclear', category: 'quality' }],
    localOnly: false, source: 'PROJECT', version: 2, forkedFromVersion: 3, bundledVersion: 4,
    lockVersion: 5, inEffectFor: ['Goal'], enabled: true, ...over,
  };
}

const FORK = definition({});
const BUNDLED = definition({ source: 'BUNDLED', version: 4, forkedFromVersion: null,
  lockVersion: 0, instructions: 'The bundled instructions.' });
const flush = () => new Promise(r => setTimeout(r, 0));

describe('ProjectDefinitionEditorComponent (#264)', () => {
  let service: Record<string, ReturnType<typeof vi.fn>>;

  async function render(key: string, canManage = true, detail = { definition: FORK, bundled: BUNDLED }) {
    service = {
      get: vi.fn().mockResolvedValue(detail),
      create: vi.fn().mockResolvedValue({ success: true, entity: definition({ key: 'house-style',
        displayName: 'House style' }) }),
      edit: vi.fn().mockResolvedValue({ success: true, entity: definition({ version: 3,
        lockVersion: 6, instructions: 'Changed.' }) }),
      fork: vi.fn().mockResolvedValue({ success: true, entity: FORK }),
      revert: vi.fn().mockResolvedValue({ success: true }),
      delete: vi.fn().mockResolvedValue({ success: true }),
    };
    TestBed.configureTestingModule({
      imports: [ProjectDefinitionEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: {
          paramMap: of(convertToParamMap({ name: 'Proj A', key })) } },
        { provide: AssistantDefinitionsService, useValue: service },
        { provide: PermissionService, useValue: {
          loadForProject: vi.fn().mockResolvedValue(undefined),
          canEdit: (type: string) => canManage && type === 'AssistantDefinition',
        } },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(ProjectDefinitionEditorComponent);
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    await flush();
    await flush();
    fixture.detectChanges();
    return { fixture, comp: fixture.componentInstance, el: fixture.nativeElement as HTMLElement,
      router };
  }

  it('shows a project copy beside its bundled baseline and says the bundled one is newer', async () => {
    const { el, comp } = await render('ai-review-goal');
    expect(service['get']).toHaveBeenCalledWith('Proj A', 'ai-review-goal');
    expect(comp.draft.instructions).toBe('Review it.');
    expect(el.querySelector('[data-testid="definition-baseline"]')?.textContent)
      .toContain('The bundled instructions.');
    expect(el.querySelector('[data-testid="definition-newer"]')?.textContent).toContain('v4');
    expect(el.querySelector('[data-testid="definition-revert"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="definition-delete"]')).toBeNull();
  });

  it('saves an edit with the lock version it read', async () => {
    const { comp } = await render('ai-review-goal');
    comp.draft.instructions = 'Changed.';
    await comp.save();
    expect(service['edit']).toHaveBeenCalledWith('Proj A', 5,
      expect.objectContaining({ key: 'ai-review-goal', instructions: 'Changed.' }));
    expect(comp.current()?.lockVersion).toBe(6);
  });

  it('puts each refused rule on its field and the rest under the form', async () => {
    const { comp, fixture, el } = await render('ai-review-goal');
    service['edit'].mockResolvedValue({ success: false, error: 'Validation failed', violations: [
      { field: 'instructions', message: 'instructions are too long' },
      { field: 'executorBean', message: "can't name an executor bean" },
    ] });
    await comp.save();
    fixture.detectChanges();
    expect(comp.fieldErrors()['instructions']).toBe('instructions are too long');
    expect(el.querySelector('[data-testid="error-instructions"]')).not.toBeNull();
    expect(comp.otherErrors()).toEqual(["can't name an executor bean"]);
  });

  it('says when someone else changed it', async () => {
    const { comp } = await render('ai-review-goal');
    service['edit'].mockResolvedValue({ success: false, error: 'Conflict', violations: null,
      status: 409 });
    await comp.save();
    expect(comp.errorMessage()).toContain('Someone else changed');
    service['edit'].mockResolvedValue({ success: false, error: 'Boom', violations: [] });
    await comp.save();
    expect(comp.errorMessage()).toBe('Boom');
  });

  it('reverts a copy and deletes a project definition, then goes back to the list', async () => {
    const { comp, router } = await render('ai-review-goal');
    await comp.revert();
    expect(service['revert']).toHaveBeenCalledWith('Proj A', 'ai-review-goal', 5);
    await comp.remove();
    expect(service['delete']).toHaveBeenCalledWith('Proj A', 'ai-review-goal', 5);
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'Proj A', 'definitions']);
    service['delete'].mockResolvedValue({ success: false, error: 'nope', violations: null });
    await comp.remove();
    expect(comp.errorMessage()).toBe('nope');
  });

  it('shows a bundled definition read-only with Customize', async () => {
    const { el, comp } = await render('ai-review-goal', true, { definition: BUNDLED, bundled: BUNDLED });
    expect(comp.readOnly()).toBe(true);
    expect(el.querySelector('[data-testid="definition-bundled-note"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="definition-save"]')).toBeNull();
    await comp.fork();
    expect(service['fork']).toHaveBeenCalledWith('Proj A', 'ai-review-goal');
    expect(service['get']).toHaveBeenCalledTimes(2);
  });

  it('creates a new policy and opens it', async () => {
    const { comp, router, el } = await render('new');
    expect(comp.isNew()).toBe(true);
    expect(el.querySelector('[data-testid="definition-key"]')).not.toBeNull();
    comp.draft.key = 'house-style';
    comp.draft.displayName = 'House style';
    comp.draft.instructions = 'Every goal names its owner.';
    comp.toggle(comp.draft.scope, 'Goal', true);
    comp.toggle(comp.draft.scope, 'Goal', true);
    comp.addVocabulary();
    comp.removeVocabulary(1);
    await comp.save();
    expect(service['create']).toHaveBeenCalledWith('Proj A', expect.objectContaining({
      kind: 'POLICY', key: 'house-style', scope: ['Goal'] }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'Proj A', 'definitions', 'house-style']);
  });

  it('switches the choices when the kind changes', async () => {
    const { comp } = await render('new');
    comp.draft.kind = 'CORPUS';
    comp.kindChanged();
    expect(comp.scopeOptions()).toEqual(['PROJECT', 'GOAL', 'USE_CASE']);
    expect(comp.providerOptions()).toEqual(['corpus-index', 'corpus-candidates']);
    expect(comp.draft.contextProviders).toEqual(['corpus-index', 'corpus-candidates']);
    comp.toggle(comp.draft.contextProviders, 'corpus-index', false);
    expect(comp.draft.contextProviders).toEqual(['corpus-candidates']);
  });

  it('is denied without AssistantDefinition[Edit]', async () => {
    const { el } = await render('ai-review-goal', false);
    expect(el.querySelector('[data-testid="definition-forbidden"]')).not.toBeNull();
    expect(service['get']).not.toHaveBeenCalled();
  });

  it('shows a retryable error when the definition fails to load', async () => {
    const { comp } = await render('ai-review-goal');
    service['get'].mockRejectedValue(new Error('down'));
    await comp.load();
    expect(comp.loadFailed()).toBe(true);
  });
});
