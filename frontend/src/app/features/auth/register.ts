import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { toApiError } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ApiError } from '../../core/models';
import { ErrorBox } from '../../shared/ui';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink, ErrorBox],
  template: `
    <div class="auth-wrap">
      <form class="card auth-card" (ngSubmit)="submit()" #f="ngForm">
        <h1>Create your account</h1>
        <app-error-box [error]="error()" />
        <div class="field">
          <label for="fullName">Full name</label>
          <input id="fullName" name="fullName" autocomplete="name" required [(ngModel)]="fullName" />
        </div>
        <div class="field">
          <label for="email">Email</label>
          <input id="email" name="email" type="email" autocomplete="username" required [(ngModel)]="email" />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input id="password" name="password" type="password" autocomplete="new-password" required minlength="12" [(ngModel)]="password" />
          <div class="hint">At least 12 characters.</div>
        </div>
        <button class="primary" type="submit" [disabled]="busy() || f.invalid">{{ busy() ? 'Creating...' : 'Register' }}</button>
        <p class="small muted" style="margin-top: 16px">Already registered? <a routerLink="/login">Sign in</a></p>
      </form>
    </div>
  `,
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly fullName = signal('');
  readonly email = signal('');
  readonly password = signal('');
  readonly busy = signal(false);
  readonly error = signal<ApiError | null>(null);

  submit() {
    this.busy.set(true);
    this.error.set(null);
    const body = { email: this.email(), password: this.password(), fullName: this.fullName() };
    this.auth.register(body).subscribe({
      next: () =>
        this.auth.login(body.email, body.password).subscribe({
          next: () => void this.router.navigateByUrl('/dashboard'),
          error: (e) => {
            this.error.set(toApiError(e));
            this.busy.set(false);
          },
        }),
      error: (e) => {
        this.error.set(toApiError(e));
        this.busy.set(false);
      },
    });
  }
}
