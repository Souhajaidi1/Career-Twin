import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import {
  ApiError,
  Catalog,
  Evidence,
  EvidenceInput,
  Goal,
  Hypothetical,
  Job,
  JobInput,
  JobMatch,
  JobSummary,
  LoginResponse,
  ProblemDetail,
  Profile,
  RegisterRequest,
  RoadmapItem,
  SimulationResult,
  Twin,
  User,
} from './models';

const BASE = '/api/v1';

/** Normalises any thrown value into a displayable RFC 7807 error. */
export function toApiError(e: unknown): ApiError {
  if (e instanceof HttpErrorResponse) {
    const p = (typeof e.error === 'object' && e.error ? e.error : {}) as ProblemDetail;
    const message =
      p.detail ||
      p.title ||
      (e.status === 0 ? 'The server could not be reached.' : `Request failed (${e.status}).`);
    return { status: e.status, message, violations: Object.entries(p.violations ?? {}) };
  }
  return { status: 0, message: e instanceof Error ? e.message : 'Unexpected error.', violations: [] };
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private catalog$?: Observable<Catalog>;

  // Auth
  register(body: RegisterRequest) {
    return this.http.post<User>(`${BASE}/auth/register`, body);
  }
  login(body: { email: string; password: string }) {
    return this.http.post<LoginResponse>(`${BASE}/auth/login`, body);
  }
  me() {
    return this.http.get<User>(`${BASE}/auth/me`);
  }

  // Profile
  getProfile() {
    return this.http.get<Profile>(`${BASE}/profile`);
  }
  putProfile(body: Profile) {
    return this.http.put<Profile>(`${BASE}/profile`, body);
  }

  // Evidence
  listEvidence() {
    return this.http.get<Evidence[]>(`${BASE}/evidence`);
  }
  createEvidence(body: EvidenceInput) {
    return this.http.post<Evidence>(`${BASE}/evidence`, body);
  }
  updateEvidence(id: string, body: EvidenceInput) {
    return this.http.put<Evidence>(`${BASE}/evidence/${encodeURIComponent(id)}`, body);
  }
  deleteEvidence(id: string) {
    return this.http.delete<void>(`${BASE}/evidence/${encodeURIComponent(id)}`);
  }

  // Goals
  createGoal(body: { roleId: string; seniority: string }) {
    return this.http.post<Goal>(`${BASE}/goals`, body);
  }

  // Catalog (cached for the session)
  catalog(): Observable<Catalog> {
    return (this.catalog$ ??= this.http
      .get<Catalog>(`${BASE}/catalog`)
      .pipe(shareReplay({ bufferSize: 1, refCount: false })));
  }

  // Analysis
  twin() {
    return this.http.get<Twin>(`${BASE}/analysis/twin`);
  }
  roadmap(hoursPerWeek: number) {
    const params = new HttpParams().set('hoursPerWeek', String(hoursPerWeek));
    return this.http.get<RoadmapItem[]>(`${BASE}/analysis/roadmap`, { params });
  }
  simulate(hypotheticals: Hypothetical[]) {
    return this.http.post<SimulationResult>(`${BASE}/analysis/simulate`, { hypotheticals });
  }

  // Jobs
  createJob(body: JobInput) {
    return this.http.post<Job>(`${BASE}/jobs`, body);
  }
  listJobs() {
    return this.http.get<JobSummary[]>(`${BASE}/jobs`);
  }
  getJob(id: string) {
    return this.http.get<Job>(`${BASE}/jobs/${encodeURIComponent(id)}`);
  }
  jobMatch(id: string) {
    return this.http.get<JobMatch>(`${BASE}/jobs/${encodeURIComponent(id)}/match`);
  }
  deleteJob(id: string) {
    return this.http.delete<void>(`${BASE}/jobs/${encodeURIComponent(id)}`);
  }
}
