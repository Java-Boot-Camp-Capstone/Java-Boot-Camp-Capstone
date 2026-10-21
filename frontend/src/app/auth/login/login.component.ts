import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // No pre-filled credentials in source; demo accounts are provided via env/secrets.
  username = '';
  password = '';
  errorMessage = '';
  loading = false;

  login(): void {
    if (!this.username.trim() || !this.password) {
      this.errorMessage = 'Enter your username and password.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.login(
      this.username.trim(),
      this.password
    ).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/customers']);
      },
      error: () => {
        this.loading = false;
        this.errorMessage =
          'Login failed. Check your credentials and backend connection.';
      }
    });
  }
}