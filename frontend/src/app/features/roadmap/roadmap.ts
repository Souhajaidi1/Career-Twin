import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, toApiError } from '../../core/api.service';
import { ApiError, Catalog, RoadmapItem } from '../../core/models';
import { Badge, ErrorBox, Num, Tone } from '../../shared/ui';
import { WhatIf } from './what-if';

const PRIORITY_TONE: Record<RoadmapItem['priority'], Tone> = { High: 'bad', Medium: 'warn', Low: 'neutral' };

@Component({
  selector: 'app-roadmap',
  imports: [FormsModule, Badge, ErrorBox, Num, WhatIf],
  template: `
    <div class="page-head">
      <h1>Roadmap</h1>
      <p>An ordered plan to close your biggest gaps.</p>
    </div>
    <form class="card row" (ngSubmit)="load()">
      <div style="max-width: 200px">
        <label for="hpw">Hours per week</label>
        <input id="hpw" name="hpw" type="number" min="1" max="80" [(ngModel)]="hours" />
      </div>
      <button class="primary" type="submit" style="align-self: flex-end" [disabled]="loading() || !hours()">Update plan</button>
    </form>
    <app-error-box [error]="error()" />
    @if (loading()) {
      <p class="muted">Loading...</p>
    }
    @for (r of items(); track r.skillId; let i = $index) {
      <article class="card">
        <div class="row between">
          <h2 style="margin: 0">{{ i + 1 }}. {{ r.title }}</h2>
          <app-badge [tone]="tone(r.priority)">{{ r.priority }} priority</app-badge>
        </div>
        <p style="margin-top: 8px">{{ r.objective }}</p>
        <div class="row small muted">
          <span>{{ r.effortHours | num }} hours</span>
          <span>Weeks {{ r.weekStart }} to {{ r.weekEnd }}</span>
          <span>Produces: {{ r.produces }}</span>
        </div>
        <p class="small" style="margin-top: 8px">
          Prerequisites: {{ prereqs(r.prerequisites) }}
        </p>
        <p><strong>+{{ r.readinessDelta | num }} readiness points</strong> <span class="small muted">(estimated by re-running the scoring)</span></p>
      </article>
    } @empty {
      @if (!loading() && !error()) {
        <p class="empty">No roadmap items. Set a goal on the dashboard first.</p>
      }
    }
    <app-what-if />
  `,
})
export class Roadmap implements OnInit {
  private readonly api = inject(ApiService);
  readonly hours = signal<number | null>(6);
  readonly items = signal<RoadmapItem[]>([]);
  readonly catalog = signal<Catalog | null>(null);
  readonly loading = signal(true);
  readonly error = signal<ApiError | null>(null);

  ngOnInit() {
    this.api.catalog().subscribe({ next: (c) => this.catalog.set(c), error: () => undefined });
    this.load();
  }

  load() {
    const h = this.hours();
    if (!h) return;
    this.loading.set(true);
    this.error.set(null);
    this.api.roadmap(h).subscribe({
      next: (l) => {
        this.items.set(l);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(toApiError(e));
        this.loading.set(false);
      },
    });
  }

  tone(p: RoadmapItem['priority']): Tone {
    return PRIORITY_TONE[p] ?? 'neutral';
  }
  prereqs(ids: string[]): string {
    if (!ids.length) return 'none';
    const skills = this.catalog()?.skills ?? [];
    return ids.map((id) => skills.find((s) => s.id === id)?.name ?? id).join(', ');
  }
}
