import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConfirmationService, MessageService } from 'primeng/api';
import { MaintenanceComponent } from './maintenance';
import { MaintenanceService } from '../../core/maintenance.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const RESULT = {
  projectsScanned: 2, stakeholdersCreated: 1, permissionsGranted: 27, projectsSkipped: 0,
};
const flush = () => new Promise(r => setTimeout(r, 0));

describe('MaintenanceComponent — accessibility', () => {
  async function render() {
    TestBed.configureTestingModule({
      imports: [MaintenanceComponent],
      providers: [
        provideNoopAnimations(),
        { provide: MaintenanceService, useValue: {
          repairProjectStakeholders: vi.fn()
            .mockResolvedValue({ success: true, entity: RESULT, error: null }),
        } },
        { provide: MessageService, useValue: { add: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(MaintenanceComponent);
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture;
  }

  it('groups the task under a fieldset with an accessible name', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    const fs = el.querySelector('fieldset[data-testid="repair-stakeholders-task"]');
    expect(fs).not.toBeNull();
    expect(fs!.querySelector('legend')?.textContent?.trim()).toBe('Repair project stakeholders');
  });

  it('has no axe-core violations before a run', async () => {
    const el = (await render()).nativeElement as HTMLElement;
    const fs = el.querySelector('fieldset[data-testid="repair-stakeholders-task"]') as HTMLElement;
    await expectNoAxeViolations(fs);
  });

  it('has no axe-core violations once the result counts are shown', async () => {
    const fixture = await render();
    const cs = fixture.debugElement.injector.get(ConfirmationService);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.spyOn(cs, 'confirm').mockImplementation((conf: any) => conf.accept?.());
    fixture.componentInstance.runRepair();
    await flush();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="repair-result"]')).not.toBeNull();
    const fs = el.querySelector('fieldset[data-testid="repair-stakeholders-task"]') as HTMLElement;
    await expectNoAxeViolations(fs);
  });
});
