import sys
from pathlib import Path

sys.path.append(str(Path(__file__).resolve().parents[1]))

import list_access


def test_fetch_valid_index():
    assert list_access.fetch(["alpha", "beta", "gamma"], 1) == "beta"


def test_fetch_out_of_range_returns_none():
    assert list_access.fetch(["only"], 3) is None


def test_fetch_negative_index_returns_none():
    assert list_access.fetch(["zero", "one"], -1) is None

