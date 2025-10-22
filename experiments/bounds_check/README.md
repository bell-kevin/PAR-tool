# Bounds check experiment

- **Bug:** `list_access.fetch` indexes a list without guarding the index.
- **Expectation:** Return the element for valid indices and `None` for negative or out-of-range access.
- **Tests:** `pytest -q` inside `project/`.
- **Repair hook:** `BoundsGuard` pattern injects `if 0 <= index < len(items):` before the access.
- **Reproduce:**
  ```bash
  javac -d out $(find ../../src/main/java -name "*.java")
  java -cp out com.par.tool.ParTool \
    --project $(pwd)/project \
    --target  $(pwd)/project/list_access.py \
    --tests   "pytest -q" \
    --budget  40 \
    --timeout 30
  ```
