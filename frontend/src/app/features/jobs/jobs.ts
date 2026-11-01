import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, toApiError } from '../../core/api.service';
import { ApiError, JobSummary } from '../../core/models';
import { Badge, ErrorBox, Num } from '../../shared/ui';

@Component({
  selector: 'app-jobs',
  imports: [FormsModule, RouterLink, Badge, ErrorBox, Num],
  template: `
    <div class="page-head">
      <h1>Jobs</h1>
      <p>Paste a job description to see how your evidence matches it.</p>
    </div>
    <form class="card" (ngSubmit)="add()">
      <h2>Add a job</h2>
      <app-error-box [error]="formError()" />
      <div class="form-grid">
        <div class="field"><label for="j-title">Title</label><input id="j-title" name="title" required [(ngModel)]="title" /></div>
        <div class="field"><label for="j-company">Company</label><input id="j-company" name="company" [(ngModel)]="company" /></div>
        <div class="field"><label for="j-loc">Location</label><input id="j-loc" name="location" [(ngModel)]="location" /></div>
        <div class="field"><label for="j-url">URL</label><input id="j-url" name="url" type="url" [(ngModel)]="url" /></div>
      </div>
      <div class="field">
        <label for="j-raw">Job description</label>
        <textarea id="j-raw" name="raw" required [(ngModel)]="raw"></textarea>
      </div>
      <button class="primary" type="submit" [disabled]="busy() || !title().trim() || !raw().trim()">{{ busy() ? 'Analysing...' : 'Add job' }}</button>
    </form>

    <app-error-box [error]="error()" />
    <section class="card">
      <h2>Saved jobs</h2>
      @if (loading()) {
        <p class="muted">Loading...</p>
      }
      @for (j of jobs(); track j.id) {
        <div class="item row between">
          <div>
            <a [routerLink]="['/jobs', j.id]"><strong>{{ j.title }}</strong></a>
            <div class="small muted">{{ j.company }}{{ j.location ? ', ' + j.location : '' }}</div>
          </div>
          <div class="row">
            @if (j.score !== null) {
              <app-badge tone="info">Match {{ j.score | num }}</app-badge>
            } @else {
              <app-badge>No match yet</app-badge>
            }
            <button class="danger" type="button" (click)="remove(j)">Delete</button>
          </div>
        </div>
      } @empty {
        @if (!loading()) {
          <p class="empty">No jobs saved yet.</p>
        }
      }
    </section>
  `,
})
export class Jobs implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly jobs = signal<JobSummary[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal<ApiError | null>(null);
  readonly formError = signal<ApiError | null>(null);
  readonly title = signal('');
  readonly company = signal('');
  readonly location = signal('');
  readonly url = signal('');
  readonly raw = signal('');

  ngOnInit() {
    this.reload();
  }

  reload() {
    this.api.listJobs().subscribe({
      next: (l) => {
        this.jobs.set(l);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(toApiError(e));
        this.loading.set(false);
      },
    });
  }

  add() {
    this.busy.set(true);
    this.formError.set(null);
    this.api
      .createJob({ title: this.title().trim(), company: this.company().trim(), location: this.location().trim(), url: this.url().trim(), raw: this.raw() })
      .subscribe({
        next: (job) => {
          this.busy.set(false);
          void this.router.navigate(['/jobs', job.id]);
        },
        error: (e) => {
          this.formError.set(toApiError(e));
          this.busy.set(false);
        },
      });
  }

  remove(j: JobSummary) {
    if (!confirm(`Delete "${j.title}"?`)) return;
    this.api.deleteJob(j.id).subscribe({
      next: () => this.jobs.update((l) => l.filter((x) => x.id !== j.id)),
      error: (e) => this.error.set(toApiError(e)),
    });
  }
}
