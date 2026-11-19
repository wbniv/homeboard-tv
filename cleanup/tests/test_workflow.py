from pathlib import Path
import tempfile
import unittest
from cleanup.tests.test_core import TV
from cleanup.workflow import Workflow
from cleanup.core import Refused

class WorkflowTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
        self.tv=TV();self.profile={'approved':True,'revision':'fixture','source_commit':'fixture',
          'match':self.tv.state['device'],'packages':[{'package':'org.example.promotions','label':'Fixture','reason':'Rehearsal'}]}
        self.flow=Workflow(None,Path(self.tmp.name),[self.profile],lambda device:self.tv)
    def test_review_apply_check_export_restart_restore(self):
        self.assertFalse(self.flow.inspect('tv')['candidates'][0]['selected'])
        plan=self.flow.prepare('tv',['org.example.promotions'])
        result=self.flow.apply(plan['token'],plan['confirmation']);key=result['session']
        self.assertEqual(self.flow.check('tv',key)['items']['org.example.promotions'],'disabled')
        self.assertEqual(self.flow.export(key)['status'],'applied')
        restarted=Workflow(None,Path(self.tmp.name),[self.profile],lambda device:self.tv)
        self.assertEqual(restarted.sessions()[0]['session'],key)
        self.assertEqual(restarted.restore('tv',key)['record']['status'],'restored')
        self.assertEqual(self.tv.state['packages']['org.example.promotions']['enabled'],0)
        with self.assertRaises(Refused):self.flow.apply(plan['token'],plan['confirmation'])
    def test_wrong_confirmation_and_unknown_profile_do_not_mutate(self):
        with self.assertRaises(Refused):self.flow.prepare('tv',[{}])
        p=self.flow.prepare('tv',['org.example.promotions'])
        with self.assertRaises(Refused):self.flow.apply(p['token'],'wrong')
        self.assertEqual(self.tv.calls,[])
        self.flow.profiles=[]
        self.assertEqual(self.flow.inspect('tv')['candidates'],[])
    def test_lost_reply_is_reconciled_before_restore(self):
        p=self.flow.prepare('tv',['org.example.promotions']);self.tv.fail='after'
        with self.assertRaises(ConnectionError):self.flow.apply(p['token'],p['confirmation'])
        key=self.flow.sessions()[0]['session'];self.tv.fail=None
        self.assertEqual(self.flow.check('tv',key)['items']['org.example.promotions'],'disabled')
        self.assertEqual(self.flow.restore('tv',key)['record']['status'],'restored')

    def test_stop_before_dispatch_preserves_every_app(self):
        plan=self.flow.prepare('tv',['org.example.promotions']);self.flow.stop_event.set()
        result=self.flow.apply(plan['token'],plan['confirmation'])
        self.assertEqual(result['record']['status'],'stopped');self.assertEqual(self.tv.calls,[])
