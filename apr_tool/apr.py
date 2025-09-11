import argparse
import ast
import difflib
import json
import os
import random
import shutil
import subprocess
import sys
import tempfile
import textwrap
from pathlib import Path
from typing import List, Tuple, Dict, Optional

from mutators import (
    ArithmeticOpReplacer,
    CompareOpReplacer,
    IfNegationToggler,
    SmallIntTweaker,
    apply_single_mutation_candidates,
)

def run_cmd(cmd: str, cwd: Path, timeout: int = 120) -> Tuple[int, str, str]:
    try:
        proc = subprocess.run(
            cmd, cwd=str(cwd), shell=True,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            timeout=timeout, text=True, encoding="utf-8", errors="replace"
        )
        return proc.returncode, proc.stdout, proc.stderr
    except subprocess.TimeoutExpired as e:
        return 124, e.stdout or "", e.stderr or "TIMEOUT"

def parse_pytest_summary(o: str) -> Dict[str, int]:
    # Heuristic parser for lines like: "=== 2 failed, 10 passed in 0.17s ==="
    failed = passed = xfailed = xpassed = skipped = errors = 0
    line = ""
    for l in o.splitlines():
        if " failed" in l or " passed" in l or " deselected" in l or " error" in l:
            line = l
    tokens = line.replace("=", " ").replace(",", " ").split()
    for i, tok in enumerate(tokens):
        if tok.isdigit():
            val = int(tok)
            if i + 1 < len(tokens):
                nxt = tokens[i + 1].lower()
                if nxt.startswith("failed"):
                    failed = val
                elif nxt.startswith("passed"):
                    passed = val
                elif nxt.startswith("skipped"):
                    skipped = val
                elif nxt.startswith("errors") or nxt.startswith("error"):
                    errors = val
                elif nxt.startswith("xfailed"):
                    xfailed = val
                elif nxt.startswith("xpassed"):
                    xpassed = val
    return {"failed": failed, "passed": passed, "skipped": skipped, "errors": errors, "xfailed": xfailed, "xpassed": xpassed}

def score_test_run(ret: int, out: str, err: str) -> Tuple[int, Dict[str, int]]:
    # Prefer exit 0; otherwise count failures+errors.
    if ret == 0:
        return 0, {"failed": 0, "errors": 0, "passed": None}
    stats = parse_pytest_summary(out + "\n" + err)
    failures = stats.get("failed", 9999) + stats.get("errors", 0)
    return failures if failures > 0 else 9999, stats

def unified_diff(a: str, b: str, fromfile: str, tofile: str) -> str:
    return "".join(difflib.unified_diff(a.splitlines(True), b.splitlines(True), fromfile, tofile))

def repair(project: Path, target_file: Path, test_cmd: str, budget: int, timeout: int, seed: int) -> Dict:
    random.seed(seed)
    results_dir = Path.cwd() / "_apr_results"
    results_dir.mkdir(exist_ok=True, parents=True)

    # Baseline: clone project to temp and run tests
    with tempfile.TemporaryDirectory(prefix="apr_") as tmpd:
        tmp = Path(tmpd)
        shutil.copytree(project, tmp / project.name, dirs_exist_ok=True)
        work_root = tmp / project.name

        # Resolve target inside copy
        rel_target = os.path.relpath(target_file, start=project)
        target_in_tmp = work_root / rel_target

        base_ret, base_out, base_err = run_cmd(test_cmd, cwd=work_root, timeout=timeout)
        base_score, base_stats = score_test_run(base_ret, base_out, base_err)
        print("BASELINE EXIT:", base_ret)
        print("BASELINE STATS:", base_stats)
        if base_ret == 0:
            print("All tests already pass. Nothing to repair.")
            return {
                "status": "already_passing",
                "baseline": {"exit": base_ret, "stats": base_stats},
            }

        # Load source and generate mutation candidates
        orig_src = target_in_tmp.read_text(encoding="utf-8")
        tree = ast.parse(orig_src)
        mutators = [
            ArithmeticOpReplacer(),
            CompareOpReplacer(),
            IfNegationToggler(),
            SmallIntTweaker(),
        ]
        candidates = apply_single_mutation_candidates(tree, orig_src, mutators, max_candidates=budget*3)

        best = {"score": base_score, "patch_src": None, "diff": None, "mutator": None, "stats": base_stats}
        tried = 0

        for cand in candidates:
            if tried >= budget:
                break
            tried += 1
            # Write patch to file
            target_in_tmp.write_text(cand["src"], encoding="utf-8")

            ret, out, err = run_cmd(test_cmd, cwd=work_root, timeout=timeout)
            score, stats = score_test_run(ret, out, err)
            print(f"[{tried}/{budget}] {cand['mutator']} -> exit={ret} score={score} stats={stats}")

            if score < best["score"]:
                best = {
                    "score": score,
                    "patch_src": cand["src"],
                    "diff": unified_diff(orig_src, cand["src"], str(target_file), str(target_file)+" (patched)"),
                    "mutator": cand["mutator"],
                    "stats": stats,
                }

            if ret == 0:
                print("🎉 Found a full fix!")
                break

        # Save results
        summary = {
            "status": "fixed" if best["score"] == 0 else ("improved" if best["score"] < base_score else "no_fix"),
            "baseline": {"exit": base_ret, "stats": base_stats},
            "best": {
                "score": best["score"],
                "mutator": best["mutator"],
                "stats": best["stats"],
            },
            "tried": tried,
        }

        (results_dir / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
        if best["patch_src"]:
            (results_dir / "best_patch.py").write_text(best["patch_src"], encoding="utf-8")
        if best["diff"]:
            (results_dir / "best_patch.diff").write_text(best["diff"], encoding="utf-8")

        return summary

def main():
    p = argparse.ArgumentParser(description="Prototype APR (generate-and-validate)")
    p.add_argument("--project", required=True, help="Path to project root")
    p.add_argument("--target", required=True, help="Path to target python file to mutate")
    p.add_argument("--tests", default="pytest -q", help="Test command (shell string)")
    p.add_argument("--budget", type=int, default=200, help="Max candidate patches to attempt")
    p.add_argument("--timeout", type=int, default=120, help="Seconds per test run")
    p.add_argument("--seed", type=int, default=0, help="Random seed")
    args = p.parse_args()

    project = Path(args.project).resolve()
    target = Path(args.target).resolve()

    if not project.exists():
        print(f"Project path does not exist: {project}", file=sys.stderr)
        sys.exit(2)
    if not target.exists():
        print(f"Target file does not exist: {target}", file=sys.stderr)
        sys.exit(2)

    summary = repair(project, target, args.tests, args.budget, args.timeout, args.seed)
    print(json.dumps(summary, indent=2))

if __name__ == "__main__":
    main()
