// @ts-check
import tseslint from 'typescript-eslint';

/**
 * Minimal flat config (issue #147). Scoped to a single guardrail so it passes
 * clean on the existing code and does not become a mass-lint cleanup: keep
 * PrimeNG imports tree-shakeable by banning the bare `primeng` barrel. Per-
 * component (`primeng/button`) and per-module (`primeng/api`) imports stay
 * allowed. The ruleset can grow in a later, dedicated lint-adoption ticket.
 *
 * The typescript-eslint plugin is registered (so pre-existing
 * `eslint-disable @typescript-eslint/...` directives in specs resolve) but its
 * rules are left OFF; unused-directive reporting is off for the same reason.
 */
export default tseslint.config({
  files: ['src/**/*.ts'],
  plugins: {
    '@typescript-eslint': tseslint.plugin,
  },
  languageOptions: {
    parser: tseslint.parser,
  },
  linterOptions: {
    reportUnusedDisableDirectives: 'off',
  },
  rules: {
    'no-restricted-imports': [
      'error',
      {
        paths: [
          {
            name: 'primeng',
            message:
              "Import from the per-component entrypoint (e.g. 'primeng/button'), not the 'primeng' barrel, to keep the production bundle tree-shakeable (#147).",
          },
        ],
      },
    ],
  },
});
