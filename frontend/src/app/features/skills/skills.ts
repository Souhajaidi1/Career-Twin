import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, toApiError } from '../../core/api.service';
import { ApiError, GapStatus, Twin } from '../../core/models';
import { Badge, ErrorBox, Num, Pct, STATUS_TONE, ScoreBar, Tone } from '../../shared/ui';

@Component({
  selector: 'app-skills',
  imports: [FormsModule, Badge, ErrorBox, ScoreBar, Num, Pct],
  template: `
    <div class="page-head">
      <h1>Skills</h1>
      <p>Your twin: each level is built only from your evidence. Expand a row to see where it came from.</p>
    </div>
    <app-error-box [error]="error()" />
    @if (loading()) {
      <p class="muted">Loading...</p>
    }
    @if (twin(); as t) {
      <div class="field" style="max-width: 280px">
        <label for="cat">Category</label>
        <select id="cat" [ngModel]="filter()" (ngModelChange)="filter.set($event)">
          <option value="all">All categories</option>
          @for (c of categories(); track c) {
            <option [value]="c">{{ c }}</option>
          }
        </select>
      </div>

      <section class="card">
        <h2>Skill levels</h2>
        @for (l of shown(); track l.skillId) {
          <div class="item">
            <div class="row between">
              <strong>{{ l.name }} <span class="small muted">{{ l.category }}</span></strong>
              <div class="row">
                @if (l.simulated) {
                  <app-badge tone="info">simulated</app-badge>
                }
                <span class="small muted">Confidence {{ l.confidence | pct: 'auto' }}</span>
                <button
                  class="link"
                  type="button"
                  [attr.aria-expanded]="isOpen(l.skillId)"
                  [attr.aria-controls]="'c-' + l.skillId"
                  (click)="toggle(l.skillId)"
                >
                  {{ isOpen(l.skillId) ? 'Hide sources' : 'Show sources (' + l.contributions.length + ')' }}
                </button>
              </div>
            </div>
            <app-score-bar [value]="l.level" [max]="99" [text]="(l.level | num) + ' / 99'" />
            @if (isOpen(l.skillId)) {
              <div [id]="'c-' + l.skillId" class="table-wrap" style="margin-top: 8px">
                <table>
                  <thead>
                    <tr><th>Evidence</th><th>Type</th><th class="num">Points</th><th>How it was calculated</th></tr>
                  </thead>
                  <tbody>
                    @for (c of l.contributions; track c.evidenceId) {
                      <tr>
                        <td>{{ c.title }}@if (c.date) { <div class="small muted">{{ c.date }}</div> }</td>
                        <td>{{ c.type }}</td>
                        <td class="num">{{ c.points | num }}</td>
                        <td>{{ c.detail }}</td>
                      </tr>
                    } @empty {
                      <tr><td colspan="4" class="muted">No evidence contributes to this skill yet.</td></tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </div>
        } @empty {
          <p class="empty">No skills to show. Add evidence to build your twin.</p>
        }
      </section>

      <section class="card">
        <h2>Gaps against your goal</h2>
        @if (!t.goal) {
          <p class="empty">Set a goal on the dashboard to see gaps.</p>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Skill</th><th class="num">Current</th><th class="num">Target</th><th class="num">Gap</th>
                  <th class="num">Weight</th><th class="num">Impact</th><th class="num">Confidence</th><th>Status</th><th>Missing prerequisites</th>
                </tr>
              </thead>
              <tbody>
                @for (g of shownGaps(); track g.skillId) {
                  <tr>
                    <td>{{ g.name }}<div class="small muted">{{ g.category }}</div></td>
                    <td class="num">{{ g.current | num }}</td>
                    <td class="num">{{ g.target | num }}</td>
                    <td class="num">{{ g.gap | num }}</td>
                    <td class="num">{{ g.weight | num }}</td>
                    <td class="num">{{ g.impact | num }}</td>
                    <td class="num">{{ g.confidence | pct: 'auto' }}</td>
                    <td><app-badge [tone]="tone(g.status)">{{ g.status }}</app-badge></td>
                    <td>{{ prereqNames(g.missingPrerequisites) }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    }
  `,
})
export class Skills implements OnInit {
  private readonly api = inject(ApiService);
  readonly twin = signal<Twin | null>(null);
  readonly loading = signal(true);
  readonly error = signal<ApiError | null>(null);
  readonly filter = signal('all');
  readonly open = signal<ReadonlySet<string>>(new Set());

  readonly categories = computed(() => [...new Set((this.twin()?.levels ?? []).map((l) => l.category))].sort());
  readonly shown = computed(() => (this.twin()?.levels ?? []).filter((l) => this.filter() === 'all' || l.category === this.filter()));
  readonly shownGaps = computed(() => (this.twin()?.gaps ?? []).filter((g) => this.filter() === 'all' || g.category === this.filter()));

  ngOnInit() {
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

  tone(s: GapStatus): Tone {
    return STATUS_TONE[s];
  }
  isOpen(id: string): boolean {
    return this.open().has(id);
  }
  toggle(id: string) {
    this.open.update((s) => {
      const n = new Set(s);
      if (n.has(id)) n.delete(id);
      else n.add(id);
      return n;
    });
  }
  prereqNames(ids: string[]): string {
    if (!ids.length) return 'none';
    const lv = this.twin()?.levels ?? [];
    return ids.map((id) => lv.find((l) => l.skillId === id)?.name ?? id).join(', ');
  }
}
