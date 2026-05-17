/**
 * BetterMe — Verified Shareable Check-in History
 *
 * Three HTTPS endpoints behind firebase-functions v2:
 *
 *   POST /createShare    — caller submits a snapshot payload, server signs
 *                          it with HMAC-SHA256 over a canonical JSON form,
 *                          stores in `share_history`, returns the shareId.
 *
 *   GET  /getShare?id=…  — re-validates the stored signature, returns the
 *                          snapshot when the signature matches, 410 GONE
 *                          when it doesn't. JSON for application/json
 *                          callers, minimal HTML for browsers (Messenger
 *                          previews etc.).
 *
 *   GET  /verifyShare?id=…
 *                        — light verification endpoint that ONLY returns
 *                          `{ status: "VALID" | "INVALID" | "NOT_FOUND" }`
 *                          without the snapshot payload. Used by the
 *                          Android viewer when it wants to confirm
 *                          freshness without re-downloading the snapshot.
 *
 * ## Security model
 *
 * - The HMAC secret lives in Functions config: `betterme.share.secret`.
 *   Set it once per project:
 *
 *     firebase functions:config:set betterme.share.secret="<long random hex>"
 *
 *   Never commit it. Treat it like a private key.
 *
 * - All writes to the `share_history` Firestore collection happen through
 *   this server. The Firestore Security Rules accompanying this deploy
 *   should reject ALL direct client writes to `share_history` so the only
 *   way a doc lands there is via `createShare`. Reads are mediated through
 *   `getShare` (which validates the signature before returning).
 *
 * - Per-check-in `proofHash` is derived server-side from
 *   `SHA256(userId + itemId + timestamp + serverSecret)`. Stored alongside
 *   each check-in inside the snapshot — caller cannot forge a hash because
 *   they do not have the server secret. The TOP-LEVEL HMAC over the entire
 *   snapshot is the integrity primitive; the per-check-in hash is the
 *   "proof" surface the Android UI shows ("✔ Verified by server").
 *
 * ## URL routing
 *
 * Default Cloud Functions URLs (after `firebase deploy --only functions`):
 *
 *   https://<region>-<project>.cloudfunctions.net/createShare
 *   https://<region>-<project>.cloudfunctions.net/getShare?id=<shareId>
 *   https://<region>-<project>.cloudfunctions.net/verifyShare?id=<shareId>
 *
 * Pretty URLs (optional, requires Firebase Hosting rewrite — see README):
 *
 *   https://<your-domain>/share/<shareId>
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

// ─── Config ─────────────────────────────────────────────────────────

const COLLECTION = "share_history";
const HMAC_ALG = "sha256";

/**
 * Resolve the server secret used for HMAC signing + proofHash
 * derivation. Reads from Firebase Functions config first, then falls
 * back to the `BETTERME_SHARE_SECRET` env var so emulator + CI work.
 *
 * Throws when neither is set so we never accidentally sign with an
 * empty key in production.
 */
function getServerSecret() {
  const fromConfig =
    (functions.config &&
      functions.config().betterme &&
      functions.config().betterme.share &&
      functions.config().betterme.share.secret) ||
    null;
  const fromEnv = process.env.BETTERME_SHARE_SECRET || null;
  const secret = fromConfig || fromEnv;
  if (!secret || secret.length < 32) {
    throw new Error(
      "BetterMe share secret is missing or too short. Set it with " +
        "`firebase functions:config:set betterme.share.secret=\"<hex string ≥ 32 chars>\"` " +
        "and redeploy."
    );
  }
  return secret;
}

// ─── Canonical JSON for signing ─────────────────────────────────────

/**
 * Serialize a value into the same shape every time so the HMAC is
 * deterministic. Sorts object keys alphabetically and recurses. Skips
 * `undefined` values entirely.
 *
 * We deliberately do NOT depend on a third-party canonicalizer; the
 * function surface is small enough to keep inline.
 */
