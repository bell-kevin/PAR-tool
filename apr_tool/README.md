# Prototype Automatic Program Repair (APR) Tool (Python)

A lightweight, *generate-and-validate* APR prototype. It mutates Python source using `ast`,
builds candidate patches, and runs your test command (e.g., `pytest`) to find a patch that
reduces or eliminates failing tests.

> ⚠️ Prototype: simple heuristics, single-file focus by default, and a tiny set of mutators.
> Good enough to demonstrate APR concepts end-to-end.

---

## Features

- **AST-based mutations**:
  - Arithmetic operator replacement: `+ ↔ -`, `* ↔ // ↔ /`, `%` neighbors
  - Comparison flip: `> ↔ >= ↔ < ↔ <=`, `== ↔ !=`
  - Boolean negation toggle: `if cond:` ↔ `if not cond:`
  - Small integer tweak: `n → n±1` for `-3..3`
- **Search**: Iterative single-edit search with budget.
- **Oracle**: Any shell test command (default: `pytest -q`).
- **Safety**: Edits occur in a temp copy of your project dir.

---

## Quickstart

1. Install test deps for your project (e.g., `pytest`).
2. Put this folder somewhere convenient, or use it in-place.
3. Run:

```bash
python apr.py   --project /path/to/your/project   --target  /path/to/your/project/mypkg/module.py   --tests   "pytest -q"   --budget  200   --timeout 120
```

- `--project` : root directory containing your code and tests.
- `--target`  : the Python file to mutate (start simple with the failing module).
- `--tests`   : shell command; should exit 0 when all tests pass.
- `--budget`  : max number of candidate patches to try (default 200).
- `--timeout` : seconds allowed per test run (default 120).

If it finds a patch that makes tests **all pass**, it will:
- save the **patched file** and a **unified diff** under `./_apr_results/`,
- emit a JSON result summary.

If it finds **improving patches** (fewer failures), it will keep the best-so-far and report them.

---

## Tips

- Start with the specific file implicated by failing tests (stack traces).
- Lower `--budget` to iterate faster, raise it for tougher cases.
- You can point `--tests` at **any** command: `python -m unittest -q`, `pytest -q`, or a custom script.
- To extend mutators, see `mutators.py` and implement another `NodeMutator`.

---

## Output

- `stdout`: running log + final JSON summary.
- `_apr_results/` directory (created in the working directory you run `apr.py` from):
  - `best_patch.diff`
  - `best_patch.py`
  - `summary.json`

---

## License

MIT (for classroom/research demos).
