import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, toApiError } from '../../core/api.service';
import { ApiError, Catalog, CatalogSkill, Evidence, EvidenceInput, EvidenceType, Origin } from '../../core/models';
import { Badge, ErrorBox, Tone } from '../../shared/ui';

const SCORED: EvidenceType[] = ['assessment', 'interview'];
const ORIGIN_TONE: Record<Origin, Tone> = { measured: 'ok', imported: 'info', stated: 'neutral', claimed: 'warn' };

@Component({
  selector: 'app-evidence',
  imports: [FormsModule, Badge, ErrorBox],
  template: `
    <div class="page-head row between">
      <div>
        <h1>Evidence</h1>
        <p>Everything your skill levels are derived from.</p>
      </div>
      @if (!formOpen()) {
        <button class="primary" type="button" (click)="startAdd()">Add evidence</button>
      }
    </div>
    <app-error-box [error]="error()" />

    @if (formOpen()) {
      <form class="card" (ngSubmit)="save()" id="evidence-form">
        <h2>{{ editingId() ? 'Edit evidence' : 'Add evidence' }}</h2>
        <app-error-box [error]="formError()" />
        <div class="form-grid">
          <div class="field">
            <label for="ev-type">Type</label>
            <select id="ev-type" name="type" [(ngModel)]="type" required>
              @for (t of catalog()?.evidenceTypes ?? []; track t.type) {
                <option [value]="t.type">{{ t.label }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label for="ev-title">Title</label>
            <input id="ev-title" name="title" [(ngModel)]="title" required />
          </div>
          <div class="field">
            <label for="ev-org">Organisation</label>
            <input id="ev-org" name="org" [(ngModel)]="org" />
          </div>
          <div class="field">
            <label for="ev-date">Date</label>
            <input id="ev-date" name="date" type="date" [(ngModel)]="date" />
          </div>
          <div class="field">
            <label for="ev-months">Months</label>
            <input id="ev-months" name="months" type="number" min="0" [(ngModel)]="months" />
          </div>
          @if (scored()) {
            <div class="field">
              <label for="ev-score">Score</label>
              <input id="ev-score" name="score" type="number" min="0" [attr.max]="maxScore()" [(ngModel)]="score" />
              @if (maxScore() !== null) {
                <div class="hint">Maximum {{ maxScore() }}.</div>
              }
            </div>
          }
        </div>
        <div class="field">
          <label for="ev-note">Note</label>
          <textarea id="ev-note" name="note" style="min-height: 70px" [(ngModel)]="note"></textarea>
        </div>

        <h3>Skills demonstrated</h3>
        @for (g of groups(); track g.category) {
          <details>
            <summary>{{ g.category }} ({{ countSelected(g.skills) }} selected)</summary>
            <div class="skill-pick">
              @for (s of g.skills; track s.id) {
                <label>
                  <input type="checkbox" [checked]="isSelected(s.id)" (change)="toggleSkill(s.id)" />
                  {{ s.name }}
                </label>
              }
            </div>
          </details>
        }
        @if (selectedList().length) {
          <h3 style="margin-top: 12px">Weights (0.1 to 1.0)</h3>
          <div class="form-grid">
            @for (s of selectedList(); track s.id) {
              <div class="field">
                <label [for]="'w-' + s.id">{{ skillName(s.id) }}</label>
                <input
                  [id]="'w-' + s.id"
                  type="number"
                  min="0.1"
                  max="1"
                  step="0.1"
                  [value]="s.weight"
                  (input)="setWeight(s.id, $any($event.target).valueAsNumber)"
                />
              </div>
            }
          </div>
        }
        <div class="row" style="margin-top: 16px">
          <button class="primary" type="submit" [disabled]="busy() || !title().trim()">{{ editingId() ? 'Save changes' : 'Save evidence' }}</button>
          <button type="button" (click)="closeForm()">Cancel</button>
        </div>
      </form>
    }

    <section class="card">
      @if (loading()) {
        <p class="muted">Loading...</p>
      }
      @for (e of items(); track e.id) {
        <article class="item">
          <div class="row between">
            <div>
              <strong>{{ e.title }}</strong>
              <div class="small muted">
                {{ typeLabel(e.type) }}{{ e.org ? ' at ' + e.org : '' }}{{ e.date ? ', ' + e.date : '' }}{{ e.months ? ', ' + e.months + ' months' : '' }}{{ e.score !== null ? ', score ' + e.score : '' }}
              </div>
            </div>
            <div class="row">
              <app-badge [tone]="originTone(e.origin)">{{ e.origin }}</app-badge>
              <button type="button" (click)="startEdit(e)">Edit</button>
              <button class="danger" type="button" (click)="remove(e)">Delete</button>
            </div>
          </div>
          @if (e.note) {
            <p class="small" style="margin-top: 6px">{{ e.note }}</p>
          }
          <div class="chips" style="margin-top: 6px">
            @for (s of e.skills; track s.id) {
              <span class="chip">{{ skillName(s.id) }} x{{ s.weight }}</span>
            }
          </div>
        </article>
      } @empty {
        @if (!loading()) {
          <p class="empty">No evidence yet. Add a project, certification or assessment to start.</p>
        }
      }
    </section>
  `,
})
export class EvidencePage implements OnInit {
  private readonly api = inject(ApiService);

