package com.par.tool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class PythonAstService {
    private static final String SCRIPT = String.join("\n",
            "import ast",
            "import copy",
            "import sys",
            "",
            "class NullGuardTransformer(ast.NodeTransformer):",
            "    def __init__(self, target):",
            "        self.target = target",
            "        self.count = 0",
            "        self.description = None",
            "",
            "    def visit_Expr(self, node):",
            "        node = self.generic_visit(node)",
            "        call = node.value if isinstance(node, ast.Expr) else None",
            "        if isinstance(call, ast.Call) and isinstance(call.func, ast.Attribute):",
            "            owner = call.func.value",
            "            if isinstance(owner, ast.Name):",
            "                if self.count == self.target:",
            "                    test = ast.Compare(",
            "                        left=ast.Name(id=owner.id, ctx=ast.Load()),",
            "                        ops=[ast.IsNot()],",
            "                        comparators=[ast.Constant(value=None)]",
            "                    )",
            "                    guarded = ast.If(test=test, body=[node], orelse=[])",
            "                    self.description = f'Guard {owner.id}.{call.func.attr} with None check'",
            "                    self.count += 1",
            "                    return guarded",
            "                self.count += 1",
            "        return node",
            "",
            "class NoneEqualityTransformer(ast.NodeTransformer):",
            "    def __init__(self, target):",
            "        self.target = target",
            "        self.count = 0",
            "        self.description = None",
            "",
            "    def visit_Compare(self, node):",
            "        node = self.generic_visit(node)",
            "        for index, comparator in enumerate(node.comparators):",
            "            if isinstance(comparator, ast.Constant) and comparator.value is None:",
            "                op = node.ops[index]",
            "                if isinstance(op, ast.Eq) or isinstance(op, ast.NotEq):",
            "                    if self.count == self.target:",
            "                        if isinstance(op, ast.Eq):",
            "                            node.ops[index] = ast.Is()",
            "                        else:",
            "                            node.ops[index] = ast.IsNot()",
            "                        self.description = 'Normalize None comparison to identity check'",
            "                        self.count += 1",
            "                        return node",
            "                    self.count += 1",
            "        return node",
            "",
            "def _index_name(node):",
            "    if isinstance(node, ast.Name):",
            "        return node.id",
            "    if isinstance(node, ast.Index) and isinstance(node.value, ast.Name):",
            "        return node.value.id",
            "    return None",
            "",
            "class BoundsGuardTransformer(ast.NodeTransformer):",
            "    def __init__(self, target):",
            "        self.target = target",
            "        self.count = 0",
            "        self.description = None",
            "",
            "    def visit_Expr(self, node):",
            "        node = self.generic_visit(node)",
            "        value = node.value if isinstance(node, ast.Expr) else None",
            "        if isinstance(value, ast.Subscript) and isinstance(value.value, ast.Name):",
            "            index_name = _index_name(value.slice)",
            "            if index_name is not None:",
            "                if self.count == self.target:",
            "                    test = ast.Compare(",
            "                        left=ast.Constant(value=0),",
            "                        ops=[ast.LtE(), ast.Lt()],",
            "                        comparators=[",
            "                            ast.Name(id=index_name, ctx=ast.Load()),",
            "                            ast.Call(func=ast.Name(id='len', ctx=ast.Load()), args=[ast.Name(id=value.value.id, ctx=ast.Load())], keywords=[])",
            "                        ]",
            "                    )",
            "                    guarded = ast.If(test=test, body=[node], orelse=[])",
            "                    self.description = f'Guard {value.value.id}[{index_name}] with bounds check'",
            "                    self.count += 1",
            "                    return guarded",
            "                self.count += 1",
            "        return node",
            "",
            "    def visit_Assign(self, node):",
            "        node = self.generic_visit(node)",
            "        if isinstance(node.value, ast.Subscript) and isinstance(node.value.value, ast.Name):",
            "            index_name = _index_name(node.value.slice)",
            "            if index_name is not None:",
            "                if self.count == self.target:",
            "                    test = ast.Compare(",
            "                        left=ast.Constant(value=0),",
            "                        ops=[ast.LtE(), ast.Lt()],",
            "                        comparators=[",
            "                            ast.Name(id=index_name, ctx=ast.Load()),",
            "                            ast.Call(func=ast.Name(id='len', ctx=ast.Load()), args=[ast.Name(id=node.value.value.id, ctx=ast.Load())], keywords=[])",
            "                        ]",
            "                    )",
            "                    guarded = ast.If(test=test, body=[node], orelse=[])",
            "                    self.description = f'Guard {node.value.value.id}[{index_name}] with bounds check'",
            "                    self.count += 1",
            "                    return guarded",
            "                self.count += 1",
            "        return node",
            "",
            "def transform(source, name, limit):",
            "    tree = ast.parse(source)",
            "    generators = {",
            "        'null_guard': NullGuardTransformer,",
            "        'none_identity': NoneEqualityTransformer,",
            "        'bounds_guard': BoundsGuardTransformer,",
            "    }",
            "    transformer_cls = generators.get(name)",
            "    if transformer_cls is None:",
            "        return []",
            "    patches = []",
            "    index = 0",
            "    while len(patches) < limit:",
            "        transformer = transformer_cls(index)",
            "        mutated = transformer.visit(copy.deepcopy(tree))",
            "        if transformer.description is None:",
            "            break",
            "        ast.fix_missing_locations(mutated)",
            "        patches.append((transformer.description, ast.unparse(mutated)))",
            "        index += 1",
            "    return patches",
            "",
            "def detect(source, name):",
            "    tree = ast.parse(source)",
            "    counters = {",
            "        'null_dereference': detect_null_dereference,",
            "        'loose_none_equality': detect_loose_none_equality,",
            "        'unsafe_index': detect_unsafe_index,",
            "    }",
            "    detector = counters.get(name)",
            "    if detector is None:",
            "        return 0",
            "    return detector(tree)",
            "",
            "def detect_null_dereference(tree):",
            "    count = 0",
            "    for node in ast.walk(tree):",
            "        if isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute):",
            "            if isinstance(node.func.value, ast.Name):",
            "                count += 1",
            "    return count",
            "",
            "def detect_loose_none_equality(tree):",
            "    count = 0",
            "    for node in ast.walk(tree):",
            "        if isinstance(node, ast.Compare):",
            "            for op, comparator in zip(node.ops, node.comparators):",
            "                if isinstance(comparator, ast.Constant) and comparator.value is None:",
            "                    if isinstance(op, ast.Eq) or isinstance(op, ast.NotEq):",
            "                        count += 1",
            "    return count",
            "",
            "def detect_unsafe_index(tree):",
            "    count = 0",
            "    for node in ast.walk(tree):",
            "        if isinstance(node, ast.Subscript) and isinstance(node.value, ast.Name):",
            "            if _index_name(node.slice) is not None:",
            "                count += 1",
            "    return count",
            "",
            "def main():",
            "    if len(sys.argv) < 3:",
            "        sys.exit('missing args')",
            "    mode = sys.argv[1]",
            "    name = sys.argv[2]",
            "    limit = int(sys.argv[3]) if len(sys.argv) > 3 else 0",
            "    source = sys.stdin.read()",
            "    if mode == 'transform':",
            "        try:",
            "            patches = transform(source, name, limit)",
            "        except SyntaxError:",
            "            patches = []",
            "        for description, code in patches:",
            "            print('===PATCH===')",
            "            print(description)",
            "            print('===SOURCE===')",
            "            print(code)",
            "            print('===END===')",
            "    elif mode == 'detect':",
            "        try:",
            "            count = detect(source, name)",
            "        except SyntaxError:",
            "            count = 0",
            "        print(count)",
            "    else:",
            "        sys.exit('unknown mode')",
            "",
            "if __name__ == '__main__':",
            "    main()"
    );

    private PythonAstService() {}

    static List<Patch> applyFix(String source, String fixName, int limit) {
        try {
            ProcessBuilder builder = new ProcessBuilder("python3", "-c", SCRIPT, "transform", fixName, Integer.toString(limit));
            Process process = builder.start();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(source);
            }
            int exit = process.waitFor();
            drainErrors(process);
            String stdout = readAll(process);
            if (exit != 0) {
                return List.of();
            }
            return parsePatches(stdout);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (IOException ex) {
            return List.of();
        }
    }

    static int countFaultOccurrences(String source, String faultName) {
        try {
            ProcessBuilder builder = new ProcessBuilder("python3", "-c", SCRIPT, "detect", faultName, "0");
            Process process = builder.start();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(source);
            }
            int exit = process.waitFor();
            drainErrors(process);
            if (exit != 0) {
                return 0;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line == null) {
                    return 0;
                }
                return Integer.parseInt(line.trim());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return 0;
        } catch (IOException | NumberFormatException ex) {
            return 0;
        }
    }

    private static String readAll(Process process) throws IOException {
        try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = stdout.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    private static void drainErrors(Process process) throws IOException {
        try (BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            while (stderr.readLine() != null) {
                // discard
            }
        }
    }

    private static List<Patch> parsePatches(String stdout) {
        if (stdout.isEmpty()) {
            return List.of();
        }
        String[] parts = stdout.split("===PATCH===\n");
        List<Patch> patches = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            int sourceIndex = part.indexOf("===SOURCE===\n");
            int endIndex = part.indexOf("===END===", sourceIndex >= 0 ? sourceIndex : 0);
            if (sourceIndex < 0 || endIndex < 0) {
                continue;
            }
            String description = part.substring(0, sourceIndex).trim();
            String source = part.substring(sourceIndex + "===SOURCE===\n".length(), endIndex).stripTrailing();
            patches.add(new Patch(source, description));
        }
        return patches;
    }
}
