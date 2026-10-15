import { describe, expect, it } from 'vitest';
import { RequelPreset } from './requel-preset';

type Tokens = { [key: string]: unknown };

function surfaceRefs(value: unknown, path: string, out: string[]): string[] {
  if (typeof value === 'string' && /\{surface\.\d+\}/.test(value)) {
    out.push(`${path} = ${value}`);
  } else if (value && typeof value === 'object') {
    for (const [k, v] of Object.entries(value)) surfaceRefs(v, `${path}.${k}`, out);
  }
  return out;
}

describe('RequelPreset dark scheme (#390)', () => {
  const preset = RequelPreset as unknown as {
    semantic: { colorScheme: { dark: Tokens } };
    components: { [name: string]: { colorScheme?: { dark?: Tokens } } };
  };
  const dark = preset.semantic.colorScheme.dark as {
    formField: Tokens; overlay: { select: Tokens }; surface: Tokens; content: Tokens; text: Tokens;
  };

  it('gives form fields a dark background and light text, not the inverted surface ramp', () => {
    expect(dark.formField['background']).toBe('{slate.950}');
    expect(dark.formField['color']).toBe('{slate.0}');
    expect(dark.overlay.select['background']).toBe('{slate.900}');
  });

  it('leaves no Aura dark token pointing at the inverted surface ramp', () => {
    const { surface: _surface, ...semantic } = dark;
    const refs = surfaceRefs(semantic, 'semantic', []);
    for (const [name, component] of Object.entries(preset.components ?? {})) {
      surfaceRefs(component.colorScheme?.dark, name, refs);
    }
    expect(refs).toEqual([]);
  });

  it("keeps Requel's own dark surface ramp and content tokens", () => {
    expect(dark.surface['0']).toBe('{slate.900}');
    expect(dark.surface['950']).toBe('{slate.0}');
    expect(dark.content['background']).toBe('{slate.900}');
    expect(dark.text['color']).toBe('{slate.100}');
  });
});
