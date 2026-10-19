import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { InteractionService } from '../interaction.service';
import { Interaction } from '../interaction.model';

@Component({
  selector: 'app-interaction-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './interaction-list.component.html',
  styleUrl: './interaction-list.component.css'
})
export class InteractionListComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly interactionService = inject(InteractionService);

  customerId = 'CUS-1001';
  interactions: Interaction[] = [];
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    this.customerId =
      this.route.snapshot.queryParamMap.get('customerId')
      ?? 'CUS-1001';

    this.loadInteractions();
  }

  loadInteractions(): void {
    this.loading = true;
    this.errorMessage = '';

    this.interactionService
      .getInteractions(this.customerId)
      .subscribe({
        next: (interactions) => {
          this.interactions = interactions;
          this.loading = false;
        },
        error: () => {
          this.errorMessage =
            'Could not load interactions. Check the backend API.';
          this.loading = false;
        }
      });
  }
}