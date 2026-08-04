import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { MessageService } from 'primeng/api';
import { StoryEditorComponent } from './story-editor';
import { StoryService } from '../../core/story.service';
import { ActorService } from '../../core/actor.service';
import { CommandService } from '../../core/command.service';
import { ProjectService } from '../../core/project.service';
import { PermissionService } from '../../core/permission.service';
import { EventStreamService } from '../../core/event-stream.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const MOCK_ACTORS = [
  { id: 1, version: 0, name: 'Customer', text: null, goals: null, referencedByUseCases: null, referencedByStories: null },
];

const MOCK_STORY = {
  id: 20, version: 4, name: 'User logs in', text: 'A user logs in with credentials.',
  storyType: 'Success', primaryActorName: 'Customer', goals: [], actors: [],
};

const flush = () => new Promise(r => setTimeout(r, 0));

/**
 * `p-confirmdialog` is excluded: PrimeNG marks its host `role="alertdialog"` even when
 * nothing is showing, so axe reports an unnamed dialog on every page that mounts one.
 * Pre-existing and app-wide — see the note on `expectNoAxeViolations` and #139.
 */
const EXCLUDE = ['p-confirmdialog'];

describe('StoryEditorComponent — migrated form accessibility (issue #158)', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: StoryEditorComponent;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1', storyId: '20' }));

    TestBed.configureTestingModule({
      imports: [StoryEditorComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: StoryService, useValue: { getStory: vi.fn().mockResolvedValue(MOCK_STORY) } },
        { provide: ActorService, useValue: { listActors: vi.fn().mockResolvedValue(MOCK_ACTORS) } },
        { provide: CommandService, useValue: { execute: vi.fn().mockResolvedValue({ success: true, entity: MOCK_STORY }) } },
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
    fixture = TestBed.createComponent(StoryEditorComponent);
    comp = fixture.componentInstance;
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  async function renderEdit(): Promise<HTMLElement> {
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  async function renderCreate(): Promise<HTMLElement> {
    paramMap$.next(convertToParamMap({ name: 'proj1', storyId: 'new' }));
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('has no axe-core violations on the edit form', async () => {
    await expectNoAxeViolations(await renderEdit(), EXCLUDE);
  });

  it('has no axe-core violations on the create wizard', async () => {
    await expectNoAxeViolations(await renderCreate(), EXCLUDE);
  });

  it('has no axe-core violations with the name error showing', async () => {
    const el = await renderCreate();
    comp.detailsForm.controls.name.markAsTouched();
    comp.submitted.set(true);
    fixture.detectChanges();

    expect(el.querySelector('[data-testid="field-error"]')).not.toBeNull();
    await expectNoAxeViolations(el, EXCLUDE);
  });

  it('renders every details control as an app-field row, with no form-grid left', async () => {
    const el = await renderEdit();
    expect(el.querySelector('.form-grid')).toBeNull();
    // Name, Type, Primary Actor, Text.
    expect(el.querySelectorAll('app-field').length).toBe(4);
  });

  describe('label and error association', () => {
    it('associates the Name label, error and required state with the input', async () => {
      const el = await renderCreate();
      comp.detailsForm.controls.name.markAsTouched();
      fixture.detectChanges();

      const input = el.querySelector<HTMLInputElement>('[data-testid="story-name"]');
      const error = el.querySelector('[data-testid="field-error"]');
      const label = el.querySelector<HTMLLabelElement>(`label[for="${input?.id}"]`);

      expect(label?.textContent).toContain('Name');
      expect(input?.getAttribute('aria-invalid')).toBe('true');
      expect(input?.getAttribute('aria-describedby')).toContain(error?.id ?? '');
      expect(input?.getAttribute('aria-required')).toBe('true');
    });

    /**
     * The reason Story is a pilot at all: `p-select` is a wrapper whose focusable input
     * is rendered inside it, so `app-field` must not stamp the label/ARIA onto the
     * custom element. Both selects pass `controlId` matching their own `inputId`, and
     * the label must resolve to a real element inside the wrapper.
     */
    it('points the Type label at the input p-select renders inside itself', async () => {
      const el = await renderEdit();
      const label = el.querySelector<HTMLLabelElement>('label[for="storyTypeInput"]');
      const target = el.querySelector('#storyTypeInput');
      const wrapper = el.querySelector('[data-testid="story-type"]');

      expect(label?.textContent).toContain('Type');
      expect(target).not.toBeNull();
      // The id belongs to something inside the wrapper, not the wrapper itself.
      expect(target).not.toBe(wrapper);
      expect(wrapper?.contains(target!)).toBe(true);
    });

    it('points the Primary Actor label at its own select input', async () => {
      const el = await renderEdit();
      const label = el.querySelector<HTMLLabelElement>('label[for="storyPrimaryActorInput"]');
      const target = el.querySelector('#storyPrimaryActorInput');

      expect(label?.textContent).toContain('Primary Actor');
      expect(target).not.toBeNull();
      expect(el.querySelector('[data-testid="story-primary-actor"]')?.contains(target!)).toBe(true);
    });

    it('gives each row a distinct control id', async () => {
      const el = await renderEdit();
      const ids = Array.from(el.querySelectorAll('label[for]')).map(l => l.getAttribute('for'));

      expect(ids.length).toBe(4);
      expect(new Set(ids).size).toBe(4);
    });
  });

  it('reaches Goals and Additional Actors during create without an isNew gate', async () => {
    const el = await renderCreate();
    const keys = Array.from(el.querySelectorAll('[data-testid^="wizard-step-"]')).map(n =>
      n.getAttribute('data-testid')
    );

    expect(keys).toEqual(['wizard-step-details', 'wizard-step-goals', 'wizard-step-actors']);
  });
});
