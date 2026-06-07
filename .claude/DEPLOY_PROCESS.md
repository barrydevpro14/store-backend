# DEPLOY_PROCESS.md — Deployment guide

> Stack: Spring Boot backend on Railway + Next.js frontend on Vercel.
> Backend repo on GitLab, mirrored to GitHub for Railway auto-deploy.

---

## 🏗 Architecture (live as of 2026-06-07)

```
GitLab dev ──push──► GitLab main ──mirror──► GitHub main ──► Railway auto-deploy
                                                               Spring Boot + PostgreSQL

GitLab dev ──push──► Vercel (connected to dev branch) ──► Frontend auto-deploy
```

**URLs:**
- Backend: `https://appstore-backend-api.up.railway.app`
- Frontend: Vercel project (dev branch = production for now)

---

## 🔧 Setup (already done)

### GitHub mirror
- GitLab → store-backend → Settings → Repository → Mirroring Repositories
- Push mirror: URL `https://barrydevpro14:<TOKEN>@github.com/barrydevpro14/store-backend.git`
- GitHub repo: `barrydevpro14/store-backend`

### Railway (backend + database)
- Service connected to `barrydevpro14/store-backend` (GitHub) — auto-deploy on main push
- Dockerfile at repo root is built automatically by Railway
- PostgreSQL service linked in same project

**Environment variables on Railway:**

| Variable | Notes |
|---|---|
| `URL_DATABASE` | Railway internal PostgreSQL host:port |
| `DB_NAME` | postgres |
| `DB_USERNAME` | From Railway PostgreSQL credentials |
| `DB_PASSWORD` | From Railway PostgreSQL credentials |
| `JWT_SECRET` | Generated with `openssl rand -base64 64` |
| `RBAC_ADMIN_PASSWORD` | Strong password for `admin` account |
| `RBAC_SYNC` | `false` (set `true` only on first boot) |
| `CORS_ALLOWED_ORIGINS` | Vercel frontend URL |
| `FRONTEND_URL` | Vercel frontend URL (for password-reset links) |
| `MAIL_HOST` | smtp.gmail.com |
| `MAIL_PORT` | 587 |
| `MAIL_USERNAME` | barrydevpro@gmail.com |
| `MAIL_PASSWORD` | Gmail App Password |
| `MAIL_FROM` | barrydevpro@gmail.com |

### Vercel (frontend)
- Connected directly to GitLab `store-frontend` → **dev branch** (auto-deploy on every push)
- `NEXT_PUBLIC_API_URL` = `https://appstore-backend-api.up.railway.app`

### GitLab CI backend (simplified)
- Pipeline only runs on `main`, single `test` stage (Maven compile ~2 min)
- No Docker build/push in CI — Railway handles it from GitHub

---

## 🚀 Subsequent deployments

### Backend
1. Develop on `dev` → commit + push to GitLab
2. Open MR `dev → main` on GitLab → merge
3. GitLab auto-mirrors to GitHub → Railway detects push → rebuild (~3-5 min)
4. Check Railway logs: Flyway migrations applied, `Started StoreApplication`

### Frontend
1. Develop on `dev` → commit + push to GitLab
2. Vercel detects push to `dev` → auto-deploys (~2-3 min)

---

## 📋 Smoke tests

```
GET  /actuator/health                       → {"status":"UP"}
GET  /api/v1/catalog/public                 → plans + types list
POST /api/v1/auth/login                     → accessToken + refreshToken
     body: {"username":"admin","password":"<RBAC_ADMIN_PASSWORD>"}
```

After first boot only:
- Set `RBAC_SYNC=false` in Railway variables
- Verify CORS: frontend calls reach the backend

---

## 💰 Cost

| Service | Plan | Cost |
|---|---|---|
| Railway (backend + DB) | Hobby | ~$5/month |
| Vercel (frontend) | Hobby | Free |
| GitHub (mirror) | Free | Free |

---

## 🆘 Rollback

```bash
# Push a revert commit to main → Railway rebuilds automatically
git revert HEAD && git push origin main
```
