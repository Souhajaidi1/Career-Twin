import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/login').then((m) => m.Login), title: 'Sign in - CareerTwin' },
  { path: 'register', loadComponent: () => import('./features/auth/register').then((m) => m.Register), title: 'Register - CareerTwin' },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard), title: 'Dashboard - CareerTwin' },
      { path: 'skills', loadComponent: () => import('./features/skills/skills').then((m) => m.Skills), title: 'Skills - CareerTwin' },
      { path: 'evidence', loadComponent: () => import('./features/evidence/evidence').then((m) => m.EvidencePage), title: 'Evidence - CareerTwin' },
      { path: 'jobs', loadComponent: () => import('./features/jobs/jobs').then((m) => m.Jobs), title: 'Jobs - CareerTwin' },
      { path: 'jobs/:id', loadComponent: () => import('./features/jobs/job-detail').then((m) => m.JobDetail), title: 'Job match - CareerTwin' },
      { path: 'roadmap', loadComponent: () => import('./features/roadmap/roadmap').then((m) => m.Roadmap), title: 'Roadmap - CareerTwin' },
      { path: 'profile', loadComponent: () => import('./features/profile/profile').then((m) => m.ProfilePage), title: 'Profile - CareerTwin' },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
