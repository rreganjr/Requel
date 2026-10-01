#!/usr/bin/env python3
"""Unit tests for score.py's matching and scoring (issue #355). Run by hand:

    python3 scripts/ai-eval/test_score.py
"""

import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import score  # noqa: E402


def entity(expect=None, silent=False, trap=False, confirm=None, type_name="Goal"):
    e = {"type": type_name, "name": "x", "expect": expect or [], "silent": silent,
         "trap": trap, "confirmPatterns": confirm or []}
    for item in e["expect"]:
        item.setdefault("types", [])
    return e


def finding(text, finding_type="AMBIGUOUS"):
    return {"findingType": finding_type, "kind": "ISSUE", "text": text}


class ScoreRunTest(unittest.TestCase):

    def test_a_pattern_match_is_a_hit_and_the_rest_are_misses(self):
        e = entity([{"id": "a", "patterns": ["measur"]}, {"id": "b", "patterns": ["owner"]}])
        result = score.score_run(e, [finding("Add a measurable threshold.")], None)
        self.assertEqual(["a"], result["hits"])
        self.assertEqual(["b"], result["misses"])
        self.assertEqual([], result["spurious"])

    def test_matching_ignores_case(self):
        e = entity([{"id": "a", "patterns": ["dana"]}])
        self.assertEqual(["a"], score.score_run(e, [finding("DANA is a person")], None)["hits"])

    def test_types_narrow_the_match(self):
        e = entity([{"id": "a", "patterns": ["measur"], "types": ["UNTESTABLE"]}])
        result = score.score_run(e, [finding("not measurable", "AMBIGUOUS")], None)
        self.assertEqual([], result["hits"])
        self.assertEqual(1, len(result["spurious"]))
        result = score.score_run(e, [finding("not measurable", "UNTESTABLE")], None)
        self.assertEqual(["a"], result["hits"])

    def test_each_finding_matches_one_item_and_each_item_counts_once(self):
        e = entity([{"id": "a", "patterns": ["measur"]}, {"id": "b", "patterns": ["measur"]}])
        result = score.score_run(e, [finding("measurable"), finding("measure it"),
                                     finding("measure again")], None)
        self.assertEqual(["a", "b"], result["hits"])
        self.assertEqual(1, len(result["spurious"]))

    def test_every_finding_on_a_silent_entity_is_spurious(self):
        e = entity(silent=True)
        result = score.score_run(e, [finding("anything")], None)
        self.assertEqual(1, len(result["spurious"]))
        self.assertEqual([], result["misses"])

    def test_the_trap_is_confirmed_by_a_finding_or_the_summary(self):
        e = entity([{"id": "t", "patterns": ["source"]}], trap=True,
                   confirm=[r"\b(are|is) (internally )?(consistent|correct)\b", r"\badds? up\b"])
        hit = score.score_run(e, [finding("Cite the source of the 32.")], "Looks fine.")
        self.assertEqual(["t"], hit["hits"])
        self.assertFalse(hit["confirmed"])
        by_summary = score.score_run(e, [], "The figures are consistent: 24 + 8 adds up to 32.")
        self.assertTrue(by_summary["confirmed"])
        self.assertEqual(["t"], by_summary["misses"])
        by_note = score.score_run(e, [finding("The counts are internally consistent.")], None)
        self.assertTrue(by_note["confirmed"])

    def test_confirmation_is_only_checked_on_the_trap(self):
        e = entity([], confirm=[r"\badds? up\b"])
        self.assertFalse(score.score_run(e, [], "it adds up")["confirmed"])


class ClassifyTest(unittest.TestCase):

    def test_outcomes(self):
        self.assertEqual("ok", score.classify({"status": "SUCCEEDED"}))
        self.assertEqual("schema", score.classify(
            {"status": "FAILED", "errorSummary": "AI requirements review failed: model reply is "
                                                 "not valid JSON: x"}))
        self.assertEqual("schema", score.classify(
            {"status": "FAILED", "errorSummary": "structured output missing summary"}))
        self.assertEqual("failed", score.classify(
            {"status": "FAILED", "errorSummary": "claude exited with status 1"}))
        self.assertEqual("timeout", score.classify(None))
        self.assertEqual("skipped", score.classify({"status": "SKIPPED"}))


