import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { RelationshipSectionComponent } from './app-relationship-section';

interface Row { id: number; name: string; type: string; }

const ROWS: Row[] = [
  { id: 1, name: 'Alpha', type: 'Supports' },
  { id: 2, name: 'Beta', type: 'Conflicts' }
];

@Component({
  standalone: true,
  imports: [RelationshipSectionComponent],
  template: `
    <app-relationship-section
      [title]="title" [showHeading]="showHeading" [headingLevel]="headingLevel"
      [items]="items" [headers]="['Name', 'Type']" [canAdd]="canAdd" [canRemove]="canRemove"
      addLabel="Add Relation" addTestid="rel-add" removeTestid="rel-remove"
      rowTestid="rel-row" testid="rel"
      [emptyText]="emptyText" [unsavedHint]="unsavedHint"
      [removeAriaLabel]="removeAria" [trackBy]="trackById" [colWidths]="colWidths" [description]="description"
      (add)="added = added + 1" (remove)="removed.push($event)">
      <ng-template #row let-item>
        <td class="name-cell">{{ item.name }}</td>
        <td class="type-cell">{{ item.type }}</td>
      </ng-template>
    </app-relationship-section>
  `
})
class HostComponent {
  title = 'Relations';
  showHeading = true;
  headingLevel: 2 | 3 = 2;
  items: Row[] = ROWS;
  canAdd = true;
  canRemove = true;
  colWidths: string[] | undefined = undefined;
  description: string | undefined = undefined;
  emptyText = 'No relations defined.';
  unsavedHint: string | undefined = 'Save first.';
  added = 0;
  removed: Row[] = [];
  removeAria = (r: Row) => 'Remove relation to ' + r.name;
  trackById = (r: Row) => r.id;
}

/** Fresh fixture per test; overrides are applied BEFORE the first detectChanges
 *  so no input mutates after render (which would trip NG0100 under zoneless). */
function setup(over: Partial<HostComponent> = {}) {
  TestBed.configureTestingModule({
    imports: [HostComponent],
    providers: [provideNoopAnimations()]
  });
  const fixture = TestBed.createComponent(HostComponent);
  const host = fixture.componentInstance;
  Object.assign(host, over);
  fixture.detectChanges();
  const el: HTMLElement = fixture.nativeElement;
  const section: RelationshipSectionComponent<Row> =
    fixture.debugElement.children[0].componentInstance;
  return { fixture, host, el, section };
}

