# DEPLOY_PROCESS.md — Deployment guide

> Stack: Spring Boot backend on Railway + Next.js frontend on Vercel.
> Both repos on GitLab. CI/CD triggers on `main` only.

---

## 🏗 Architecture

```
GitLab (dev) ──► MR ──► main ──► CI/CD pipeline
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
              Backend (store/)             Frontend (store-frontend/)
              eclipse-temurin:21           node:20-alpine
              Docker image                Vercel deploy
              GitLab Registry             (auto or manual)
                    │
              Railway webhook
              (manual trigger in CI)
                    │
              Railway service
              Spring Boot + PostgreSQL
```

---

## 🔧 One-time setup

### Railway (backend + database)

1. Create account at railway.com — Trial plan (free, $5 credit, no card needed)
2. New project → **Add PostgreSQL** service
3. New service → **Docker Image** → point to your GitLab Container Registry:
   ```
   registry.gitlab.com/<your-namespace>/store-backend
   ```
4. Set environment variables on the Railway service:

   | Variable | Value |
   |---|---|
   | `URL_DATABASE` | Host:port from Railway PostgreSQL (e.g. `postgres.railway.internal:5432`) |
   | `DB_NAME` | postgres (or your db name) |
   | `DB_USERNAME` | From Railway PostgreSQL credentials |
   | `DB_PASSWORD` | From Railway PostgreSQL credentials |
   | `JWT_SECRET` | Generate: `openssl rand -base64 64` |
   | `RBAC_ADMIN_PASSWORD` | Strong password for the `admin` account |
   | `RBAC_SYNC` | `true` (first boot only, then set to false) |
   | `CORS_ALLOWED_ORIGINS` | Your Vercel domain (e.g. `https://store.vercel.app`) |
   | `MAIL_HOST` | SMTP host (optional) |
   | `MAIL_PORT` | `587` (optional) |
   | `MAIL_USERNAME` | SMTP username (optional) |
   | `MAIL_PASSWORD` | SMTP password (optional) |
   | `MAIL_FROM` | Sender address (optional) |
   | `SUBSCRIPTION_TRIAL_DAYS` | `30` (default) |
   | `SALE_CANCEL_WINDOW_HOURS` | `24` (default) |
   | `PURCHASE_CANCEL_WINDOW_HOURS` | `24` (default) |

5. Copy the **deploy webhook URL** from Railway (Settings → Deploy → Deploy Webhook)

### GitLab CI/CD variables (backend repo)

Go to GitLab → store-backend → Settings → CI/CD → Variables:

| Variable | Value |
|---|---|
| `CI_REGISTRY_USER` | Your GitLab username |
| `CI_REGISTRY_PASSWORD` | GitLab personal access token (scope: `read_registry` + `write_registry`) |
| `DEPLOY_HOOK_URL` | Railway webhook URL from the step above |

### Vercel (frontend)

**Option A — Direct connection (recommended, auto-deploys on merge)**
1. Create account at vercel.com
2. New project → Import from GitLab → select `store-frontend`
3. Framework: Next.js (auto-detected)
4. Set environment variable:
   ```
   NEXT_PUBLIC_API_URL=https://<your-railway-domain>.railway.app
   ```
5. Deploy — Vercel handles everything automatically on each push to `main`
6. Every push to `dev` creates a **preview URL** for testing before merge

**Option B — CI-controlled deploy (manual trigger via GitLab)**
1. Create account at vercel.com
2. Run locally: `cd store-frontend && npx vercel link`
3. Grab values from `.vercel/project.json`: `orgId` + `projectId`
4. Create a Vercel token at vercel.com → Account Settings → Tokens
5. Add to GitLab CI/CD variables (frontend repo):
   ```
   VERCEL_TOKEN       = <token>
   VERCEL_ORG_ID      = <orgId>
   VERCEL_PROJECT_ID  = <projectId>
   ```
6. Set `NEXT_PUBLIC_API_URL` in Vercel project env vars

---

## 🚀 First deployment (Saturday)

### Step 1 — Merge to main

```bash
# Open MRs on GitLab for both repos: dev → main
# Review and merge both
```

### Step 2 — Backend pipeline (auto-triggered on main)

Pipeline runs automatically:
1. `test` — Maven compile check (~2 min)
2. `build` — Docker image built + pushed to GitLab registry (~5 min)
3. `deploy` — **manual trigger** → click in GitLab CI/CD to fire the Railway webhook

### Step 3 — Frontend (if using Option B / manual)

Click the manual `deploy` job in the frontend pipeline.

If using Option A (Vercel direct), deployment is already done automatically after merge.

### Step 4 — First boot checks

Railway logs should show:
```
Started StoreApplication in X.Xs
Flyway: Successfully applied V1 migration
RBAC sync: created X permissions, X roles
```

### Step 5 — Smoke test

| Check | URL |
|---|---|
| API health | `GET https://<railway-domain>/actuator/health` |
| Public catalog | `GET https://<railway-domain>/api/v1/catalog/public` |
| Frontend loads | `https://<vercel-domain>` |
| Admin login | POST `/api/v1/auth/login` with `admin` / `<RBAC_ADMIN_PASSWORD>` |

### Step 6 — Post first-boot

- Change the admin password immediately
- Set `RBAC_SYNC=false` in Railway env vars (prevents re-sync on every restart)
- Verify CORS: frontend can call the backend API

---

## 🔁 Subsequent deployments

1. Develop on `dev` — pushes trigger no pipeline (workflow rule)
2. When ready → open MR `dev` → `main`
3. Merge → backend pipeline auto-runs → click manual deploy
4. Frontend auto-deploys via Vercel (Option A) or click manual job (Option B)

---

## 💰 Cost estimate

| Service | Plan | Cost |
|---|---|---|
| Railway (backend + DB) | Trial → Hobby | Free trial → $5/month |
| Vercel (frontend) | Hobby | Free |
| **Total** | | **~$5/month** |

---

## 🆘 Rollback

```bash
# Re-trigger a previous deploy in Railway dashboard (select an older image tag)
# Or push a revert commit to main → pipeline rebuilds the image
```
