# ExecOS

This repo contains the Android app **and** a simple startup-style landing page you can host on GitHub.

## Landing page (GitHub Pages)

The landing page lives in `docs/`:

- `docs/index.html`
- `docs/styles.css`
- `docs/script.js`

### Publish on GitHub Pages

In your GitHub repo:

- Go to **Settings → Pages**
- **Build and deployment**
  - **Source**: Deploy from a branch
  - **Branch**: `main`
  - **Folder**: `/docs`
- Save

After a minute or two, GitHub will show your Pages URL on that same screen.

### Preview locally

Open `docs/index.html` in a browser.

## Waitlist (serverless endpoint)

The landing page waitlist modal can POST emails to a simple serverless endpoint.

This repo includes a Cloudflare Worker example at `serverless/waitlist-worker/` that stores signups in **Cloudflare KV**.

### Deploy (Cloudflare Worker)

1. Create a Cloudflare account and install Wrangler locally.
2. Create a KV namespace (example name: `execos_waitlist`).
3. Update `serverless/waitlist-worker/wrangler.toml` with your KV namespace id.
4. Deploy:

```bash
cd serverless/waitlist-worker
npm install
npm run deploy
```

5. Copy your deployed endpoint URL and set it in `docs/index.html`:

```js
window.__WAITLIST_ENDPOINT__ = "https://YOUR_WORKER_SUBDOMAIN.workers.dev/waitlist";
```


