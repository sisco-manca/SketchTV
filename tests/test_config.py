import copy
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('configure',ROOT/'tools/configure.py')
config=importlib.util.module_from_spec(spec);spec.loader.exec_module(config)

class ConfigTest(unittest.TestCase):
    def setUp(self):self.c=json.loads((ROOT/'project.json').read_text())
    def rejects(self, change):
        change(self.c)
        with self.assertRaises((ValueError,KeyError,TypeError)):config.validate(self.c)
    def test_default(self):
        config.validate(self.c)
        self.assertTrue(all(not w['enabled'] for w in self.c['widgets'].values()))
    def test_overlap(self):self.rejects(lambda c:c['tiles'][1].update(rect=c['tiles'][2]['rect']))
    def test_focus_gap(self):self.rejects(lambda c:c['tiles'][4].update(rect=[456,617,347,204]))
    def test_out_of_bounds(self):self.rejects(lambda c:c['tiles'][0].update(rect=[0,0,2000,1080]))
    def test_duplicate_id(self):self.rejects(lambda c:c['tiles'][1].update(id=c['tiles'][0]['id']))
    def test_unknown_action(self):self.rejects(lambda c:c['tiles'][0].update(action='shell:delete'))
    def test_wrong_widget(self):self.rejects(lambda c:c['tiles'][0].update(action='widget:unknown'))
    def test_incomplete_provider(self):self.rejects(lambda c:c['widgets']['f1'].update(enabled=True))
    def test_rate(self):self.rejects(lambda c:c['widgets']['weather'].update(refreshMinutes=1))
    def test_coordinate(self):self.rejects(lambda c:c['widgets']['weather'].update(latitude=91))
    def test_nan(self):self.rejects(lambda c:c['tiles'][0].update(rect=[float('nan'),0,100,100]))
    def test_asset_traversal(self):self.rejects(lambda c:c['tiles'][0].update(asset='../key.jks'))
    def test_timezone(self):self.rejects(lambda c:c['clock'].update(timeZone='invalid/zone'))
    def test_bad_color(self):self.rejects(lambda c:c['theme'].update(accent='red'))
    def test_scale(self):self.rejects(lambda c:c['theme'].update(textScale=10))
    def test_version_boolean(self):self.rejects(lambda c:c.update(version=True))
    def test_neighbors(self):
        n=config.neighbors(self.c['tiles']);self.assertEqual(n[3][2],4);self.assertEqual(n[4][0],3)
        self.assertEqual(n[8][2],9);self.assertEqual(n[3][3],8)
        for row in n:self.assertTrue(all(0<=v<len(n) for v in row))
    def test_generated_single_geometry(self):
        self.c['tiles'][0]['rect']=[108,150,680,440]
        with tempfile.TemporaryDirectory() as d:
            config.generate(config.validate(self.c),Path(d))
            self.assertIn('108f,150f,680f,440f',(Path(d)/'LayoutSpec.java').read_text())
            self.assertIn('"Localit\\u00e0"',(Path(d)/'Config.java').read_text())
    def test_java_string_escaping(self):
        self.c['tiles'][0]['label']='A "quoted" label \\ example'
        with tempfile.TemporaryDirectory() as d:
            config.generate(config.validate(self.c),Path(d))
            self.assertIn('A \\"quoted\\" label \\\\ example',(Path(d)/'LayoutSpec.java').read_text())
if __name__=='__main__':unittest.main()
