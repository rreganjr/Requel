import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { render, screen, fireEvent } from '@testing-library/angular';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ListPageComponent } from './list-page';

@Component({
  standalone: true,
  imports: [ListPageComponent],
  template: `<app-list-page title="T" [fill]="fill"></app-list-page>`
})
class FillHostComponent {
  fill = false;
}

describe('ListPageComponent', () => {
  it('renders the title in the page header', async () => {
    await render(ListPageComponent, {
      providers: [provideNoopAnimations()],
      inputs: { title: 'My Goals' }
    });
    expect(screen.getByRole('heading', { name: 'My Goals' })).toBeInTheDocument();
  });

  it('renders projected [actions] slot content', async () => {
    const { fixture } = await render(
      `<app-list-page title="T"><button actions>New</button></app-list-page>`,
      { imports: [ListPageComponent], providers: [provideNoopAnimations()] }
    );
    fixture.detectChanges();
    expect(screen.getByRole('button', { name: 'New' })).toBeInTheDocument();
  });

  it('renders the search input by default', async () => {
    await render(ListPageComponent, {
      providers: [provideNoopAnimations()],
      inputs: { title: 'T' }
    });
    expect(screen.getByRole('textbox', { name: 'Search...' })).toBeInTheDocument();
  });

  it('hides the search input when showSearch is false', async () => {
    await render(ListPageComponent, {
      providers: [provideNoopAnimations()],
      inputs: { title: 'T', showSearch: false }
    });
    expect(screen.queryByRole('textbox', { name: 'Search...' })).not.toBeInTheDocument();
  });

  it('emits (search) with the typed value on input', async () => {
    const searchSpy = vi.fn();
    await render(ListPageComponent, {
      providers: [provideNoopAnimations()],
      inputs: { title: 'T' },
      on: { search: searchSpy }
    });
    const input = screen.getByRole('textbox', { name: 'Search...' });
    fireEvent.input(input, { target: { value: 'hello' } });
    expect(searchSpy).toHaveBeenCalledWith('hello');
  });

  it('uses searchAriaLabel as the search box accessible name when provided (#138)', async () => {
    await render(ListPageComponent, {
      providers: [provideNoopAnimations()],
      inputs: { title: 'T', searchAriaLabel: 'Search goals' }
    });
    expect(screen.getByRole('textbox', { name: 'Search goals' })).toBeInTheDocument();
  });
  it('adds lp-fill on the host and fills the card when [fill] is true (#221)', () => {
    TestBed.configureTestingModule({ imports: [FillHostComponent], providers: [provideNoopAnimations()] });
    const fixture = TestBed.createComponent(FillHostComponent);
    fixture.componentInstance.fill = true;
    fixture.detectChanges();
    const host = fixture.nativeElement.querySelector('app-list-page') as HTMLElement;
    expect(host.classList.contains('lp-fill')).toBe(true);
    expect(host.querySelector('app-card')?.classList.contains('app-card--fill')).toBe(true);
  });

  it('does not add lp-fill by default (#221)', () => {
    TestBed.configureTestingModule({ imports: [FillHostComponent], providers: [provideNoopAnimations()] });
    const fixture = TestBed.createComponent(FillHostComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement.querySelector('app-list-page') as HTMLElement;
    expect(host.classList.contains('lp-fill')).toBe(false);
  });
});
