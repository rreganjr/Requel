import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { RelationshipSectionComponent } from './app-relationship-section';
import { expectNoAxeViolations } from './testing/a11y';

interface Row { id: number; name: string; }

@Component({
  standalone: true,
  imports: [RelationshipSectionComponent],
  template: `
    <app-relationship-section
      [title]="title" [items]="items" [headers]="['Name']" [canAdd]="canAdd"
      addLabel="Add Goal" addTestid="a11y-add" removeTestid="a11y-remove" testid="a11y"
      [emptyText]="'No goals associated.'" [unsavedHint]="unsavedHint"
      [removeAriaLabel]="removeAria">
      <ng-template #row let-item>
        <td>{{ item.name }}</td>
      </ng-template>
    </app-relationship-section>
  `
})
class HostComponent {
  title = 'Goals';
  items: Row[] = [{ id: 1, name: 'Alpha' }, { id: 2, name: 'Beta' }];
  canAdd = true;
  unsavedHint: string | undefined;
  removeAria = (r: Row) => 'Remove goal ' + r.name;
}

describe('RelationshipSectionComponent — accessibility', () => {
  function render(over: Partial<HostComponent> = {}): HTMLElement {
    TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideNoopAnimations()]
    });
    const fixture = TestBed.createComponent(HostComponent);
    Object.assign(fixture.componentInstance, over);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('has no axe-core violations with a populated list (Add + remove buttons, table headers)', async () => {
    await expectNoAxeViolations(render());
  });

  it('has no axe-core violations in the unsaved-hint state', async () => {
    await expectNoAxeViolations(render({ canAdd: false, items: [], unsavedHint: 'Save first to add goals.' }));
  });

  it('has no axe-core violations in the empty state', async () => {
    await expectNoAxeViolations(render({ items: [] }));
  });

  it('gives the Add and remove controls accessible names', () => {
    const el = render();
    expect(el.querySelector('[data-testid="a11y-add"] button')?.getAttribute('aria-label')
      ?? el.querySelector('[data-testid="a11y-add"] button')?.textContent?.trim()).toBeTruthy();
    const remove = el.querySelector('[data-testid="a11y-remove"] button');
    expect(remove?.getAttribute('aria-label')).toBe('Remove goal Alpha');
  });
});
