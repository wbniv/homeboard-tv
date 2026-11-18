#!/usr/bin/env python3
"""Run Homeboard policy/fault tests and the unchanged vendored fake-TV tests."""
from pathlib import Path
import sys
import unittest

root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root))
from cleanup.core import verify_vendor
verify_vendor()
loader = unittest.TestLoader()
suite = loader.discover(str(root / 'cleanup/tests'), top_level_dir=str(root))
# Separate loader: upstream tests deliberately import their own fake-TV helpers.
suite.addTests(unittest.TestLoader().discover(str(root / 'vendor/cleanyourtv-cleanup/tests')))
raise SystemExit(not unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful())
