import copy
import json
from pathlib import Path
import tempfile
import unittest
from normalize import normalize, write
from assemble import assemble

class PipelineTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name)
        self.providers=self.root/'providers';self.out=self.root/'snapshot.json'
        self.facts=Path(__file__).parent/'reviewed'
    def tearDown(self):self.temp.cleanup()
    def test_replay_is_deterministic_and_has_all_six_providers(self):
        normalize(self.facts,self.providers);assemble(self.providers,self.out)
        before=self.out.read_bytes()
        normalize(self.facts,self.providers);assemble(self.providers,self.out)
        self.assertEqual(before,self.out.read_bytes())
        self.assertEqual(6,len(json.loads(before)['providers']))
    def test_packaged_snapshot_matches_reviewed_provider_facts(self):
        normalize(self.facts,self.providers);assemble(self.providers,self.out)
        packaged=Path(__file__).resolve().parents[2]/'launcher/app/src/main/assets/calendar-snapshot.json'
        self.assertEqual(packaged.read_bytes(),self.out.read_bytes())
    def test_bad_handoff_preserves_last_good_snapshot(self):
        normalize(self.facts,self.providers);assemble(self.providers,self.out)
        original=self.out.read_bytes()
        path=self.providers/'usa-calendar.json';provider=json.loads(path.read_text())
        invalid=copy.deepcopy(provider);invalid['events'][0]['url']='http://example.org'
        write(path,invalid)
        with self.assertRaises(ValueError):assemble(self.providers,self.out)
        self.assertEqual(original,self.out.read_bytes())
    def test_duplicate_and_out_of_coverage_are_rejected(self):
        normalize(self.facts,self.providers);path=self.providers/'usa-calendar.json'
        provider=json.loads(path.read_text());provider['events'].append(provider['events'][0]);write(path,provider)
        with self.assertRaises(ValueError):assemble(self.providers,self.out)
        provider['events'].pop();provider['events'][0]['date']='1900-01-01';write(path,provider)
        with self.assertRaises(ValueError):assemble(self.providers,self.out)
    def test_china_periods_and_working_days_are_not_conflated(self):
        normalize(self.facts,self.providers)
        provider=json.loads((self.providers/'china-calendar.json').read_text())
        days={e['date']:e for e in provider['events']}
        self.assertEqual('HOLIDAY',days['2026-01-03']['kind'])
        self.assertEqual('WORKING',days['2026-01-04']['kind'])
    def test_municipality_and_anchors_are_retained(self):
        normalize(self.facts,self.providers)
        provider=json.loads((self.providers/'spain-calendar.json').read_text())
        local=[e['date'] for e in provider['events'] if e['scope']=='tivissa']
        self.assertEqual(['2026-07-23','2026-07-24'],local)
        flowers=json.loads((self.providers/'flowers-calendar.json').read_text())
        event=next(e for e in flowers['events'] if e['date']=='2026-11-29')
        self.assertEqual('2026-12-02',event['anchor'])
if __name__=='__main__':unittest.main()