class AggregateTest(unittest.TestCase):

    def test_rates_count_only_successful_runs(self):
        flawed = entity([{"id": "a", "patterns": ["x"]}, {"id": "b", "patterns": ["y"]}])
        silent = entity(silent=True)
        trap = entity([{"id": "t", "patterns": ["source"]}], trap=True, confirm=["adds up"],
                      type_name="Story")
        results = [
            {"entity": flawed, "runs": [
                {"outcome": "ok", "findings": [finding("x")],
                 "score": score.score_run(flawed, [finding("x")], None)},
                {"outcome": "schema", "findings": []}]},
            {"entity": silent, "runs": [
                {"outcome": "ok", "findings": [],
                 "score": score.score_run(silent, [], None)},
                {"outcome": "ok", "findings": [finding("z")],
                 "score": score.score_run(silent, [finding("z")], None)}]},
            {"entity": trap, "runs": [
                {"outcome": "ok", "findings": [],
                 "score": score.score_run(trap, [], "it adds up")}]},
            {"entity": entity(type_name="GlossaryTerm"), "runs": [], "skipped": "not reviewable"},
        ]
        rows = score.aggregate(results)
        goal = rows["Goal"]
        self.assertEqual(3, goal["ok"])
        self.assertEqual(4, goal["runs"])
        self.assertEqual(1, goal["schema"])
        self.assertEqual(2, goal["expected"])  # 2 items x 1 ok run of the flawed goal
        self.assertEqual(1, goal["hits"])
        self.assertEqual(1, goal["spurious"])
        self.assertEqual((1, 2), (goal["silentKept"], goal["silentRuns"]))
        self.assertEqual(1, rows["Story"]["confirmed"])
        self.assertEqual(1, rows["GlossaryTerm"]["skipped"])

    def test_the_report_renders(self):
        e = entity([{"id": "a", "patterns": ["x"]}])
        report = score.render_report({"date": "d", "label": "l", "baseUrl": "u", "project": "p",
                                      "runsPerEntity": 1},
                                     [{"entity": e, "runs": [
                                         {"outcome": "ok", "findings": [finding("q")],
                                          "score": score.score_run(e, [finding("q")], None)}]}])
        self.assertIn("| Goal | 1 | 1/1 | 0% (0/1) | 1.0 |", report)
        self.assertIn("- **definitions:** not recorded", report)
        self.assertIn("spurious `AMBIGUOUS`: q", report)

    def test_unverified_evidence_is_totalled_and_reported(self):
        e = entity([{"id": "a", "patterns": ["x"]}])
        runs = [{"outcome": "ok", "findings": [finding("x")], "evidenceUnverified": 2,
                 "definition": "ai-requirements-review@1",
                 "score": score.score_run(e, [finding("x")], None)},
                {"outcome": "ok", "findings": [finding("x")],
                 "score": score.score_run(e, [finding("x")], None)}]
        results = [{"entity": e, "runs": runs}]
        self.assertEqual(2, score.aggregate(results)["Goal"]["unverified"])
        report = score.render_report({"runsPerEntity": 2}, results)
        self.assertIn("- **definitions:** ai-requirements-review@1", report)
        self.assertIn("| 0 | 0 | 0 | 0 | 2 |", report)
        self.assertIn("; 2 with unverified evidence", report)


class RescoreTest(unittest.TestCase):

    def test_rescore_applies_the_current_patterns_to_saved_findings(self):
        import json
        import tempfile
        e = entity([{"id": "a", "patterns": ["old"]}])
        raw = {"meta": {"label": "x", "runsPerEntity": 1},
               "results": [{"entity": e, "runs": [
                   {"outcome": "ok", "findings": [finding("a new finding")], "summary": None,
                    "score": score.score_run(e, [finding("a new finding")], None)}]}]}
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "raw.json")
            with open(path, "w") as f:
                json.dump(raw, f)
            current = {"entities": [entity([{"id": "a", "patterns": ["new"]}])]}
            score.rescore(path, current, tmp)
            out = [d for d in os.listdir(tmp) if d.startswith("rescore-")][0]
            with open(os.path.join(tmp, out, "raw.json")) as f:
                rescored = json.load(f)
        self.assertEqual(["a"], rescored["results"][0]["runs"][0]["score"]["hits"])


class ExpectationsFileTest(unittest.TestCase):

    def test_the_checked_in_patterns_compile(self):
        doc = score.load_expectations(os.path.join(score.HERE, "expectations.json"))
        import re
        for e in doc["entities"]:
            for item in e["expect"]:
                for p in item["patterns"]:
                    re.compile(p)
            for p in e["confirmPatterns"]:
                re.compile(p)


if __name__ == "__main__":
    unittest.main()
