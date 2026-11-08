"""Regression tests use fake ADB only. Never contacts hardware."""
import copy
import io
import contextlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('device',ROOT/'tools/device.py')
device=importlib.util.module_from_spec(spec);spec.loader.exec_module(device)
IDENTITY='a'*64

class Fake:
    def __init__(self):
        self.settings={key:'1.0' for key in device.ALLOWED}
        self.settings['global:animator_duration_scale']=None
        self.settings['secure:sleep_timeout']='1800000'
        self.settings['secure:screensaver_enabled']='1'
        self.settings['system:screen_off_timeout']='300000'
        self.writes=[];self.id=IDENTITY;self.before_write=None
    def identity(self):return self.id
    def get(self,key):return self.settings.get(key)
    def put(self,key,value):
        if self.before_write:self.before_write(key,value)
        self.writes.append((key,value));self.settings[key]=value
    def shell(self,*args):
        if args==('cat','/proc/meminfo'):return 'MemTotal: 900000 kB\nMemFree: 100000 kB\nMemAvailable: 200000 kB'
        return 'synthetic example'
    def snapshot(self):return {'schema':1,'identity':self.id,'settings':copy.deepcopy(self.settings),'memory_kib':{'MemFree':100000,'MemAvailable':200000}}

class DeviceTest(unittest.TestCase):
    def setUp(self):
        silence=contextlib.redirect_stdout(io.StringIO());silence.__enter__();self.addCleanup(silence.__exit__,None,None,None)
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
        self.path=Path(self.tmp.name)/'journal.json';self.fake=Fake()
    def plan(self):return device.make_plan(self.fake.snapshot(),animations=True,sleep=True)
    def test_inspect_is_read_only(self):
        device.inspect(self.fake,Path(self.tmp.name)/'before.json');self.assertEqual(self.fake.writes,[])
    def test_no_implicit_plan(self):self.assertEqual(device.make_plan(self.fake.snapshot())['changes'],[])
    def test_plan_only_allowlisted(self):
        keys={r['key'] for r in self.plan()['changes']};self.assertEqual(keys,set(device.ALLOWED));self.assertNotIn('secure:screensaver_enabled',keys)
    def test_decline_every_change(self):
        asked=[];device.apply(self.fake,self.plan(),self.path,lambda message:asked.append(message) or False)
        self.assertEqual(len(asked),4);self.assertEqual(self.fake.writes,[]);self.assertFalse(self.path.exists())
    def test_confirm_individually(self):
        answers=iter([True,False,True,False]);device.apply(self.fake,self.plan(),self.path,lambda _:next(answers));self.assertEqual(len(self.fake.writes),2)
    def test_journal_before_write(self):
        def check(key,value):self.assertEqual(device.read_json(self.path)['entries'][-1]['status'],'pending')
        self.fake.before_write=check;device.apply(self.fake,self.plan(),self.path,lambda _:True)
        self.assertEqual(device.read_json(self.path)['entries'][0]['status'],'applied')
    def test_original_absent_restored(self):
        device.apply(self.fake,self.plan(),self.path,lambda _:True);self.assertEqual(self.fake.get('global:animator_duration_scale'),'0')
        device.undo(self.fake,self.path,lambda _:True);self.assertIsNone(self.fake.get('global:animator_duration_scale'));self.assertEqual(self.fake.get('secure:sleep_timeout'),'1800000')
        self.assertEqual(self.fake.get('secure:screensaver_enabled'),'1')
    def test_idempotent_undo(self):
        device.apply(self.fake,self.plan(),self.path,lambda _:True);device.undo(self.fake,self.path,lambda _:True);writes=len(self.fake.writes)
        device.undo(self.fake,self.path,lambda _:self.fail('No prompt when already restored'));self.assertEqual(len(self.fake.writes),writes)
    def test_wrong_device_apply(self):
        plan=self.plan();self.fake.id='b'*64
        with self.assertRaises(device.DeviceError):device.apply(self.fake,plan,self.path,lambda _:True)
        self.assertEqual(self.fake.writes,[])
    def test_wrong_device_undo(self):
        device.apply(self.fake,self.plan(),self.path,lambda _:True);self.fake.id='b'*64;writes=len(self.fake.writes)
        with self.assertRaises(device.DeviceError):device.undo(self.fake,self.path,lambda _:True)
        self.assertEqual(len(self.fake.writes),writes)
    def test_drift_before_apply(self):
        plan=self.plan();self.fake.settings['global:window_animation_scale']='2'
        with self.assertRaises(device.DeviceError):device.apply(self.fake,plan,self.path,lambda _:True)
        self.assertEqual(self.fake.writes,[])
    def test_drift_during_prompt(self):
        def ask(_):self.fake.settings['global:window_animation_scale']='2';return True
        with self.assertRaises(device.DeviceError):device.apply(self.fake,self.plan(),self.path,ask)
        self.assertEqual(self.fake.writes,[])
    def test_drift_undo(self):
        device.apply(self.fake,self.plan(),self.path,lambda _:True);self.fake.settings['secure:sleep_timeout']='600000';writes=len(self.fake.writes)
        with self.assertRaises(device.DeviceError):device.undo(self.fake,self.path,lambda _:True)
        self.assertEqual(len(self.fake.writes),writes)
    def test_crash_after_write_can_undo(self):
        def put(key,value):self.fake.settings[key]=value;raise device.DeviceError('Synthetic interrupted transport')
        original=self.fake.put;self.fake.put=put
        with self.assertRaises(device.DeviceError):device.apply(self.fake,self.plan(),self.path,lambda _:True)
        self.fake.put=original;self.assertEqual(device.read_json(self.path)['entries'][0]['status'],'pending')
        device.undo(self.fake,self.path,lambda _:True);self.assertEqual(self.fake.settings['global:window_animation_scale'],'1.0')
    def test_existing_journal_not_overwritten(self):
        self.path.write_text('keep')
        with self.assertRaises(device.DeviceError):device.apply(self.fake,self.plan(),self.path,lambda _:True)
        self.assertEqual(self.path.read_text(),'keep');self.assertEqual(self.fake.writes,[])
    def test_tampered_plan(self):
        plan=self.plan();plan['changes'][0]['key']='secure:enabled_accessibility_services'
        with self.assertRaises(device.DeviceError):device.apply(self.fake,plan,self.path,lambda _:True)
        self.assertEqual(self.fake.writes,[])
    def test_invalid_value(self):
        plan=self.plan();plan['changes'][0]['before']='1; dangerous'
        with self.assertRaises(device.DeviceError):device.validate_plan(plan)
    def test_unchanged_numeric_setting_skipped(self):
        self.fake.settings['global:window_animation_scale']='0.0';self.assertEqual(len(self.plan()['changes']),3)
    def test_private_outputs(self):
        with self.assertRaises(device.DeviceError):device.private_output(ROOT/'accidental-device.json')
        self.assertEqual(device.private_output(ROOT/'.local/device/report.json'),ROOT/'.local/device/report.json')
    def test_owner_only_file(self):
        device.atomic_json(self.path,{'test':True});self.assertEqual(self.path.stat().st_mode&0o777,0o600)
    def test_memory_comparison(self):
        before=self.fake.snapshot();after=copy.deepcopy(before);after['memory_kib']['MemAvailable']-=1024
        self.assertIn('-1.0',device.compare(before,after));self.assertIn('Non provano',device.compare(before,after))
    def test_memory_unavailable_not_fabricated(self):
        before=self.fake.snapshot();after=copy.deepcopy(before);del after['memory_kib']['MemAvailable']
        self.assertIn('non disponibile',device.compare(before,after))
    def test_compare_wrong_device(self):
        a=self.fake.snapshot();b=copy.deepcopy(a);b['identity']='b'*64
        with self.assertRaises(device.DeviceError):device.compare(a,b)
    def test_adb_quotes_and_detaches_stdin(self):
        class Result:returncode=0;stdout='ok';stderr=''
        with patch.object(device.subprocess,'run',return_value=Result()) as run:
            adb=device.Adb('synthetic-serial');adb.shell('getprop','a;b')
            args=run.call_args.args[0];self.assertEqual(args[:5],['adb','-s','synthetic-serial','shell','-n']);self.assertEqual(args[-1],"getprop 'a;b'")
            self.assertIs(run.call_args.kwargs['stdin'],device.subprocess.DEVNULL)
    def test_null_undo_uses_settings_delete(self):
        with patch.object(device.Adb,'shell',return_value='') as shell:
            device.Adb('synthetic').put('global:animator_duration_scale',None)
            shell.assert_called_once_with('settings','delete','global','animator_duration_scale')
if __name__=='__main__':unittest.main()
