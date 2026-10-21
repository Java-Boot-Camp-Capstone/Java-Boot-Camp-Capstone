import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../environments/environment';

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  roles: string[];
}

/**
 * Exchanges credentials for a JWT issued by the Spring Boot API (POST /api/auth/login).
 * The UI only stores and forwards the token - the API is the security boundary.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly loginUrl = `${environment.apiRoot}/auth/login`;

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(this.loginUrl, { username, password })
      .pipe(tap((response) => localStorage.setItem('token', response.token)));
  }

  logout(): void {
    localStorage.removeItem('token');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }
}
