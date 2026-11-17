import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { IdeaInputComponent } from './idea-input.component';
import { IdeaService, IdeaServiceError } from '../idea.service';

describe('IdeaInputComponent', () => {
  let component: IdeaInputComponent;
  let fixture: ComponentFixture<IdeaInputComponent>;
  const startNewProject = vi.fn();

  beforeEach(async () => {
    startNewProject.mockReset();
    await TestBed.configureTestingModule({
      imports: [IdeaInputComponent],
      providers: [provideRouter([]), { provide: IdeaService, useValue: { startNewProject } }],
    }).compileComponents();
    fixture = TestBed.createComponent(IdeaInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('is invalid until a name and a 10+ char description are given', () => {
    expect(component.ideaForm.invalid).toBe(true);
    component.ideaForm.setValue({ projectName: 'P', text: 'short' });
    expect(component.ideaForm.invalid).toBe(true);
    component.ideaForm.setValue({ projectName: 'P', text: 'long enough text' });
    expect(component.ideaForm.valid).toBe(true);
  });

  it('starts the project and opens its review page', async () => {
    startNewProject.mockResolvedValue({ projectName: 'Gym', ideaId: 'abc' });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.ideaForm.setValue({ projectName: ' Gym ', text: 'long enough text' });
    await component.onSubmit();
    expect(startNewProject).toHaveBeenCalledWith('Gym', 'long enough text');
    expect(navigate).toHaveBeenCalledWith(['/ideas', 'Gym', 'abc']);
    expect(component.isSubmitting).toBe(false);
  });

  it('shows the server error and stays on the page', async () => {
    startNewProject.mockRejectedValue(new IdeaServiceError('Project name taken'));
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.ideaForm.setValue({ projectName: 'P', text: 'long enough text' });
    await component.onSubmit();
    expect(component.submissionError).toBe('Project name taken');
    expect(navigate).not.toHaveBeenCalled();
    expect(component.isSubmitting).toBe(false);
  });
});
