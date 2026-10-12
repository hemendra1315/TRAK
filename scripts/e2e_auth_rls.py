#!/usr/bin/env python3
"""
End-to-end check against the LOCAL Supabase stack (never a hosted project).

Covers acceptance test A22 (email OTP delivers a 6-digit code that verifies) and exercises
profile RLS through the real API with two independently signed-in users:

  1. Request an email code for user A and user B (POST /auth/v1/otp).
  2. Read each code from the local mail catcher (Mailpit or Inbucket).
  3. Verify each code (POST /auth/v1/verify) and keep the access tokens.
  4. A upserts own profile; A cannot upsert B's id; B cannot read or change A's row;
     anon (publishable key, no user) sees nothing; a stale upsert from A is ignored and the
     upsert returns no row (the signal the app uses to fetch and adopt the newer server row).

Environment: API_URL, ANON_KEY (local publishable/anon key), MAIL_URL (default http://127.0.0.1:54324).
Exit code 0 only if every check passes. Prints one line per check.
"""
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
import uuid

API = os.environ["API_URL"].rstrip("/")
KEY = os.environ["ANON_KEY"]
MAIL = os.environ.get("MAIL_URL", "http://127.0.0.1:54324").rstrip("/")

results = []


def check(name, ok, detail=""):
    results.append(ok)
    print(f"{'PASS' if ok else 'FAIL'}  {name}" + (f"  ({detail})" if detail and not ok else ""))


def http(method, url, body=None, token=None, headers=None):
    h = {"apikey": KEY, "Content-Type": "application/json"}
    if token:
        h["Authorization"] = f"Bearer {token}"
    h.update(headers or {})
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read().decode()
            return r.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw)
        except ValueError:
            return e.code, raw


def get_json(url):
    with urllib.request.urlopen(url, timeout=30) as r:
        return json.loads(r.read().decode())


def fetch_code(email, attempts=30):
    """Reads the newest message for `email` from Mailpit, falling back to Inbucket."""
    local = email.split("@")[0]
    for _ in range(attempts):
        text = None
        try:  # Mailpit
            res = get_json(f"{MAIL}/api/v1/search?query=" + urllib.request.quote(f"to:{email}"))
            msgs = res.get("messages") or []
            if msgs:
                msg = get_json(f"{MAIL}/api/v1/message/{msgs[0]['ID']}")
                text = (msg.get("Text") or "") + (msg.get("HTML") or "")
        except Exception:
            pass
        if text is None:
            try:  # Inbucket
                box = get_json(f"{MAIL}/api/v1/mailbox/{local}")
                if box:
                    msg = get_json(f"{MAIL}/api/v1/mailbox/{local}/{box[-1]['id']}")
                    text = msg["body"].get("text", "") + msg["body"].get("html", "")
            except Exception:
                pass
        if text:
            m = re.search(r"\b(\d{6})\b", text)
            if m:
                return m.group(1)
        time.sleep(1)
    return None


def sign_in(email):
    status, body = http("POST", f"{API}/auth/v1/otp", {"email": email, "create_user": True})
    if status not in (200, 204):
        return None, None, f"otp request {status}: {body}"
    code = fetch_code(email)
    if not code:
        return None, None, "no 6-digit code found in the local inbox"
    status, body = http("POST", f"{API}/auth/v1/verify", {"type": "email", "email": email, "token": code})
    if status != 200 or not isinstance(body, dict) or "access_token" not in body:
        return None, None, f"verify {status}: {body}"
    return body["access_token"], body["user"]["id"], ""


def profile(uid, name, modified):
    return {
        "id": uid,
        "display_name": name,
        "primary_role": "all_rounder",
        "time_zone": "Asia/Kolkata",
        "adult_confirmed_at": "2026-10-12T10:00:00Z",
        "client_modified_at": modified,
    }


def upsert(token, row):
    return http("POST", f"{API}/rest/v1/profiles?on_conflict=id", [row], token=token,
                headers={"Prefer": "resolution=merge-duplicates,return=representation"})


def main():
    run = uuid.uuid4().hex[:8]
    email_a, email_b = f"a-{run}@trak.test", f"b-{run}@trak.test"

    tok_a, uid_a, err = sign_in(email_a)
    check("A22 email code for user A arrives and verifies", tok_a is not None, err)
    tok_b, uid_b, err = sign_in(email_b)
    check("A22 email code for user B arrives and verifies", tok_b is not None, err)
    if not (tok_a and tok_b):
        return finish()

    status, body = upsert(tok_a, profile(uid_a, "Arjun", "2026-10-12T10:00:00Z"))
    check("A upserts own profile", status in (200, 201) and isinstance(body, list) and len(body) == 1,
          f"{status} {body}")

    status, body = upsert(tok_a, profile(uid_a, "Arjun again", "2026-10-12T10:00:00Z"))
    check("Retrying the same upsert is idempotent (one row, same id)",
          status in (200, 201) and isinstance(body, list) and len(body) == 1 and body[0]["id"] == uid_a,
          f"{status} {body}")

    status, body = upsert(tok_a, profile(uid_b, "Impostor", "2026-10-12T10:00:00Z"))
    check("A cannot create a profile with B's id", status in (401, 403), f"{status} {body}")

    status, body = http("GET", f"{API}/rest/v1/profiles?select=id", token=tok_b)
    check("B sees no rows before creating own profile", status == 200 and body == [], f"{status} {body}")

    status, body = http("GET", f"{API}/rest/v1/profiles?id=eq.{uid_a}&select=id", token=tok_b)
    check("B cannot read A's profile by id", status == 200 and body == [], f"{status} {body}")

    status, body = http("PATCH", f"{API}/rest/v1/profiles?id=eq.{uid_a}",
                        {"display_name": "hacked", "client_modified_at": "2026-10-12T11:00:00Z"},
                        token=tok_b, headers={"Prefer": "return=representation"})
    check("B's update of A's row changes nothing", status in (200, 204) and not body, f"{status} {body}")

    status, body = http("GET", f"{API}/rest/v1/profiles?select=display_name", token=tok_a)
    check("A's row is unchanged after B's attempt",
          status == 200 and body == [{"display_name": "Arjun again"}], f"{status} {body}")

    status, body = http("GET", f"{API}/rest/v1/profiles?select=id")
    check("Anonymous (publishable key, no user) cannot read profiles",
          status in (401, 403) or body == [], f"{status} {body}")

    status, body = upsert(tok_a, profile(uid_a, "Stale", "2026-10-11T09:00:00Z"))
    check("Stale upsert returns no row (signal to fetch and adopt the server copy)",
          status in (200, 201) and body == [], f"{status} {body}")
    status, body = http("GET", f"{API}/rest/v1/profiles?select=display_name", token=tok_a)
    check("Stale upsert did not overwrite the newer row",
          status == 200 and body == [{"display_name": "Arjun again"}], f"{status} {body}")

    status, body = http("DELETE", f"{API}/rest/v1/profiles?id=eq.{uid_a}", token=tok_a)
    check("A cannot delete own profile through the API (deletion is a Phase 5 server flow)",
          status in (401, 403), f"{status} {body}")

    return finish()


def finish():
    passed = sum(results)
    print(f"SUMMARY e2e checks: {passed}/{len(results)} passed")
    return 0 if results and all(results) else 1


if __name__ == "__main__":
    sys.exit(main())
