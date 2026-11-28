import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { EntityEditorBodyDirective, EntityEditorComponent, EntityEditorState } from './entity-editor';
import { AnnotationsSectionComponent } from './annotations-section';
import { SourcesSectionComponent } from './sources-section';
import { expectNoAxeViolations } from './testing/a11y';

@Component({ selector: 'app-annotations-section', standalone: true, template: '<section aria-label="Annotations"></section>' })
class StubAnnotationsComponent {
  @Input() projectName = '';
  @Input() entityType = '';
  @Input() entityId: number | null = null;
  @Output() resolved = new EventEmitter<void>();
}

@Component({ selector: 'app-sources-section', standalone: true, template: '<section aria-label="Sources"></section>' })
class StubSourcesComponent {
  @Input() projectName = '';
  @Input() entityType = '';
  @Input() entityId: number | null = null;
}

/** Counts body instantiations, to show the body is only built once ready. */
let bodyBuilds = 0;
@Component({ selector: 'rq-test-body', standalone: true, template: '<p data-testid="body">form</p>' })
class TestBodyComponent {
  constructor() { bodyBuilds++; }
}

@Component({
  standalone: true,
  imports: [EntityEditorComponent, EntityEditorBodyDirective, TestBodyComponent],
  template: `
    <app-entity-editor [title]="title()" entityLabel="Goal" [state]="state()" [loadError]="loadError()"
                       [listLink]="['/projects', 'Acme', 'goals']"
                       [saveError]="saveError()" [saveRetryable]="true" saveErrorTestid="goal-editor-error"
                       [updateAvailable]="updateAvailable()" updateTestid="goal-update-banner"
                       projectName="Acme" entityType="Goal" [entityId]="entityId()"
                       [showAnnotations]="showAnnotations()" testid="goal-editor"
                       (retryLoad)="events.push('retryLoad')" (retrySave)="events.push('retrySave')"
                       (reloadUpdate)="events.push('reloadUpdate')" (dismissUpdate)="events.push('dismissUpdate')"
                       (issueResolved)="events.push('issueResolved')">
      <button actions type="button" data-testid="goal-delete">Delete</button>
      <ng-template rqEditorBody><rq-test-body /></ng-template>
      <p after data-testid="after">dialogs</p>
    </app-entity-editor>
  `,
})
class HostComponent {
  title = signal('Login works');
  state = signal<EntityEditorState>('loading');
  loadError = signal<string | null>(null);
  saveError = signal<string | null>(null);
  updateAvailable = signal(false);
  entityId = signal<number | null>(7);
  showAnnotations = signal(true);
  events: string[] = [];
}