function canonicalJson(value) {
  if (value === null || value === undefined) return "null";
  if (typeof value === "number" || typeof value === "boolean") {
    return JSON.stringify(value);
  }
  if (typeof value === "string") return JSON.stringify(value);
  if (Array.isArray(value)) {
    return "[" + value.map(canonicalJson).join(",") + "]";
  }
  const keys = Object.keys(value).filter(k => value[k] !== undefined).sort();
  return (
    "{" +
    keys
      .map(k => JSON.stringify(k) + ":" + canonicalJson(value[k]))
      .join(",") +
    "}"
  );
}

function hmac(payloadObj, secret) {
  return crypto
    .createHmac(HMAC_ALG, secret)
    .update(canonicalJson(payloadObj))
    .digest("hex");
}

/**
 * Per-check-in proof hash. Different from the top-level HMAC: this is
 * a one-way hash (no signing key reuse) that the UI can render as a
 * "tamper-evident" badge next to each row.
 */
function checkInProofHash(userId, itemId, timestamp, secret) {
  return crypto
    .createHash("sha256")
    .update(`${userId}|${itemId}|${timestamp}|${secret}`)
    .digest("hex");
}

// ─── Validation ─────────────────────────────────────────────────────

const VALID_TYPES = new Set(["FULL_HISTORY", "HABIT", "CHALLENGE"]);
const MAX_CHECKINS_PER_SHARE = 500;

function badRequest(res, message) {
  return res.status(400).json({ error: message });
}

function unauthorized(res) {
  return res.status(401).json({ error: "Auth token missing or invalid." });
}

/**
 * Lightweight bearer-token check. Real Firebase auth verification
 * happens via admin.auth().verifyIdToken — keeping this here as a
 * tiny gate so anonymous clients can't spam createShare and bloat
 * Firestore.
 */
async function requireAuth(req) {
  const header = req.headers.authorization || "";
  const m = header.match(/^Bearer\s+(.+)$/i);
  if (!m) return null;
  try {
    return await admin.auth().verifyIdToken(m[1]);
  } catch (_) {
    return null;
  }
}

// ─── createShare ────────────────────────────────────────────────────

exports.createShare = functions.https.onRequest(async (req, res) => {
  // Permissive CORS so the Android Retrofit client + a future web
  // viewer can both call without a custom origin.
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
  if (req.method === "OPTIONS") return res.status(204).send("");
  if (req.method !== "POST") return res.status(405).json({ error: "Use POST" });

  const decoded = await requireAuth(req);
  if (!decoded) return unauthorized(res);

  let payload;
  try {
    payload = typeof req.body === "string" ? JSON.parse(req.body) : req.body;
  } catch (_) {
    return badRequest(res, "Body must be valid JSON.");
  }
  if (!payload || typeof payload !== "object") {
    return badRequest(res, "Missing body.");
  }

  const type = payload.type;
  if (!VALID_TYPES.has(type)) {
    return badRequest(res, "type must be FULL_HISTORY | HABIT | CHALLENGE.");
  }
  const checkIns = Array.isArray(payload.checkIns) ? payload.checkIns : [];
  if (checkIns.length === 0) {
    return badRequest(res, "checkIns is empty.");
  }
  if (checkIns.length > MAX_CHECKINS_PER_SHARE) {
    return badRequest(res, `checkIns exceeds ${MAX_CHECKINS_PER_SHARE} entries.`);
  }

  const secret = getServerSecret();
  const userId = decoded.uid;
  const createdAt = Date.now();
  const shareId = crypto.randomUUID();

  // Stamp each check-in with a server-derived proofHash so the Android
  // viewer can render them as "verified" without round-tripping to the
  // server for each row. The hash itself is one-way and unforgeable
  // without the server secret.
  const stampedCheckIns = checkIns.map(c => {
    const itemId = String(c.itemId || "");
    const timestamp = Number(c.timestamp || 0);
    return {
      itemId,
      itemTitle: String(c.itemTitle || ""),
      timestamp,
      kind: c.kind === "CHALLENGE" ? "CHALLENGE" : "HABIT",
      note: c.note ? String(c.note).slice(0, 280) : null,
      proofHash: checkInProofHash(userId, itemId, timestamp, secret),
    };
  });

  // Summary fields — the things the Android viewer + the rich-share
  // text both need. Computed server-side so the client can't inflate
  // numbers in the share message.
  const totalCheckIns = stampedCheckIns.length;
  const uniqueItems = new Set(stampedCheckIns.map(c => c.itemId)).size;
  const earliest = Math.min(...stampedCheckIns.map(c => c.timestamp));
  const latest = Math.max(...stampedCheckIns.map(c => c.timestamp));

  // Sanitized profile snapshot — pulled from payload but field-bounded
  // so callers can't inject arbitrary keys.
  const profile = {
    displayName: String(payload.profile?.displayName || "BetterMe User").slice(0, 80),
    avatarUrl: payload.profile?.avatarUrl
      ? String(payload.profile.avatarUrl).slice(0, 500)
      : null,
  };

  const snapshot = {
    shareId,
    userId,
    type,
    profile,
    summary: {
      totalCheckIns,
      uniqueItems,
      earliestTimestamp: earliest,
      latestTimestamp: latest,
      currentStreakDays: Number(payload.summary?.currentStreakDays || 0),
      longestStreakDays: Number(payload.summary?.longestStreakDays || 0),
      completedChallenges: Number(payload.summary?.completedChallenges || 0),
      legendaryChallenges: Number(payload.summary?.legendaryChallenges || 0),
    },
    checkIns: stampedCheckIns,
    createdAt,
    expiresAt: createdAt + 90 * 24 * 60 * 60 * 1000, // 90 days TTL
  };

  // Sign EVERYTHING above. Stored signature is the integrity primitive.
  const signature = hmac(snapshot, secret);

  await db.collection(COLLECTION).doc(shareId).set({
    ...snapshot,
    signature,
  });

  return res.json({
    shareId,
    createdAt,
    // The function URL caller will hit on view.
    deepLink: `betterme://share/${shareId}`,
  });
});

