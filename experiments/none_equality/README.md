# None equality experiment

- **Bug:** `status_checks.is_missing` compares with `== None`, triggering user-defined equality overloads.
- **Expectation:** The helper should treat `None` as missing without invoking custom `__eq__` hooks.
- **Tests:** `pytest -q` inside `project/`.
- **Repair hook:** `NoneEquality` pattern rewrites the condition to `is None`.
- **Reproduce:**
  ```bash
  javac -d out $(find ../../src/main/java -name "*.java")
  java -cp out com.par.tool.ParTool \
    --project $(pwd)/project \
    --target  $(pwd)/project/status_checks.py \
    --tests   "pytest -q" \
    --budget  40 \
    --timeout 30
  ```
