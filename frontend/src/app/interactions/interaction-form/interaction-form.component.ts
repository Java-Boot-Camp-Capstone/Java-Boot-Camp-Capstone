
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { InteractionService } from '../interaction.service';

@Component({
  selector: 'app-interaction-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './interaction-form.component.html',
  styleUrl: './interaction-form.component.css'
})
export class InteractionFormComponent {
  private readonly interactionService = inject(InteractionService);

  customerId = 'CUS-1001';
  interactionType = 'CALL';
  summary = '';
  correlationId = 'lab-request-001';

  saving = false;
  errorMessage = '';
  successMessage = '';

  submitForm(): void {
    if (!this.summary.trim()) {
      this.errorMessage = 'Please enter a summary.';
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.interactionService.createInteraction({
      customerId: this.customerId,
      interactionType: this.interactionType,
      summary: this.summary.trim(),
      correlationId: this.correlationId.trim() || undefined
    }).subscribe({
      next: () => {
        this.saving = false;
        this.successMessage = 'Interaction created successfully!';
        this.summary = '';
      },
      error: (error) => {
        console.error('Failed to create interaction:', error);
        this.saving = false;
        this.errorMessage =
          error.error?.message ||
          'Could not create interaction. Check the backend API.';
      }
    });
  }
}
