
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Interaction } from './interaction.model';

@Injectable({
  providedIn: 'root'
})
export class InteractionService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl =
    `${environment.apiBaseUrl.replace(/\/$/, '')}/interactions`;

  getInteractions(customerId?: string): Observable<Interaction[]> {
    const url = customerId
      ? `${this.apiUrl}?customerId=${encodeURIComponent(customerId)}`
      : this.apiUrl;

    return this.http.get<Interaction[]>(url);
  }

  createInteraction(
    interaction: {
      customerId: string;
      interactionType: string;
      summary: string;
      correlationId?: string;
    }
  ): Observable<Interaction> {
    return this.http.post<Interaction>(
      this.apiUrl,
      interaction
    );
  }
}
