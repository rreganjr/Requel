import { render, screen } from '@testing-library/angular';
import { FormControl } from '@angular/forms';
import { InlineErrorComponent } from './app-inline-error';
import { notBlank } from './form-errors';

describe('InlineErrorComponent', () => {
  async function setup(inputs: Partial<{ submitted: boolean }> = {}) {
    const control = new FormControl('', { nonNullable: true, validators: [notBlank()] });
    const view = await render(InlineErrorComponent, {
      inputs: {
        control,
        id: 'the-error',
        testid: 'the-error',
        overrides: { required: 'Value is required.' },
        submitted: inputs.submitted ?? false,
      },
    });
    return { control, view };
  }

  it('renders nothing on a pristine, untouched, invalid control', async () => {
    await setup();
    expect(screen.queryByTestId('the-error')).not.toBeInTheDocument();
  });

  it('renders the override message once the control is touched', async () => {
    const { control, view } = await setup();
    control.markAsTouched();
    view.detectChanges();
    const err = screen.getByTestId('the-error');
    expect(err.textContent).toContain('Value is required.');
    expect(err.getAttribute('role')).toBe('alert');
    expect(err.id).toBe('the-error');
  });

  it('renders the message when submitted even if untouched', async () => {
    const { view } = await setup({ submitted: true });
    view.detectChanges();
    expect(screen.getByTestId('the-error').textContent).toContain('Value is required.');
  });

  it('renders nothing once the control becomes valid', async () => {
    const { control, view } = await setup({ submitted: true });
    view.detectChanges();
    expect(screen.getByTestId('the-error')).toBeInTheDocument();
    control.setValue('ok');
    view.detectChanges();
    expect(screen.queryByTestId('the-error')).not.toBeInTheDocument();
  });

  it('message() returns null when not showing and the message when showing', async () => {
    const { control, view } = await setup();
    const comp = view.fixture.componentInstance as InlineErrorComponent;
    expect(comp.message()).toBeNull();
    control.markAsTouched();
    expect(comp.message()).toBe('Value is required.');
  });
});
