import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConfirmationService, MessageService } from 'primeng/api';
import { MaintenanceComponent } from './maintenance';
import { MaintenanceService } from '../../core/maintenance.service';

const RESULT = {
  projectsScanned: 524,
  stakeholdersCreated: 377,
  permissionsGranted: 9801,
  projectsSkipped: 3,
};

const flush = () => new Promise(r => setTimeout(r, 0));

describe('MaintenanceComponent (issue #256)', () => {
  let maintenanceServiceMock: { repairProjectStakeholders: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: MaintenanceComponent;

  beforeEach(() => {
    maintenanceServiceMock = {
      repairProjectStakeholders: vi.fn()
        .mockResolvedValue({ success: true, entity: RESULT, error: null }),
    };

    TestBed.configureTestingModule({
      imports: [MaintenanceComponent],
      providers: [
        provideNoopAnimations(),
        { provide: MaintenanceService, useValue: maintenanceServiceMock },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });

    fixture = TestBed.createComponent(MaintenanceComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
  });

  /**
   * Spy on the component's own ConfirmationService rather than replacing it: p-confirmDialog
   * subscribes to requireConfirmation$ in its constructor, so a stub carrying only confirm()
   * fails to instantiate the template.
   */
  function stubConfirm(accept: boolean): ReturnType<typeof vi.spyOn> {
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return vi.spyOn(cs, 'confirm').mockImplementation((conf: any) =>
      accept ? conf.accept?.() : undefined);
  }

  it('repairs one project without confirming when a name is given', async () => {
    const confirm = stubConfirm(true);
    comp.repairForm.controls.projectName.setValue('  Imported Project 2  ');
    comp.runRepair();
    await flush();
    expect(confirm).not.toHaveBeenCalled();
    expect(maintenanceServiceMock.repairProjectStakeholders)
      .toHaveBeenCalledWith('Imported Project 2');
  });

  it('confirms before repairing every project, then passes null', async () => {
    const confirm = stubConfirm(true);
    comp.runRepair();
    await flush();
    expect(confirm).toHaveBeenCalledTimes(1);
    expect(maintenanceServiceMock.repairProjectStakeholders).toHaveBeenCalledWith(null);
  });

  it('does not repair when the all-projects confirmation is declined', async () => {
    stubConfirm(false);
    comp.runRepair();
    await flush();
    expect(maintenanceServiceMock.repairProjectStakeholders).not.toHaveBeenCalled();
  });

  it('renders the four counts on the page, not only in a toast', async () => {
    stubConfirm(true);
    comp.runRepair();
    await flush();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="repair-scanned"]')?.textContent?.trim()).toBe('524');
    expect(el.querySelector('[data-testid="repair-created"]')?.textContent?.trim()).toBe('377');
    expect(el.querySelector('[data-testid="repair-granted"]')?.textContent?.trim()).toBe('9801');
    expect(el.querySelector('[data-testid="repair-skipped"]')?.textContent?.trim()).toBe('3');
  });

  it('clears running() and surfaces the error when the command fails', async () => {
    stubConfirm(true);
    maintenanceServiceMock.repairProjectStakeholders
      .mockResolvedValue({ success: false, entity: null, error: 'Requires role: SystemAdminUserRole' });
    comp.runRepair();
    await flush();
    expect(comp.errorMessage()).toBe('Requires role: SystemAdminUserRole');
    expect(comp.result()).toBeNull();
    expect(comp.running()).toBe(false);
  });

  it('reports and recovers when the command throws rather than failing cleanly', async () => {
    stubConfirm(true);
    maintenanceServiceMock.repairProjectStakeholders.mockRejectedValue(new Error('boom'));
    comp.runRepair();
    await flush();
    expect(comp.running()).toBe(false);
    expect(comp.errorMessage()).toBe('Repair failed.');
    expect(comp.result()).toBeNull();
  });

  it('shows no result block before a run', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="repair-result"]')).toBeNull();
  });
});
