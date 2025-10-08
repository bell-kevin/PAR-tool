import ast
import copy
from typing import Dict, List, Optional, Tuple

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


def _annotate_with_uids(tree: ast.AST) -> None:
    """Attach a stable identifier to every node in-place."""
    for uid, node in enumerate(ast.walk(tree)):
        setattr(node, "_apr_uid", uid)


def _find_node_with_uid(tree: ast.AST, uid: Optional[int]) -> Optional[ast.AST]:
    if uid is None:
        return None
    for node in ast.walk(tree):
        if getattr(node, "_apr_uid", None) == uid:
            return node
    return None


def _strip_uids(tree: ast.AST) -> None:
    for node in ast.walk(tree):
        if hasattr(node, "_apr_uid"):
            delattr(node, "_apr_uid")


def _iter_statement_lists(tree: ast.AST) -> List[Tuple[ast.AST, str, List[ast.stmt]]]:
    containers: List[Tuple[ast.AST, str, List[ast.stmt]]] = []
    for parent in ast.walk(tree):
        for field, value in ast.iter_fields(parent):
            if isinstance(value, list) and value and all(isinstance(v, ast.stmt) for v in value):
                containers.append((parent, field, value))
    return containers


def _statement_parent_map(tree: ast.AST) -> Dict[int, Tuple[List[ast.stmt], int, ast.AST, str]]:
    mapping: Dict[int, Tuple[List[ast.stmt], int, ast.AST, str]] = {}
    for parent, field, stmt_list in _iter_statement_lists(tree):
        for index, stmt in enumerate(stmt_list):
            mapping[id(stmt)] = (stmt_list, index, parent, field)
    return mapping


def _ensure_non_empty(stmt_list: List[ast.stmt], parent: ast.AST) -> None:
    if stmt_list:
        return
    if isinstance(parent, ast.Module):
        return
    stmt_list.append(ast.Pass())

def apply_single_mutation_candidates(tree: ast.AST, orig_src: str, mutators: List[NodeMutator], max_candidates: int = 600) -> List[Dict]:
    candidates: List[Dict] = []
    _annotate_with_uids(tree)
    for mut in mutators:
        nodes = mut.find_nodes(tree)
        for node in nodes:
            t2 = clone_tree(tree)
            target = _find_node_with_uid(t2, getattr(node, "_apr_uid", None))
            if target is None:
                continue
            try:
                t2 = mut.mutate(t2, target)
                ast.fix_missing_locations(t2)
                _strip_uids(t2)
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


class StatementDuplicator(NodeMutator):
    """Insert operation: duplicate an existing statement."""

    def find_nodes(self, tree: ast.AST) -> List[ast.AST]:
        stmts: List[ast.AST] = []
        for _, _, stmt_list in _iter_statement_lists(tree):
            stmts.extend(stmt_list)
        return stmts

    def mutate(self, tree: ast.AST, node: ast.AST) -> ast.AST:
        parents = _statement_parent_map(tree)
        info = parents.get(id(node))
        if not info:
            return tree
        stmt_list, index, parent, _ = info
        duplicate = copy.deepcopy(node)
        stmt_list.insert(index, duplicate)
        _ensure_non_empty(stmt_list, parent)
        return tree

    def describe(self, node: ast.AST) -> str:
        return f"StatementDuplicator@{getattr(node, 'lineno', '?')}"


class StatementDeletionMutator(NodeMutator):
    """Delete operation: remove a statement while keeping the body valid."""

    def find_nodes(self, tree: ast.AST) -> List[ast.AST]:
        stmts: List[ast.AST] = []
        for _, _, stmt_list in _iter_statement_lists(tree):
            stmts.extend(stmt_list)
        return stmts

    def mutate(self, tree: ast.AST, node: ast.AST) -> ast.AST:
        parents = _statement_parent_map(tree)
        info = parents.get(id(node))
        if not info:
            return tree
        stmt_list, index, parent, _ = info
        if index < len(stmt_list):
            del stmt_list[index]
        _ensure_non_empty(stmt_list, parent)
        return tree

    def describe(self, node: ast.AST) -> str:
        return f"StatementDeletionMutator@{getattr(node, 'lineno', '?')}"


class AdjacentStatementSwapper(NodeMutator):
    """Swap operation: exchange a statement with its next sibling."""

    def find_nodes(self, tree: ast.AST) -> List[ast.AST]:
        stmts: List[ast.AST] = []
        for _, _, stmt_list in _iter_statement_lists(tree):
            stmts.extend(stmt_list)
        return stmts

    def mutate(self, tree: ast.AST, node: ast.AST) -> ast.AST:
        parents = _statement_parent_map(tree)
        info = parents.get(id(node))
        if not info:
            return tree
        stmt_list, index, parent, _ = info
        if index + 1 >= len(stmt_list):
            return tree
        stmt_list[index], stmt_list[index + 1] = stmt_list[index + 1], stmt_list[index]
        _ensure_non_empty(stmt_list, parent)
        return tree

    def describe(self, node: ast.AST) -> str:
        return f"AdjacentStatementSwapper@{getattr(node, 'lineno', '?')}"
