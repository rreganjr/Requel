#!/usr/bin/env python3
#
# This file is part of Requel - the Collaborative Requirements Elicitation System.
# Copyright 2026 Ron Regan Jr. GPL v3 or later; see LICENSE.
#
"""Score Requel's AI review against the evaluation fixture (issue #355).

Imports scripts/ai-eval/fixture-project.xml into a running Requel, reviews every entity named
in expectations.json a few times through POST /api/ai/reviews, reads each run back with
GET /api/ai/reviews, and writes a per-type report. A dev tool: it needs a running server with an
AI provider (usually the `cli` provider), it is not part of the build, and CI never runs it.

Standard library only. See README.md beside this file.
"""

import argparse
import datetime as _dt
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

HERE = os.path.dirname(os.path.abspath(__file__))
TYPES = ["Goal", "Story", "UseCase", "Scenario", "Step", "Actor", "GlossaryTerm"]
TERMINAL = {"SUCCEEDED", "FAILED", "SKIPPED", "CANCELLED"}
# ReviewResultMapper's messages for a reply that is not the schema's shape.
SCHEMA_FAILURE = re.compile(r"model reply|structured output|finding missing", re.I)


# ---- expectations and matching (pure; covered by test_score.py) -----------------------------

def load_expectations(path):
    with open(path, encoding="utf-8") as f:
        doc = json.load(f)
    for entity in doc["entities"]:
        entity.setdefault("expect", [])
        entity.setdefault("silent", False)
        entity.setdefault("trap", False)
        entity.setdefault("confirmPatterns", [])
        for item in entity["expect"]:
            item.setdefault("types", [])
    return doc


def _matches_any(patterns, text):
    return any(re.search(p, text or "", re.I) for p in patterns)


def score_run(entity, findings, summary):
    """Score one run's findings on one entity.

    Returns {"hits": [item ids], "misses": [item ids], "spurious": [findings],
    "confirmed": bool}. A finding matches an expected item when its text matches one of the
    item's patterns and, if the item lists types, its findingType is one of them. Each finding
    matches at most one item (first in file order) and each item counts once. On a silent
    entity every finding is spurious.
    """
    remaining = list(entity["expect"])
    hits, spurious = [], []
    for finding in findings:
        text = finding.get("text") or ""
        matched = None
        for item in remaining:
            if item["types"] and finding.get("findingType") not in item["types"]:
                continue
            if _matches_any(item["patterns"], text):
                matched = item
                break
        if matched is None:
            spurious.append(finding)
        else:
            hits.append(matched["id"])
            remaining.remove(matched)
    confirmed = False
    if entity["trap"]:
        texts = [f.get("text") or "" for f in findings] + [summary or ""]
        confirmed = any(_matches_any(entity["confirmPatterns"], t) for t in texts)
    return {"hits": hits, "misses": [i["id"] for i in remaining], "spurious": spurious,
            "confirmed": confirmed}


def classify(view):
    """'ok', 'schema' (the reply was not the schema's shape), 'failed', 'timeout' or the
    run's other terminal status in lower case."""
    if view is None:
        return "timeout"
    status = view.get("status")
    if status == "SUCCEEDED":
        return "ok"
    if status == "FAILED":
        return "schema" if SCHEMA_FAILURE.search(view.get("errorSummary") or "") else "failed"
    return (status or "unknown").lower()


def aggregate(results):
    """Per-type rows from [{"entity", "runs": [{"outcome", "score"}], "skipped"}]."""
    rows = {}
    for result in results:
        entity = result["entity"]
        row = rows.setdefault(entity["type"], {
            "entities": 0, "skipped": 0, "runs": 0, "ok": 0, "expected": 0, "hits": 0,
            "spurious": 0, "silentRuns": 0, "silentKept": 0, "schema": 0, "failed": 0,
            "timeout": 0, "confirmed": 0})
        row["entities"] += 1
        if result.get("skipped"):
            row["skipped"] += 1
            continue
        for run in result["runs"]:
            row["runs"] += 1
            outcome = run["outcome"]
            if outcome != "ok":
                key = outcome if outcome in ("schema", "timeout") else "failed"
                row[key] += 1
                continue
            row["ok"] += 1
            score = run["score"]
            row["expected"] += len(entity["expect"])
            row["hits"] += len(score["hits"])
            row["spurious"] += len(score["spurious"])
            if entity["silent"]:
                row["silentRuns"] += 1
                if not run["findings"]:
                    row["silentKept"] += 1
            if score["confirmed"]:
                row["confirmed"] += 1
    return rows


