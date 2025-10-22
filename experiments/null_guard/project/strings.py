"""Utility helpers for string normalization."""


def safe_title(text: str | None) -> str | None:
    """Return ``text.title()`` while tolerating ``None`` inputs.

    The buggy variant dereferences ``None`` without checking it, leading to an
    ``AttributeError`` when ``text`` is ``None``. The repair tool is expected to
    wrap the call in a null guard so ``None`` propagates cleanly.
    """
    return text.title()
