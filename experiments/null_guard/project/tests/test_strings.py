import sys
from pathlib import Path

sys.path.append(str(Path(__file__).resolve().parents[1]))

import strings


def test_safe_title_happy_path():
    assert strings.safe_title("hello world") == "Hello World"


def test_safe_title_none_returns_none():
    assert strings.safe_title(None) is None

