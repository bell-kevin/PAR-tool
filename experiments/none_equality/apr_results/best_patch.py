"""Helpers that inspect optional status tokens."""


def is_missing(token) -> bool:
    """Return True when ``token`` is ``None``.

    The buggy version uses equality comparison, which triggers ``__eq__`` on
    arbitrary tokens. Some tokens raise during comparison, so the repair tool
    must normalize the check to ``is None``.
    """
    if token is None:  # noqa: E711
        return True
    return False
