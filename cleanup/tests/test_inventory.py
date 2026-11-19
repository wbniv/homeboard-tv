import unittest
from cleanup.inventory import parse_packages
from cleanup.core import Refused

DUMP='''Packages:
  Package [org.example.promotions] (abc):
    userId=10012
    versionCode=3 minSdk=24
    User 0: installed=true hidden=false enabled=0
  Package [net.biohack.homeboard.debug] (def):
    appId=10013
    versionCode=69 minSdk=26
    User 0: installed=true hidden=false enabled=1
'''
LIST='package:org.example.promotions\npackage:net.biohack.homeboard.debug'
class InventoryTest(unittest.TestCase):
    def test_exact_states_and_unknown_dependencies(self):
        value=parse_packages(LIST,DUMP)
        self.assertEqual(value['org.example.promotions']['enabled'],0)
        self.assertEqual(value['net.biohack.homeboard.debug']['uid'],10013)
        self.assertFalse(value['org.example.promotions']['dependencies_verified'])
    def test_missing_state_version_user_or_package_refused(self):
        for text in [DUMP.replace('enabled=0',''),DUMP.replace('versionCode=3','versionName=3'),DUMP.replace('User 0:','User 10:'),DUMP.replace('installed=true','installed=false'),DUMP.split('  Package [net.')[0]]:
            with self.subTest(text=text),self.assertRaises(Refused):parse_packages(LIST,text)
    def test_duplicate_and_malformed_reports_refused(self):
        for listing,text in [(LIST,DUMP+DUMP),(LIST+'\ninvalid',DUMP),('',DUMP)]:
            with self.assertRaises(Refused):parse_packages(listing,text)

class CollectorTest(unittest.TestCase):
    def transport(self):
        class Fake:
            connections={'one':{'address':'192.168.1.9:33333','serial':'tv-1','firmware':'fixture/1'}}
            user='0'
            def identity(self,address):return dict(serial='tv-1',firmware='fixture/1',manufacturer='Example',model='Fixture',product='fixture',board='fixture')
            def command(self,*args):
                command=args[3:]
                if command==('am','get-current-user'):return self.user
                if command==('pm','list','packages','--user','0'):return LIST
                if command==('dumpsys','package'):return DUMP
                if command[0:3]==('cmd','package','resolve-activity'):return 'net.biohack.homeboard.debug/com.gathin.tvhome.MainActivity'
                if command[0:3]==('cmd','package','query-activities'):return 'net.biohack.homeboard.debug/com.gathin.tvhome.MainActivity'
                if command[0]=='ps':return 'UID NAME\n10012 org.example.promotions\n'
                raise AssertionError(command)
        return Fake()
    def test_complete_collection_retains_active_and_home_roles(self):
        from cleanup.inventory import Executor
        value=Executor(self.transport(),'one').inspect()
        self.assertEqual(value['device']['id'],'tv-1')
        self.assertIn('home',value['packages']['net.biohack.homeboard.debug']['roles'])
        self.assertIn('active process',value['packages']['org.example.promotions']['roles'])
        self.assertFalse(value['packages']['org.example.promotions']['dependencies_verified'])
    def test_other_user_and_changed_identity_refused(self):
        from cleanup.inventory import Executor
        transport=self.transport();transport.user='10'
        with self.assertRaises(Refused):Executor(transport,'one').inspect()
        transport=self.transport();transport.connections={'one':dict(transport.connections['one'],serial='other')}
        with self.assertRaises(Refused):Executor(transport,'one').inspect()
