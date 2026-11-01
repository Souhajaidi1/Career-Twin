import { Component, Pipe, PipeTransform, input } from '@angular/core';
import { ApiError, GapStatus } from '../core/models';

export type Tone = 'neutral' | 'ok' | 'info' | 'warn' | 'bad';

export const STATUS_TONE: Record<GapStatus, Tone> = { met: 'ok', minor: 'info', major: 'warn', critical: 'bad' };

@Component({
  selector: 'app-badge',
  template: '<span class="badge" [class]="tone()"><ng-content /></span>',
})
export class Badge {
  readonly tone = input<Tone>('neutral');
}

/** Horizontal bar. Pure presentation: width = value / max, optional marker at the same scale. */
@Component({
  selector: 'app-score-bar',
  template: `
    <div class="bar-row">
      @if (label()) {
        <span class="bar-label">{{ label() }}</span>
      }
      <div
        class="bar"
        role="meter"
        [attr.aria-label]="label() || 'value'"
        [attr.aria-valuenow]="value()"
        aria-valuemin="0"
        [attr.aria-valuemax]="max()"
      >
        <div class="bar-fill" [style.width.%]="pctOf(value())"></div>
        @if (marker() !== null) {
          <div class="bar-marker" [style.left.%]="pctOf(marker()!)" title="Target"></div>
        }
      </div>
      <span class="bar-text">{{ text() }}</span>
    </div>
  `,
})
export class ScoreBar {
  readonly label = input('');
  readonly value = input.required<number>();
  readonly max = input(1);
  readonly marker = input<number | null>(null);
  readonly text = input('');

  pctOf(v: number): number {
    const m = this.max();
    return m > 0 ? Math.max(0, Math.min(100, (v / m) * 100)) : 0;
  }
}

@Component({
  selector: 'app-error-box',
  template: `
    @if (error(); as e) {
      <div class="alert" role="alert">
        <strong>{{ e.message }}</strong>
        @if (e.violations.length) {
          <ul>
            @for (v of e.violations; track v[0]) {
              <li>
                <code>{{ v[0] }}</code
                >: {{ v[1] }}
              </li>
            }
          </ul>
        }
      </div>
    }
  `,
})
export class ErrorBox {
  readonly error = input<ApiError | null>(null);
}

/** 'frac' (default): 0-1 -> percent. 'auto': values <= 1 are fractions, larger values are already percent. */
@Pipe({ name: 'pct' })
export class Pct implements PipeTransform {
  transform(v: number | null | undefined, mode: 'frac' | 'auto' = 'frac'): string {
    if (v === null || v === undefined) return 'n/a';
    const p = mode === 'auto' && v > 1 ? v : v * 100;
    return `${Math.round(p)}%`;
  }
}

/** Rounds to at most 1 decimal for display (formatting only). */
@Pipe({ name: 'num' })
export class Num implements PipeTransform {
  transform(v: number | null | undefined): string {
    if (v === null || v === undefined) return 'n/a';
    return String(Math.round(v * 10) / 10);
  }
}
