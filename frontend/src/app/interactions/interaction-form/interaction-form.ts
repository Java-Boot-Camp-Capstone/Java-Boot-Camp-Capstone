import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { InteractionService } from '../interaction.service';

@Component({
  selector: 'app-interaction-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './interaction-form.component.html',
  styleUrl: './interaction-form.component.css'
})
export class InteractionFormComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly interactionService = inject(InteractionService);

  customerId =
    this.route.snapshot.queryParamMap.get('customerId')
    ?? 'CUS-1001';

  type = 'CALL';
  notes = '';
  saving = false;
  errorMessage = '';

  submit(): void {
    if (!this.customerId.trim() || !this.notes.trim()) {
      this.errorMessage =
        'Customer ID and interaction notes are required.';
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    this.interactionService.createInteraction({
      customerId: this.customerId.trim(),
      type: this.type,
      notes: this.notes.trim()
    }).subscribe({
      next: () => {
        this.saving = false;

        this.router.navigate(['/interactions'], {
          queryParams: {
            customerId: this.customerId.trim()
          }
        });
      },
      error: () => {
        this.saving = false;
        this.errorMessage =
          'Could not save the interaction. Check the API and request format.';
      }
    });
  }
}