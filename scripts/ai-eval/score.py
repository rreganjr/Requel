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
        entity.setdefault("alsoValid", [])
        entity.setdefault("policyExpect", [])
        entity.setdefault("policyAlsoValid", [])
        for item in (entity["expect"] + entity["alsoValid"] + entity["policyExpect"]
                     + entity["policyAlsoValid"]):
            item.setdefault("types", [])
            item.setdefault("acceptTypes", [])
    corpus = doc.setdefault("corpus", {})
    for key in ("expect", "alsoValid", "silent"):
        corpus.setdefault(key, [])
        for item in corpus[key]:
            item.setdefault("acceptTypes", [])
            item.setdefault("finder", False)
            item["participants"] = [tuple(p) for p in item["participants"]]
    return doc


def _matches_any(patterns, text):
    return any(re.search(p, text or "", re.I) for p in patterns)


def is_extraction(finding):
    """#263: an extraction suggestion (EXTRACT_*) is advisory, not a quality finding."""
    return (finding.get("findingType") or "").startswith("EXTRACT_")


def score_run(entity, findings, summary):
    """Score one run's findings on one entity.

    Returns {"hits": [item ids], "misses": [item ids], "spurious": [findings],
    "suggestions": [findings], "confirmed": bool}. A finding matches an expected item when its
    text matches one of the item's patterns, or its findingType is one of the item's acceptTypes
    (#263), and, if the item lists types, its findingType is one of them. Findings of an accepted
    type claim their items first, so a finding that only matches on wording cannot take an item
    from the finding that names the flaw (#263). Each finding matches at most one item (first in
    file order) and each item counts once. A finding left over that matches an "alsoValid" item
    the same way is a valid extra: a real flaw the expectations don't target, neither a hit nor
    spurious (#263). An extraction finding that matches nothing is a suggestion, not spurious
    (#263). On a silent entity every other finding is spurious.
    """
    remaining = list(entity["expect"])
    claimed = {}

    def eligible(item, finding):
        return not item.get("types") or finding.get("findingType") in item["types"]

    for index, finding in enumerate(findings):
        for item in remaining:
            if eligible(item, finding) and finding.get("findingType") in item.get("acceptTypes", []):
                claimed[index] = item
                remaining.remove(item)
                break
    for index, finding in enumerate(findings):
        if index in claimed:
            continue
        for item in remaining:
            if eligible(item, finding) and _matches_any(item["patterns"], finding.get("text")):
                claimed[index] = item
                remaining.remove(item)
                break
    valid = {}
    extras = list(entity.get("alsoValid", []))
    for index, finding in enumerate(findings):
        if index in claimed:
            continue
        for item in extras:
            if eligible(item, finding) and (
                    finding.get("findingType") in item.get("acceptTypes", [])
                    or _matches_any(item.get("patterns", []), finding.get("text"))):
                valid[index] = item
                extras.remove(item)
                break
    hits, spurious, suggestions, also_valid = [], [], [], []
    for index, finding in enumerate(findings):
        if index in claimed:
            hits.append(claimed[index]["id"])
        elif index in valid:
            also_valid.append(dict(finding, alsoValid=valid[index]["id"]))
        elif is_extraction(finding):
            suggestions.append(finding)
        else:
            spurious.append(finding)
    confirmed = False
    if entity["trap"]:
        texts = [f.get("text") or "" for f in findings] + [summary or ""]
        # #263: "the figures add up, but nothing says where they come from" questions the
        # premise; only a text that vouches for the figures without questioning them confirms.
        questioning = [p for item in entity["expect"] for p in item["patterns"]]
        confirmed = any(_matches_any(entity["confirmPatterns"], t)
                        and not _matches_any(questioning, t) for t in texts)
    order = [i["id"] for i in entity["expect"]]
    hits.sort(key=order.index)
    return {"hits": hits, "misses": [i["id"] for i in remaining], "spurious": spurious,
            "suggestions": suggestions, "alsoValid": also_valid, "confirmed": confirmed}