describe('EntityEditorComponent (#525)', () => {
  function render(setup?: (host: HostComponent) => void) {
    bodyBuilds = 0;
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideRouter([]), provideNoopAnimations()],
    });
    TestBed.overrideComponent(EntityEditorComponent, {
      remove: { imports: [AnnotationsSectionComponent, SourcesSectionComponent] },
      add: { imports: [StubAnnotationsComponent, StubSourcesComponent] },
    });
    const fixture = TestBed.createComponent(HostComponent);
    setup?.(fixture.componentInstance);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const q = (id: string) => el.querySelector(`[data-testid="${id}"]`);
    return { fixture, host: fixture.componentInstance, el, q };
  }

  it('loading: shows the skeleton and the actions, not the body or sections', () => {
    const { el, q } = render();
    expect(q('goal-editor-loading')).toBeTruthy();
    expect(q('goal-delete')).toBeTruthy();
    expect(q('body')).toBeNull();
    expect(bodyBuilds).toBe(0);
    expect(el.querySelector('app-annotations-section')).toBeNull();
    expect(el.querySelector('app-sources-section')).toBeNull();
    expect(el.querySelector('h1')!.textContent).toContain('Login works');
  });

  it('error: shows the error state with Retry, which emits retryLoad', () => {
    const { host, q, fixture } = render(h => { h.state.set('error'); h.loadError.set('Failed to load goal.'); });
    const error = q('goal-editor-load-error')!;
    expect(error.textContent).toContain('Failed to load goal.');
    expect(q('body')).toBeNull();
    (error.querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(host.events).toEqual(['retryLoad']);
  });

  it('ready: builds the body, then Annotations and Sources, in that order', () => {
    const { el, q } = render(h => h.state.set('ready'));
    expect(q('body')).toBeTruthy();
    expect(bodyBuilds).toBe(1);
    const order = Array.from(el.querySelectorAll('[data-testid="body"], app-annotations-section, app-sources-section'))
      .map(n => n.getAttribute('data-testid') ?? n.tagName.toLowerCase());
    expect(order).toEqual(['body', 'app-annotations-section', 'app-sources-section']);
  });

  it('ready: passes the entity to the sections and relays a resolved issue', () => {
    const { el, host, fixture } = render(h => h.state.set('ready'));
    const annotations = fixture.debugElement.query(d => d.name === 'app-annotations-section')
      .componentInstance as StubAnnotationsComponent;
    expect([annotations.projectName, annotations.entityType, annotations.entityId]).toEqual(['Acme', 'Goal', 7]);
    annotations.resolved.emit();
    expect(host.events).toEqual(['issueResolved']);
    expect(el.querySelector('app-sources-section')).toBeTruthy();
  });

  it('ready: showAnnotations=false leaves out Annotations and keeps Sources', () => {
    const { el } = render(h => { h.state.set('ready'); h.showAnnotations.set(false); });
    expect(el.querySelector('app-annotations-section')).toBeNull();
    expect(el.querySelector('app-sources-section')).toBeTruthy();
  });

  it('not-found: titles the page, links to the list, and shows no actions, banners, body or sections', () => {
    const { el, q } = render(h => {
      h.state.set('not-found');
      h.saveError.set('Save failed.');
      h.updateAvailable.set(true);
    });
    expect(el.querySelector('h1')!.textContent).toContain('Goal not found');
    expect(q('goal-editor-not-found')).toBeTruthy();
    const link = q('goal-editor-not-found-list') as HTMLAnchorElement;
    expect(link.textContent).toContain('Back to goals');
    expect(link.getAttribute('href')).toBe('/projects/Acme/goals');
    expect(q('goal-delete')).toBeNull();
    expect(q('goal-editor-error')).toBeNull();
    expect(q('goal-update-banner')).toBeNull();
    expect(q('body')).toBeNull();
    expect(el.querySelector('app-annotations-section')).toBeNull();
    expect(el.querySelector('app-sources-section')).toBeNull();
  });

  it('the [after] slot renders in every state', () => {
    for (const state of ['loading', 'error', 'not-found', 'ready'] as EntityEditorState[]) {
      TestBed.resetTestingModule();
      const { q } = render(h => h.state.set(state));
      expect(q('after')).toBeTruthy();
    }
  });

  it('save error and update banners show with their test ids and emit', () => {
    const { host, q, fixture } = render(h => {
      h.state.set('ready');
      h.saveError.set('Save failed.');
      h.updateAvailable.set(true);
    });
    expect(q('goal-editor-error')!.textContent).toContain('Save failed.');
    const banner = q('goal-update-banner')!;
    expect(banner.textContent).toContain('This goal was changed elsewhere.');
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    buttons.find(b => b.textContent?.includes('Retry'))!.click();
    buttons.find(b => b.textContent?.includes('Reload'))!.click();
    fixture.detectChanges();
    expect(host.events).toContain('retrySave');
    expect(host.events).toContain('reloadUpdate');
  });

  it('the body is built once when the state turns ready', () => {
    const { host, fixture, q } = render();
    expect(bodyBuilds).toBe(0);
    host.state.set('ready');
    fixture.detectChanges();
    expect(q('body')).toBeTruthy();
    expect(bodyBuilds).toBe(1);
  });

  describe('accessibility', () => {
    for (const state of ['ready', 'not-found', 'error', 'loading'] as EntityEditorState[]) {
      it(`has no axe-core violations when ${state}`, async () => {
        const { el } = render(h => { h.state.set(state); h.loadError.set('Failed to load goal.'); });
        await expectNoAxeViolations(el);
      });
    }
  });
});
