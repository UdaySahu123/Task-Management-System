# Deployment Guide — Render + Vercel

Deploy the three pieces:

| Piece     | Host                    | What runs                          |
| --------- | ----------------------- | ---------------------------------- |
| Database  | Railway (or Aiven)      | Managed MySQL 8                    |
| Backend   | Render (Docker)         | Spring Boot API, Flyway migrations |
| Frontend  | Vercel (static)         | React build                        |

```
Browser ──► Vercel (React SPA)
                 │  HTTPS  /api/*
                 ▼
            Render (Spring Boot)  ──►  Railway (MySQL 8)
```

The files this guide relies on are already in the repo:
`backend/Dockerfile`, `backend/.dockerignore`, `render.yaml`, `frontend/vercel.json`.

---

## Step 0 — Push the repo

If not already done:

```powershell
git add -A
git commit -m "Add deployment configuration"
git push
```

---

## Step 1 — Create the MySQL database (Railway)

1. Sign in at https://railway.app → **New Project → Provision MySQL**.
2. Open the MySQL service → **Variables / Connect** tab and copy:
   - host, port, database name, username, password
3. From the **Public Networking** area, generate a public TCP proxy if needed
   (Railway shows a `containers-*.railway.app` host with a mapped port).
4. Build the JDBC URL (keep it on one line):

   ```
   jdbc:mysql://<HOST>:<PORT>/<DATABASE>?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
   ```

Keep these four values for the next step:

| Value        | Example                                   |
| ------------ | ----------------------------------------- |
| `DB_URL`     | `jdbc:mysql://containers-us-west-1…:6543/railway?...` |
| `DB_USERNAME`| `root`                                    |
| `DB_PASSWORD`| `…`                                       |
| database     | `railway`                                 |

> Aiven works too: create a MySQL service, copy the **Service URI**, and convert
> it to the `jdbc:mysql://…` form (add `allowPublicKeyRetrieval=true`).

Flyway creates the tables on the first backend start — no manual SQL needed.

---

## Step 2 — Deploy the backend (Render)

### Option A — Blueprint (uses `render.yaml`)

1. Sign in at https://render.com with GitHub.
2. **New + → Blueprint** → select the `Task-Management-System` repo → **Apply**.
3. Render creates the `taskflow-api` service. When prompted, fill the env vars
   marked `sync: false`:
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (from Step 1)
   - `CORS_ALLOWED_ORIGINS` — you can put a placeholder now (e.g.
     `http://localhost:5173`) and update it after Step 3.
   - `JWT_SECRET` is generated automatically.
4. Wait for the first build (Docker build takes a few minutes).

### Option B — Manual web service

1. **New + → Web Service** → connect the repo.
2. **Language/Runtime:** Docker
3. **Dockerfile Path:** `backend/Dockerfile`
4. **Docker Context / Root Directory:** `backend`
5. **Health Check Path:** `/actuator/health`
6. Add the same environment variables as above.

### Verify

Open `https://<your-service>.onrender.com/actuator/health` → expect
`{"status":"UP"}`.

> Free plan note: the service sleeps after ~15 minutes of inactivity; the first
> request afterwards takes ~30–60 s to wake. The database is external, so no
> data is lost.

---

## Step 3 — Deploy the frontend (Vercel)

1. Sign in at https://vercel.com with GitHub → **Add New → Project**.
2. Import the `Task-Management-System` repo.
3. **Root Directory:** `frontend` (click Edit).
4. Framework preset: **Vite** (detected automatically).
   - Build command `npm run build`, output `dist` — defaults are correct.
5. Add an **Environment Variable** (Production and Preview):

   | Name                   | Value                                             |
   | ---------------------- | ------------------------------------------------- |
   | `VITE_API_BASE_URL`    | `https://<your-service>.onrender.com/api`         |

6. **Deploy.** Vercel gives you `https://<project>.vercel.app`.

`frontend/vercel.json` already rewrites all routes to `index.html`, so deep
links such as `/tasks/5` work on refresh.

---

## Step 4 — Allow the frontend origin in the backend (CORS)

1. In Render → `taskflow-api` → **Environment**, set:

   ```
   CORS_ALLOWED_ORIGINS=https://<project>.vercel.app
   ```

   (add extra origins comma-separated, e.g. a custom domain)
2. Save → Render redeploys automatically.

---

## Step 5 — If you change `VITE_API_BASE_URL` later

Vite bakes env vars in at **build time**. After changing it, redeploy the
frontend (Vercel → Deployments → Redeploy) for the new value to take effect.

---

## Alternative — Single origin

To avoid CORS entirely you can serve `frontend/dist` from the backend itself
(Spring Boot serves static resources). That requires copying the build into
`backend/src/main/resources/static` before packaging — not configured here,
since the Render + Vercel split is cleaner.

---

## Common gotchas

| Symptom                                   | Cause / fix                                                        |
| ----------------------------------------- | ------------------------------------------------------------------ |
| Browser CORS error on API calls           | `CORS_ALLOWED_ORIGINS` doesn't include the exact Vercel URL (no trailing slash) |
| Frontend calls `localhost:8080` in prod   | `VITE_API_BASE_URL` not set, or set but the app wasn't rebuilt     |
| Backend crashes on start                  | Missing/invalid `JWT_SECRET` (must be ≥ 32 chars) or `DB_*` values |
| `401` on every request after deploy       | Old localStorage token from localhost — log out and back in        |
| `Overdue`/date off by hours              | MySQL `serverTimezone=UTC` is already set in the JDBC URL          |
| First load very slow                      | Render free instance cold start — expected                       |

---

## Redeploying after code changes

- **Backend:** push to `main` → Render auto-deploys (Docker rebuild).
- **Frontend:** push to `main` → Vercel auto-deploys.

No manual steps needed once both are connected.