def policy_entity(entity):
    """#265: the entity as the policy pass is scored: its policyExpect items (silent when it has
    none) and its policyAlsoValid items (real flaws the policy may raise; neither hit nor
    spurious)."""
    return {"type": entity["type"], "name": entity["name"],
            "expect": entity.get("policyExpect", []),
            "silent": not entity.get("policyExpect"), "trap": False, "confirmPatterns": [],
            "alsoValid": entity.get("policyAlsoValid", [])}


def score_policy(entity, findings):
    """#265: score one policy run against the entity's policyExpect."""
    return score_run(policy_entity(entity), findings, None)


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
            "timeout": 0, "confirmed": 0, "unverified": 0, "vocabularyMisses": 0,
            "suggestions": 0, "alsoValid": 0})
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
            # #263: unmatched extraction findings, advisory.
            suggestions = score.get("suggestions", [])
            row["suggestions"] += len(suggestions)
            row["alsoValid"] += len(score.get("alsoValid", []))
            # #260: findings whose cited evidence is not in the entity (absent before #260).
            row["unverified"] += run.get("evidenceUnverified") or 0
            # #263: findings whose type is not in the definition's vocabulary.
            row["vocabularyMisses"] += run.get("vocabularyMisses") or 0
            if entity["silent"]:
                row["silentRuns"] += 1
                if not score["spurious"] and not score["hits"]:
                    row["silentKept"] += 1
            if score["confirmed"]:
                row["confirmed"] += 1
    return rows


def aggregate_policies(results):
    """#265: per-type rows for the policy runs saved beside the reviews."""
    rows = {}
    for result in results:
        if result.get("skipped"):
            continue
        entity = result["entity"]
        for run in result["runs"]:
            policy = run.get("policy")
            if policy is None:
                continue
            row = rows.setdefault(entity["type"], {"runs": 0, "ok": 0, "expected": 0,
                                                   "hits": 0, "spurious": 0, "silentRuns": 0,
                                                   "silentKept": 0, "failed": 0, "calls": 0})
            row["runs"] += 1
            if policy["outcome"] != "ok":
                row["failed"] += 1
                continue
            row["ok"] += 1
            row["calls"] += policy.get("calls", 1)
            score = policy["score"]
            row["expected"] += len(entity.get("policyExpect", []))
            row["hits"] += len(score["hits"])
            row["spurious"] += len(score["spurious"])
            if not entity.get("policyExpect"):
                row["silentRuns"] += 1
                if not score["spurious"]:
                    row["silentKept"] += 1
    return rows


def _ratio(numerator, denominator):
    return "-" if denominator == 0 else "%.0f%% (%d/%d)" % (
        100.0 * numerator / denominator, numerator, denominator)


