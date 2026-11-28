# Refund build: server endpoint (build server on Render)

The app calls this when you (the owner) refund a user:

    POST /api/admin/refund-build
    Headers: X-Admin-Password: <your owner key>, Content-Type: application/json
    Body:    { "clientId": "<user's Support ID>", "count": 1 }
    Reply:   200 { "used": 2, "limit": 5, "remaining": 3, "resetsAt": "..." }
             401/403 wrong owner key, 404 = server not updated yet

What it must do: find that clientId's build count for today (the same counter /api/build-limit
reads) and lower it by `count`, never below 0. Same check for the owner key as /api/build-limit
already uses for X-Admin-Password.

Sketch (Express, adapt the counter lines to your real storage):

    app.post('/api/admin/refund-build', express.json(), (req, res) => {
      if (req.get('X-Admin-Password') !== process.env.ADMIN_PASSWORD) return res.status(403).json({ error: 'forbidden' });
      const id = String(req.body?.clientId || '').trim();
      const n = Math.max(1, Math.min(50, parseInt(req.body?.count, 10) || 1));
      if (!id) return res.status(400).json({ error: 'clientId required' });
      const rec = getTodaysCount(id);              // <- your existing per-client counter
      setTodaysCount(id, Math.max(0, rec.used - n));
      const used = Math.max(0, rec.used - n), limit = DAILY_LIMIT;
      res.json({ used, limit, remaining: Math.max(0, limit - used), resetsAt: nextMidnightUtc() });
    });


## Refund popup for the user (app v326+)

So the user sees a "Build refunded" popup, the server also keeps a running total of refunds per client
and sends it back from the build-limit route:

1. In refund-build: after lowering the count, also raise a lifetime counter for that clientId:
       setRefundedTotal(id, getRefundedTotal(id) + n);   // never reset it, not even at midnight
   (it can also be returned in the refund reply as `refundedTotal`).
2. In GET /api/build-limit: add the field to the reply:
       { "used": 2, "limit": 5, "remaining": 3, "resetsAt": "...", "refundedTotal": getRefundedTotal(clientId) }

The app remembers the last total it saw. When the number goes up it shows one popup
("Your build was refunded. You have N builds left today."). The first time the app sees the
number it only records it, so old refunds never pop up. If the field is missing, no popup shows.
