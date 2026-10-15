import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ProjectAssistantsPanelComponent } from './project-assistants-panel';
import { ProjectAssistantsService } from '../../core/project-assistants.service';
import { expectNoAxeViolations } from '../../shared/testing/a11y';

const flush = () => new Promise(r => setTimeout(r, 0));

// #268: the switches carry their labels, and the read-only state (no Project[Edit]) is where a
// disabled control without a name would show up.
describe('ProjectAssistantsPanelComponent - accessibility', () => {
  async function render(canEdit: boolean) {
    TestBed.configureTestingModule({
      imports: [ProjectAssistantsPanelComponent],
      providers: [
        provideNoopAnimations(),
        { provide: ProjectAssistantsService, useValue: {
            list: vi.fn().mockResolvedValue([
              { assistantId: 'legacy-lexical', displayName: 'Spelling', enabled: true },
              { assistantId: 'legacy-lexical-vague-word', displayName: 'Vague words', enabled: false },
            ]),
            setEnabled: vi.fn(),
            analyzeProject: vi.fn(),
          } },
      ],
    });
    const fixture = TestBed.createComponent(ProjectAssistantsPanelComponent);
    fixture.componentRef.setInput('projectName', 'Acme');
    fixture.componentRef.setInput('canEdit', canEdit);
    fixture.componentRef.setInput('canAnalyze', canEdit);
    fixture.detectChanges();
    await flush();
    fixture.detectChanges();
    return fixture;
  }

  it('has no axe-core violations for an editor', async () => {
    const fixture = await render(true);
    await expectNoAxeViolations(fixture.nativeElement);
  });

  it('has no axe-core violations when read-only', async () => {
    const fixture = await render(false);
    await expectNoAxeViolations(fixture.nativeElement);
  });
});