def render_report(meta, results):
    rows = aggregate(results)
    out = ["# AI review evaluation", ""]
    for key in ("date", "label", "baseUrl", "project", "runsPerEntity"):
        out.append("- **%s:** %s" % (key, meta.get(key)))
    definitions = sorted({run["definition"] for result in results
                          for run in result.get("runs", []) if run.get("definition")})
    out.append("- **definitions:** %s" % (", ".join(definitions) or "not recorded"))
    out += ["", "| Type | Entities | Runs ok/total | Hit rate | Spurious per run | Silent kept "
            "silent | Schema failures | Other failures | Timeouts | Confirmed (trap) | "
            "Unverified evidence | Off-vocabulary types | Suggestions per run | Also valid per run |",
            "|---|---|---|---|---|---|---|---|---|---|---|---|---|---|"]
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
            "successful runs on silent entities that raised nothing. Unverified evidence = "
            "findings citing text that is not in the entity (#260; they are still written). "
            "Off-vocabulary types = findings whose type is not in the definition's vocabulary "
            "(#263). Suggestions = extraction findings (EXTRACT_*) that matched no expected "
            "finding: advisory, so neither spurious nor breaking a silent case (#263). Also valid = "
            "findings matching an entity's alsoValid list: real flaws the fixture doesn't target "
            "(#263).",
            ""]

    policy_rows = aggregate_policies(results)
    if policy_rows:
        out += ["## Policies (#265)", "",
                "| Type | Runs ok/total | Hit rate | Spurious per run | Silent kept silent |"
                " Failures |", "|---|---|---|---|---|---|"]
        total = None
        for type_name in TYPES + sorted(set(policy_rows) - set(TYPES)):
            row = policy_rows.get(type_name)
            if row is None:
                continue
            if total is None:
                total = {k: 0 for k in row}
            for k in row:
                total[k] += row[k]
            out.append(_policy_row(type_name, row))
        out.append(_policy_row("**All**", total))
        out += ["", "One policy run per review: every applicable policy in one provider call. "
                "Hit rate = policyExpect items matched; an entity without policyExpect is "
                "policy-silent.", ""]

    out += ["## Details", ""]
    for result in results:
        entity = result["entity"]
        title = "### %s: %s" % (entity["type"], entity["name"])
        if result.get("skipped"):
            out += [title, "", "Skipped: %s" % result["skipped"], ""]
            continue
        out += [title, ""]
        if entity["silent"]:
            out.append("Silent case (any quality finding is spurious).")
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
            if run.get("evidenceUnverified"):
                line += "; %d with unverified evidence" % run["evidenceUnverified"]
            if run.get("vocabularyMisses"):
                line += "; %d off-vocabulary" % run["vocabularyMisses"]
            out.append(line)
            for finding in score["spurious"]:
                out.append("  - spurious `%s`: %s" % (finding.get("findingType"),
                                                      _one_line(finding.get("text"))))
            policy = run.get("policy")
            if policy is not None:
                if policy["outcome"] != "ok":
                    out.append("  - policy run: **%s** %s" % (
                        policy["outcome"], (policy.get("error") or "").strip()[:300]))
                else:
                    pscore = policy["score"]
                    out.append("  - policy run: hits %s; misses %s" % (
                        ", ".join(pscore["hits"]) or "none",
                        ", ".join(pscore["misses"]) or "none"))
                    for finding in pscore["spurious"]:
                        out.append("    - policy spurious `%s`: %s" % (
                            finding.get("findingType"), _one_line(finding.get("text"))))
            for finding in score.get("alsoValid", []):
                out.append("  - also valid (%s) `%s`: %s" % (
                    finding.get("alsoValid"), finding.get("findingType"),
                    _one_line(finding.get("text"))))
            for finding in score.get("suggestions", []):
                out.append("  - suggestion `%s`: %s" % (finding.get("findingType"),
                                                        _one_line(finding.get("text"))))
        out.append("")
    return "\n".join(out) + "\n"


def _row(name, row):
    entities = "%d" % (row["entities"] - row["skipped"])
    if row["skipped"]:
        entities += " (%d skipped)" % row["skipped"]
    return "| %s | %s | %d/%d | %s | %s | %s | %d | %d | %d | %d | %d | %d | %s | %s |" % (
        name, entities, row["ok"], row["runs"],
        _ratio(row["hits"], row["expected"]),
        "-" if row["ok"] == 0 else "%.1f" % (row["spurious"] / float(row["ok"])),
        _ratio(row["silentKept"], row["silentRuns"]),
        row["schema"], row["failed"], row["timeout"], row["confirmed"], row["unverified"],
        row["vocabularyMisses"],
        "-" if row["ok"] == 0 else "%.1f" % (row["suggestions"] / float(row["ok"])),
        "-" if row["ok"] == 0 else "%.1f" % (row["alsoValid"] / float(row["ok"])))


def _policy_row(name, row):
    return "| %s | %d/%d | %s | %s | %s | %d |" % (
        name, row["ok"], row["runs"], _ratio(row["hits"], row["expected"]),
        "-" if row["ok"] == 0 else "%.1f" % (row["spurious"] / float(row["ok"])),
        _ratio(row["silentKept"], row["silentRuns"]), row["failed"])


def _definition(view):
    """#260: "key@version" the run used, or None before #260."""
    keys = (view or {}).get("definitionKeys")
    if not keys:
        return None
    return "%s@%s" % (keys, (view or {}).get("definitionVersions") or "?")


def _error_message(body):
    try:
        return json.loads(body).get("message") or body
    except (ValueError, AttributeError):
        return _one_line(body)


def _one_line(text):
    return re.sub(r"\s+", " ", text or "").strip()[:300]


