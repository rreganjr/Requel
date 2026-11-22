/// <reference types="node" />
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

/**
 * #519: components outside theme/ take colours from tokens, so they follow dark mode and the
 * chosen accent. A colour literal (hex, rgb(), hsl()) or a fixed palette stop (--p-blue-50,
 * --p-red-600, ...) doesn't change with either. The surface ramp (--p-surface-NNN) is allowed:
 * the preset inverts it for dark mode. Tokens with literal values live in theme/ and
 * styles.scss.
 */
describe('component colours', () => {
  const root = join(process.cwd(), 'src', 'app');

  // A hex colour with a letter in it, or any hex value in a CSS declaration. All-digit
  // "#123" outside a declaration is an issue number in prose.
  const HEX_WITH_LETTER = /(?:^|[\s:(,'"])#(?=[0-9]*[a-fA-F])(?:[0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{3,4})(?![\w-])/;
  const HEX_IN_DECLARATION = /[a-z-]+\s*:\s*[^;{}]*#[0-9a-fA-F]{3,8}(?![\w-])/;
  const FUNCTION = /\b(?:rgba?|hsla?)\(/;
  const PALETTE_STOP = /--p-(?!surface-)[a-z]+-\d{2,3}\b/;

  function sources(dir: string): string[] {
    return readdirSync(dir).flatMap(name => {
      const path = join(dir, name);
      if (statSync(path).isDirectory()) {
        return relative(root, path) === 'theme' ? [] : sources(path);
      }
      return /\.(ts|html|scss|css)$/.test(name) && !name.endsWith('.spec.ts') ? [path] : [];
    });
  }

  /** Comments mention issue numbers (#159) and colours in prose; only code counts. */
  function withoutComments(text: string): string {
    return text
      .replace(/\/\*[\s\S]*?\*\//g, '')
      .replace(/<!--[\s\S]*?-->/g, '')
      .replace(/(^|\s)\/\/.*$/gm, '$1');
  }

  it('use no colour literal or fixed palette stop outside theme/', () => {
    const offenders: string[] = [];
    for (const file of sources(root)) {
      withoutComments(readFileSync(file, 'utf-8')).split('\n').forEach((line, i) => {
        if (HEX_WITH_LETTER.test(line) || HEX_IN_DECLARATION.test(line) || FUNCTION.test(line)
            || PALETTE_STOP.test(line)) {
          offenders.push(`${relative(root, file)}:${i + 1}: ${line.trim()}`);
        }
      });
    }
    expect(offenders).toEqual([]);
  });
});
