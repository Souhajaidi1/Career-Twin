import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { ApiService } from './api.service';
import { RegisterRequest, User } from './models';

const KEY = 'careertwin.token';

function readToken(): string | null {
  try {
    return sessionStorage.getItem(KEY);
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly token = signal<string | null>(readToken());
  readonly user = signal<User | null>(null);

  isLoggedIn(): boolean {
    return !!this.token();
  }

  login(email: string, password: string) {
    return this.api.login({ email, password }).pipe(
      tap((res) => {
        try {
          sessionStorage.setItem(KEY, res.accessToken);
        } catch {
          /* storage unavailable: token lives in memory only */
        }
        this.token.set(res.accessToken);
      }),
    );
  }

  register(body: RegisterRequest) {
    return this.api.register(body);
  }

  loadUser() {
    this.api.me().subscribe({ next: (u) => this.user.set(u), error: () => undefined });
  }

  /** Clears the session; redirect is optional so the guard/interceptor can share it. */
  logout(redirect = true) {
    try {
      sessionStorage.removeItem(KEY);
    } catch {
      /* ignore */
    }
    this.token.set(null);
    this.user.set(null);
    if (redirect) void this.router.navigateByUrl('/login');
  }
}
