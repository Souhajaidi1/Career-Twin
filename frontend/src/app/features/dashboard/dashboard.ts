import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, toApiError } from '../../core/api.service';
import { ApiError, GapStatus, Twin } from '../../core/models';
import { Badge, ErrorBox, Num, Pct, STATUS_TONE, ScoreBar, Tone } from '../../shared/ui';
import { GoalPicker } from './goal-picker';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, Badge, ErrorBox, ScoreBar, GoalPicker, Pct, Num],
  template: `
    <div class="page-head">
      <h1>Dashboard</h1>
      @if (twin()?.goal; as g) {
        <p>Target: {{ g.roleName }} ({{ g.seniority }})</p>
      }
    </div>
    <app-error-box [error]="error()" />
    @if (loading()) {
      <p class="muted">Loading...</p>
    }
    @if (twin(); as t) {
      @if (!t.goal || changing()) {
        <app-goal-picker (saved)="onGoalSaved()" />
      }
      @if (t.goal && t.readiness; as r) {
        <div class="grid two">
          <section class="card">
            <h2>Readiness</h2>
            <div class="big-number">{{ r.score | num }}</div>
            <p class="explain">Computed by the scoring service from your evidence and the weighted parts below.</p>
            <button type="button" (click)="changing.set(!changing())">{{ changing() ? 'Cancel' : 'Change goal' }}</button>
          </section>
          <section class="card">
            <h2>How this is calculated</h2>
            <div class="stack">
              @for (p of r.parts; track p.key) {
                <div>
                  <app-score-bar [label]="p.label" [value]="p.value" [text]="(p.value | pct)" />
                  <div class="explain">Weight {{ p.weight | pct: 'auto' }}. {{ p.detail }}</div>
                </div>
              }
            </div>
          </section>
        </div>
        <section class="card">
          <div class="row between">
            <h2>Top gaps</h2>
            <a routerLink="/skills">All skills and gaps</a>
          </div>
          @for (g of topGaps(); track g.skillId) {
            <div class="item">
              <div class="row between">
                <strong>{{ g.name }}</strong>
                <app-badge [tone]="tone(g.status)">{{ g.status }}</app-badge>
              </div>
              <app-score-bar [value]="g.current" [max]="99" [marker]="g.target" [text]="(g.current | num) + ' / ' + (g.target | num)" />
              <div class="hint">Current level versus target (marker). Category: {{ g.category }}.</div>
            </div>
          } @empty {
            <p class="empty">No gaps reported.</p>
          }
        </section>
      }
    }
  `,
})
export class Dashboard implements OnInit {
  private readonly api = inject(ApiService);
  readonly twin = signal<Twin | null>(null);
  readonly loading = signal(true);
  readonly error = signal<ApiError | null>(null);
  readonly changing = signal(false);
  readonly topGaps = computed(() => (this.twin()?.gaps ?? []).slice(0, 5));

  ngOnInit() {
    this.load();
  }

  tone(s: GapStatus): Tone {
    return STATUS_TONE[s];
  }

  load() {
    this.loading.set(true);
    this.api.twin().subscribe({
      next: (t) => {
        this.twin.set(t);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(toApiError(e));
        this.loading.set(false);
      },
    });
  }

  onGoalSaved() {
    this.changing.set(false);
    this.load();
  }
}