def _ratio(numerator, denominator):
    return "-" if denominator == 0 else "%.0f%% (%d/%d)" % (
        100.0 * numerator / denominator, numerator, denominator)


def render_report(meta, results):
    rows = aggregate(results)
    out = ["# AI review evaluation", ""]
    for key in ("date", "label", "baseUrl", "project", "runsPerEntity"):
        out.append("- **%s:** %s" % (key, meta.get(key)))
    out += ["", "| Type | Entities | Runs ok/total | Hit rate | Spurious per run | Silent kept "
            "silent | Schema failures | Other failures | Timeouts | Confirmed (trap) |",
            "|---|---|---|---|---|---|---|---|---|---|"]
    total = None
    for type_name in TYPES + sorted(set(rows) - set(TYPES)):
        row = rows.get(type_name)
        if row is None:
            continue
        if total is None:
            total = {k: 0 for k in row}
        for k in row:
            total[k] += row[k]
        out.append(_row(type_name, row))
    if total is not None:
        out.append(_row("**All**", total))
    out += ["", "Hit rate = expected findings matched / (expected findings x successful runs). "
            "Spurious = findings that matched no expected finding. Silent kept silent = "
            "successful runs on silent entities that raised nothing.", ""]

    out += ["## Details", ""]
    for result in results:
        entity = result["entity"]
        title = "### %s: %s" % (entity["type"], entity["name"])
        if result.get("skipped"):
            out += [title, "", "Skipped: %s" % result["skipped"], ""]
            continue
        out += [title, ""]
        if entity["silent"]:
            out.append("Silent case (any finding is spurious).")
        else:
            out.append("Expects: " + ", ".join(i["id"] for i in entity["expect"]))
        for n, run in enumerate(result["runs"], 1):
            if run["outcome"] != "ok":
                out.append("- run %d: **%s** %s" % (n, run["outcome"],
                                                     (run.get("error") or "").strip()[:300]))
                continue
            score = run["score"]
            line = "- run %d: hits %s; misses %s" % (
                n, ", ".join(score["hits"]) or "none", ", ".join(score["misses"]) or "none")
            if score["confirmed"]:
                line += "; **CONFIRMED the figures**"
            out.append(line)
            for finding in score["spurious"]:
                out.append("  - spurious `%s`: %s" % (finding.get("findingType"),
                                                      _one_line(finding.get("text"))))
        out.append("")
    return "\n".join(out) + "\n"


def _row(name, row):
    entities = "%d" % (row["entities"] - row["skipped"])
    if row["skipped"]:
        entities += " (%d skipped)" % row["skipped"]
    return "| %s | %s | %d/%d | %s | %s | %s | %d | %d | %d | %d |" % (
        name, entities, row["ok"], row["runs"],
        _ratio(row["hits"], row["expected"]),
        "-" if row["ok"] == 0 else "%.1f" % (row["spurious"] / float(row["ok"])),
        _ratio(row["silentKept"], row["silentRuns"]),
        row["schema"], row["failed"], row["timeout"], row["confirmed"])


def _error_message(body):
    try:
        return json.loads(body).get("message") or body
    except (ValueError, AttributeError):
        return _one_line(body)


def _one_line(text):
    return re.sub(r"\s+", " ", text or "").strip()[:300]


# ---- HTTP ------------------------------------------------------------------------------------