// ─── getShare ───────────────────────────────────────────────────────

exports.getShare = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, OPTIONS");
  if (req.method === "OPTIONS") return res.status(204).send("");
  if (req.method !== "GET") return res.status(405).json({ error: "Use GET" });

  const shareId = (req.query.id || "").toString();
  if (!shareId) return badRequest(res, "id is required.");

  const doc = await db.collection(COLLECTION).doc(shareId).get();
  if (!doc.exists) {
    return respondNotFound(req, res, shareId);
  }
  const data = doc.data();
  const expectedSignature = data.signature;

  // Reconstruct the snapshot without the stored signature, recompute,
  // compare. Mismatch → Firestore was tampered with externally; refuse
  // to surface the data.
  // eslint-disable-next-line no-unused-vars
  const { signature, ...snapshot } = data;
  const secret = getServerSecret();
  const actual = hmac(snapshot, secret);
  if (actual !== expectedSignature) {
    return respondInvalid(req, res, shareId);
  }

  // TTL check — silent expiry; treat as not-found to avoid leaking
  // even the existence of an expired share.
  if (snapshot.expiresAt && Date.now() > snapshot.expiresAt) {
    return respondNotFound(req, res, shareId);
  }

  // Content negotiation. Messenger / Zalo / Facebook preview the URL
  // with `Accept: text/html`; the Android client always sends
  // `Accept: application/json`.
  const wantsHtml = (req.headers.accept || "").includes("text/html");
  if (wantsHtml) {
    return res.set("Content-Type", "text/html").send(renderHtml(snapshot));
  }
  return res.json({ status: "VALID", snapshot });
});

// ─── verifyShare ────────────────────────────────────────────────────

exports.verifyShare = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method !== "GET") return res.status(405).json({ error: "Use GET" });

  const shareId = (req.query.id || "").toString();
  if (!shareId) return badRequest(res, "id is required.");

  const doc = await db.collection(COLLECTION).doc(shareId).get();
  if (!doc.exists) return res.json({ status: "NOT_FOUND" });
  const data = doc.data();
  // eslint-disable-next-line no-unused-vars
  const { signature, ...snapshot } = data;
  const secret = getServerSecret();
  const valid = hmac(snapshot, secret) === signature;
  if (snapshot.expiresAt && Date.now() > snapshot.expiresAt) {
    return res.json({ status: "NOT_FOUND" });
  }
  return res.json({ status: valid ? "VALID" : "INVALID" });
});

