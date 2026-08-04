import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppFieldComponent, AppFieldControlDirective } from './app-field';
import { AppFieldGroupComponent } from './app-field-group';

/** Group with a variable number of rows, so last-row maths can be exercised. */
@Component({
  standalone: true,
  imports: [
    AppFieldGroupComponent,
    AppFieldComponent,
    AppFieldControlDirective,
    ReactiveFormsModule,
  ],
  template: `
    <form [formGroup]="form">
      <app-field-group [columns]="columns">
        @for (name of visible; track name) {
          <app-field [label]="name" [control]="form.controls[name]">
            <input appFieldControl [formControl]="form.controls[name]" [attr.data-row]="name" />
          </app-field>
        }
      </app-field-group>
    </form>
  `,
})
class GroupHostComponent {
  columns: number | string = 2;
  visible: Array<'a' | 'b' | 'c' | 'd'> = ['a', 'b', 'c', 'd'];
  form = new FormGroup({
    a: new FormControl('', { validators: Validators.required, nonNullable: true }),
    b: new FormControl('', { nonNullable: true }),
    c: new FormControl('', { nonNullable: true }),
    d: new FormControl('', { nonNullable: true }),
  });
}

/** A row outside any group — the regression guard for #158's existing callers. */
@Component({
  standalone: true,
  imports: [AppFieldComponent, AppFieldControlDirective, ReactiveFormsModule],
  template: `
    <app-field label="Name" [control]="control">
      <input appFieldControl [formControl]="control" />
    </app-field>
  `,
})
class UngroupedHostComponent {
  control = new FormControl('', { validators: Validators.required, nonNullable: true });
}

describe('AppFieldGroupComponent (issue #172)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [GroupHostComponent, UngroupedHostComponent] });
  });

  /**
   * Host inputs are set BEFORE the first change-detection pass — mutating them
   * afterwards raises NG0100 under the zoneless TestBed. Same constraint as
   * app-field.spec.ts; where a test needs a mid-run change it re-creates the
   * fixture instead.
   */
  function render(
    configure?: (host: GroupHostComponent) => void
  ): ComponentFixture<GroupHostComponent> {
    const fixture = TestBed.createComponent(GroupHostComponent);
    configure?.(fixture.componentInstance);
    fixture.detectChanges();
    return fixture;
  }

  const rows = (fixture: ComponentFixture<unknown>) =>
    Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('app-field'));

  const grid = (fixture: ComponentFixture<unknown>) =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>('.app-field-group');

  it('publishes the column count as a custom property for the global grid rule', () => {
    const fixture = render();
    expect(grid(fixture)?.style.getPropertyValue('--rq-field-group-columns')).toBe('2');
  });

  it('coerces a string column count', () => {
    const fixture = render(host => (host.columns = '3'));
    expect(grid(fixture)?.style.getPropertyValue('--rq-field-group-columns')).toBe('3');
  });

  it.each([0, -1, Number.NaN, 'nonsense'])(
    'falls back to a single column for the invalid value %p',
    value => {
      const fixture = render(host => (host.columns = value as number));
      expect(grid(fixture)?.style.getPropertyValue('--rq-field-group-columns')).toBe('1');
    }
  );

  it('tags every projected row as a cell', () => {
    const fixture = render();
    expect(rows(fixture)).toHaveLength(4);
    for (const row of rows(fixture)) {
      expect(row.classList.contains('rq-field-cell')).toBe(true);
    }
  });

  it('suppresses the divider on a full last row and nowhere else', () => {
    const fixture = render();
    const lastRow = rows(fixture).map(row => row.classList.contains('rq-field-cell-last-row'));
    // 4 rows over 2 columns: the final line holds rows 3 and 4.
    expect(lastRow).toEqual([false, false, true, true]);
  });

  it('suppresses the divider on a partial last row, so no hairline stops mid-row', () => {
    const fixture = render(host => (host.visible = ['a', 'b', 'c']));
    const lastRow = rows(fixture).map(row => row.classList.contains('rq-field-cell-last-row'));
    // 3 rows over 2 columns: the final line holds row 3 alone.
    expect(lastRow).toEqual([false, false, true]);
  });

  it('treats a single row as its own last row', () => {
    const fixture = render(host => (host.visible = ['a']));
    expect(rows(fixture)[0].classList.contains('rq-field-cell-last-row')).toBe(true);
  });

  it('moves the divider boundary when the column count changes', () => {
    const fixture = render(host => (host.columns = 4));
    // 4 rows over 4 columns: one line, so every row is a last row.
    expect(
      rows(fixture).map(row => row.classList.contains('rq-field-cell-last-row'))
    ).toEqual([true, true, true, true]);
  });

  it('re-tags rows when the projected set changes after content init', () => {
    const fixture = render();
    fixture.componentInstance.visible = ['a', 'b', 'c'];
    fixture.detectChanges();

    const lastRow = rows(fixture).map(row => row.classList.contains('rq-field-cell-last-row'));
    expect(lastRow).toEqual([false, false, true]);
  });

  it('renders no group markup and stays inert with no rows', () => {
    const fixture = render(host => (host.visible = []));
    expect(rows(fixture)).toHaveLength(0);
    expect(grid(fixture)).not.toBeNull();
  });

  it('leaves nothing stamped on the rows when the group is destroyed', () => {
    const fixture = render();
    const [firstRow] = rows(fixture);
    expect(firstRow.classList.contains('rq-field-cell')).toBe(true);

    fixture.destroy();

    expect(firstRow.classList.contains('rq-field-cell')).toBe(false);
    expect(firstRow.classList.contains('rq-field-cell-last-row')).toBe(false);
  });

  it('owns no label, error or ARIA of its own', () => {
    const fixture = render();
    const group = grid(fixture)!;
    expect(group.getAttribute('role')).toBeNull();
    expect(group.getAttribute('aria-label')).toBeNull();
    expect(group.querySelector(':scope > label')).toBeNull();
    expect(group.querySelector(':scope > [data-testid="field-error"]')).toBeNull();
  });

  it('leaves each row owning its own label and error', () => {
    const fixture = render(host => host.form.controls.a.markAsTouched());
    const [firstRow] = rows(fixture);

    expect(firstRow.querySelector('label')).not.toBeNull();
    expect(firstRow.querySelector('[data-testid="field-error"]')).not.toBeNull();
  });

  it('does not change how an ungrouped row renders', () => {
    const fixture = TestBed.createComponent(UngroupedHostComponent);
    fixture.detectChanges();
    const row = (fixture.nativeElement as HTMLElement).querySelector('app-field')!;

    expect(row.classList.contains('rq-field-cell')).toBe(false);
    expect(row.classList.contains('rq-field-cell-last-row')).toBe(false);
    expect(row.querySelector('.app-field-bordered')).not.toBeNull();
  });
});