class RequelClient:
    def __init__(self, base_url, username, password):
        self.base_url = base_url.rstrip("/")
        self.username = username
        self.password = password
        self.token = None

    def login(self):
        body = json.dumps({"username": self.username, "password": self.password}).encode()
        status, data = self._send("POST", "/api/auth/login", body,
                                  {"Content-Type": "application/json"}, auth=False)
        if status != 200:
            raise SystemExit("login as %s failed: HTTP %d %s" % (self.username, status, data))
        self.token = json.loads(data)["token"]

    def _send(self, method, path, body=None, headers=None, auth=True):
        request = urllib.request.Request(self.base_url + path, data=body, method=method,
                                         headers=dict(headers or {}))
        if auth and self.token:
            request.add_header("Authorization", "Bearer " + self.token)
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                return response.status, response.read().decode("utf-8")
        except urllib.error.HTTPError as e:
            return e.code, e.read().decode("utf-8", "replace")

    def call(self, method, path, body=None, headers=None):
        status, data = self._send(method, path, body, headers)
        if status == 401:  # token expired on a long run: log in again once
            self.login()
            status, data = self._send(method, path, body, headers)
        return status, data

    def get_json(self, path):
        status, data = self.call("GET", path)
        if status != 200:
            raise RuntimeError("GET %s: HTTP %d %s" % (path, status, data[:300]))
        return json.loads(data)

    def import_project(self, xml_path, name):
        boundary = "----requel-ai-eval-" + uuid.uuid4().hex
        with open(xml_path, "rb") as f:
            xml = f.read()
        parts = [
            b"--" + boundary.encode(),
            b'Content-Disposition: form-data; name="input"',
            b"Content-Type: application/json", b"",
            json.dumps({"name": name}).encode(),
            b"--" + boundary.encode(),
            b'Content-Disposition: form-data; name="file"; filename="fixture-project.xml"',
            b"Content-Type: application/xml", b"", xml,
            b"--" + boundary.encode() + b"--", b""]
        status, data = self.call("POST", "/api/commands/ImportProject", b"\r\n".join(parts),
                                 {"Content-Type": "multipart/form-data; boundary=" + boundary})
        if status not in (200, 201):
            raise SystemExit("import failed: HTTP %d %s" % (status, data[:500]))

    def delete_project(self, name):
        project = self.get_json("/api/projects/" + _q(name))
        body = json.dumps({"projectName": name, "version": project.get("version")}).encode()
        return self.call("POST", "/api/commands/DeleteProject", body,
                         {"Content-Type": "application/json"})

    def entity_ids(self, project):
        """{(type, name): id} for every entity the expectations can name."""
        base = "/api/projects/" + _q(project)
        ids = {}
        for type_name, path in (("Goal", "/goals"), ("Story", "/stories"), ("Actor", "/actors"),
                                ("UseCase", "/use-cases"), ("GlossaryTerm", "/terms"),
                                ("Scenario", "/scenarios")):
            for item in self.get_json(base + path):
                ids[(type_name, item["name"])] = item["id"]
        for (type_name, _), scenario_id in list(ids.items()):
            if type_name != "Scenario":
                continue
            detail = self.get_json("%s/scenarios/%d" % (base, scenario_id))
            for step in detail.get("steps") or []:
                key = ("Scenario" if step.get("isScenario") else "Step", step["name"])
                ids.setdefault(key, step["id"])
        return ids

    def latest_review(self, entity_type, entity_id):
        """(status, view or None). 204 = never reviewed."""
        status, data = self.call("GET", "/api/ai/reviews?" + urllib.parse.urlencode(
            {"entityType": entity_type, "entityId": entity_id}))
        if status == 204:
            return status, None
        if status != 200:
            return status, {"error": data}
        return status, json.loads(data)

    def request_review(self, entity_type, entity_id):
        status, data = self.call("POST", "/api/ai/reviews?" + urllib.parse.urlencode(
            {"entityType": entity_type, "entityId": entity_id}))
        return status, data


def _q(name):
    return urllib.parse.quote(name, safe="")


def review_once(client, entity_type, entity_id, timeout, poll):
    """Run one review and wait for it. Returns (outcome, view, error)."""
    _, before = client.latest_review(entity_type, entity_id)
    previous = before.get("runId") if before else None
    status, data = client.request_review(entity_type, entity_id)
    if status == 400:
        return "skipped", None, data
    if status != 202:
        return "failed", None, "POST HTTP %d %s" % (status, data[:300])
    deadline = time.time() + timeout
    while time.time() < deadline:
        time.sleep(poll)
        _, view = client.latest_review(entity_type, entity_id)
        if view and view.get("runId") and view.get("runId") != previous \
                and view.get("status") in TERMINAL:
            return classify(view), view, view.get("errorSummary")
    return "timeout", None, "no finished run after %ds" % timeout


def rescore(raw_path, expectations, out):
    """Re-score the findings saved in a raw.json with the current expectations."""
    with open(raw_path, encoding="utf-8") as f:
        raw = json.load(f)
    current = {(e["type"], e["name"]): e for e in expectations["entities"]}
    for result in raw["results"]:
        key = (result["entity"]["type"], result["entity"]["name"])
        if key in current:
            result["entity"] = current[key]
        for run in result.get("runs", []):
            if run.get("outcome") == "ok":
                run["score"] = score_run(result["entity"], run.get("findings") or [],
                                         run.get("summary"))
    meta = dict(raw["meta"])
    meta["label"] = "%s (re-scored %s from %s)" % (
        meta.get("label") or "", _dt.datetime.now().isoformat(timespec="seconds"), raw_path)
    out_dir = os.path.join(out, "rescore-" + _dt.datetime.now().strftime("%Y%m%d-%H%M%S"))
    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, "report.md"), "w", encoding="utf-8") as f:
        f.write(render_report(meta, raw["results"]))
    with open(os.path.join(out_dir, "raw.json"), "w", encoding="utf-8") as f:
        json.dump({"meta": meta, "results": raw["results"]}, f, indent=2)
    print("report: %s" % os.path.join(out_dir, "report.md"))
    return 0


