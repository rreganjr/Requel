import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import { InstallDictionaryComponent } from './install-dictionary';
import { DictionaryService } from '../../core/dictionary.service';

const WORDS = [{ id: 5, lemma: 'widely' }];
const flush = () => new Promise(r => setTimeout(r, 0));

describe('InstallDictionaryComponent (#319)', () => {
  let dictionary: {
    listInstallWords: ReturnType<typeof vi.fn>;
    addInstallWord: ReturnType<typeof vi.fn>;
    removeInstallWord: ReturnType<typeof vi.fn>;
  };

  async function render() {
    dictionary = {
      listInstallWords: vi.fn().mockResolvedValue(WORDS),
      addInstallWord: vi.fn().mockResolvedValue({ success: true, entity: { id: 6, lemma: 'more' } }),
      removeInstallWord: vi.fn().mockResolvedValue({ success: true }),
    };
    TestBed.configureTestingModule({
      imports: [InstallDictionaryComponent],
      providers: [
        provideNoopAnimations(),
        { provide: DictionaryService, useValue: dictionary },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(InstallDictionaryComponent);
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return { fixture, comp: fixture.componentInstance, el: fixture.nativeElement as HTMLElement };
  }

  it('loads the installation words under the page title', async () => {
    const { comp, el } = await render();
    expect(dictionary.listInstallWords).toHaveBeenCalled();
    expect(comp.words()).toEqual(WORDS);
    expect(el.textContent).toContain('Installation Dictionary');
    expect(el.querySelector('[data-testid="install-dictionary-add-form"]')).not.toBeNull();
  });

  it('adds and removes words, reloading each time', async () => {
    const { comp } = await render();
    await comp.addWord('more');
    expect(dictionary.addInstallWord).toHaveBeenCalledWith('more');
    await comp.removeWord(WORDS[0]);
    expect(dictionary.removeInstallWord).toHaveBeenCalledWith(5);
    expect(dictionary.listInstallWords).toHaveBeenCalledTimes(3);
  });

  it('shows a failed remove at page level', async () => {
    const { comp } = await render();
    dictionary.removeInstallWord.mockResolvedValue({ success: false, error: 'CONFLICT' });
    await comp.removeWord(WORDS[0]);
    expect(comp.errorMessage()).toBe('CONFLICT');
  });
});
