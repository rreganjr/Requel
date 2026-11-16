import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { IdeaInputComponent } from './idea-input.component';
import { IdeaService } from '../idea.service';

describe('IdeaInputComponent', () => {
  let component: IdeaInputComponent;
  let fixture: ComponentFixture<IdeaInputComponent>;
  const submitIdea = vi.fn();

  beforeEach(async () => {
    submitIdea.mockReset();
    await TestBed.configureTestingModule({
      imports: [IdeaInputComponent],
      providers: [provideRouter([]), { provide: IdeaService, useValue: { submitIdea } }],
    }).compileComponents();
    fixture = TestBed.createComponent(IdeaInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('is invalid until project and a 10+ char description are given', () => {
    expect(component.ideaForm.invalid).toBe(true);
    component.ideaForm.setValue({ projectName: 'P', text: 'short' });
    expect(component.ideaForm.invalid).toBe(true);
    component.ideaForm.setValue({ projectName: 'P', text: 'long enough text' });
    expect(component.ideaForm.valid).toBe(true);
  });

  it('shows the error from a failed command', async () => {
    submitIdea.mockResolvedValue({ success: false, error: 'nope' });
    component.ideaForm.setValue({ projectName: 'P', text: 'long enough text' });
    await component.onSubmit();
    expect(component.submissionError).toBe('nope');
    expect(component.submissionSuccess).toBe(false);
  });
});
