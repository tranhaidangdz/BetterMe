# BetterMe — Verified Share Cloud Functions

Three HTTPS Cloud Functions that power the Verified Shareable Check-in
History feature:

- `createShare` — caller submits snapshot, server signs + stores.
- `getShare` — server-side HMAC re-validation on every read.
- `verifyShare` — lightweight `{ status }` probe.

## One-time setup

```bash
cd functions
npm install
```

### 1. Generate a server secret

The HMAC signing key must be a long random string. Generate one and
keep it secret:

```bash
openssl rand -hex 32   # → produces 64 hex chars, e.g. 9f...
```

### 2. Plumb the secret into Firebase Functions config

```bash
firebase functions:config:set betterme.share.secret="<your-hex>"
```

For local development with the emulator:

```bash
export BETTERME_SHARE_SECRET="<your-hex>"
```

### 3. Deploy

```bash
firebase deploy --only functions
```

You should see three function URLs in the output, e.g.:

```
✔  functions[createShare(us-central1)]   https://us-central1-<project>.cloudfunctions.net/createShare
✔  functions[getShare(us-central1)]      https://us-central1-<project>.cloudfunctions.net/getShare
✔  functions[verifyShare(us-central1)]   https://us-central1-<project>.cloudfunctions.net/verifyShare
```

### 4. Wire the Android client

In `app/build.gradle.kts`, set:

```kotlin
buildConfigField(
  "String",
  "SHARE_FUNCTIONS_BASE_URL",
  "\"https://us-central1-<project>.cloudfunctions.net/\""
)
```

The Android share repository reads `BuildConfig.SHARE_FUNCTIONS_BASE_URL`
and points Retrofit at it.

## Firestore Security Rules

The `share_history` collection must be **server-only writes**. Add this
to your `firestore.rules`:

```
match /share_history/{shareId} {
  allow read: if true;          // anyone with the shareId can read
  allow write: if false;        // only Cloud Functions (with admin SDK) can write
}
```

Reads stay open because the URL itself is the access token — anyone
with the URL is by definition someone the user shared it with.

## Pretty URLs via Firebase Hosting (optional)

If you want `https://your-domain.com/share/<id>` instead of the raw
Cloud Functions URL, add a rewrite to `firebase.json`:

```json
{
  "hosting": {
    "rewrites": [
      { "source": "/share/**", "function": "getShare" }
    ]
  }
}
```

Then accept `Accept: text/html` for browser hits (the function already
serves a minimal HTML page for those callers).

## Security recap

- HMAC-SHA256 over the entire snapshot's canonical JSON form (sorted
  keys, no whitespace).
- Per-check-in `proofHash = SHA256(userId + itemId + timestamp + secret)`
  so the Android viewer can render a per-row "verified" badge without
  another server round-trip.
- Signature verified on every `getShare` / `verifyShare` call. If a
  malicious actor tampers with Firestore directly, the signature mismatch
  surfaces as a red `❌ Không hợp lệ` page (HTTP 410).
- 90-day expiry stamped at creation. Expired shares return 404 (silent
  — don't leak existence).
- Bearer-token gate on `createShare` so anonymous spam can't fill the
  collection.