# ---- corpus analyses (#266; pure, covered by test_score.py) ---------------------------------

def relationships(findings, names):
    """The run's findings grouped into relationships: one per shared annotation, with the
    (type, name) of every participant. `names` maps (type, id) to a name."""
    groups = {}
    for f in findings or []:
        key = f.get("annotationId") or f.get("findingId")
        group = groups.setdefault(key, {"type": f.get("findingType"), "text": f.get("text"),
                                        "annotationId": f.get("annotationId"),
                                        "participants": set()})
        participant = (f.get("targetType"), names.get((f.get("targetType"), f.get("targetId")),
                                                      "#%s" % f.get("targetId")))
        group["participants"].add(participant)
    return list(groups.values())


def _covers(rel, item):
    return set(item["participants"]) <= rel["participants"]


def score_corpus(corpus, rels):
    """Expectations hit, also-valid, silent pairs raised and spurious relationships in one run.
    A relationship hits an expectation when it names every expected participant with an accepted
    type (any type when none are listed); it is exact when it names no others."""
    hits, exact, used = [], [], set()
    for item in corpus["expect"]:
        for i, rel in enumerate(rels):
            if _covers(rel, item) and (not item["acceptTypes"]
                                       or rel["type"] in item["acceptTypes"]):
                hits.append(item["id"])
                used.add(i)
                if rel["participants"] == set(item["participants"]):
                    exact.append(item["id"])
                break
    also = []
    for item in corpus["alsoValid"]:
        for i, rel in enumerate(rels):
            if i not in used and _covers(rel, item):
                also.append(item["id"])
                used.add(i)
                break
    silent = [item["id"] for item in corpus["silent"] if any(_covers(r, item) for r in rels)]
    spurious = [r for i, r in enumerate(rels) if i not in used]
    return {"hits": hits, "exact": exact, "alsoValid": also, "silentRaised": silent,
            "spurious": [{"type": r["type"], "participants": sorted(r["participants"]),
                          "text": r["text"]} for r in spurious]}


def score_finder(corpus, rels):
    """The finder's pairs against the expectations marked `finder`, and silent pairs."""
    wanted = [item for item in corpus["expect"] if item["finder"]]
    found = [item["id"] for item in wanted if any(_covers(r, item) for r in rels)]
    silent = [item["id"] for item in corpus["silent"] if any(_covers(r, item) for r in rels)]
    return {"found": found, "wanted": [item["id"] for item in wanted], "silentRaised": silent,
            "pairs": len(rels)}


def duplicates(runs):
    """Relationships reported by two consecutive runs under different issues: a re-run that
    should have reused the issue made a new one."""
    count, previous = 0, {}
    for run in runs:
        current = {}
        for rel in run.get("relationships") or []:
            key = (rel["type"], tuple(sorted(tuple(p) for p in rel["participants"])))
            current[key] = rel.get("annotationId")
            if key in previous and previous[key] != current[key]:
                count += 1
        previous = current
    return count


