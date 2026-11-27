#!/usr/bin/env python3
"""Checks that the build server sends every field the app reads.

Run before shipping:   python3 tool/check_contract.py path/to/server.js

It needs no network and no packages. It catches the kind of bug where the app expects a field
(like refundedTotal, which drives the "Build refunded" popup) that the server never sends.
Exit code 0 = fine, 1 = something is missing.
"""
import re, sys, pathlib

APP_API = pathlib.Path(__file__).resolve().parent.parent / 'lib' / 'api.dart'

# route -> (how the route starts in server.js, fields the app reads from the reply, and which are required)
CONTRACT = [
    ('GET',  '/api/build-limit',         ['used', 'limit', 'remaining', 'resetsAt', 'refundedTotal'], ['used', 'limit', 'remaining', 'refundedTotal']),
    ('POST', '/api/admin/refund-build',  ['used', 'limit', 'remaining', 'refundedTotal'],             ['used', 'limit', 'remaining', 'refundedTotal']),
]

def handler_text(server, method, route):
    m = re.search(r"app\.%s\(\s*'%s'" % (method.lower(), re.escape(route)), server)
    if not m:
        return None
    start = m.start()
    end = server.find('\n});', start)   # the route handler ends at the first closing "});" on its own line
    return server[start:end if end != -1 else len(server)]

def main():
    if len(sys.argv) < 2:
        print(__doc__); return 2
    server = pathlib.Path(sys.argv[1]).read_text(encoding='utf-8')
    dart = APP_API.read_text(encoding='utf-8')
    bad = 0
    for method, route, fields, required in CONTRACT:
        # 1. the app really reads these fields (keeps this table honest when the app changes)
        for f in fields:
            if "j['%s']" % f not in dart:
                print('NOTE  app no longer reads %s (%s %s): update tool/check_contract.py' % (f, method, route))
        # 2. the server sends them
        body = handler_text(server, method, route)
        if body is None:
            print('FAIL  %s %s is missing from the server' % (method, route)); bad += 1; continue
        for f in fields:
            if not re.search(r'\b%s\b' % re.escape(f), body):
                level = 'FAIL' if f in required else 'warn'
                print('%s  %s %s does not send "%s"' % (level, method, route, f))
                if f in required: bad += 1
    print('contract OK' if not bad else '%d problem(s)' % bad)
    return 1 if bad else 0

if __name__ == '__main__':
    sys.exit(main())
