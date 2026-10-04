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
        self.assertIn("| 0 | 0 | 0 | 0 | 2 | 0 |", report)
        self.assertIn("; 2 with unverified evidence", report)


class Issue263Test(unittest.TestCase):

    def test_an_accepted_type_matches_whatever_its_wording(self):
        e = entity([{"id": "a", "patterns": ["circular"], "acceptTypes": ["CIRCULAR_DEFINITION"]}])
        hit = score.score_run(e, [finding("uses the word it defines", "CIRCULAR_DEFINITION")], None)
        miss = score.score_run(e, [finding("uses the word it defines", "VAGUE_DEFINITION")], None)
        self.assertEqual(["a"], hit["hits"])
        self.assertEqual(["a"], miss["misses"])

    def test_vocabulary_misses_are_totalled_and_reported(self):
        e = entity([{"id": "a", "patterns": ["x"]}])
        results = [{"entity": e, "runs": [
            {"outcome": "ok", "findings": [finding("x")], "vocabularyMisses": 2,
             "score": score.score_run(e, [finding("x")], None)}]}]
        self.assertEqual(2, score.aggregate(results)["Goal"]["vocabularyMisses"])
        report = score.render_report({"runsPerEntity": 1}, results)
        self.assertIn("| 0 | 2 |", report)
        self.assertIn("; 2 off-vocabulary", report)

    def test_an_accepted_type_claims_its_item_before_a_wording_match(self):
        e = entity([{"id": "a", "patterns": ["two"], "acceptTypes": ["CONJUNCTIVE_GOAL"]}])
        first = finding("says the same as two sibling goals", "DUPLICATE_AT_DIFFERENT_ABSTRACTION")
        second = finding("welds two goals", "CONJUNCTIVE_GOAL")
        result = score.score_run(e, [first, second], None)
        self.assertEqual(["a"], result["hits"])
        self.assertEqual([first], result["spurious"])

    def test_an_unmatched_extraction_is_a_suggestion_and_keeps_a_silent_case_silent(self):
        e = entity(silent=True)
        suggestion = finding("define 'loan'", "EXTRACT_GLOSSARY_TERM")
        result = score.score_run(e, [suggestion], None)
        self.assertEqual([], result["spurious"])
        self.assertEqual([suggestion], result["suggestions"])
        results = [{"entity": e, "runs": [{"outcome": "ok", "findings": [suggestion],
                                           "score": result}]}]
        row = score.aggregate(results)["Goal"]
        self.assertEqual((1, 1, 1), (row["silentKept"], row["silentRuns"], row["suggestions"]))
        report = score.render_report({"runsPerEntity": 1}, results)
        self.assertIn("| 1.0 |", report)
        self.assertIn("suggestion `EXTRACT_GLOSSARY_TERM`", report)

    def test_an_expected_extraction_is_a_hit(self):
        e = entity([{"id": "a", "patterns": [], "acceptTypes": ["EXTRACT_SCENARIO"]}])
        result = score.score_run(e, [finding("a flow", "EXTRACT_SCENARIO")], None)
        self.assertEqual((["a"], []), (result["hits"], result["suggestions"]))

    def test_an_also_valid_finding_is_neither_a_hit_nor_spurious(self):
        e = entity([{"id": "a", "patterns": ["two"]}])
        e["alsoValid"] = [{"id": "privacy", "patterns": ["privacy"], "acceptTypes": []}]
        extra = finding("conflicts with the privacy goal", "INCONSISTENT")
        result = score.score_run(e, [finding("two goals"), extra], None)
        self.assertEqual(["a"], result["hits"])
        self.assertEqual([], result["spurious"])
        self.assertEqual(["privacy"], [f["alsoValid"] for f in result["alsoValid"]])
        results = [{"entity": e, "runs": [{"outcome": "ok", "findings": [extra],
                                           "score": result}]}]
        self.assertEqual(1, score.aggregate(results)["Goal"]["alsoValid"])
        self.assertIn("also valid (privacy) `INCONSISTENT`",
                      score.render_report({"runsPerEntity": 1}, results))

    def test_a_trap_text_that_also_questions_the_source_does_not_confirm(self):
        e = entity([{"id": "t", "patterns": ["source"]}], trap=True, confirm=[r"\badds? up\b"])
        questioned = score.score_run(
            e, [finding("The figures add up, but no source is given.", "UNSOURCED_CLAIM")], None)
        vouched = score.score_run(e, [], "The figures add up.")
        self.assertFalse(questioned["confirmed"])
        self.assertTrue(vouched["confirmed"])


