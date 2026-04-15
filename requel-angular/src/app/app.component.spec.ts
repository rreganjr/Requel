import { render } from '@testing-library/angular';
import { App } from './app';

describe('App smoke test', () => {
  it('renders without error', async () => {
    await render(App);
    expect(document.body).toBeInTheDocument();
  });
});