def render_corpus_report(meta, corpus, runs):
    out = ["# Corpus analysis evaluation", ""]
    for key in ("date", "label", "baseUrl", "project", "runs"):
        out.append("- **%s:** %s" % (key, meta.get(key)))
    ok = [r for r in runs if (r.get("analysis") or {}).get("outcome") == "ok"]
    finder_ok = [r for r in runs if (r.get("finder") or {}).get("outcome") == "ok"]
    out += ["", "## Find overlaps (no AI)", "",
            "| Expected pairs found | Silent pairs raised | Pairs per run |", "|---|---|---|"]
    if finder_ok:
        wanted = sum(len(r["finder"]["score"]["wanted"]) for r in finder_ok)
        found = sum(len(r["finder"]["score"]["found"]) for r in finder_ok)
        silent = sum(len(r["finder"]["score"]["silentRaised"]) for r in finder_ok)
        pairs = sum(r["finder"]["score"]["pairs"] for r in finder_ok)
        out.append("| %s | %d | %.1f |" % (_ratio(found, wanted), silent, pairs / len(finder_ok)))
    out += ["", "## Analysis (AI)", "",
            "| Runs ok/total | Hit rate | Exact participants | Spurious per run | Also valid per run "
            "| Silent pairs raised | Duplicate issues | Unverified evidence | Partial runs |",
            "|---|---|---|---|---|---|---|---|---|"]
    expected = len(corpus["expect"])
    hits = sum(len(r["analysis"]["score"]["hits"]) for r in ok)
    exact = sum(len(r["analysis"]["score"]["exact"]) for r in ok)
    spurious = sum(len(r["analysis"]["score"]["spurious"]) for r in ok)
    also = sum(len(r["analysis"]["score"]["alsoValid"]) for r in ok)
    silent = sum(len(r["analysis"]["score"]["silentRaised"]) for r in ok)
    unverified = sum(r["analysis"].get("evidenceUnverified") or 0 for r in ok)
    partial = sum(1 for r in ok if (r["analysis"].get("summary") or "").startswith("Partial"))
    out.append("| %d/%d | %s | %s | %s | %s | %d | %d | %d | %d |" % (
        len(ok), len(runs), _ratio(hits, expected * len(ok)), _ratio(exact, hits),
        "-" if not ok else "%.1f" % (spurious / len(ok)),
        "-" if not ok else "%.1f" % (also / len(ok)), silent,
        duplicates([r["analysis"] for r in ok]), unverified, partial))
    out += ["", "### Per expectation", "", "| Expectation | Hits |", "|---|---|"]
    for item in corpus["expect"]:
        n = sum(1 for r in ok if item["id"] in r["analysis"]["score"]["hits"])
        out.append("| %s | %s |" % (item["id"], _ratio(n, len(ok))))
    spurious_rows = [s for r in ok for s in r["analysis"]["score"]["spurious"]]
    if spurious_rows:
        out += ["", "### Spurious", ""]
        for s in spurious_rows:
            out.append("- %s %s: %s" % (s["type"], ", ".join("%s \"%s\"" % tuple(p)
                                                             for p in s["participants"]),
                                        _one_line(s["text"])))
    return "\n".join(out) + "\n"


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

    def latest_review(self, entity_type, entity_id, task_type=None):
        """(status, view or None). 204 = never reviewed. task_type POLICY_REVIEW reads the
        policy pass (#265)."""
        params = {"entityType": entity_type, "entityId": entity_id}
        if task_type:
            params["taskType"] = task_type
        status, data = self.call("GET", "/api/ai/reviews?" + urllib.parse.urlencode(params))
        if status == 204:
            return status, None
        if status != 200:
            return status, {"error": data}
        return status, json.loads(data)

    def project_id(self, name):
        return self.get_json("/api/projects/" + _q(name))["id"]

    def latest_corpus(self, project_id, mode):
        status, data = self.call("GET", "/api/ai/corpus?" + urllib.parse.urlencode(
            {"projectId": project_id, "mode": mode}))
        if status == 204:
            return status, None
        if status != 200:
            return status, {"error": data}
        return status, json.loads(data)

    def request_corpus(self, project_id, mode):
        return self.call("POST", "/api/ai/corpus?" + urllib.parse.urlencode(
            {"projectId": project_id, "mode": mode}))

    def request_review(self, entity_type, entity_id):
        status, data = self.call("POST", "/api/ai/reviews?" + urllib.parse.urlencode(
            {"entityType": entity_type, "entityId": entity_id}))
        return status, data


def _q(name):
    return urllib.parse.quote(name, safe="")


POLICY_TASK = "POLICY_REVIEW"


def _run_id(client, entity_type, entity_id, task_type=None):
    status, view = client.latest_review(entity_type, entity_id, task_type)
    return view.get("runId") if status == 200 and view else None


def _wait(client, entity_type, entity_id, previous, timeout, poll, task_type=None):
    deadline = time.time() + timeout
    while time.time() < deadline:
        time.sleep(poll)
        _, view = client.latest_review(entity_type, entity_id, task_type)
        if view and view.get("runId") and view.get("runId") != previous \
                and view.get("status") in TERMINAL:
            return classify(view), view, view.get("errorSummary")
    return "timeout", None, "no finished run after %ds" % timeout