  readonly items = signal<Evidence[]>([]);
  readonly catalog = signal<Catalog | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal<ApiError | null>(null);
  readonly formError = signal<ApiError | null>(null);
  readonly formOpen = signal(false);
  readonly editingId = signal<string | null>(null);

  readonly type = signal<EvidenceType>('project');
  readonly title = signal('');
  readonly org = signal('');
  readonly date = signal('');
  readonly months = signal<number | null>(null);
  readonly score = signal<number | null>(null);
  readonly note = signal('');
  readonly picked = signal<Record<string, number>>({});
  private editOrigin: Origin | undefined;

  readonly scored = computed(() => SCORED.includes(this.type()));
  readonly maxScore = computed(() => this.catalog()?.evidenceTypes.find((t) => t.type === this.type())?.max ?? null);
  readonly selectedList = computed(() => Object.entries(this.picked()).map(([id, weight]) => ({ id, weight })));
  readonly groups = computed(() => {
    const m = new Map<string, CatalogSkill[]>();
    for (const s of this.catalog()?.skills ?? []) {
      const list = m.get(s.category) ?? [];
      list.push(s);
      m.set(s.category, list);
    }
    return [...m.entries()].sort((a, b) => a[0].localeCompare(b[0])).map(([category, skills]) => ({ category, skills }));
  });

  ngOnInit() {
    this.api.catalog().subscribe({
      next: (c) => {
        this.catalog.set(c);
        if (c.evidenceTypes.length && !c.evidenceTypes.some((t) => t.type === this.type())) this.type.set(c.evidenceTypes[0].type);
      },
      error: (e) => this.error.set(toApiError(e)),
    });
    this.reload();
  }

  reload() {
    this.api.listEvidence().subscribe({
      next: (list) => {
        this.items.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(toApiError(e));
        this.loading.set(false);
      },
    });
  }

  skillName(id: string): string {
    return this.catalog()?.skills.find((s) => s.id === id)?.name ?? id;
  }
  typeLabel(t: string): string {
    return this.catalog()?.evidenceTypes.find((x) => x.type === t)?.label ?? t;
  }
  originTone(o: Origin): Tone {
    return ORIGIN_TONE[o] ?? 'neutral';
  }
  isSelected(id: string): boolean {
    return id in this.picked();
  }
  countSelected(skills: CatalogSkill[]): number {
    return skills.filter((s) => this.isSelected(s.id)).length;
  }
  toggleSkill(id: string) {
    this.picked.update((p) => {
      const n = { ...p };
      if (id in n) delete n[id];
      else n[id] = 1;
      return n;
    });
  }
  setWeight(id: string, w: number) {
    if (Number.isNaN(w)) return;
    this.picked.update((p) => ({ ...p, [id]: w }));
  }

  private resetForm() {
    this.type.set(this.catalog()?.evidenceTypes[0]?.type ?? 'project');
    this.title.set('');
    this.org.set('');
    this.date.set('');
    this.months.set(null);
    this.score.set(null);
    this.note.set('');
    this.picked.set({});
    this.editOrigin = undefined;
    this.formError.set(null);
  }
  startAdd() {
    this.resetForm();
    this.editingId.set(null);
    this.formOpen.set(true);
  }
  startEdit(e: Evidence) {
    this.resetForm();
    this.editingId.set(e.id);
    this.editOrigin = e.origin;
    this.type.set(e.type);
    this.title.set(e.title);
    this.org.set(e.org ?? '');
    this.date.set(e.date ?? '');
    this.months.set(e.months);
    this.score.set(e.score);
    this.note.set(e.note ?? '');
    this.picked.set(Object.fromEntries(e.skills.map((s) => [s.id, s.weight])));
    this.formOpen.set(true);
    setTimeout(() => document.getElementById('evidence-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }
  closeForm() {
    this.formOpen.set(false);
    this.editingId.set(null);
  }

  save() {
    const body: EvidenceInput = {
      type: this.type(),
      title: this.title().trim(),
      org: this.org().trim() || null,
      date: this.date() || null,
      months: this.months(),
      score: this.scored() ? this.score() : null,
      note: this.note().trim() || null,
      skills: this.selectedList(),
      ...(this.editOrigin ? { origin: this.editOrigin } : {}),
    };
    this.busy.set(true);
    this.formError.set(null);
    const id = this.editingId();
    const req = id ? this.api.updateEvidence(id, body) : this.api.createEvidence(body);
    req.subscribe({
      next: () => {
        this.busy.set(false);
        this.closeForm();
        this.reload();
      },
      error: (e) => {
        this.formError.set(toApiError(e));
        this.busy.set(false);
      },
    });
  }

  remove(e: Evidence) {
    if (!confirm(`Delete "${e.title}"?`)) return;
    this.api.deleteEvidence(e.id).subscribe({
      next: () => this.items.update((l) => l.filter((x) => x.id !== e.id)),
      error: (err) => this.error.set(toApiError(err)),
    });
  }
}
