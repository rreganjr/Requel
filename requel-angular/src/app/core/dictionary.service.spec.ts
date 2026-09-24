import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { DictionaryService } from './dictionary.service';
import { CommandService } from './command.service';

describe('DictionaryService (#319)', () => {
  let service: DictionaryService;
  let httpMock: HttpTestingController;
  let execute: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    execute = vi.fn().mockResolvedValue({ success: true });
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(),
        { provide: CommandService, useValue: { execute } }]
    });
    service = TestBed.inject(DictionaryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listProjectWords() GETs the project dictionary', async () => {
    const promise = service.listProjectWords('My Project');
    const req = httpMock.expectOne('/api/projects/My%20Project/dictionary');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, lemma: 'requel' }]);
    expect((await promise)[0].lemma).toBe('requel');
  });

  it('listInstallWords() GETs the admin dictionary', async () => {
    const promise = service.listInstallWords();
    const req = httpMock.expectOne('/api/admin/dictionary');
    expect(req.request.method).toBe('GET');
    req.flush([]);
    expect(await promise).toEqual([]);
  });

  it('dispatches the four commands with their input fields', async () => {
    await service.addProjectWord('P', 'word');
    await service.removeProjectWord('P', 7);
    await service.addInstallWord('wide');
    await service.removeInstallWord(9);
    expect(execute).toHaveBeenNthCalledWith(1, 'AddProjectDictionaryWord', { projectName: 'P', lemma: 'word' });
    expect(execute).toHaveBeenNthCalledWith(2, 'DeleteProjectDictionaryWord', { projectName: 'P', wordId: 7 });
    expect(execute).toHaveBeenNthCalledWith(3, 'AddInstallDictionaryWord', { lemma: 'wide' });
    expect(execute).toHaveBeenNthCalledWith(4, 'DeleteInstallDictionaryWord', { wordId: 9 });
  });
});
