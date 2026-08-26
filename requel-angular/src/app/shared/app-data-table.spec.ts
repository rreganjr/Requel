import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Menu } from 'primeng/menu';
import { provideRouter } from '@angular/router';
import { AppDataTableComponent, DataTableColumn, RowAction } from './app-data-table';

interface Row {
  id: number;
  name: string;
  createdBy: string;
  nested?: { label: string };
}

const ROWS: Row[] = [
  { id: 1, name: 'Alpha', createdBy: 'alice', nested: { label: 'A-label' } },
  { id: 2, name: 'Beta', createdBy: 'bob', nested: { label: 'B-label' } }
];

const COLUMNS: DataTableColumn<Row>[] = [
  { field: 'name', header: 'Name', sortable: true },
  { field: 'createdBy', header: 'Created By' },
  { field: 'nested.label', header: 'Label' }
];

/** A Menu stub whose toggle we can assert without opening the real popup. */
const menuStub = () => ({ toggle: vi.fn() }) as unknown as Menu;

describe('AppDataTableComponent (issue #157)', () => {
  function make() {
    TestBed.configureTestingModule({
      imports: [AppDataTableComponent],
      providers: [provideNoopAnimations()]
    });
    const fixture = TestBed.createComponent(AppDataTableComponent<Row>);
    const comp = fixture.componentInstance;
    comp.value = ROWS;
    comp.columns = COLUMNS;
    return { fixture, comp };
  }

  it('renders a header per column and default text cells (incl. dotted paths)', () => {
    const { fixture } = make();
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    const headers = Array.from(el.querySelectorAll('thead th')).map(h => h.textContent?.trim());
    expect(headers).toContain('Name');
    expect(headers).toContain('Created By');
    expect(headers).toContain('Label');

    const firstRowCells = Array.from(el.querySelectorAll('tbody tr')[0].querySelectorAll('td'))
      .map(td => td.textContent?.trim());
    expect(firstRowCells).toContain('Alpha');
    expect(firstRowCells).toContain('alice');
    expect(firstRowCells).toContain('A-label');
  });

  it('marks a sortable column with a sort icon', () => {
    const { fixture } = make();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('p-sorticon')).not.toBeNull();
  });

  it('emits rowClick when a row is clicked', () => {
    const { fixture, comp } = make();
    const seen: Row[] = [];
    comp.rowClick.subscribe((r: Row) => seen.push(r));
    fixture.detectChanges();

    fixture.nativeElement.querySelector('tbody tr.dt-row').dispatchEvent(new MouseEvent('click'));
    expect(seen).toEqual([ROWS[0]]);
  });

  it('filters via the global filter when searching', () => {
    const { fixture, comp } = make();
    fixture.detectChanges();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const dt = (comp as any).dt;
    const spy = vi.spyOn(dt, 'filterGlobal');
    comp.onSearch('beta');
    expect(spy).toHaveBeenCalledWith('beta', 'contains');
  });

  it('renders an empty state when there are no rows', () => {
    const { fixture, comp } = make();
    comp.value = [];
    comp.emptyTitle = 'Nothing yet';
    fixture.detectChanges();

    const empty = fixture.nativeElement.querySelector('[data-testid="data-table-empty"]');
    expect(empty).not.toBeNull();
    expect(empty.textContent).toContain('Nothing yet');
  });

  describe('selection column', () => {
    it('renders checkbox controls and emits selectionChange', () => {
      const { fixture, comp } = make();
      comp.selectable = true;
      const emitted: Row[][] = [];
      comp.selectionChange.subscribe((s: Row[]) => emitted.push(s));
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('p-tableheadercheckbox')).not.toBeNull();
      expect(fixture.nativeElement.querySelectorAll('p-tablecheckbox').length).toBe(ROWS.length);

      comp.onSelectionChange([ROWS[1]]);
      expect(emitted).toEqual([[ROWS[1]]]);
    });

    it('has no selection column by default', () => {
      const { fixture } = make();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('p-tableheadercheckbox')).toBeNull();
    });
  });

  it('omits the actions column when defaultActions is false and no override is given', () => {
    const { fixture, comp } = make();
    comp.defaultActions = false;
    fixture.detectChanges();
    expect(comp.hasRowActions).toBe(false);
    expect(fixture.nativeElement.querySelector('[data-testid="data-table-row-actions"]')).toBeNull();
  });

  it('does not emit rowClick or mark rows clickable when rowClickable is false', () => {
    const { fixture, comp } = make();
    comp.rowClickable = false;
    const seen: Row[] = [];
    comp.rowClick.subscribe((r: Row) => seen.push(r));
    fixture.detectChanges();

    const row = fixture.nativeElement.querySelector('tbody tr.dt-row');
    expect(row.classList.contains('dt-row--clickable')).toBe(false);
    row.dispatchEvent(new MouseEvent('click'));
    expect(seen).toEqual([]);
  });

  describe('row-action menu', () => {
    it('gives the ⋯ trigger an accessible name (#136/#137)', () => {
      const { fixture } = make();
      fixture.detectChanges();
      const trigger = fixture.nativeElement.querySelector('[data-testid="data-table-row-actions"]');
      expect(trigger.getAttribute('aria-label')).toBe('Row actions');
    });

    it('builds the default Open/Edit/Delete menu and wires the outputs', () => {
      const { comp } = make();
      const opened: Row[] = [];
      const edited: Row[] = [];
      const deleted: Row[] = [];
      comp.open.subscribe((r: Row) => opened.push(r));
      comp.edit.subscribe((r: Row) => edited.push(r));
      comp.delete.subscribe((r: Row) => deleted.push(r));

      const menu = menuStub();
      comp.openRowMenu(ROWS[0], new MouseEvent('click'), menu);
      expect(menu.toggle).toHaveBeenCalled();
      expect(comp.menuModel.map(m => m.label)).toEqual(['Open', 'Edit', 'Delete']);

      comp.menuModel.forEach(m => m.command?.({} as never));
      expect(opened).toEqual([ROWS[0]]);
      expect(edited).toEqual([ROWS[0]]);
      expect(deleted).toEqual([ROWS[0]]);
    });

    it('gates Edit and Delete by canEdit/canDelete', () => {
      const { comp } = make();
      comp.canEdit = false;
      comp.canDelete = false;
      comp.openRowMenu(ROWS[0], new MouseEvent('click'), menuStub());
      expect(comp.menuModel.map(m => m.label)).toEqual(['Open']);
    });

    it('replaces the default menu with the [rowActions] input (respecting visible)', () => {
      const { comp } = make();
      const archived: Row[] = [];
      const actions: RowAction<Row>[] = [
        { label: 'Archive', command: r => archived.push(r) },
        { label: 'Restore', command: () => {}, visible: r => r.id === 99 }
      ];
      comp.rowActions = actions;
      comp.openRowMenu(ROWS[0], new MouseEvent('click'), menuStub());

      expect(comp.menuModel.map(m => m.label)).toEqual(['Archive']);
      comp.menuModel[0].command?.({} as never);
      expect(archived).toEqual([ROWS[0]]);
    });
  });
});

