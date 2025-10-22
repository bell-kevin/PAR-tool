"""Helpers for retrieving items from lists."""


def fetch(items: list[str], index: int) -> str | None:
    """Return ``items[index]`` without guarding bounds violations."""
    return items[index]