class Issue265Test(unittest.TestCase):

    def test_a_policy_run_is_scored_against_policy_expect_and_reported(self):
        e = entity([{"id": "a", "patterns": ["x"]}])
        e["policyExpect"] = [{"id": "patron", "patterns": ["patron"],
                              "acceptTypes": ["NON_CANONICAL_TERM"], "types": []}]
        silent = entity([{"id": "b", "patterns": ["y"]}], type_name="Story")
        hit = score.score_policy(e, [finding("uses patron", "NON_CANONICAL_TERM")])
        noisy = score.score_policy(silent, [finding("uses patron", "NON_CANONICAL_TERM")])
        self.assertEqual(["patron"], hit["hits"])
        self.assertEqual(1, len(noisy["spurious"]))
        results = [
            {"entity": e, "runs": [{"outcome": "ok", "findings": [], "score":
                                    score.score_run(e, [], None),
                                    "policy": {"outcome": "ok", "findings": [], "score": hit}}]},
            {"entity": silent, "runs": [{"outcome": "ok", "findings": [], "score":
                                         score.score_run(silent, [], None),
                                         "policy": {"outcome": "ok", "findings": [],
                                                    "score": noisy}}]}]
        rows = score.aggregate_policies(results)
        self.assertEqual((1, 1, 0), (rows["Goal"]["hits"], rows["Goal"]["expected"],
                                     rows["Goal"]["spurious"]))
        self.assertEqual((1, 0), (rows["Story"]["silentRuns"], rows["Story"]["silentKept"]))
        report = score.render_report({"runsPerEntity": 1}, results)
        self.assertIn("## Policies (#265)", report)
        self.assertIn("| Goal | 1/1 | 100% (1/1) | 0.0 |", report)
        self.assertIn("policy run: hits patron", report)
        self.assertIn("policy spurious `NON_CANONICAL_TERM`", report)

    def test_a_policy_also_valid_finding_keeps_a_policy_silent_entity_silent(self):
        e = entity([{"id": "a", "patterns": ["x"]}])
        e["policyAlsoValid"] = [{"id": "member-def", "patterns": [],
                                 "acceptTypes": ["CONFLICTING_USAGE"], "types": []}]
        result = score.score_policy(e, [finding("contradicts Member", "CONFLICTING_USAGE")])
        self.assertEqual([], result["spurious"])
        self.assertEqual(["member-def"], [f["alsoValid"] for f in result["alsoValid"]])

    def test_no_policy_table_without_policy_runs(self):
        e = entity([{"id": "a", "patterns": ["x"]}])
        results = [{"entity": e, "runs": [{"outcome": "ok", "findings": [],
                                           "score": score.score_run(e, [], None)}]}]
        self.assertNotIn("Policies (#265)", score.render_report({"runsPerEntity": 1}, results))

    def test_review_once_waits_for_the_policy_run_only_when_one_was_dispatched(self):
        class Client:
            def __init__(self, dispatch):
                self.dispatch = dispatch
                self.runs = {None: None, "POLICY_REVIEW": None}

            def latest_review(self, entity_type, entity_id, task_type=None):
                view = self.runs[task_type]
                return (200, view) if view else (204, None)

            def request_review(self, entity_type, entity_id):
                self.runs[None] = {"runId": "r1", "status": "SUCCEEDED", "findings": []}
                if self.dispatch:
                    self.runs["POLICY_REVIEW"] = {"runId": "p1", "status": "SUCCEEDED",
                                                  "findings": []}
                return 202, ""

        outcome, view, _, policy = score.review_once(Client(True), "Goal", 1, 5, 0)
        self.assertEqual(("ok", "r1"), (outcome, view["runId"]))
        self.assertEqual(("ok", "p1"), (policy[0], policy[1]["runId"]))
        self.assertIsNone(score.review_once(Client(False), "Goal", 1, 5, 0)[3])

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


def corpus(expect=None, also=None, silent=None):
    doc = {"corpus": {"expect": expect or [], "alsoValid": also or [], "silent": silent or []},
           "entities": []}
    for key in ("expect", "alsoValid", "silent"):
        for item in doc["corpus"][key]:
            item.setdefault("acceptTypes", [])
            item.setdefault("finder", False)
            item["participants"] = [tuple(p) for p in item["participants"]]
    return doc["corpus"]


def row(annotation, target_type, target_id, finding_type="CONTRADICTORY_REQUIREMENTS"):
    return {"findingType": finding_type, "text": "t%d" % annotation, "annotationId": annotation,
            "targetType": target_type, "targetId": target_id}


NAMES = {("Goal", 1): "A", ("Goal", 2): "B", ("Story", 3): "C", ("Actor", 4): "D"}


