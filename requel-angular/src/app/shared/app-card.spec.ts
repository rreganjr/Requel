import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AppCardComponent } from './app-card';

@Component({
  standalone: true,
  imports: [AppCardComponent],
  template: `
    <app-card [title]="title">
      <p data-testid="card-body">Body content</p>
    </app-card>`
})
class BodyHostComponent {
  title = '';
}

@Component({
  standalone: true,
  imports: [AppCardComponent],
  template: `
    <app-card [title]="title">
      <button actions data-testid="card-action">New</button>
      <p data-testid="card-body">Body content</p>
    </app-card>`
})
class TitledHostComponent {
  title = 'Details';
}

describe('AppCardComponent (issue #156)', () => {
  it('renders the card surface and projected body content', () => {
    TestBed.configureTestingModule({ imports: [BodyHostComponent] });
    const fixture = TestBed.createComponent(BodyHostComponent);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('section.app-card')).not.toBeNull();
    expect(el.querySelector('[data-testid="card-body"]')?.textContent?.trim()).toBe(
      'Body content'
    );
  });

  it('does not render a card header (or <h2>) when no title is set', () => {
    TestBed.configureTestingModule({ imports: [BodyHostComponent] });
    const fixture = TestBed.createComponent(BodyHostComponent);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.app-card-header')).toBeNull();
    expect(el.querySelectorAll('h2').length).toBe(0);
  });

  it('renders the title as an <h2> and projects the [actions] slot when titled', () => {
    TestBed.configureTestingModule({ imports: [TitledHostComponent] });
    const fixture = TestBed.createComponent(TitledHostComponent);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const h2 = el.querySelector('h2.app-card-title');
    expect(h2?.textContent?.trim()).toBe('Details');
    expect(h2?.classList.contains('rq-section-title')).toBe(true);
    expect(el.querySelector('[data-testid="card-action"]')?.textContent?.trim()).toBe('New');
  });
});