/** Host exercising the custom cell + full row-action template overrides. */
@Component({
  standalone: true,
  imports: [AppDataTableComponent],
  template: `
    <app-data-table [value]="rows" [columns]="columns">
      <ng-template #rowActions let-row>
        <button type="button" data-testid="custom-action">Do {{ row.name }}</button>
      </ng-template>
    </app-data-table>
  `
})
class OverrideHostComponent {
  rows: Row[] = ROWS;
  columns: DataTableColumn<Row>[] = [{ field: 'name', header: 'Name' }];
}

describe('AppDataTableComponent template overrides', () => {
  it('replaces the ⋯ menu with a projected <ng-template #rowActions>', () => {
    TestBed.configureTestingModule({
      imports: [OverrideHostComponent],
      providers: [provideNoopAnimations()]
    });
    const fixture = TestBed.createComponent(OverrideHostComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('[data-testid="data-table-row-actions"]')).toBeNull();
    const custom = el.querySelectorAll('[data-testid="custom-action"]');
    expect(custom.length).toBe(ROWS.length);
    expect(custom[0].textContent?.trim()).toBe('Do Alpha');
  });
});

describe('AppDataTableComponent link column (issue #129)', () => {
  const LINK_COLUMNS: DataTableColumn<Row>[] = [
    { field: 'name', header: 'Name', link: r => ['/projects', r.name] },
    { field: 'createdBy', header: 'Created By' }
  ];

  function make() {
    TestBed.configureTestingModule({
      imports: [AppDataTableComponent],
      providers: [provideNoopAnimations(), provideRouter([{ path: '**', children: [] }])]
    });
    const fixture = TestBed.createComponent(AppDataTableComponent<Row>);
    const comp = fixture.componentInstance;
    comp.value = ROWS;
    comp.columns = LINK_COLUMNS;
    return { fixture, comp };
  }

  it('renders a name cell as a real routerLink anchor, other cells stay plain text', () => {
    const { fixture } = make();
    fixture.detectChanges();
    const firstRow = fixture.nativeElement.querySelectorAll('tbody tr')[0] as HTMLElement;

    const link = firstRow.querySelector('a.dt-link') as HTMLAnchorElement | null;
    expect(link).not.toBeNull();
    expect(link!.textContent?.trim()).toBe('Alpha');
    expect(link!.getAttribute('href')).toBe('/projects/Alpha');

    const createdCell = Array.from(firstRow.querySelectorAll('td'))
      .find(td => td.textContent?.trim() === 'alice');
    expect(createdCell?.querySelector('a')).toBeNull();
  });

  it('does not emit rowClick when the name link is clicked (stopPropagation)', () => {
    const { fixture, comp } = make();
    const seen: Row[] = [];
    comp.rowClick.subscribe((r: Row) => seen.push(r));
    fixture.detectChanges();

    const link = fixture.nativeElement.querySelector('a.dt-link') as HTMLAnchorElement;
    link.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    expect(seen).toEqual([]);
  });

  it('falls back to plain text when the link factory returns null', () => {
    const { fixture, comp } = make();
    comp.columns = [
      { field: 'name', header: 'Name', link: () => null },
      { field: 'createdBy', header: 'Created By' }
    ];
    fixture.detectChanges();
    const firstRow = fixture.nativeElement.querySelectorAll('tbody tr')[0] as HTMLElement;
    expect(firstRow.querySelector('a.dt-link')).toBeNull();
    expect(firstRow.textContent).toContain('Alpha');
  });
});
