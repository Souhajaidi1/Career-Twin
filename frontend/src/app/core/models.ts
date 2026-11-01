export type EvidenceType =
  | 'experience'
  | 'project'
  | 'certification'
  | 'education'
  | 'repository'
  | 'assessment'
  | 'interview'
  | 'claim';
export type Origin = 'stated' | 'imported' | 'measured' | 'claimed';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: string;
}
export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}
export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface Profile {
  name: string;
  currentRole: string;
  experienceYears: number;
  location: string;
}

export interface EvidenceSkill {
  id: string;
  weight: number;
}
export interface Evidence {
  id: string;
  type: EvidenceType;
  title: string;
  org: string | null;
  date: string | null;
  months: number | null;
  score: number | null;
  note: string | null;
  origin: Origin;
  skills: EvidenceSkill[];
}
export type EvidenceInput = Omit<Evidence, 'id' | 'origin'> & { origin?: Origin };

export interface Goal {
  id: string;
  roleId: string;
  seniority: string;
  active: boolean;
  createdAt: string;
}

export interface CatalogSkill {
  id: string;
  name: string;
  category: string;
  prerequisites: string[];
}
export interface RoleRequirement {
  skillId: string;
  target: number;
  weight: number;
}
export interface CatalogRole {
  id: string;
  name: string;
  typicalYears: number;
  requirements: RoleRequirement[];
}
export interface CatalogEvidenceType {
  type: EvidenceType;
  label: string;
  max: number;
  trust: string | number;
}
export interface Catalog {
  skills: CatalogSkill[];
  roles: CatalogRole[];
  seniorities: string[];
  evidenceTypes: CatalogEvidenceType[];
}

export interface ReadinessPart {
  key: string;
  label: string;
  weight: number;
  value: number;
  detail: string;
}
export interface Contribution {
  evidenceId: string;
  type: string;
  title: string;
  date: string | null;
  points: number;
  detail: string;
}
export interface SkillLevel {
  skillId: string;
  name: string;
  category: string;
  level: number;
  confidence: number;
  simulated: boolean;
  contributions: Contribution[];
}
export type GapStatus = 'met' | 'minor' | 'major' | 'critical';
export interface Gap {
  skillId: string;
  name: string;
  category: string;
  current: number;
  target: number;
  gap: number;
  weight: number;
  status: GapStatus;
  missingPrerequisites: string[];
  impact: number;
  confidence: number;
}
export interface Twin {
  goal: { roleId: string; roleName: string; seniority: string } | null;
  readiness: { score: number; parts: ReadinessPart[] } | null;
  levels: SkillLevel[];
  gaps: Gap[];
}

export interface RoadmapItem {
  skillId: string;
  title: string;
  objective: string;
  effortHours: number;
  weekStart: number;
  weekEnd: number;
  priority: 'High' | 'Medium' | 'Low';
  prerequisites: string[];
  produces: 'interview' | 'assessment' | 'project';
  gapAtCreation: number;
  readinessDelta: number;
  skillDelta: number;
}

export type HypotheticalType = 'assessment' | 'project' | 'certification';
export interface Hypothetical {
  skillId: string;
  type: HypotheticalType;
  score: number | null;
  months: number | null;
}
export interface SimulationResult {
  baseScore: number;
  simScore: number;
  delta: number;
  levels: { skillId: string; name: string; base: number; sim: number }[];
}

export interface JobInput {
  title: string;
  company: string;
  location: string;
  url: string;
  raw: string;
}
export interface JobSummary {
  id: string;
  title: string;
  company: string;
  location: string;
  createdAt: string;
  score: number | null;
}
export interface JobRequirementSkill {
  id: string;
  required: boolean;
  mentions: number;
  quote: string;
}
export interface Job {
  id: string;
  title: string;
  company: string;
  location: string;
  url: string;
  raw: string;
  createdAt: string;
  requirements: {
    skills: JobRequirementSkill[];
    years: number | null;
    degree: string | null;
    seniority: string;
  };
}
export interface MatchRow {
  skillId: string;
  name: string;
  required: boolean;
  level: number;
  need: number;
  ratio: number;
  state: 'strong' | 'partial' | 'missing';
}
export interface MatchCategory {
  key: string;
  label: string;
  weight: number;
  effective: number;
  value: number;
  points: number;
}
export interface JobMatch {
  score: number;
  completeness: number;
  requiredLevel: number;
  categories: MatchCategory[];
  excluded: string[];
  strong: MatchRow[];
  partial: MatchRow[];
  missing: MatchRow[];
}

export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  violations?: Record<string, string>;
}
export interface ApiError {
  status: number;
  message: string;
  violations: [string, string][];
}
