import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TermsUsedComponent, TERMS_USED_DEBOUNCE_MS } from './terms-used';
import { TermService } from '../core/term.service';
import { GlossaryTermDto } from '../models/term';
import { expectNoAxeViolations } from './testing/a11y';

function term(id: number, name: string, text: string, canonicalTermId: number | null = null): GlossaryTermDto {
  return { id, version: 1, name, text, createdBy: null, canonicalTermId, canonicalTermName: null,
    alternateTerms: null, referers: null };
}

const TERMS = [
  term(1, 'Host', 'The person running the room.'),
  term(2, 'video room', 'Where a meeting happens.'),
  term(3, 'room', 'Any space.'),
  term(10, 'meeting', 'A scheduled session.'),
  term(11, 'call', 'Old word.', 10),
];

describe('TermsUsedComponent (#506)', () => {
  let listTerms: ReturnType<typeof vi.fn>;
  let fixture: ComponentFixture<TermsUsedComponent>;

  async function create(inputs: Record<string, unknown>) {
    fixture = TestBed.createComponent(TermsUsedComponent);
    fixture.componentRef.setInput('projectName', 'Acme');
    for (const [k, v] of Object.entries(inputs)) {
      fixture.componentRef.setInput(k, v);
    }
    await settle();
  }

  async function settle() {
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(TERMS_USED_DEBOUNCE_MS + 1);
    fixture.detectChanges();
  }

  const chips = () => Array.from(fixture.nativeElement.querySelectorAll('[data-testid="term-chip"]')) as HTMLAnchorElement[];
  const chipText = () => chips().map(c => c.textContent!.replace(/\s+/g, ' ').trim());

  beforeEach(() => {
    vi.useFakeTimers();
    listTerms = vi.fn().mockResolvedValue(TERMS);
    TestBed.configureTestingModule({
      imports: [TermsUsedComponent],
      providers: [provideRouter([]), provideNoopAnimations(), { provide: TermService, useValue: { listTerms } }],
    });
  });

  afterEach(() => vi.useRealTimers());

  it('shows a chip per term used, the longest match winning, linking to the term', async () => {
    await create({ name: 'End a meeting room', text: 'The Host ends the video room' });
    expect(listTerms).toHaveBeenCalledWith('Acme');
    expect(chipText()).toEqual(['meeting', 'room', 'Host', 'video room']);
    expect(chips()[2].getAttribute('href')).toBe('/projects/Acme/terms/1');
  });

  it("describes each chip with the term's definition", async () => {
    await create({ text: 'The Host' });
    const chip = chips()[0];
    const description = fixture.nativeElement.querySelector(`#${chip.getAttribute('aria-describedby')}`);
    expect(description.textContent).toBe('The person running the room.');
  });

  it('shows an alternate as alternate → canonical, opening the canonical', async () => {
    await create({ text: 'Join the call' });
    expect(chipText()).toEqual(['call →, preferred term meeting']);
    expect(chips()[0].getAttribute('href')).toBe('/projects/Acme/terms/10');
    const description = fixture.nativeElement.querySelector(`#${chips()[0].getAttribute('aria-describedby')}`);
    expect(description.textContent).toBe('A scheduled session.');
  });

  it('catches up with typing after a pause, without saving', async () => {
    await create({ text: 'Nothing yet' });
    expect(chips()).toHaveLength(0);
    fixture.componentRef.setInput('text', 'Nothing yet for the Host');
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(TERMS_USED_DEBOUNCE_MS - 50);
    fixture.detectChanges();
    expect(chips()).toHaveLength(0);
    await settle();
    expect(chipText()).toEqual(['Host']);
  });

  it('renders nothing when no term matches', async () => {
    await create({ text: 'bedrooms and rooms' });
    expect(fixture.nativeElement.querySelector('[data-testid="terms-used"]')).toBeNull();
  });

  it('leaves out the excluded term', async () => {
    await create({ name: 'Host', text: 'Runs the video room', excludeTermId: 1 });
    expect(chipText()).toEqual(['video room']);
  });

  it('loads the terms once however much is typed', async () => {
    await create({ text: 'Host' });
    fixture.componentRef.setInput('text', 'Host room');
    await settle();
    expect(listTerms).toHaveBeenCalledTimes(1);
  });

  it('shows nothing if the terms cannot be loaded', async () => {
    listTerms.mockRejectedValue(new Error('boom'));
    await create({ text: 'Host' });
    expect(chips()).toHaveLength(0);
  });

  it('has no axe violations', async () => {
    await create({ text: 'The Host joins the call' });
    vi.useRealTimers();
    await expectNoAxeViolations(fixture.nativeElement);
  });
});
