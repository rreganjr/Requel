#!/usr/bin/env python3
#
# This file is part of Requel - the Collaborative Requirements Elicitation System.
# Copyright 2026 Ron Regan Jr. GPL v3 or later; see LICENSE.
#
"""Sit between Requel and an OpenAI-compatible server and keep every exchange (issue #365).

Requel's Spring AI client doesn't record what it sends or what comes back, so a local model's
reply can't be inspected after a run. Point Requel at this proxy instead of the server
(REQUEL_AI_BASE_URL=http://localhost:11435) and each POST is forwarded unchanged to the real
server (default http://localhost:11434). The request and the reply are written to the capture
directory as NNNN-request.json and NNNN-response.json, and one line per exchange is printed:

  0007 qwen3.6:27b 200 41.2s prompt=7311 completion=912 finish=stop reasoning=0 json=ok

json=ok means the reply's message content parses as JSON; json=BAD means it doesn't (the run will
fail with a schema error); finish=length means the model hit the output cap. A prompt count near
the server's context length means the prompt was cut. A dev tool: standard library only, local
use only, never part of the build.
"""

import argparse
import http.server
import json
import os
import sys
import threading
import time
import urllib.error
import urllib.request


def summarize(number, request, status, seconds, response):
    model = request.get("model", "?") if isinstance(request, dict) else "?"
    usage = response.get("usage") or {} if isinstance(response, dict) else {}
    choice = ((response.get("choices") or [{}])[0]) if isinstance(response, dict) else {}
    message = choice.get("message") or {}
    content = message.get("content") or ""
    reasoning = message.get("reasoning") or message.get("reasoning_content") or ""
    try:
        json.loads(content.strip().removeprefix("```json").removesuffix("```").strip())
        parsed = "ok"
    except ValueError:
        parsed = "BAD"
    return (f"{number:04d} {model} {status} {seconds:.1f}s"
            f" prompt={usage.get('prompt_tokens', '?')}"
            f" completion={usage.get('completion_tokens', '?')}"
            f" finish={choice.get('finish_reason', '?')}"
            f" reasoning={len(reasoning)} json={parsed if content else 'EMPTY'}")


def make_handler(target, out_dir):
    # A local server: never route the forward through an HTTP(S)_PROXY from the environment.
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    counter = {"n": 0}
    lock = threading.Lock()

    class Handler(http.server.BaseHTTPRequestHandler):
        def do_POST(self):
            with lock:
                counter["n"] += 1
                number = counter["n"]
            body = self._read_body()
            self._write(number, "request", body)
            headers = {k: v for k, v in self.headers.items()
                       if k.lower() not in ("host", "content-length", "accept-encoding",
                                            "transfer-encoding", "connection")}
            started = time.time()
            try:
                with opener.open(urllib.request.Request(
                        target + self.path, data=body, headers=headers, method="POST"),
                        timeout=3600) as upstream:
                    status, reply = upstream.status, upstream.read()
                    content_type = upstream.headers.get("Content-Type", "application/json")
            except urllib.error.HTTPError as e:
                status, reply = e.code, e.read()
                content_type = e.headers.get("Content-Type", "application/json")
            seconds = time.time() - started
            self._write(number, "response", reply)
            try:
                line = summarize(number, json.loads(body or b"{}"), status, seconds,
                                 json.loads(reply or b"{}"))
            except ValueError:
                line = f"{number:04d} {status} {seconds:.1f}s (not JSON)"
            print(line, flush=True)
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(reply)))
            self.end_headers()
            self.wfile.write(reply)

        def _read_body(self):
            # Spring AI's HTTP client may stream the request with chunked transfer encoding and
            # no Content-Length; read either form.
            if "chunked" in self.headers.get("Transfer-Encoding", "").lower():
                chunks = []
                while True:
                    size = int(self.rfile.readline().split(b";")[0].strip() or b"0", 16)
                    if size == 0:
                        while self.rfile.readline() not in (b"\r\n", b"\n", b""):
                            pass  # trailers
                        return b"".join(chunks)
                    chunks.append(self.rfile.read(size))
                    self.rfile.readline()  # the CRLF after each chunk
            return self.rfile.read(int(self.headers.get("Content-Length", 0)))

        def _write(self, number, kind, data):
            path = os.path.join(out_dir, f"{number:04d}-{kind}.json")
            try:
                pretty = json.dumps(json.loads(data), indent=2, ensure_ascii=False)
                with open(path, "w", encoding="utf-8") as f:
                    f.write(pretty)
            except ValueError:
                with open(path, "wb") as f:
                    f.write(data)

        def log_message(self, *args):
            pass

    return Handler


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--listen", type=int, default=11435, help="port to listen on")
    parser.add_argument("--target", default="http://localhost:11434", help="the real server")
    parser.add_argument("--out", default="target/ai-eval/capture", help="capture directory")
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    server = http.server.ThreadingHTTPServer(("127.0.0.1", args.listen),
                                             make_handler(args.target.rstrip("/"), args.out))
    print(f"capturing http://127.0.0.1:{args.listen} -> {args.target} into {args.out}",
          flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    return 0


if __name__ == "__main__":
    sys.exit(main())
