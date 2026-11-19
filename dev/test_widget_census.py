import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('census', Path(__file__).with_name('read-widget-census.py'))
census = importlib.util.module_from_spec(spec)
spec.loader.exec_module(census)


class CensusEvidenceTest(unittest.TestCase):
    def test_missing_result_is_not_empty_census(self):
        with self.assertRaises(ValueError):
            census.extract('Activity started; no result logged')

    def test_truncated_provider_evidence_is_rejected(self):
        with self.assertRaises(ValueError):
            census.extract('I/HB_WIDGET_CENSUS: summary={"schema":1,"providerCount":2}')

    def test_last_completed_observation_wins(self):
        text = '\n'.join([
            'I/HB_WIDGET_CENSUS: provider={"component":"old/widget"}',
            'I/HB_WIDGET_CENSUS: summary={"schema":1,"providerCount":1}',
            'I/HB_WIDGET_CENSUS: summary={"schema":1,"providerCount":0,"outcome":"new-binding-unavailable"}',
        ])
        self.assertEqual([], census.extract(text)['providers'])

    def test_unrelated_json_is_not_a_probe_result(self):
        with self.assertRaises(ValueError):
            census.extract('I/OtherApp: summary={"schema":1,"providerCount":0}')


if __name__ == '__main__':
    unittest.main()
