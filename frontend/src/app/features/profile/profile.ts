import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, toApiError } from '../../core/api.service';
import { ApiError } from '../../core/models';
import { ErrorBox } from '../../shared/ui';

@Component({
  selector: 'app-profile',
  imports: [FormsModule, ErrorBox],
  template: `
    <div class="page-head">
      <h1>Profile</h1>
      <p>Context used when interpreting your evidence.</p>
    </div>
    <form class="card" (ngSubmit)="save()">
      <app-error-box [error]="error()" />
      @if (saved()) {
        <div class="notice" role="status">Profile saved.</div>
      }
      <div class="form-grid">
        <div class="field"><label for="p-name">Name</label><input id="p-name" name="name" required [(ngModel)]="name" /></div>
        <div class="field"><label for="p-role">Current role</label><input id="p-role" name="currentRole" [(ngModel)]="currentRole" /></div>
        <div class="field"><label for="p-exp">Years of experience</label><input id="p-exp" name="experienceYears" type="number" min="0" step="0.5" [(ngModel)]="experienceYears" /></div>
        <div class="field"><label for="p-loc">Location</label><input id="p-loc" name="location" [(ngModel)]="location" /></div>
      </div>
      <button class="primary" type="submit" [disabled]="busy()">Save profile</button>
    </form>
  `,
})
export class ProfilePage implements OnInit {
  private readonly api = inject(ApiService);
  readonly name = signal('');
  readonly currentRole = signal('');
  readonly experienceYears = signal<number | null>(0);
  readonly location = signal('');
  readonly busy = signal(false);
  readonly saved = signal(false);
  readonly error = signal<ApiError | null>(null);

  ngOnInit() {
    this.api.getProfile().subscribe({
      next: (p) => {
        this.name.set(p.name ?? '');
        this.currentRole.set(p.currentRole ?? '');
        this.experienceYears.set(p.experienceYears ?? 0);
        this.location.set(p.location ?? '');
      },
      error: (e) => this.error.set(toApiError(e)),
    });
  }

  save() {
    this.busy.set(true);
    this.saved.set(false);
    this.error.set(null);
    this.api
      .putProfile({ name: this.name(), currentRole: this.currentRole(), experienceYears: this.experienceYears() ?? 0, location: this.location() })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.saved.set(true);
        },
        error: (e) => {
          this.error.set(toApiError(e));
          this.busy.set(false);
        },
      });
  }
}
