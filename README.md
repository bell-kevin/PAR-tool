<a name="readme-top"></a>

# Java Program-based Automatic Repair (PAR) Prototype

This repository hosts a Java implementation of a prototype Program-based Automatic Repair (PAR) tool. The tool copies your project
into an isolated working directory, applies mutation operators to a designated Python source file, and runs your regression test
command to search for a repairing patch.

Even though the repaired program is Python, the entire repair workflow, mutation search, and pattern matching system are written
in Java as requested. The implementation includes a lightweight fault/fix knowledge base, crossover search, and an extensible
API for pattern-driven fixes (for example, automatically adding null checks).

This is Python code being fixed by Java code.

---

## Features

- **Java CLI runner** (`com.par.tool.ParTool`) orchestrates project cloning, candidate patch evaluation, and result reporting.
- **Mutation operators**
  - Statement duplication, deletion, and adjacent swap.
  - Arithmetic and comparison operator substitutions.
  - Conditional negation and small-integer tweaking.
  - Pattern-driven fixes sourced from a fix database (e.g., injecting `None` guards, normalizing `None` comparisons, and
    inserting bounds checks).
- **Crossover search** combines pairs of promising candidates to explore multi-edit repairs.
- **Fault and fix databases** encode three diagnostic patterns and three repair templates that the pattern matcher can leverage
  before random search begins.
- **Result artifacts** under `_apr_results/` mirror the original Python prototype (`summary.json`, `best_patch.diff`, and
  `best_patch.py`).

---

## Project Layout

```
src/main/java/com/par/tool/
├── CandidateGenerator.java
├── Config.java
├── CrossoverOperator.java
├── FaultDatabase.java
├── FaultPattern.java
├── FileUtils.java
├── FixDatabase.java
├── FixPattern.java
├── MutationContext.java
├── MutationOperator.java
├── ParRunner.java
├── ParTool.java
├── Patch.java
├── PatternMatcher.java
├── ProcessUtils.java
├── Score.java
├── SummaryWriter.java
├── TestRunResult.java
└── operators/
    ├── ArithmeticOperator.java
    ├── CompareOperator.java
    ├── IfNegationOperator.java
    ├── PatternBasedOperator.java
    ├── SmallIntTweakerOperator.java
    ├── StatementDeleteOperator.java
    ├── StatementDuplicateOperator.java
    └── StatementSwapOperator.java
```

---

## Quickstart

### Prerequisites

- Java 11 or newer
- [Apache Maven](https://maven.apache.org/) 3.9+

### Build

Use Maven to compile the project and produce the runnable classes:

```bash
mvn -q clean compile
```

The command places compiled classes under `target/classes`. You can also build
the distributable JAR with `mvn -q package`, which writes
`target/par-tool-1.0-SNAPSHOT.jar`.

### Run the repair search

After compiling, invoke the CLI entrypoint with either the compiled classes or
the packaged JAR:

```bash
# Using compiled classes
java -cp target/classes com.par.tool.ParTool \
  --project /path/to/project \
  --target  /path/to/project/mypkg/module.py \
  --tests   "pytest -q" \
  --budget  200 \
  --timeout 120

# Using the packaged JAR
java -cp target/par-tool-1.0-SNAPSHOT.jar com.par.tool.ParTool [options]
```

Pattern matching happens automatically when you run the CLI. `ParTool` builds a
`ParRunner` that constructs a `PatternMatcher` and registers the
`PatternBasedOperator` alongside the stochastic operators, so every invocation
detects faults and attempts template-based fixes without extra flags.

Arguments mirror the original prototype:

- `--project`: project root that will be copied into a temporary workspace (defaults to the parent of `--target`).
- `--target`: Python file to mutate.
- `--tests`: shell command that returns exit code `0` when all tests pass.
- `--budget`: maximum number of candidates to evaluate (default `200`).
- `--timeout`: seconds allowed per test run (default `120`).
- `--seed`: seed for the mutation search (default `1337`).
- `--threads`: worker threads for parallel candidate evaluation (defaults to the detected logical processor count).

During execution the tool prints baseline test results, enumerates mutation attempts, and stops early if a full repair is found.
All intermediate work happens on a temporary copy so your original project stays untouched.

### Run the unit tests

This repository includes JUnit tests for the scoring logic, process runner, and
pattern matcher. Execute them with:

```bash
mvn test
```

---

## Pattern Matching API

The repair engine ships with a fault and fix database:

- **Fault patterns** – `NullDereference`, `LooseNoneEquality`, and `UnsafeIndex` – capture common defects the tool can reason about.
- **Fix patterns** – `NullCheckGuard`, `NoneEquality`, and `BoundsGuard` – supply ready-made repairs such as null checks, identity
  comparisons for `None`, and list bounds guards.

`PatternMatcher` exposes a simple API that other operators can call to detect known faults and generate fixes. The
`PatternBasedOperator` integrates this API into the mutation search so these template-driven fixes are attempted alongside the
stochastic operators. New patterns can be added by extending `FixPattern` and registering the implementation inside
`FixDatabase`.

This interface is entirely in-process Java, not a REST service. Components such as `PatternBasedOperator` call methods like
`detectFaults` and `createFixes` directly on in-memory source strings, and the matcher returns pattern names or `Patch`
objects synchronously with no network layer involved.

---

## Output

After every run the `_apr_results/` directory contains:

- `summary.json` – JSON summary of the baseline run, best candidate, fault detections, and overall status (`fixed`, `improved`, or
  `no_fix`).
- `best_patch.py` – source code of the best candidate found (if any candidate improved the score).
- `best_patch.diff` – simplified diff between the original target and the best candidate.

---

## Submission Artifacts

The `experiments/` directory bundles three data points that exercise the built-in pattern matcher:

- `null_guard/` reproduces a missing `None` check on a method call, which the `NullCheckGuard` template repairs.
- `none_equality/` captures an equality comparison against `None` that trips a user-defined `__eq__`, fixed by the `NoneEquality` template.
- `bounds_check/` demonstrates the `BoundsGuard` template on an unchecked list index.

Each experiment records the buggy Python module, its `pytest` suite, and the artifacts written by the Java runner (`summary.json`,
`best_patch.diff`, and `best_patch.py`). The `report/project_report.tex` file provides the accompanying one-page LaTeX report summarizing the tool and its evaluation.

---

## Extending the Tool

- Add more `MutationOperator` implementations for specialized repairs.
- Register additional fault/fix patterns to grow the knowledge base.
- Customize the crossover logic in `CrossoverOperator` to blend candidates differently.
- Swap out the scoring heuristic in `Score` if your test framework prints different summary lines.

Feel free to fork the project and build richer search strategies on top of this Java foundation.

https://github.com/TruX-DTF/FL-VS-APR/tree/master/kPAR/src/main/java/edu/lu/uni/serval/par/templates

https://docs.python.org/3/library/ast.html
