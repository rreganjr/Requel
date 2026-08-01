import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { PageHeaderComponent } from './page-header';

@Component({
  standalone: true,
  imports: [PageHeaderComponent],
  template: `<app-page-header [title]="title" [eyebrow]="eyebrow" />`
})
class HostComponent {
  title = 'My Page';
  eyebrow = '';
}

@Component({
  standalone: true,
  imports: [PageHeaderComponent],
  template: `
    <app-page-header [title]="title" [eyebrow]="eyebrow">
      <span metadata data-testid="meta-tag">Draft</span>
    </app-page-header>`
})
class MetaHostComponent {
  title = 'Goals';
  eyebrow = 'Acme Project';
}

describe('PageHeaderComponent (issues #135, #127)', () => {
  it('renders exactly one <h1> containing the title and no <h2>', () => {
    TestBed.configureTestingModule({ imports: [HostComponent] });
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const h1s = el.querySelectorAll('h1');
    expect(h1s.length).toBe(1);
    expect(h1s[0].textContent?.trim()).toBe('My Page');
    expect(el.querySelectorAll('h2').length).toBe(0);
  });

  it('does not render the eyebrow when none is provided', () => {
    TestBed.configureTestingModule({ imports: [HostComponent] });
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="page-eyebrow"]')).toBeNull();
  });

  it('renders the eyebrow and projected metadata while keeping a single <h1>', () => {
    TestBed.configureTestingModule({ imports: [MetaHostComponent] });
    const fixture = TestBed.createComponent(MetaHostComponent);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('h1').length).toBe(1);

    const eyebrow = el.querySelector('[data-testid="page-eyebrow"]');
    expect(eyebrow?.textContent?.trim()).toBe('Acme Project');
    expect(eyebrow?.classList.contains('rq-eyebrow')).toBe(true);

    expect(el.querySelector('[data-testid="meta-tag"]')?.textContent?.trim()).toBe('Draft');
  });
});
