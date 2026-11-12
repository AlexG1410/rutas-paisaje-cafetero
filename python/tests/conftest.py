import sys
from pathlib import Path

PYTHON = Path(__file__).resolve().parents[1]
if str(PYTHON) not in sys.path:
    sys.path.insert(0, str(PYTHON))
