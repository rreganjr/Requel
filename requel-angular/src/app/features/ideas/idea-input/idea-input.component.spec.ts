import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { IdeaInputComponent } from './idea-input.component';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { FileUploadModule } from 'primeng/fileupload';

describe('IdeaInputComponent', () => {
  let component: IdeaInputComponent;
  let fixture: ComponentFixture<IdeaInputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        TextareaModule,
        FileUploadModule,
        IdeaInputComponent
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IdeaInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
