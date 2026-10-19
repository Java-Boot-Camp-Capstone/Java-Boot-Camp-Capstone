import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InteractionFormComponent } from './interaction-form.component';

describe('InteractionFormComponent', () => {
  let component: InteractionFormComponent;
  let fixture: ComponentFixture<InteractionFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InteractionFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InteractionFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
