import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { InteractionListComponent } from './interaction-list.component';

describe('InteractionListComponent', () => {
  let component: InteractionListComponent;
  let fixture: ComponentFixture<InteractionListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InteractionListComponent],
      // component injects ActivatedRoute + InteractionService (HttpClient)
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InteractionListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
