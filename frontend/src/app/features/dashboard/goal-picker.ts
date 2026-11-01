import { Component, OnInit, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, toApiError } from '../../core/api.service';
import { ApiError, Catalog } from '../../core/models';
import { ErrorBox } from '../../shared/ui';

@Component({
  selector: 'app-goal-picker',
  imports: [FormsModule, ErrorBox],
  template: `
    <form class="card" (ngSubmit)="save()">
      <h2>Choose your target role</h2>
      <p class="muted">Readiness and gaps are measured against this goal.</p>
      <app-error-box [error]="error()" />
      @if (catalog(); as c) {
        <div class="form-grid">
          <div class="field">
            <label for="role">Role</label>
            <select id="role" name="role" [(ngModel)]="roleId" required>
              <option value="" disabled>Select a role</option>
              @for (r of c.roles; track r.id) {
                <option [value]="r.id">{{ r.name }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label for="seniority">Seniority</label>
            <select id="seniority" name="seniority" [(ngModel)]="seniority" required>
              <option value="" disabled>Select seniority</option>
              @for (s of c.seniorities; track s) {
                <option [value]="s">{{ s }}</option>
              }
            </select>
          </div>
        </div>
        <button class="primary" type="submit" [disabled]="busy() || !roleId() || !seniority()">Set goal</button>
      } @else {
        <p class="muted">Loading catalog...</p>
      }
    </form>
  `,
})
export class GoalPicker implements OnInit {
  private readonly api = inject(ApiService);
  readonly saved = output<void>();
  readonly catalog = signal<Catalog | null>(null);
  readonly roleId = signal('');
  readonly seniority = signal('');
  readonly busy = signal(false);
  readonly error = signal<ApiError | null>(null);

  ngOnInit() {
    this.api.catalog().subscribe({ next: (c) => this.catalog.set(c), error: (e) => this.error.set(toApiError(e)) });
  }

  save() {
    this.busy.set(true);
    this.error.set(null);
    this.api.createGoal({ roleId: this.roleId(), seniority: this.seniority() }).subscribe({
      next: () => {
        this.busy.set(false);
        this.saved.emit();
      },
      error: (e) => {
        this.error.set(toApiError(e));
        this.busy.set(false);
      },
    });
  }
}
