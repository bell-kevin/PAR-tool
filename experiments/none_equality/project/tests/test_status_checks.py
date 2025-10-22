import sys
from pathlib import Path

sys.path.append(str(Path(__file__).resolve().parents[1]))

import pytest

import status_checks


class Explosive:
    def __eq__(self, other):  # pragma: no cover - behavior tested indirectly
        raise RuntimeError("comparison should not be invoked")


def test_is_missing_handles_none():
    assert status_checks.is_missing(None) is True


def test_is_missing_handles_value():
    assert status_checks.is_missing("ready") is False


def test_is_missing_skips_eq_overload():
    token = Explosive()
    with pytest.raises(RuntimeError):
        _ = token == None  # noqa: E711

    assert status_checks.is_missing(token) is False

