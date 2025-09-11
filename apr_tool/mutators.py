import ast
import copy
from typing import List, Dict

# ---- Base class ----

class NodeMutator:
    def find_nodes(self, tree: ast.AST) -> List[ast.AST]:
        raise NotImplementedError

    def mutate(self, tree: ast.AST, node: ast.AST) -> ast.AST:
        raise NotImplementedError

    def describe(self, node: ast.AST) -> str:
        return f"{self.__class__.__name__}:{getattr(node, 'lineno', '?')}"

# ---- Utilities ----

def clone_tree(tree: ast.AST) -> ast.AST:
    return copy.deepcopy(tree)

def apply_single_mutation_candidates(tree: ast.AST, orig_src: str, mutators: List[NodeMutator], max_candidates: int = 600) -> List[Dict]:
    candidates: List[Dict] = []
    for mut in mutators:
        nodes = mut.find_nodes(tree)
        for node in nodes:
            t2 = clone_tree(tree)
            try:
                t2 = mut.mutate(t2, node)
                src2 = ast.unparse(t2)
                candidates.append({"src": src2, "mutator": mut.describe(node)})
            except Exception:
                continue
            if len(candidates) >= max_candidates:
                return candidates
    return candidates

# ---- Concrete mutators ----

_ARITH_MAP = {
    ast.Add: [ast.Sub],
    ast.Sub: [ast.Add],
    ast.Mult: [ast.FloorDiv, ast.Div],
    ast.Div: [ast.FloorDiv, ast.Mult],
    ast.FloorDiv: [ast.Div, ast.Mult],
    ast.Mod: [ast.FloorDiv, ast.Mult],
}

class ArithmeticOpReplacer(NodeMutator):
    def find_nodes(self, tree: ast.AST) -> List[ast.AST]:
        return [n for n in ast.walk(tree) if isinstance(n, ast.BinOp) and type(n.op) in _ARITH_MAP]

    def mutate(self, tree: ast.AST, node: ast.AST) -> ast.AST:
        class Rewriter(ast.NodeTransformer):
            def __init__(self, target_node):
                self.target = target_node
                super().__init__()
            def visit_BinOp(self, n: ast.BinOp):
                self.generic_visit(n)
                if n is self.target and type(n.op) in _ARITH_MAP:
                    n.op = _ARITH_MAP[type(n.op)][0]()  # first alternative
                return n
        return Rewriter(node).visit(tree)

    def describe(self, node: ast.AST) -> str:
        return f"ArithmeticOpReplacer@{getattr(node, 'lineno', '?')}"

_CMP_MAP = {
    ast.Gt: [ast.GtE, ast.Lt, ast.LtE],
    ast.Lt: [ast.LtE, ast.Gt, ast.GtE],
    ast.GtE: [ast.Gt, ast.LtE],
    ast.LtE: [ast.Lt, ast.GtE],
    ast.Eq: [ast.NotEq],
    ast.NotEq: [ast.Eq],
}

class CompareOpReplacer(NodeMutator):
    def find_nodes(self, tree: ast.AST) -> List[ast.AST]:
        return [n for n in ast.walk(tree) if isinstance(n, ast.Compare) and all(type(op) in _CMP_MAP for op in n.ops)]

    def mutate(self, tree: ast.AST, node: ast.AST) -> ast.AST:
        class Rewriter(ast.NodeTransformer):
            def __init__(self, target):
                self.target = target
                super().__init__()
            def visit_Compare(self, n: ast.Compare):
                self.generic_visit(n)
                if n is self.target:
                    # flip only first op to keep single-edit
                    op = n.ops[0]
                    alts = _CMP_MAP.get(type(op))
                    if alts:
                        n.ops[0] = alts[0]()
                return n
        return Rewriter(node).visit(tree)

    def describe(self, node: ast.AST) -> str:
        return f"CompareOpReplacer@{getattr(node, 'lineno', '?')}"

class IfNegationToggler(NodeMutator):
    def find_nodes(self, tree: ast.AST) -> List[ast.AST]:
        return [n for n in ast.walk(tree) if isinstance(n, ast.If)]

    def mutate(self, tree: ast.AST, node: ast.AST) -> ast.AST:
        class Rewriter(ast.NodeTransformer):
            def __init__(self, target):
                self.target = target
                super().__init__()
            def visit_If(self, n: ast.If):
                self.generic_visit(n)
                if n is self.target:
                    n.test = ast.UnaryOp(op=ast.Not(), operand=n.test)
                return n
        return Rewriter(node).visit(tree)

    def describe(self, node: ast.AST) -> str:
        return f"IfNegationToggler@{getattr(node, 'lineno', '?')}"

class SmallIntTweaker(NodeMutator):
    def find_nodes(self, tree: ast.AST) -> List[ast.AST]:
        return [n for n in ast.walk(tree) if isinstance(n, ast.Constant) and isinstance(n.value, int) and -3 <= n.value <= 3]

    def mutate(self, tree: ast.AST, node: ast.AST) -> ast.AST:
        class Rewriter(ast.NodeTransformer):
            def __init__(self, target):
                self.target = target
                super().__init__()
            def visit_Constant(self, n: ast.Constant):
                self.generic_visit(n)
                if n is self.target and isinstance(n.value, int):
                    n.value = n.value + 1 if n.value >= 0 else n.value - 1
                return n
        return Rewriter(node).visit(tree)

    def describe(self, node: ast.AST) -> str:
        return f"SmallIntTweaker@{getattr(node, 'lineno', '?')}"
