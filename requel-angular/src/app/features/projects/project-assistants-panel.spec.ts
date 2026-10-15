import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { ProjectAssistantsPanelComponent } from './project-assistants-panel';
import { ProjectAssistantsService } from '../../core/project-assistants.service';
import { ProjectAssistantDto } from '../../models/project-assistant';

const FOUR: ProjectAssistantDto[] = [
  { assistantId: 'legacy-lexical', displayName: 'Spelling', enabled: true },
  { assistantId: 'legacy-lexical-vague-word', displayName: 'Vague words', enabled: false },
  { assistantId: 'legacy-lexical-glossary-term', displayName: 'Glossary candidates', enabled: true },
  { assistantId: 'legacy-lexical-complexity', displayName: 'Complex sentences', enabled: true },
];

describe('ProjectAssistantsPanelComponent (#268)', () => {
  let list: ReturnType<typeof vi.fn>;
  let setEnabled: ReturnType<typeof vi.fn>;
  let analyzeProject: ReturnType<typeof vi.fn>;
  let dataHandling: ReturnType<typeof vi.fn>;
  let setDataHandling: ReturnType<typeof vi.fn>;

  const flush = () => new Promise(r => setTimeout(r, 0));

  async function render(canEdit: boolean, canAnalyze: boolean, canManageDefinitions = false) {
    TestBed.configureTestingModule({
      imports: [ProjectAssistantsPanelComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ProjectAssistantsService,
          useValue: { list, setEnabled, analyzeProject, dataHandling, setDataHandling } },
      ],
    });
    const fixture = TestBed.createComponent(ProjectAssistantsPanelComponent);
    fixture.componentRef.setInput('projectName', 'Acme');
    fixture.componentRef.setInput('canEdit', canEdit);
    fixture.componentRef.setInput('canAnalyze', canAnalyze);
    fixture.componentRef.setInput('canManageDefinitions', canManageDefinitions);
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    list = vi.fn().mockResolvedValue(FOUR.map(a => ({ ...a })));
    setEnabled = vi.fn().mockResolvedValue({ success: true, error: null });
    analyzeProject = vi.fn().mockResolvedValue({ success: true, error: null });
    dataHandling = vi.fn().mockResolvedValue({
      externalProviderAllowed: true,
      redaction: { credentials: true, email: true, phone: false, ssn: true, card: true },
    });
    setDataHandling = vi.fn().mockResolvedValue({ success: true, error: null });
  });

  it('links to the AI definitions only with AssistantDefinition[Edit] (#264)', async () => {
    const without = await render(true, true);
    expect(without.nativeElement.querySelector('[data-testid="assistants-manage-definitions"]'))
      .toBeNull();
    TestBed.resetTestingModule();
    const withIt = await render(true, true, true);
    const link = withIt.nativeElement.querySelector('[data-testid="assistants-manage-definitions"]');
    expect(link?.getAttribute('href')).toBe('/projects/Acme/definitions');
  });

  it('renders a switch per assistant from the query, labelled with its name', async () => {
    const fixture = await render(true, true);
    const el: HTMLElement = fixture.nativeElement;
    expect(list).toHaveBeenCalledWith('Acme');
    expect(el.querySelectorAll('.assistant-list p-toggleswitch').length).toBe(4);
    const labels = Array.from(el.querySelectorAll('.assistant-row label')).map(l => l.textContent?.trim());
    expect(labels).toEqual(['Spelling', 'Vague words', 'Glossary candidates', 'Complex sentences']);
    expect(el.textContent).toContain('The issues it already raised stay.');
  });

  it('sends EditProjectAssistantSetting when a switch is flipped and says so', async () => {
    const fixture = await render(true, false);
    const panel = fixture.componentInstance;

    await panel.toggle(panel.assistants()[0], false);
    fixture.detectChanges();

    expect(setEnabled).toHaveBeenCalledWith('Acme', 'legacy-lexical', false);
    expect(panel.assistants()[0].enabled).toBe(false);
    expect(fixture.nativeElement.querySelector('[data-testid="assistants-status"]').textContent)
      .toContain('Spelling is off.');
  });

  it('puts the switch back and shows the error when the server refuses', async () => {
    setEnabled = vi.fn().mockResolvedValue({ success: false, error: 'Not allowed' });
    const fixture = await render(true, false);
    const panel = fixture.componentInstance;

    await panel.toggle(panel.assistants()[0], false);
    fixture.detectChanges();

    expect(panel.assistants()[0].enabled).toBe(true);
    expect(fixture.nativeElement.querySelector('[data-testid="assistants-error"]').textContent)
      .toContain('Not allowed');
  });

  it('is read-only without Project[Edit]: switches disabled, nothing sent', async () => {
    const fixture = await render(false, false);
    const panel = fixture.componentInstance;

    const inputs = fixture.nativeElement.querySelectorAll('.assistant-list p-toggleswitch input');
    expect(inputs.length).toBe(4);
    inputs.forEach((i: HTMLInputElement) => expect(i.disabled).toBe(true));
    await panel.toggle(panel.assistants()[0], false);
    expect(setEnabled).not.toHaveBeenCalled();
  });

  it('Re-run analysis sends AnalyzeProject and announces that it is running', async () => {
    const fixture = await render(false, true);
    const button = fixture.nativeElement.querySelector('[data-testid="assistants-rerun"] button') as HTMLButtonElement;
    expect(button).not.toBeNull();

    button.click();
    await flush();
    fixture.detectChanges();

    expect(analyzeProject).toHaveBeenCalledWith('Acme');
    expect(fixture.nativeElement.querySelector('[data-testid="assistants-status"]').textContent)
      .toContain('Analysis is running');
  });

  it('disables Re-run analysis when every check is off, and says why', async () => {
    list = vi.fn().mockResolvedValue(FOUR.map(a => ({ ...a, enabled: false })));
    const fixture = await render(true, true);
    const button = fixture.nativeElement.querySelector('[data-testid="assistants-rerun"] button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('[data-testid="assistants-all-off"]')).not.toBeNull();

    await fixture.componentInstance.rerun();
    expect(analyzeProject).not.toHaveBeenCalled();

    // Switching one back on enables it again.
    await fixture.componentInstance.toggle(fixture.componentInstance.assistants()[0], true);
    fixture.detectChanges();
    expect(button.disabled).toBe(false);
    expect(fixture.nativeElement.querySelector('[data-testid="assistants-all-off"]')).toBeNull();
  });

  it('groups the switches under headings once AI review definitions are listed (#263)', async () => {
    list = vi.fn().mockResolvedValue([
      ...FOUR.map(a => ({ ...a, group: 'Lexical checks' })),
      { assistantId: 'ai-review-goal', displayName: 'AI goal review', enabled: true, group: 'AI review' },
    ]);
    const fixture = await render(true, true);
    const el: HTMLElement = fixture.nativeElement;
    const headings = Array.from(el.querySelectorAll('h3[data-testid^="assistant-group-"]'))
      .map(h => h.textContent?.trim());
    expect(headings).toEqual(['Lexical checks', 'AI review']);
    expect(el.querySelectorAll('.assistant-list').length).toBe(2);
    expect(el.querySelector('[data-testid="assistant-toggle-ai-review-goal"]')).not.toBeNull();
  });

  it('shows no group heading when every switch is in one group', async () => {
    const fixture = await render(true, true);
    expect(fixture.nativeElement.querySelector('h3[data-testid^="assistant-group-"]')).toBeNull();
  });

  it('does not count AI review switches for Re-run analysis, which runs the lexical checks (#263)', async () => {
    list = vi.fn().mockResolvedValue([
      ...FOUR.map(a => ({ ...a, enabled: false, group: 'Lexical checks' })),
      { assistantId: 'ai-review-goal', displayName: 'AI goal review', enabled: true, group: 'AI review' },
    ]);
    const fixture = await render(true, true);
    const button = fixture.nativeElement.querySelector('[data-testid="assistants-rerun"] button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
  });
  it('lists policies under their own heading and does not count them for Re-run analysis (#265)', async () => {
    list = vi.fn().mockResolvedValue([
      ...FOUR.map(a => ({ ...a, enabled: false, group: 'Lexical checks' })),
      { assistantId: 'ai-review-goal', displayName: 'AI goal review', enabled: true, group: 'AI review' },
      { assistantId: 'ai-policy-terminology', displayName: 'AI terminology policy', enabled: true, group: 'Policies' },
    ]);
    const fixture = await render(true, true);
    const headings = Array.from(fixture.nativeElement.querySelectorAll('h3[data-testid^="assistant-group-"]')).map((h: any) => h.textContent?.trim());
    expect(headings).toEqual(['Lexical checks', 'AI review', 'Policies']);
    const button = fixture.nativeElement.querySelector('[data-testid="assistants-rerun"] button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
  });


  it('hides Re-run analysis without Annotation[Edit]', async () => {
    const fixture = await render(true, false);
    expect(fixture.nativeElement.querySelector('[data-testid="assistants-rerun"]')).toBeNull();
  });

  it('says so when the assistants cannot be loaded', async () => {
    list = vi.fn().mockRejectedValue(new Error('boom'));
    const fixture = await render(true, true);
    expect(fixture.nativeElement.querySelector('[data-testid="assistants-load-error"]')).not.toBeNull();
  });

  // ---- #262: AI data handling --------------------------------------------------------

  it('renders the egress switch and one switch per redaction category', async () => {
    const fixture = await render(true, false);
    const el: HTMLElement = fixture.nativeElement;
    expect(dataHandling).toHaveBeenCalledWith('Acme');
    const labels = Array.from(el.querySelectorAll('.dh-row label')).map(l => l.textContent?.trim());
    expect(labels).toEqual([
      'Allow external AI providers',
      'Mask credentials (API keys, tokens, passwords)',
      'Mask email addresses',
      'Mask phone numbers',
      'Mask US social security numbers',
      'Mask payment card numbers',
    ]);
    const rows = fixture.componentInstance.dataHandlingRows();
    expect(rows.find(r => r.key === 'redaction.phone')?.enabled).toBe(false);
    expect(rows.find(r => r.key === 'egress.external')?.enabled).toBe(true);
  });

  it('sends EditProjectDataHandlingSetting when a data-handling switch is flipped', async () => {
    const fixture = await render(true, false);
    const panel = fixture.componentInstance;

    await panel.toggleDataHandling(panel.dataHandlingRows()[0], false);
    fixture.detectChanges();

    expect(setDataHandling).toHaveBeenCalledWith('Acme', 'egress.external', false);
    expect(panel.dataHandling()?.externalProviderAllowed).toBe(false);
    expect(fixture.nativeElement.querySelector('[data-testid="assistants-status"]').textContent)
      .toContain('Allow external AI providers: off.');
  });

  it('puts a data-handling switch back when the server refuses', async () => {
    setDataHandling = vi.fn().mockResolvedValue({ success: false, error: 'Not allowed' });
    const fixture = await render(true, false);
    const panel = fixture.componentInstance;
    const email = panel.dataHandlingRows().find(r => r.key === 'redaction.email')!;

    await panel.toggleDataHandling(email, false);
    fixture.detectChanges();

    expect(panel.dataHandling()?.redaction['email']).toBe(true);
    expect(fixture.nativeElement.querySelector('[data-testid="assistants-error"]').textContent)
      .toContain('Not allowed');
  });

  it('data-handling switches are read-only without Project[Edit]', async () => {
    const fixture = await render(false, false);
    const inputs = fixture.nativeElement.querySelectorAll('.dh-list p-toggleswitch input');
    expect(inputs.length).toBe(6);
    inputs.forEach((i: HTMLInputElement) => expect(i.disabled).toBe(true));
    await fixture.componentInstance.toggleDataHandling(
      fixture.componentInstance.dataHandlingRows()[0], false);
    expect(setDataHandling).not.toHaveBeenCalled();
  });

  it('hides the section when the settings cannot be read', async () => {
    dataHandling = vi.fn().mockRejectedValue(new Error('boom'));
    const fixture = await render(true, false);
    expect(fixture.nativeElement.querySelector('[data-testid="data-handling"]')).toBeNull();
    expect(fixture.nativeElement.querySelectorAll('.assistant-list p-toggleswitch').length).toBe(4);
  });
});