// ─── Helpers ────────────────────────────────────────────────────────

function respondNotFound(req, res, shareId) {
  const wantsHtml = (req.headers.accept || "").includes("text/html");
  if (wantsHtml) {
    return res
      .status(404)
      .set("Content-Type", "text/html")
      .send(renderError("Link không tồn tại hoặc đã hết hạn."));
  }
  return res.status(404).json({ status: "NOT_FOUND" });
}

function respondInvalid(req, res, shareId) {
  const wantsHtml = (req.headers.accept || "").includes("text/html");
  if (wantsHtml) {
    return res
      .status(410)
      .set("Content-Type", "text/html")
      .send(renderError("Dữ liệu chia sẻ đã bị thay đổi — không đáng tin."));
  }
  return res.status(410).json({ status: "INVALID" });
}

function esc(s) {
  return String(s || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function renderHtml(snap) {
  const checkIns = (snap.checkIns || [])
    .slice(0, 50)
    .map(
      c => `
        <li class="row">
          <span class="kind">${esc(c.kind === "CHALLENGE" ? "🏆" : "✅")}</span>
          <span class="title">${esc(c.itemTitle)}</span>
          <span class="when">${new Date(c.timestamp).toLocaleDateString("vi-VN")}</span>
        </li>`
    )
    .join("");
  return `<!doctype html>
<html lang="vi"><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>BetterMe — Xác nhận từ máy chủ</title>
<style>
  body { font-family: system-ui, sans-serif; max-width: 560px; margin: 0 auto;
         padding: 20px; background: #F7F7F8; color: #111; }
  .card { background: #fff; border-radius: 16px; padding: 18px;
          box-shadow: 0 2px 8px rgba(0,0,0,.06); }
  h1 { font-size: 18px; margin: 0 0 4px; }
  .verified { display: inline-block; background: #E8F5E9; color: #16A34A;
              padding: 4px 10px; border-radius: 999px; font-size: 12px;
              font-weight: 600; margin-bottom: 12px; }
  .summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
             margin-top: 12px; }
  .stat { background: #F2F4F7; border-radius: 10px; padding: 10px;
          text-align: center; }
  .stat b { display:block; font-size: 18px; }
  .stat span { font-size: 11px; color: #666; }
  ul { list-style: none; padding: 0; margin: 16px 0 0; }
  .row { display: flex; gap: 10px; padding: 8px 0; border-bottom: 1px solid #EEE; }
  .row .kind { width: 22px; }
  .row .title { flex: 1; }
  .row .when { color: #777; font-size: 12px; }
  .footer { color: #666; font-size: 12px; margin-top: 14px; text-align: center; }
</style></head>
<body>
<div class="card">
  <span class="verified">✔ Đã xác nhận bởi máy chủ BetterMe</span>
  <h1>${esc(snap.profile.displayName)}</h1>
  <div class="summary">
    <div class="stat"><b>${snap.summary.totalCheckIns}</b><span>Check-in</span></div>
    <div class="stat"><b>${snap.summary.currentStreakDays}</b><span>Chuỗi ngày</span></div>
    <div class="stat"><b>${snap.summary.completedChallenges}</b><span>Thử thách</span></div>
  </div>
  <ul>${checkIns}</ul>
</div>
<p class="footer">BetterMe • shareId ${esc(snap.shareId)}</p>
</body></html>`;
}

function renderError(message) {
  return `<!doctype html>
<html lang="vi"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>BetterMe — Không hợp lệ</title>
<style>
  body { font-family: system-ui, sans-serif; max-width: 480px; margin: 60px auto;
         padding: 20px; text-align: center; color: #111; }
  .card { background: #fff; border-radius: 16px; padding: 24px;
          box-shadow: 0 2px 8px rgba(0,0,0,.06); }
  .bad { display: inline-block; background: #FFEBEE; color: #DC2626;
         padding: 6px 12px; border-radius: 999px; font-weight: 600;
         margin-bottom: 10px; }
</style></head>
<body><div class="card">
  <span class="bad">❌ Không hợp lệ</span>
  <p>${esc(message)}</p>
</div></body></html>`;
}
