import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ReportService } from './report.service';

describe('ReportService', () => {
  let service: ReportService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ReportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('listReports() sends GET to the project reports endpoint', async () => {
    const promise = service.listReports('My Project');
    const req = httpMock.expectOne('/api/projects/My%20Project/reports');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, name: 'Summary Report', text: '<xsl:stylesheet/>' }]);
    const result = await promise;
    expect(result[0].name).toBe('Summary Report');
  });

  it('getReport() sends GET to the individual report endpoint', async () => {
    const promise = service.getReport('My Project', 2);
    const req = httpMock.expectOne('/api/projects/My%20Project/reports/2');
    req.flush({ id: 2, name: 'Detail Report', text: '<xsl:stylesheet/>' });
    const result = await promise;
    expect(result.id).toBe(2);
  });

  it('saveReport() dispatches EditReportGenerator command', async () => {
    const promise = service.saveReport('My Project', null, 'New Report', '<xsl/>');
    const req = httpMock.expectOne('/api/commands/EditReportGenerator');
    expect(req.request.body).toMatchObject({ projectName: 'My Project', name: 'New Report' });
    req.flush({ success: true, entityType: 'EditReportGenerator', entity: null, error: null, violations: null });
    const result = await promise;
    expect(result.success).toBe(true);
  });

  it('deleteReport() dispatches DeleteReportGenerator command', async () => {
    const promise = service.deleteReport('My Project', 3);
    const req = httpMock.expectOne('/api/commands/DeleteReportGenerator');
    expect(req.request.body).toMatchObject({ projectName: 'My Project', reportId: 3 });
    req.flush({ success: true, entityType: 'DeleteReportGenerator', entity: null, error: null, violations: null });
    const result = await promise;
    expect(result.success).toBe(true);
  });
});
