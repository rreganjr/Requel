import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard';
import { AuthService } from '../../core/auth.service';

describe('DashboardComponent (issue #135)', () => {
  it('renders exactly one <h1> page title and no <h2>', () => {
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: AuthService, useValue: { user: signal({ username: 'admin', name: 'Admin' }) } }
      ]
    });
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('h1').length).toBe(1);
    expect(el.querySelectorAll('h2').length).toBe(0);
    expect(el.querySelector('h1')?.textContent).toContain('Welcome, Admin');
  });
});
