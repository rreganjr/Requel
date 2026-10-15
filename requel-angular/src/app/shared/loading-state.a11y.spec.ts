import { TestBed } from '@angular/core/testing';
import { LoadingStateComponent } from './loading-state';
import { expectNoAxeViolations } from './testing/a11y';

describe('LoadingStateComponent — accessibility', () => {
  it('has no axe-core violations', async () => {
    TestBed.configureTestingModule({ imports: [LoadingStateComponent] });
    const fixture = TestBed.createComponent(LoadingStateComponent);
    fixture.componentInstance.label = 'Loading project…';
    fixture.detectChanges();
    await expectNoAxeViolations(fixture.nativeElement);
  });
});
