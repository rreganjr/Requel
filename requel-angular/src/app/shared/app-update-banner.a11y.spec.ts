import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { UpdateBannerComponent } from './app-update-banner';
import { expectNoAxeViolations } from './testing/a11y';

describe('UpdateBannerComponent — accessibility', () => {
  it('has no axe-core violations', async () => {
    TestBed.configureTestingModule({
      imports: [UpdateBannerComponent],
      providers: [provideNoopAnimations()]
    });
    const fixture = TestBed.createComponent(UpdateBannerComponent);
    Object.assign(fixture.componentInstance, { message: 'This goal was changed elsewhere.', testid: 'goal-update' });
    fixture.detectChanges();
    await expectNoAxeViolations(fixture.nativeElement as HTMLElement);
  });
});
