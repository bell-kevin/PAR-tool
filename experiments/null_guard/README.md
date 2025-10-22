# Null guard experiment

- **Bug:** `strings.safe_title` calls `.title()` on `None` inputs, raising `AttributeError`.
- **Expectation:** Return the title-cased string for non-null inputs and propagate `None` without crashing.
- **Tests:** `pytest -q` inside `project/`.
- **Repair hook:** `NullCheckGuard` fix pattern wraps the dereference in an `if text is not None:` block.
- **Reproduce:**
  ```bash
  javac -d out $(find ../../src/main/java -name "*.java")
  java -cp out com.par.tool.ParTool \
    --project $(pwd)/project \
    --target  $(pwd)/project/strings.py \
    --tests   "pytest -q" \
    --budget  40 \
    --timeout 30
  ```
