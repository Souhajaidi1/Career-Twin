import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { toApiError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ApiError } from '../../core/models';
import { ErrorBox } from '../../shared/ui';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink, ErrorBox],
  template: `
    <div class="auth-wrap">
      <form class="card auth-card" (ngSubmit)="submit()" #f="ngForm">
        <h1>Sign in to CareerTwin</h1>
        <p class="muted">Your evidence-based career model.</p>
        <app-error-box [error]="error()" />
        <div class="field">
          <label for="email">Email</label>
          <input id="email" name="email" type="email" autocomplete="username" required [(ngModel)]="email" />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input id="password" name="password" type="password" autocomplete="current-password" required [(ngModel)]="password" />
        </div>
        <button class="primary" type="submit" [disabled]="busy() || f.invalid">{{ busy() ? 'Signing in...' : 'Sign in' }}</button>
        <p class="small muted" style="margin-top: 16px">No account? <a routerLink="/register">Register</a></p>
      </form>
    </div>
  `,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly email = signal('');
  readonly password = signal('');
  readonly busy = signal(false);
  readonly error = signal<ApiError | null>(null);

  submit() {
    this.busy.set(true);
    this.error.set(null);
    this.auth.login(this.email(), this.password()).subscribe({
      next: () => void this.router.navigateByUrl('/dashboard'),
      error: (e) => {
        this.error.set(toApiError(e));
        this.busy.set(false);
      },
    });
  }
}
