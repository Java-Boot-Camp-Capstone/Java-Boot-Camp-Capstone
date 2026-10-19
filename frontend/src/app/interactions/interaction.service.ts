import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Interaction } from './interaction.model';

@Injectable({
  providedIn: 'root'
})
export class InteractionService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl =
    `${environment.apiBaseUrl}/interactions`;

  getInteractions(
    customerId: string
  ): Observable<Interaction[]> {
    const params = new HttpParams()
      .set('customerId', customerId);

    return this.http.get<Interaction[]>(
      this.apiUrl,
      { params }
    );
  }

  createInteraction(
    interaction: Interaction
  ): Observable<Interaction> {
    return this.http.post<Interaction>(
      this.apiUrl,
      interaction
    );
  }
}