# ---- main ------------------------------------------------------------------------------------

def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--user", default=os.environ.get("REQUEL_USER", "admin"))
    parser.add_argument("--password", default=os.environ.get("REQUEL_PASSWORD", "admin"))
    parser.add_argument("--runs", type=int, default=3, help="reviews per entity (default 3)")
    parser.add_argument("--timeout", type=int, default=300, help="seconds to wait for a run")
    parser.add_argument("--poll", type=float, default=2.0, help="seconds between reads")
    parser.add_argument("--type", action="append", choices=TYPES,
                        help="only these types (repeatable)")
    parser.add_argument("--project", help="score an already-imported fixture project by name "
                                          "instead of importing a new one")
    parser.add_argument("--label", default="", help="free text for the report, e.g. cli/claude")
    parser.add_argument("--out", default="target/ai-eval", help="report directory")
    parser.add_argument("--expectations", default=os.path.join(HERE, "expectations.json"))
    parser.add_argument("--delete-after", action="store_true",
                        help="delete the imported project when done")
    parser.add_argument("--rescore", metavar="RAW_JSON",
                        help="no server: re-score a previous run's raw.json against the current "
                             "expectations (to tune patterns) and write a new report")
    args = parser.parse_args(argv)

    expectations = load_expectations(args.expectations)
    if args.rescore:
        return rescore(args.rescore, expectations, args.out)
    entities = [e for e in expectations["entities"] if not args.type or e["type"] in args.type]
    stamp = _dt.datetime.now().strftime("%Y%m%d-%H%M%S")

    client = RequelClient(args.base_url, args.user, args.password)
    client.login()
    project = args.project or "AI Eval " + stamp
    if not args.project:
        fixture = os.path.join(os.path.dirname(os.path.abspath(args.expectations)),
                               expectations["fixture"])
        client.import_project(fixture, project)
        print("imported the fixture as \"%s\"" % project)
    ids = client.entity_ids(project)

    results, skipped_types = [], {}
    for index, entity in enumerate(entities, 1):
        key = (entity["type"], entity["name"])
        result = {"entity": entity, "runs": []}
        results.append(result)
        if key not in ids:
            result["skipped"] = "not found in project \"%s\"" % project
            continue
        if entity["type"] in skipped_types:
            result["skipped"] = skipped_types[entity["type"]]
            continue
        for n in range(1, args.runs + 1):
            print("[%d/%d] %s \"%s\" run %d ..." % (index, len(entities), entity["type"],
                                                   entity["name"], n), end=" ", flush=True)
            outcome, view, error = review_once(client, entity["type"], ids[key], args.timeout,
                                               args.poll)
            if outcome == "skipped":
                reason = "%s is not reviewable: %s" % (entity["type"], _error_message(error))
                skipped_types[entity["type"]] = reason
                result["skipped"] = reason
                print("skipped")
                break
            findings = (view or {}).get("findings") or []
            run = {"outcome": outcome, "runId": (view or {}).get("runId"), "error": error,
                   "summary": (view or {}).get("summary"), "findings": findings,
                   "latencyMs": (view or {}).get("latencyMs")}
            if outcome == "ok":
                run["score"] = score_run(entity, findings, run["summary"])
                print("%d findings, %d hits" % (len(findings), len(run["score"]["hits"])))
            else:
                print(outcome)
            result["runs"].append(run)

    meta = {"date": _dt.datetime.now().isoformat(timespec="seconds"), "label": args.label,
            "baseUrl": args.base_url, "project": project, "runsPerEntity": args.runs}
    out_dir = os.path.join(args.out, stamp)
    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, "report.md"), "w", encoding="utf-8") as f:
        f.write(render_report(meta, results))
    with open(os.path.join(out_dir, "raw.json"), "w", encoding="utf-8") as f:
        json.dump({"meta": meta, "results": results}, f, indent=2)
    print("report: %s" % os.path.join(out_dir, "report.md"))

    if args.delete_after and not args.project:
        status, data = client.delete_project(project)
        print("deleted \"%s\"" % project if status == 200 else
              "could not delete \"%s\": HTTP %d %s" % (project, status, data[:200]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
