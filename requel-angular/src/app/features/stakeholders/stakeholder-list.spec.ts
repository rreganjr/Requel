import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { StakeholderListComponent } from './stakeholder-list';
import { StakeholderService } from '../../core/stakeholder.service';
import { PermissionService } from '../../core/permission.service';

const MOCK_STAKEHOLDERS = [
  { id: 50, version: 0, name: 'Alice', type: 'user' as const, createdBy: null,
    userDetails: { username: 'alice', teamName: 'Dev', emailAddress: 'alice@example.com', phoneNumber: '', permissionKeys: [] },
    nonUserDetails: null },
  { id: 51, version: 0, name: 'FASB', type: 'non-user' as const, createdBy: null,
    userDetails: null,
    nonUserDetails: { text: 'Financial standards body' } }
];

const flush = () => new Promise(r => setTimeout(r, 0));

describe('StakeholderListComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let stakeholderServiceMock: { listStakeholders: ReturnType<typeof vi.fn> };
  let permissionServiceMock: { loadForProject: ReturnType<typeof vi.fn>; canEdit: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: StakeholderListComponent;
  let router: Router;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ name: 'proj1' }));

    stakeholderServiceMock = { listStakeholders: vi.fn().mockResolvedValue(MOCK_STAKEHOLDERS) };
    permissionServiceMock = {
      loadForProject: vi.fn().mockResolvedValue(undefined),
      canEdit: vi.fn().mockReturnValue(true)
    };

    TestBed.configureTestingModule({
      imports: [StakeholderListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } },
        { provide: StakeholderService, useValue: stakeholderServiceMock },
        { provide: PermissionService, useValue: permissionServiceMock }
      ]
    });
    fixture = TestBed.createComponent(StakeholderListComponent);
    comp = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('stakeholders() populated from stakeholderService.listStakeholders on init', async () => {
    fixture.detectChanges();
    await flush();
    expect(stakeholderServiceMock.listStakeholders).toHaveBeenCalledWith('proj1');
    expect(comp.stakeholders().length).toBe(2);
    expect(comp.loading()).toBe(false);
  });

  it('both user and non-user stakeholders are present', async () => {
    fixture.detectChanges();
    await flush();
    const types = comp.stakeholders().map(s => s.type);
    expect(types).toContain('user');
    expect(types).toContain('non-user');
  });

  it('canEdit() reflects permissionService.canEdit("Stakeholder")', async () => {
    fixture.detectChanges();
    await flush();
    expect(permissionServiceMock.canEdit).toHaveBeenCalledWith('Stakeholder');
    expect(comp.canEdit()).toBe(true);
  });

  it('onRowSelect navigates to stakeholder editor', async () => {
    fixture.detectChanges();
    await flush();
    comp.onRowSelect({ data: MOCK_STAKEHOLDERS[0] });
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'stakeholders', 50]);
  });

  it('errorMessage set when listStakeholders throws', async () => {
    stakeholderServiceMock.listStakeholders.mockRejectedValue(new Error('Network error'));
    fixture.detectChanges();
    await flush();
    expect(comp.errorMessage()).toBe('Failed to load stakeholders.');
    expect(comp.loading()).toBe(false);
  });

  it('renders the name as a real link and still navigates on row click (issue #129)', async () => {
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    // AC #2 - the name cell is a real link to the detail route
    const link = el.querySelector('tbody tr.dt-row a.dt-link') as HTMLAnchorElement;
    expect(link).not.toBeNull();
    expect(link.textContent?.trim()).toBe('Alice');
    expect(link.getAttribute('href')).toBe('/projects/proj1/stakeholders/50');

    // AC #3 - whole-row click still navigates to the same target (kept as a convenience)
    const row = el.querySelector('tbody tr.dt-row') as HTMLElement;
    row.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    expect(router.navigate).toHaveBeenCalledWith(['/projects', 'proj1', 'stakeholders', 50]);
  });
});