describe('RelationshipSectionComponent (issue #130)', () => {
  it('renders the title as an h2 by default', () => {
    const { el } = setup();
    expect(el.querySelector('h2.rq-section-title')?.textContent?.trim()).toBe('Relations');
    expect(el.querySelector('h3.rq-section-title')).toBeNull();
  });

  it('renders the title as an h3 when headingLevel=3', () => {
    const { el } = setup({ headingLevel: 3 });
    expect(el.querySelector('h2.rq-section-title')).toBeNull();
    expect(el.querySelector('h3.rq-section-title')?.textContent?.trim()).toBe('Relations');
  });

  it('hides the heading when showHeading is false', () => {
    const { el } = setup({ showHeading: false });
    expect(el.querySelector('.rq-section-title')).toBeNull();
  });

  it('renders a muted description under the heading when provided', () => {
    const { el } = setup({ description: 'What this list means.' });
    expect(el.querySelector('[data-testid="rel-description"]')?.textContent?.trim())
      .toBe('What this list means.');
  });

  it('omits the description when the heading is hidden', () => {
    const { el } = setup({ showHeading: false, description: 'What this list means.' });
    expect(el.querySelector('[data-testid="rel-description"]')).toBeNull();
  });

  it('omits the Add button when canAdd is false', () => {
    const { el } = setup({ canAdd: false, unsavedHint: undefined });
    expect(el.querySelector('[data-testid="rel-add"]')).toBeNull();
  });

  it('shows the Add button when canAdd, forwards its testid, and emits (add)', () => {
    const { el, host } = setup();
    const addBtn = el.querySelector('[data-testid="rel-add"] button') as HTMLButtonElement;
    expect(addBtn).not.toBeNull();
    addBtn.click();
    expect(host.added).toBe(1);
  });

  it('renders projected row cells and a remove button per row with an accessible name', () => {
    const { el } = setup();
    const rows = el.querySelectorAll('tbody tr[data-testid="rel-row"]');
    expect(rows.length).toBe(2);
    expect(rows[0].querySelector('.name-cell')?.textContent?.trim()).toBe('Alpha');
    expect(rows[0].querySelector('.type-cell')?.textContent?.trim()).toBe('Supports');

    const removeBtn = rows[0].querySelector('[data-testid="rel-remove"] button') as HTMLButtonElement;
    expect(removeBtn).not.toBeNull();
    expect(removeBtn.getAttribute('aria-label')).toBe('Remove relation to Alpha');
  });

  it('emits (remove) with the row when a remove button is clicked', () => {
    const { el, host } = setup();
    const removeBtns = el.querySelectorAll('[data-testid="rel-remove"] button');
    (removeBtns[1] as HTMLButtonElement).click();
    expect(host.removed).toEqual([ROWS[1]]);
  });

  it('renders read-only rows with no actions column when canRemove is false', () => {
    const { el } = setup({ canRemove: false });
    const rows = el.querySelectorAll('tbody tr[data-testid="rel-row"]');
    expect(rows.length).toBe(2);
    expect(rows[0].querySelector('.name-cell')?.textContent?.trim()).toBe('Alpha');
    expect(el.querySelector('[data-testid="rel-remove"]')).toBeNull();
    expect(el.querySelector('.rq-rel-actions-col')).toBeNull();
  });

  it('uses a fixed layout with a colgroup (data widths + reserved actions col) when colWidths is set', () => {
    const { el } = setup({ colWidths: ['', '12rem'] });
    const table = el.querySelector('table.rq-rel-table') as HTMLTableElement;
    expect(table.classList.contains('rq-rel-fixed')).toBe(true);
    const cols = table.querySelectorAll('colgroup col');
    // one col per data column plus one reserved actions col, so read-only siblings align.
    expect(cols.length).toBe(3);
    expect((cols[1] as HTMLElement).style.width).toBe('12rem');
    expect(cols[2].classList.contains('rq-rel-actions-col')).toBe(true);
  });

  it('reserves the actions colgroup col even when read-only, so columns line up with editable siblings', () => {
    const { el } = setup({ colWidths: ['', '12rem'], canRemove: false });
    const cols = el.querySelectorAll('table.rq-rel-table colgroup col');
    expect(cols.length).toBe(3);
    expect(el.querySelector('[data-testid="rel-remove"]')).toBeNull();
  });

  it('shows the unsaved hint when not addable', () => {
    const { el } = setup({ canAdd: false, items: [], unsavedHint: 'Save first.' });
    expect(el.querySelector('[data-testid="rel-hint"]')?.textContent?.trim()).toBe('Save first.');
    expect(el.querySelector('table')).toBeNull();
  });

  it('shows the empty text when addable but empty', () => {
    const { el } = setup({ canAdd: true, items: [] });
    expect(el.querySelector('[data-testid="rel-hint"]')).toBeNull();
    expect(el.querySelector('[data-testid="rel-empty"]')?.textContent?.trim())
      .toBe('No relations defined.');
  });

  it('announceAdded/announceRemoved set the live-region text and focus the Add button', () => {
    const { fixture, el, section } = setup();
    section.announceAdded('Alpha');
    fixture.detectChanges();
    const status = el.querySelector('[data-testid="rel-status"]');
    expect(status?.textContent?.trim()).toBe('Added Alpha');
    expect(document.activeElement).toBe(el.querySelector('[data-testid="rel-add"] button'));

    section.announceRemoved('Beta');
    fixture.detectChanges();
    expect(status?.textContent?.trim()).toBe('Removed Beta');
  });
});
