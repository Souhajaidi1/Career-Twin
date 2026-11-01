import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, toApiError } from '../../core/api.service';
import { ApiError, Catalog, HypotheticalType, SimulationResult } from '../../core/models';
import { ErrorBox, Num } from '../../shared/ui';

@Component({
  selector: 'app-what-if',
  imports: [FormsModule, ErrorBox, Num],
  template: `
    <form class="card" (ngSubmit)="run()">
      <h2>What if?</h2>
      <p class="muted">Add a hypothetical piece of evidence and see the readiness change. The scoring service runs the numbers.</p>
      <app-error-box [error]="error()" />
      <div class="form-grid">
        <div class="field">
          <label for="wi-skill">Skill</label>
          <select id="wi-skill" name="skill" [(ngModel)]="skillId" required>
            <option value="" disabled>Select a skill</option>
            @for (s of catalog()?.skills ?? []; track s.id) {
              <option [value]="s.id">{{ s.name }}</option>
            }
          </select>
        </div>
        <div class="field">
          <label for="wi-type">Evidence type</label>
          <select id="wi-type" name="type" [(ngModel)]="type">
            <option value="assessment">Assessment</option>
            <option value="project">Project</option>
            <option value="certification">Certification</option>
          </select>
        </div>
        @if (type() === 'assessment') {
          <div class="field">
            <label for="wi-score">Score</label>
            <input id="wi-score" name="score" type="number" min="0" [(ngModel)]="score" />
          </div>
        } @else {
          <div class="field">
            <label for="wi-months">Months</label>
            <input id="wi-months" name="months" type="number" min="0" [(ngModel)]="months" />
          </div>
        }
      </div>
      <button class="primary" type="submit" [disabled]="busy() || !skillId()">Simulate</button>
      @if (result(); as r) {
        <div class="item" style="margin-top: 16px">
          <p>
            <span class="big-number" style="font-size: 2rem">{{ r.baseScore | num }} to {{ r.simScore | num }}</span>
            <strong> ({{ r.delta >= 0 ? '+' : '' }}{{ r.delta | num }})</strong>
          </p>
          @for (l of changed(); track l.skillId) {
            <div class="small">{{ l.name }}: level {{ l.base | num }} to {{ l.sim | num }}</div>
          }
        </div>
      }
    </form>
  `,
})
export class WhatIf implements OnInit {
  private readonly api = inject(ApiService);
  readonly catalog = signal<Catalog | null>(null);
  readonly skillId = signal('');
  readonly type = signal<HypotheticalType>('assessment');
  readonly score = signal<number | null>(80);
  readonly months = signal<number | null>(3);
  readonly busy = signal(false);
  readonly error = signal<ApiError | null>(null);
  readonly result = signal<SimulationResult | null>(null);
  readonly changed = computed(() => (this.result()?.levels ?? []).filter((l) => l.base !== l.sim));

  ngOnInit() {
    this.api.catalog().subscribe({ next: (c) => this.catalog.set(c), error: (e) => this.error.set(toApiError(e)) });
  }

  run() {
    const isAssessment = this.type() === 'assessment';
    this.busy.set(true);
    this.error.set(null);
    this.api
      .simulate([{ skillId: this.skillId(), type: this.type(), score: isAssessment ? this.score() : null, months: isAssessment ? null : this.months() }])
      .subscribe({
        next: (r) => {
          this.result.set(r);
          this.busy.set(false);
        },
        error: (e) => {
          this.error.set(toApiError(e));
          this.busy.set(false);
        },
      });
  }
}