def review_once(client, entity_type, entity_id, timeout, poll):
    """Run one review and wait for it, and for the policy pass when the request dispatched one
    (#265). Returns (outcome, view, error, policy) where policy is (outcome, view, error) or
    None."""
    previous = _run_id(client, entity_type, entity_id)
    previous_policy = _run_id(client, entity_type, entity_id, POLICY_TASK)
    status, data = client.request_review(entity_type, entity_id)
    if status == 400:
        return "skipped", None, data, None
    if status != 202:
        return "failed", None, "POST HTTP %d %s" % (status, data[:300]), None
    # Both runs are queued before the POST returns, so a policy run that isn't there now
    # was never dispatched: no policy applies.
    dispatched = _run_id(client, entity_type, entity_id, POLICY_TASK) != previous_policy
    outcome, view, error = _wait(client, entity_type, entity_id, previous, timeout, poll)
    policy = None
    if dispatched:
        policy = _wait(client, entity_type, entity_id, previous_policy, timeout, poll,
                       POLICY_TASK)
    return outcome, view, error, policy


def corpus_once(client, project_id, mode, timeout, poll):
    """Run one corpus run over the whole project and wait for it (#266)."""
    _, before = client.latest_corpus(project_id, mode)
    previous = (before or {}).get("runId")
    status, data = client.request_corpus(project_id, mode)
    if status != 202:
        return "failed", None, "POST HTTP %d %s" % (status, data[:300])
    deadline = time.time() + timeout
    while time.time() < deadline:
        time.sleep(poll)
        _, view = client.latest_corpus(project_id, mode)
        if view and view.get("runId") and view.get("runId") != previous \
                and view.get("status") in TERMINAL:
            return classify(view), view, view.get("errorSummary")
    return "timeout", None, "no finished run after %ds" % timeout


def run_corpus(client, args, expectations, project, ids, stamp):
    """#266: Find overlaps then the AI analysis, --runs times, over the fixture project."""
    corpus = expectations["corpus"]
    names = {(t, i): n for (t, n), i in ids.items()}
    project_id = client.project_id(project)
    runs = []
    for n in range(1, args.runs + 1):
        run = {}
        for mode, key in (("CANDIDATES", "finder"), ("ANALYSIS", "analysis")):
            print("corpus %s run %d ..." % (mode, n), end=" ", flush=True)
            outcome, view, error = corpus_once(client, project_id, mode, args.timeout, args.poll)
            rels = relationships((view or {}).get("findings"), names)
            part = {"outcome": outcome, "runId": (view or {}).get("runId"), "error": error,
                    "summary": (view or {}).get("summary"),
                    "evidenceUnverified": (view or {}).get("evidenceUnverified") or 0,
                    "relationships": [dict(r, participants=sorted(r["participants"]))
                                      for r in rels]}
            if outcome == "ok":
                part["score"] = (score_finder if key == "finder" else score_corpus)(corpus, rels)
                print("%d relationships" % len(rels))
            else:
                print("%s %s" % (outcome, _one_line(error or "")))
            run[key] = part
        runs.append(run)
    meta = {"date": _dt.datetime.now().isoformat(timespec="seconds"), "label": args.label,
            "baseUrl": args.base_url, "project": project, "runs": args.runs}
    out_dir = os.path.join(args.out, "corpus-" + stamp)
    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, "report.md"), "w", encoding="utf-8") as f:
        f.write(render_corpus_report(meta, corpus, runs))
    with open(os.path.join(out_dir, "raw.json"), "w", encoding="utf-8") as f:
        json.dump({"meta": meta, "corpus": runs}, f, indent=2)
    print("report: %s" % os.path.join(out_dir, "report.md"))


def rescore_corpus(raw, expectations, out):
    corpus = expectations["corpus"]
    for run in raw["corpus"]:
        for key, scorer in (("finder", score_finder), ("analysis", score_corpus)):
            part = run.get(key) or {}
            if part.get("outcome") == "ok":
                rels = [dict(r, participants={tuple(p) for p in r["participants"]})
                        for r in part.get("relationships") or []]
                part["score"] = scorer(corpus, rels)
    out_dir = os.path.join(out, "rescore-corpus-" + _dt.datetime.now().strftime("%Y%m%d-%H%M%S"))
    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, "report.md"), "w", encoding="utf-8") as f:
        f.write(render_corpus_report(raw["meta"], corpus, raw["corpus"]))
    print("report: %s" % os.path.join(out_dir, "report.md"))
    return 0


