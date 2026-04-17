# ExecOS waitlist worker

This Cloudflare Worker receives waitlist signups from the landing page and stores them in **Cloudflare KV**.

## Endpoint

- `POST /waitlist`
- Body JSON: `{ "email": "...", "name": "...", "goal": "...", "source": "landing_page" }`

## Deploy (local)

1. Install Wrangler and deps:

```bash
cd serverless/waitlist-worker
npm install
```

2. Create a KV namespace in Cloudflare and copy its id.
3. Put the id into `wrangler.toml` (replace `REPLACE_WITH_YOUR_KV_NAMESPACE_ID`).
4. Deploy:

```bash
npm run deploy
```

## Deploy (GitHub Actions)

This repo includes a workflow: `.github/workflows/deploy-waitlist-worker.yml`.

Add these **GitHub repo secrets**:

- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ACCOUNT_ID`
- `WAITLIST_KV_NAMESPACE_ID`

Then pushing changes under `serverless/waitlist-worker/` to `main` will deploy automatically.

