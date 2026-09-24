import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConfirmationService } from 'primeng/api';
import { DictionaryWordListComponent } from './dictionary-word-list';
import { expectNoAxeViolations } from './testing/a11y';

const WORDS = [{ id: 1, lemma: 'requel' }, { id: 2, lemma: 'stakeholderish' }];
const flush = () => new Promise(r => setTimeout(r, 0));

describe('DictionaryWordListComponent (#319)', () => {
  function render(inputs: Partial<DictionaryWordListComponent> = {}) {
    TestBed.configureTestingModule({
      imports: [DictionaryWordListComponent],
      providers: [provideNoopAnimations()],
    });
    const fixture = TestBed.createComponent(DictionaryWordListComponent);
    const comp = fixture.componentInstance;
    comp.words = WORDS;
    comp.canAdd = true;
    comp.canRemove = true;
    comp.testid = 'dict';
    Object.assign(comp, inputs);
    fixture.detectChanges();
    return { fixture, comp, el: fixture.nativeElement as HTMLElement };
  }

  /** Spy on the component's own ConfirmationService, as maintenance.spec does. */
  function stubConfirm(fixture: ReturnType<typeof render>['fixture'], accept: boolean) {
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => {
      if (accept) conf.accept?.();
      return cs;
    });
  }

  it('renders one row per word', async () => {
    const { fixture, el } = render();
    await flush();
    fixture.detectChanges();
    const lemmas = Array.from(el.querySelectorAll('[data-testid="dict-lemma"]')).map(e => e.textContent);
    expect(lemmas).toEqual(['requel', 'stakeholderish']);
  });

  it('emits the trimmed word on add', () => {
    const { comp } = render();
    const emitted: string[] = [];
    comp.add.subscribe(w => emitted.push(w));
    comp.addForm.controls.lemma.setValue('  newword  ');
    comp.submit();
    expect(emitted).toEqual(['newword']);
  });

  it.each([
    ['', 'Enter a word.'],
    ['   ', 'Enter a word.'],
    ['two words', 'One word only, with no spaces.'],
    ['x'.repeat(81), 'At most 80 characters.'],
  ])('refuses %j client-side with a field message', (value, message) => {
    const { fixture, comp, el } = render();
    const emitted: string[] = [];
    comp.add.subscribe(w => emitted.push(w));
    comp.addForm.controls.lemma.setValue(value);
    comp.submit();
    fixture.detectChanges();
    expect(emitted).toEqual([]);
    expect(el.querySelector('[data-testid="dict-word-error"]')?.textContent).toContain(message);
    const input = el.querySelector('[data-testid="dict-word"]') as HTMLElement;
    expect(input.getAttribute('aria-invalid')).toBe('true');
  });

  it('accepts an 80-character word', () => {
    const { comp } = render();
    const emitted: string[] = [];
    comp.add.subscribe(w => emitted.push(w));
    comp.addForm.controls.lemma.setValue('y'.repeat(80));
    comp.submit();
    expect(emitted).toEqual(['y'.repeat(80)]);
  });

  it('shows a server lemma violation on the field and returns the rest', () => {
    const { fixture, comp, el } = render();
    comp.addForm.controls.lemma.setValue('word');
    comp.submit();
    const rest = comp.showAddErrors([
      { field: 'lemma', message: 'validation failed: the word cannot be empty.' },
      { field: null, message: 'something else' },
    ]);
    fixture.detectChanges();
    expect(rest).toEqual(['something else']);
    expect(el.querySelector('[data-testid="dict-word-error"]')?.textContent).toContain('the word cannot be empty');
  });

  it('clearAdd() empties the field', () => {
    const { comp } = render();
    comp.addForm.controls.lemma.setValue('word');
    comp.clearAdd();
    expect(comp.addForm.controls.lemma.value).toBe('');
  });

  it('asks before removing, then emits the word', async () => {
    const { fixture, comp, el } = render();
    await flush();
    fixture.detectChanges();
    const confirm = stubConfirm(fixture, true);
    const removed: number[] = [];
    comp.remove.subscribe(w => removed.push(w.id));
    (el.querySelector('[data-testid="dict-remove"] button') as HTMLButtonElement).click();
    expect(confirm).toHaveBeenCalledTimes(1);
    expect(removed).toEqual([1]);
  });

  it('does not emit when the removal is declined', async () => {
    const { fixture, comp } = render();
    await flush();
    fixture.detectChanges();
    stubConfirm(fixture, false);
    const removed: number[] = [];
    comp.remove.subscribe(w => removed.push(w.id));
    comp.confirmRemove(WORDS[0]);
    expect(removed).toEqual([]);
  });

  it('hides the add form and the remove buttons when not allowed', async () => {
    const { fixture, el } = render({ canAdd: false, canRemove: false });
    await flush();
    fixture.detectChanges();
    expect(el.querySelector('[data-testid="dict-add-form"]')).toBeNull();
    expect(el.querySelector('[data-testid="dict-remove"]')).toBeNull();
    expect(el.querySelectorAll('[data-testid="dict-lemma"]').length).toBe(2);
  });

  it('has no axe-core violations in the add form', async () => {
    const { el } = render();
    await expectNoAxeViolations(el.querySelector('[data-testid="dict-add-form"]') as HTMLElement);
  });
});
