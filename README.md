<a name="readme-top"></a>

# Prototype Program-based Automatic Program Repair (PAR) Tool (Python)

Goal: implement a prototype-level automatic program repair (APR) tool that mutates Python source code using the `ast` module and validates candidate fixes against your project test suite.

This repository contains a runnable, generate-and-validate APR prototype: it copies your project into a temporary workspace, applies single-edit mutations, and runs your test command to search for a patch that reduces or eliminates failing tests.

---

## Features

- **Program-based edit operations (PAR-style)**
  - **Insert**: duplicate an existing statement to explore guard-style fixes.
  - **Delete**: remove a statement while keeping bodies syntactically valid.
  - **Swap**: exchange adjacent statements to reorder logic.
  - **Mutate**: targeted expression-level rewrites (arithmetic operator swaps, comparison flips, boolean negation, and small integer tweaks).
- **Search**: iterative single-edit search with a configurable budget.
- **Oracle**: works with any shell test command (e.g., `pytest -q`, `python -m unittest -q`).
- **Safety**: edits occur in a temporary copy of your project directory so your sources remain untouched.

> ⚠️ Prototype: simple heuristics, single-file focus by default, and a minimal set of mutators. Good enough to demonstrate APR concepts end-to-end.

---

## Project Layout

- `apr.py` — CLI runner that orchestrates copying your project, applying one-edit patches, running tests, and tracking the best patch found.
- `mutators.py` — collection of AST node mutators implementing arithmetic/operator tweaks, condition negations, and other expression-level rewrites. Extend `NodeMutator` to add more repair operators (e.g., `None` checks, return-default templates, list bounds guards).
- `_apr_results/` — output directory created at runtime containing `summary.json`, `best_patch.diff`, and `best_patch.py` for the strongest candidate discovered.

---

## Quickstart

```bash
python apr.py \
  --project /path/to/your/project \
  --target  /path/to/your/project/mypkg/module.py \
  --tests   "pytest -q" \
  --budget  200 \
  --timeout 120
```

1. Install the test dependencies for your project (e.g., `pytest`).
2. Place this repository somewhere convenient, or operate in-place.
3. Run the command above, adjusting paths and parameters as needed.

### FAQ

**Is the PAR tool fully functional?**  Yes—this repository contains a working
prototype that you can execute directly with `python apr.py ...`.  It already
implements mutation operators, a search loop, and result reporting, so you do
not need any additional binaries or third-party frameworks to try it out.

**Do I need a sample program or tests?**  The tool works against *your* Python
project.  Provide the path to a project directory plus a shell command that
runs its tests (for example, `pytest -q` or `python -m unittest`).  The tool
copies that project into a temporary workspace, mutates the designated target
file, and repeatedly re-runs the test command to see whether any mutation fixes
the bug.  Without a project and a meaningful test suite the tool has no oracle,
so make sure you can run your tests successfully outside of the tool first.

**Do I need datasets from program-repair.org?**  No external downloads are
required.  Public benchmarks from program-repair.org (or anywhere else) can be
useful if you want reproducible bug suites for experimentation, but they are
optional.  Point the tool at any local project you want to repair.

### Arguments

- `--project`: root directory containing your code and tests.
- `--target`: Python file to mutate (start with the file implicated by failing stack traces).
- `--tests`: shell command that exits 0 when all tests pass.
- `--budget`: maximum number of candidate patches to evaluate (default `200`).
- `--timeout`: seconds allowed per test run (default `120`).

If the tool finds a patch that makes the tests pass, it saves the patched file and unified diff under `./_apr_results/` and emits a JSON result summary. If it only finds improvements (fewer failures), it retains and reports the best-so-far candidate.

---

## Tips & Extensions

- Start with the specific file implicated by failing tests.
- Lower `--budget` for quicker iterations; raise it for tougher bugs.
- Point `--tests` at any command that accurately reflects project correctness.
- Extend `mutators.py` to explore new mutation templates and search strategies.
- Want more power? Potential next steps include coverage-guided fault localization, multi-edit search, or template-based repairs.

---

## Output

- **Standard output**: running log plus the final JSON summary.
- **`_apr_results/` directory**:
  - `best_patch.diff`
  - `best_patch.py`
  - `summary.json`

---

## We're Using GitHub Under Protest

This project is currently hosted on GitHub. This is not ideal; GitHub is a proprietary, trade-secret system that is not Free and Open Source Software (FOSS). We are deeply concerned about using a proprietary system like GitHub to develop our FOSS project. I have a [website](https://bellKevin.me) where the project contributors are actively discussing how we can move away from GitHub in the long term. We urge you to read about the [Give up GitHub](https://GiveUpGitHub.org) campaign from [the Software Freedom Conservancy](https://sfconservancy.org) to understand some of the reasons why GitHub is not a good place to host FOSS projects.

If you are a contributor who personally has already quit using GitHub, please email **bellKevin@pm.me** for instructions on contributing without using GitHub directly.

Any use of this project's code by GitHub Copilot, past or present, is done without our permission. We do not consent to GitHub's use of this project's code in Copilot.

![Logo of the GiveUpGitHub campaign](https://sfconservancy.org/img/GiveUpGitHub.png)

<p align="right"><a href="#readme-top">back to top</a></p>
