type Env = {
  WAITLIST: KVNamespace;
  ALLOWED_ORIGIN?: string;
};

function corsHeaders(req: Request, env: Env) {
  const origin = req.headers.get("Origin") ?? "";
  const allowed = env.ALLOWED_ORIGIN ?? origin;

  return {
    "Access-Control-Allow-Origin": allowed || "*",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
    "Access-Control-Max-Age": "86400",
    "Vary": "Origin",
  };
}

function json(body: unknown, init?: ResponseInit) {
  return new Response(JSON.stringify(body), {
    ...init,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      ...(init?.headers ?? {}),
    },
  });
}

function normalizeEmail(email: string) {
  return email.trim().toLowerCase();
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    const url = new URL(req.url);

    if (req.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders(req, env) });
    }

    if (url.pathname !== "/waitlist") {
      return json({ ok: false, error: "Not found" }, { status: 404 });
    }

    if (req.method !== "POST") {
      return json(
        { ok: false, error: "Method not allowed" },
        { status: 405, headers: corsHeaders(req, env) },
      );
    }

    let payload: any;
    try {
      payload = await req.json();
    } catch {
      return json(
        { ok: false, error: "Invalid JSON" },
        { status: 400, headers: corsHeaders(req, env) },
      );
    }

    const email = normalizeEmail(String(payload?.email ?? ""));
    const name = String(payload?.name ?? "").trim();
    const goal = String(payload?.goal ?? "").trim();
    const source = String(payload?.source ?? "").trim();

    if (!email || !email.includes("@") || email.length > 254) {
      return json(
        { ok: false, error: "Invalid email" },
        { status: 400, headers: corsHeaders(req, env) },
      );
    }

    const now = new Date().toISOString();
    const key = `email:${email}`;

    const existing = await env.WAITLIST.get(key, "json").catch(() => null);
    const record = {
      email,
      name,
      goal,
      source,
      createdAt: existing?.createdAt ?? now,
      updatedAt: now,
      count: (existing?.count ?? 0) + 1,
      ipHash: null,
    };

    await env.WAITLIST.put(key, JSON.stringify(record));

    return json({ ok: true }, { status: 200, headers: corsHeaders(req, env) });
  },
};

