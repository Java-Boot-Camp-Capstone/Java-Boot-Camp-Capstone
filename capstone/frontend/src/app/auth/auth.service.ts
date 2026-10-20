
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';

export interface LoginResponse {
  token: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly demoUsername = 'crm';
  private readonly demoPassword = 'crm';

  login(
    username: string,
    password: string
  ): Observable<LoginResponse> {
    if (
      username !== this.demoUsername ||
      password !== this.demoPassword
    ) {
      return throwError(() =>
        new Error('Invalid username or password.')
      );
    }

    const response: LoginResponse = {
      token: 'lab-demo-token'
    };

    localStorage.setItem('token', response.token);

    return of(response);
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
