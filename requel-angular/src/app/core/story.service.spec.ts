import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { StoryService } from './story.service';

describe('StoryService', () => {
  let service: StoryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(StoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listStories() sends GET to the project stories endpoint', async () => {
    const promise = service.listStories('My Project');
    const req = httpMock.expectOne('/api/projects/My%20Project/stories');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, name: 'User login', text: null, primaryActorName: 'Administrator' }]);
    const result = await promise;
    expect(result[0].primaryActorName).toBe('Administrator');
  });

  it('getStory() sends GET to the individual story endpoint', async () => {
    const promise = service.getStory('My Project', 7);
    const req = httpMock.expectOne('/api/projects/My%20Project/stories/7');
    req.flush({ id: 7, name: 'User login', text: 'User logs in.', primaryActorName: null });
    const result = await promise;
    expect(result.id).toBe(7);
  });
});
