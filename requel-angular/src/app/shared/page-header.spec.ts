import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { PageHeaderComponent } from './page-header';

@Component({
  standalone: true,
  imports: [PageHeaderComponent],
  template: `<app-page-header [title]="title" />`
})
class HostComponent {
  title = 'My Page';
}

describe('PageHeaderComponent (issue #135)', () => {
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
});