class CorpusTest(unittest.TestCase):

    def test_rows_of_one_annotation_are_one_relationship(self):
        rels = score.relationships([row(7, "Goal", 1), row(7, "Story", 3), row(8, "Goal", 2),
                                    row(8, "Goal", 1)], NAMES)
        self.assertEqual(2, len(rels))
        self.assertEqual({("Goal", "A"), ("Story", "C")}, rels[0]["participants"])

    def test_a_relationship_hits_when_it_names_every_participant_with_an_accepted_type(self):
        c = corpus([{"id": "x", "participants": [["Goal", "A"], ["Story", "C"]],
                     "acceptTypes": ["CONTRADICTORY_REQUIREMENTS"]},
                    {"id": "y", "participants": [["Goal", "A"], ["Goal", "B"]],
                     "acceptTypes": ["DUPLICATE_AT_DIFFERENT_ABSTRACTION"]}])
        rels = score.relationships([row(7, "Goal", 1), row(7, "Story", 3), row(7, "Actor", 4),
                                    row(8, "Goal", 1), row(8, "Goal", 2)], NAMES)
        result = score.score_corpus(c, rels)
        self.assertEqual(["x"], result["hits"])
        self.assertEqual([], result["exact"], "a third participant makes it inexact")
        self.assertEqual(1, len(result["spurious"]), "the wrong type is no hit")

    def test_also_valid_and_silent_pairs(self):
        c = corpus(also=[{"id": "a", "participants": [["Goal", "A"], ["Goal", "B"]]}],
                   silent=[{"id": "s", "participants": [["Goal", "B"], ["Actor", "D"]]}])
        rels = score.relationships([row(8, "Goal", 1), row(8, "Goal", 2), row(9, "Goal", 2),
                                    row(9, "Actor", 4)], NAMES)
        result = score.score_corpus(c, rels)
        self.assertEqual(["a"], result["alsoValid"])
        self.assertEqual(["s"], result["silentRaised"])
        self.assertEqual(1, len(result["spurious"]))

    def test_the_finder_is_scored_on_the_pairs_marked_for_it(self):
        c = corpus([{"id": "x", "participants": [["Goal", "A"], ["Story", "C"]], "finder": True},
                    {"id": "y", "participants": [["Goal", "A"], ["Goal", "B"]]}])
        rels = score.relationships([row(7, "Goal", 1, "POSSIBLE_CONFLICT"),
                                    row(7, "Story", 3, "POSSIBLE_CONFLICT")], NAMES)
        result = score.score_finder(c, rels)
        self.assertEqual(["x"], result["found"])
        self.assertEqual(["x"], result["wanted"])

    def test_a_new_issue_for_a_relationship_the_last_run_reported_is_a_duplicate(self):
        same = {"type": "T", "participants": [("Goal", "A"), ("Goal", "B")], "annotationId": 1}
        moved = dict(same, annotationId=2)
        self.assertEqual(0, score.duplicates([{"relationships": [same]},
                                              {"relationships": [same]}]))
        self.assertEqual(1, score.duplicates([{"relationships": [same]},
                                              {"relationships": [moved]}]))

    def test_the_report_renders(self):
        c = corpus([{"id": "x", "participants": [["Goal", "A"], ["Story", "C"]], "finder": True}])
        rels = score.relationships([row(7, "Goal", 1), row(7, "Story", 3)], NAMES)
        runs = [{"finder": {"outcome": "ok", "score": score.score_finder(c, rels)},
                 "analysis": {"outcome": "ok", "summary": "s", "relationships": [],
                              "score": score.score_corpus(c, rels)}}]
        report = score.render_corpus_report({"runs": 1}, c, runs)
        self.assertIn("| 1/1 | 100% (1/1) | 100% (1/1) |", report)


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

    def test_every_corpus_participant_is_in_the_fixture(self):
        import xml.etree.ElementTree as ET
        doc = score.load_expectations(os.path.join(score.HERE, "expectations.json"))
        tags = {"actor": "Actor", "goal": "Goal", "story": "Story", "usecase": "UseCase",
                "scenario": "Scenario", "step": "Step", "term": "GlossaryTerm"}
        names = set()
        for element in ET.parse(os.path.join(score.HERE, doc["fixture"])).iter():
            tag = element.tag.split("}")[-1]
            name = element.find("{*}name")
            if tag in tags and name is not None:
                names.add((tags[tag], name.text))
        for key in ("expect", "alsoValid", "silent"):
            for item in doc["corpus"][key]:
                for participant in item["participants"]:
                    self.assertIn(tuple(participant), names,
                                  "%s names an entity the fixture lacks" % item["id"])


if __name__ == "__main__":
    unittest.main()