def rescore(raw_path, expectations, out):
    """Re-score the findings saved in a raw.json with the current expectations."""
    with open(raw_path, encoding="utf-8") as f:
        raw = json.load(f)
    if "corpus" in raw:
        return rescore_corpus(raw, expectations, out)
    current = {(e["type"], e["name"]): e for e in expectations["entities"]}
    for result in raw["results"]:
        key = (result["entity"]["type"], result["entity"]["name"])
        if key in current:
            result["entity"] = current[key]
        for run in result.get("runs", []):
            if run.get("outcome") == "ok":
                run["score"] = score_run(result["entity"], run.get("findings") or [],
                                         run.get("summary"))
            policy = run.get("policy")
            if policy and policy.get("outcome") == "ok":
                policy["score"] = score_policy(result["entity"], policy.get("findings") or [])
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
    parser.add_argument("--reload-definitions", action="store_true",
                        help="first POST /api/dev/ai/definitions/reload, so edited files in "
                             "requel.ai.definitions.dir take effect (#263, dev only)")
    parser.add_argument("--corpus", action="store_true",
                        help="score the corpus analyses instead of the reviews: Find overlaps and "
                             "the AI analysis over the whole fixture, --runs times (#266)")
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
    if args.reload_definitions:
        status, data = client.call("POST", "/api/dev/ai/definitions/reload", b"",
                                   {"Content-Type": "application/json"})
        if status != 200:
            raise SystemExit("reload failed: HTTP %d %s (is requel.ai.definitions.dir set?)"
                             % (status, data[:500]))
        print("reloaded definitions: %s" % json.loads(data).get("reloaded"))
    project = args.project or "AI Eval " + stamp
    if not args.project:
        fixture = os.path.join(os.path.dirname(os.path.abspath(args.expectations)),
                               expectations["fixture"])
        client.import_project(fixture, project)
        print("imported the fixture as \"%s\"" % project)
    ids = client.entity_ids(project)
    if args.corpus:
        run_corpus(client, args, expectations, project, ids, stamp)
        if args.delete_after and not args.project:
            client.delete_project(project)
        return 0

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
            outcome, view, error, policy = review_once(client, entity["type"], ids[key],
                                                       args.timeout, args.poll)
            if outcome == "skipped":
                reason = "%s is not reviewable: %s" % (entity["type"], _error_message(error))
                skipped_types[entity["type"]] = reason
                result["skipped"] = reason
                print("skipped")
                break
            findings = (view or {}).get("findings") or []
            run = {"outcome": outcome, "runId": (view or {}).get("runId"), "error": error,
                   "summary": (view or {}).get("summary"), "findings": findings,
                   "latencyMs": (view or {}).get("latencyMs"),
                   "evidenceUnverified": (view or {}).get("evidenceUnverified") or 0,
                   "vocabularyMisses": (view or {}).get("vocabularyMisses") or 0,
                   "definition": _definition(view)}
            if policy is not None:
                p_outcome, p_view, p_error = policy
                run["policy"] = {"outcome": p_outcome, "runId": (p_view or {}).get("runId"),
                                 "error": p_error, "summary": (p_view or {}).get("summary"),
                                 "findings": (p_view or {}).get("findings") or [],
                                 "vocabularyMisses": (p_view or {}).get("vocabularyMisses") or 0,
                                 "definition": _definition(p_view)}
                if p_outcome == "ok":
                    run["policy"]["score"] = score_policy(entity, run["policy"]["findings"])
            if outcome == "ok":
                run["score"] = score_run(entity, findings, run["summary"])
                line = "%d findings, %d hits" % (len(findings), len(run["score"]["hits"]))
            else:
                line = outcome
            if run.get("policy"):
                p = run["policy"]
                line += "; policy " + ("%d findings, %d hits" % (
                    len(p["findings"]), len(p["score"]["hits"])) if p["outcome"] == "ok"
                    else p["outcome"])
            print(line)
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
