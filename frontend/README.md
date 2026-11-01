# CareerTwin frontend

Angular (standalone components, signals, strict TypeScript, plain CSS) dashboard for the CareerTwin
career-intelligence platform. It talks to the REST API at same-origin `/api/v1/...`.
All scores and explanations come from the backend; the UI never computes them.

- `npm ci` - install dependencies
- `npm start` - dev server on http://localhost:4200 (proxies `/api` to http://localhost:8080)
- `npm run build` - production build into `dist/frontend/browser`

Docker: `docker build -t careertwin-frontend .` serves the build with nginx and proxies
`/api/` to `http://api-gateway:8080`.

Layout: `src/app/core` (auth, interceptor, API service, models), `src/app/features/<page>`,
`src/app/shared` (badge, score bar, error box, pipes).
