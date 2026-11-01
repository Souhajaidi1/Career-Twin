import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <header class="shell-head">
        <span class="brand">CareerTwin</span>
        <div class="row">
          <span class="small">{{ auth.user()?.fullName ?? '' }}</span>
          <button type="button" (click)="auth.logout()">Log out</button>
        </div>
      </header>
      <nav class="shell-nav" aria-label="Main">
        <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
        <a routerLink="/skills" routerLinkActive="active">Skills</a>
        <a routerLink="/evidence" routerLinkActive="active">Evidence</a>
        <a routerLink="/jobs" routerLinkActive="active">Jobs</a>
        <a routerLink="/roadmap" routerLinkActive="active">Roadmap</a>
        <a routerLink="/profile" routerLinkActive="active">Profile</a>
      </nav>
      <main class="shell-main"><router-outlet /></main>
    </div>
  `,
})
export class Shell implements OnInit {
  readonly auth = inject(AuthService);
  ngOnInit() {
    if (!this.auth.user()) this.auth.loadUser();
  }
}
