import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ProjectDictionaryComponent } from './project-dictionary';
import { DictionaryService } from '../../core/dictionary.service';
import { PermissionService } from '../../core/permission.service';
import { ProjectService } from '../../core/project.service';

const WORDS = [{ id: 1, lemma: 'requel' }];
const IGNORED = [{ id: 7, subject: 'groal', findingType: 'unknown-word', entityType: 'Goal', entityId: 3,
  entityName: 'groal intake', propertyName: 'Name', createdBy: 'project', dateCreated: null }];
const flush = () => new Promise(r => setTimeout(r, 0));

describe('ProjectDictionaryComponent (#319)', () => {
  let dictionary: {
    listProjectWords: ReturnType<typeof vi.fn>;
    addProjectWord: ReturnType<typeof vi.fn>;
    removeProjectWord: ReturnType<typeof vi.fn>;
    listIgnoredFindings: ReturnType<typeof vi.fn>;
    removeIgnoredFinding: ReturnType<typeof vi.fn>;
  };
  let notifyTreeChanged: ReturnType<typeof vi.fn>;

  async function render(canEditProject: boolean) {
    dictionary = {
      listProjectWords: vi.fn().mockResolvedValue(WORDS),
      addProjectWord: vi.fn().mockResolvedValue({ success: true, entity: { id: 2, lemma: 'new' } }),
      removeProjectWord: vi.fn().mockResolvedValue({ success: true }),
      listIgnoredFindings: vi.fn().mockResolvedValue(IGNORED),
      removeIgnoredFinding: vi.fn().mockResolvedValue({ success: true }),
    };
    notifyTreeChanged = vi.fn();
    TestBed.configureTestingModule({
      imports: [ProjectDictionaryComponent],
      providers: [
        provideNoopAnimations(),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ name: 'Proj A' })) } },
        { provide: DictionaryService, useValue: dictionary },
        { provide: PermissionService, useValue: {
          loadForProject: vi.fn().mockResolvedValue(undefined),
          canEdit: (type: string) => canEditProject && type === 'Project',
        } },
        { provide: ProjectService, useValue: { notifyTreeChanged } },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(ProjectDictionaryComponent);
    fixture.detectChanges();
    await flush();
    await flush();
    fixture.detectChanges();
    return { fixture, comp: fixture.componentInstance, el: fixture.nativeElement as HTMLElement };
  }

  it('loads the project words for the route project', async () => {
    const { comp } = await render(true);
    expect(dictionary.listProjectWords).toHaveBeenCalledWith('Proj A');
    expect(comp.words()).toEqual(WORDS);
    expect(comp.loading()).toBe(false);
  });

  it('adds a word, refreshes the list and the nav counts', async () => {
    const { comp } = await render(true);
    await comp.addWord('new');
    expect(dictionary.addProjectWord).toHaveBeenCalledWith('Proj A', 'new');
    expect(dictionary.listProjectWords).toHaveBeenCalledTimes(2);
    expect(notifyTreeChanged).toHaveBeenCalled();
  });

  it('shows a non-field failure at page level', async () => {
    const { comp } = await render(true);
    dictionary.addProjectWord.mockResolvedValue({ success: false, error: 'FORBIDDEN', violations: null });
    await comp.addWord('new');
    expect(comp.errorMessage()).toBe('FORBIDDEN');
    expect(notifyTreeChanged).not.toHaveBeenCalled();
  });

  it('removes a word and refreshes', async () => {
    const { comp } = await render(true);
    await comp.removeWord(WORDS[0]);
    expect(dictionary.removeProjectWord).toHaveBeenCalledWith('Proj A', 1);
    expect(dictionary.listProjectWords).toHaveBeenCalledTimes(2);
    expect(notifyTreeChanged).toHaveBeenCalled();
  });

  it('keeps a failed remove message on screen after the reload', async () => {
    const { comp } = await render(true);
    dictionary.removeProjectWord.mockResolvedValue({ success: false, error: 'No word found' });
    await comp.removeWord(WORDS[0]);
    expect(comp.errorMessage()).toBe('No word found');
    expect(notifyTreeChanged).not.toHaveBeenCalled();
  });

  it('is read-only without Project[Edit]', async () => {
    const { el } = await render(false);
    expect(el.querySelector('[data-testid="project-dictionary-add-form"]')).toBeNull();
    expect(el.querySelector('[data-testid="project-dictionary-remove"]')).toBeNull();
    expect(el.querySelectorAll('[data-testid="project-dictionary-lemma"]').length).toBe(1);
  });

  it('offers the add form and remove buttons with Project[Edit]', async () => {
    const { el } = await render(true);
    expect(el.querySelector('[data-testid="project-dictionary-add-form"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="project-dictionary-remove"]')).not.toBeNull();
  });

  it('shows a retryable error when the list fails to load', async () => {
    const { comp } = await render(true);
    dictionary.listProjectWords.mockRejectedValue(new Error('down'));
    await comp.load();
    expect(comp.errorMessage()).toBe('Failed to load the project dictionary.');
    expect(comp.loadFailed()).toBe(true);
  });

  it('loads the ignored findings for the route project (#320)', async () => {
    const { comp, el } = await render(true);
    expect(dictionary.listIgnoredFindings).toHaveBeenCalledWith('Proj A');
    expect(comp.ignoredFindings()).toEqual(IGNORED);
    expect(el.querySelector('[data-testid="ignored-findings-title"]')?.textContent).toContain('Ignored findings');
  });

  it('removes an ignored finding and reloads both lists (#320)', async () => {
    const { comp } = await render(true);
    await comp.removeIgnoredFinding(IGNORED[0]);
    expect(dictionary.removeIgnoredFinding).toHaveBeenCalledWith('Proj A', 7);
    expect(dictionary.listIgnoredFindings).toHaveBeenCalledTimes(2);
    expect(dictionary.listProjectWords).toHaveBeenCalledTimes(2);
  });

  it('keeps a failed ignored-finding remove message on screen (#320)', async () => {
    const { comp } = await render(true);
    dictionary.removeIgnoredFinding.mockResolvedValue({ success: false, error: 'FORBIDDEN' });
    await comp.removeIgnoredFinding(IGNORED[0]);
    expect(comp.errorMessage()).toBe('FORBIDDEN');
  });
});
