import { NgTemplateOutlet } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService, toApiError } from '../../core/api.service';
import { ApiError, Job, JobMatch } from '../../core/models';
import { Badge, ErrorBox, Num, Pct, ScoreBar } from '../../shared/ui';

@Component({
  selector: 'app-job-detail',
  imports: [NgTemplateOutlet, RouterLink, Badge, ErrorBox, ScoreBar, Num, Pct],
  template: `
    <p><a routerLink="/jobs">Back to jobs</a></p>
    <app-error-box [error]="error()" />
    @if (job(); as j) {
      <div class="page-head row between">
        <div>
          <h1>{{ j.title }}</h1>
          <p>{{ j.company }}{{ j.location ? ', ' + j.location : '' }}</p>
          @if (j.url) {
            <p class="small"><a [href]="j.url" target="_blank" rel="noopener noreferrer">Original posting</a></p>
          }
        </div>
        <button class="danger" type="button" (click)="remove()">Delete job</button>
      </div>
      <p class="small muted">
        Extracted: seniority {{ j.requirements.seniority }}, years {{ j.requirements.years ?? 'not stated' }}, degree {{ j.requirements.degree ?? 'not stated' }}.
      </p>
    }
    <app-error-box [error]="matchError()" />
    @if (match(); as m) {
      <div class="grid two">
        <section class="card">
          <h2>Match score</h2>
          <div class="big-number">{{ m.score | num }}</div>
          <p class="explain">
            Completeness {{ m.completeness | pct: 'auto' }}: how much of the posting could be understood and scored. Required level {{ m.requiredLevel | num }}.
          </p>
        </section>
        <section class="card">
          <h2>Category breakdown</h2>
          <div class="stack">
            @for (c of m.categories; track c.key) {
              <div>
                <app-score-bar [label]="c.label" [value]="c.value" [text]="(c.value | pct)" />
                <div class="explain">Effective weight {{ c.effective | pct: 'auto' }}, contributes {{ c.points | num }} points.</div>
              </div>
            }
          </div>
          @if (m.excluded.length) {
            <p class="small muted">Excluded (no data in the posting): {{ m.excluded.join(', ') }}.</p>
          }
        </section>
      </div>
      <div class="grid three">
        <section class="card">
          <h2>Strong ({{ m.strong.length }})</h2>
          @for (r of m.strong; track r.skillId) {
            <ng-container *ngTemplateOutlet="row; context: { $implicit: r }" />
          } @empty { <p class="empty">None.</p> }
        </section>
        <section class="card">
          <h2>Partial ({{ m.partial.length }})</h2>
          @for (r of m.partial; track r.skillId) {
            <ng-container *ngTemplateOutlet="row; context: { $implicit: r }" />
          } @empty { <p class="empty">None.</p> }
        </section>
        <section class="card">
          <h2>Missing ({{ m.missing.length }})</h2>
          @for (r of m.missing; track r.skillId) {
            <ng-container *ngTemplateOutlet="row; context: { $implicit: r }" />
          } @empty { <p class="empty">None.</p> }
        </section>
      </div>
    }
    <ng-template #row let-r>
      <div class="item">
        <div class="row between">
          <strong>{{ r.name }}</strong>
          <app-badge [tone]="r.required ? 'bad' : 'neutral'">{{ r.required ? 'required' : 'preferred' }}</app-badge>
        </div>
        <app-score-bar [value]="r.level" [max]="99" [marker]="r.need" [text]="(r.level | num) + ' / ' + (r.need | num)" />
        <div class="hint">Your level versus level needed (marker).</div>
      </div>
    </ng-template>
  `,
})
export class JobDetail implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly id = input.required<string>();
  readonly job = signal<Job | null>(null);
  readonly match = signal<JobMatch | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly matchError = signal<ApiError | null>(null);

  ngOnInit() {
    this.api.getJob(this.id()).subscribe({ next: (j) => this.job.set(j), error: (e) => this.error.set(toApiError(e)) });
    this.api.jobMatch(this.id()).subscribe({ next: (m) => this.match.set(m), error: (e) => this.matchError.set(toApiError(e)) });
  }

  remove() {
    if (!confirm('Delete this job?')) return;
    this.api.deleteJob(this.id()).subscribe({
      next: () => void this.router.navigateByUrl('/jobs'),
      error: (e) => this.error.set(toApiError(e)),
    });
  }
}
