#!/usr/bin/env python3
"""Inspect, plan and reversibly tune a user-selected Android TV. No auto-discovery or package removal."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import tempfile
from datetime import datetime, timezone

ROOT=Path(__file__).resolve().parents[1]
# Deliberately small allowlist. No arbitrary settings, package changes or screensaver control.
ALLOWED={
    'global:window_animation_scale':('0','Riduce l’animazione delle finestre'),
    'global:transition_animation_scale':('0','Riduce le transizioni fra schermate'),
    'global:animator_duration_scale':('0','Riduce le animazioni degli elementi'),
    'secure:sleep_timeout':('300000','Richiede sospensione dopo 5 minuti; il firmware può interpretarla diversamente'),
}
READ_SETTINGS=tuple(ALLOWED)+('secure:screensaver_enabled','system:screen_off_timeout')

class DeviceError(Exception):pass

def private_output(path):
    resolved=Path(path).resolve()
    try:relative=resolved.relative_to(ROOT)
    except ValueError:return resolved
    if not relative.parts or relative.parts[0]!='.local':raise DeviceError('Salva i dati del dispositivo sotto .local/ (ignorata da Git), oppure fuori dal checkout')
    return resolved

def atomic_json(path,value):
    path=private_output(path);path.parent.mkdir(parents=True,exist_ok=True,mode=0o700)
    handle,name=tempfile.mkstemp(prefix='.sketchtv-',dir=path.parent)
    try:
        with os.fdopen(handle,'w') as stream:
            os.fchmod(stream.fileno(),0o600)
            json.dump(value,stream,ensure_ascii=False,indent=2);stream.write('\n');stream.flush();os.fsync(stream.fileno())
        os.replace(name,path)
        if os.name=='posix':
            directory=os.open(path.parent,os.O_RDONLY)
            try:os.fsync(directory)
            finally:os.close(directory)
    finally:
        if os.path.exists(name):os.unlink(name)

def read_json(path):
    with Path(path).open() as stream:value=json.load(stream)
    if type(value) is not dict:raise DeviceError('Il documento deve essere un oggetto JSON')
    return value

def timestamp():return datetime.now(timezone.utc).isoformat()

def parse_memory(raw):
    result={}
    for key,value in re.findall(r'^(MemTotal|MemFree|MemAvailable):\s+(\d+)\s+kB',raw,re.M):result[key]=int(value)
    if not result:raise DeviceError('Nessun contatore RAM valido restituito dal dispositivo')
    return result

class Adb:
    def __init__(self,serial,executable='adb'):
        if not serial or serial.startswith('-') or any(ord(c)<33 for c in serial):raise DeviceError('Seriale ADB non valido')
        self.serial=serial;self.executable=executable
    def run(self,*args):
        try:
            p=subprocess.run([self.executable,'-s',self.serial,*args],stdin=subprocess.DEVNULL,capture_output=True,text=True,timeout=25)
        except (OSError,subprocess.TimeoutExpired) as e:raise DeviceError('ADB non disponibile o comando scaduto') from e
        if p.returncode:raise DeviceError('Comando ADB fallito: '+p.stderr.strip()[:250])
        return p.stdout.replace('\r','').strip()
    def shell(self,*args):
        # ADB forwards a command to the remote shell. Quote each token as well as using a local argv.
        return self.run('shell','-n',' '.join(shlex.quote(str(a)) for a in args))
    def identity(self):
        if self.run('get-state')!='device':raise DeviceError('Dispositivo assente o non autorizzato')
        fingerprint=self.shell('getprop','ro.build.fingerprint')
        if not fingerprint:raise DeviceError('Impossibile identificare il dispositivo')
        unique=self.shell('getprop','ro.serialno')
        if not unique or unique.lower() in ('unknown','null'):
            unique=self.shell('settings','get','secure','android_id')
            if not re.fullmatch(r'[a-fA-F0-9]{16}',unique):raise DeviceError('Identificativo univoco non disponibile: nessuna modifica consentita')
        return hashlib.sha256((self.serial+'\0'+unique+'\0'+fingerprint).encode()).hexdigest()
    def get(self,key):
        namespace,name=key.split(':');value=self.shell('settings','get',namespace,name)
        return None if value=='null' else value
    def put(self,key,value):
        namespace,name=key.split(':')
        if value is None:self.shell('settings','delete',namespace,name)
        else:self.shell('settings','put',namespace,name,value)

def inspect(device,path):
    identity=device.identity()
    snapshot={'schema':1,'identity':identity,'captured':timestamp(),
        'properties':{key:device.shell('getprop',prop) for key,prop in [('manufacturer','ro.product.manufacturer'),('model','ro.product.model'),('android','ro.build.version.release'),('api','ro.build.version.sdk')]},
        'memory_kib':parse_memory(device.shell('cat','/proc/meminfo')),
        'settings':{key:device.get(key) for key in READ_SETTINGS},
        'storage':device.shell('df','-k','/data'), 'uptime':device.shell('cat','/proc/uptime')}
    # These are readings, never gfxinfo reset, force-stop, a launch or a reboot.
    for key,command in [('home_memory',('dumpsys','meminfo','dev.sketchtv.home')),('home_frames',('dumpsys','gfxinfo','dev.sketchtv.home'))]:
        try:snapshot[key]=device.shell(*command)
        except DeviceError:snapshot[key]='Non disponibile'
    atomic_json(path,snapshot)
    return snapshot

def make_plan(snapshot,animations=False,sleep=False):
    if snapshot.get('schema')!=1 or not re.fullmatch('[a-f0-9]{64}',snapshot.get('identity','')):raise DeviceError('Ispezione non valida')
    changes=[]
    for key,(target,reason) in ALLOWED.items():
        if not (animations if key.startswith('global:') else sleep):continue
        if key not in snapshot['settings']:raise DeviceError('Manca lo stato originale: '+key)
        old=snapshot['settings'][key]
        if not equivalent(key,old,target):changes.append({'key':key,'before':old,'after':target,'reason':reason})
    return {'schema':1,'identity':snapshot['identity'],'created':timestamp(),'changes':changes}

def equivalent(key,a,b):
    if a is None or b is None:return a is b
    if key.startswith('global:'):
        try:return float(a)==float(b)
        except (TypeError,ValueError):return False
    return str(a)==str(b)

def validate_plan(plan):
    if type(plan) is not dict:raise DeviceError('Piano non valido')
    if plan.get('schema')!=1 or not re.fullmatch('[a-f0-9]{64}',plan.get('identity','')):raise DeviceError('Piano non valido')
    if type(plan.get('changes')) is not list or len(plan['changes'])>len(ALLOWED):raise DeviceError('Elenco modifiche non valido')
    seen=set()
    for row in plan['changes']:
        if type(row) is not dict:raise DeviceError('Modifica non valida')
        key=row['key'];before=row['before']
        if key not in ALLOWED or key in seen or row['after']!=ALLOWED[key][0]:raise DeviceError('Il piano contiene una modifica non consentita')
        if before is not None and (type(before) is not str or not re.fullmatch(r'-?\d+(?:\.\d+)?',before)):raise DeviceError('Valore originale non valido')
        seen.add(key)
    return plan

def confirm(message):
    try:return input(message+' Scrivi SI: ').strip()=='SI'
    except EOFError:return False

def apply(device,plan,journal_path,ask=confirm):
    validate_plan(plan)
    private_output(journal_path)
    if Path(journal_path).exists():raise DeviceError('Registro già presente: usa undo o un nome nuovo; non sovrascrivere il ripristino')
    if device.identity()!=plan['identity']:raise DeviceError('Il piano appartiene a un altro dispositivo/firmware/indirizzo ADB')
    journal={'schema':1,'identity':plan['identity'],'created':timestamp(),'entries':[]}
    for row in plan['changes']:
        key=row['key'];current=device.get(key)
        if not equivalent(key,current,row['before']):raise DeviceError('Stato cambiato dopo l’ispezione: '+key+'; crea un nuovo piano')
        print(f"{key}: {current!r} → {row['after']!r}. {ALLOWED[key][1]}")
        if not ask('Confermi questa singola modifica?'):continue
        if not equivalent(key,device.get(key),current):raise DeviceError('Stato cambiato durante la conferma: '+key)
        entry={'key':key,'before':current,'after':row['after'],'status':'pending'}
        journal['entries'].append(entry)
        atomic_json(journal_path,journal) # Persist BEFORE the remote write, including absent/null values.
        device.put(key,row['after'])
        if not equivalent(key,device.get(key),row['after']):raise DeviceError('Verifica non riuscita; conserva il registro per undo: '+key)
        entry['status']='applied';atomic_json(journal_path,journal)
    return journal

def undo(device,journal_path,ask=confirm):
    private_output(journal_path)
    journal=read_json(journal_path)
    if any(entry.get('status') not in ('pending','applied','undone') for entry in journal['entries']):raise DeviceError('Stato del registro non valido')
    validate_plan({'schema':journal['schema'],'identity':journal['identity'],'changes':journal['entries']})
    if device.identity()!=journal['identity']:raise DeviceError('Registro di un altro dispositivo: ripristino fermato')
    for entry in reversed(journal['entries']):
        if entry.get('status')=='undone':continue
        key=entry['key'];current=device.get(key)
        if equivalent(key,current,entry['before']):entry['status']='undone';atomic_json(journal_path,journal);continue
        if not equivalent(key,current,entry['after']):raise DeviceError('Impostazione cambiata da altri: '+key+'; nessuna sovrascrittura automatica')
        if not ask(f"Ripristinare {key}: {current!r} → {entry['before']!r}?"):continue
        device.put(key,entry['before'])
        if not equivalent(key,device.get(key),entry['before']):raise DeviceError('Ripristino non verificato: '+key)
        entry['status']='undone';atomic_json(journal_path,journal)
    return journal

def compare(before,after):
    if before['identity']!=after['identity']:raise DeviceError('Le misure appartengono a dispositivi/firmware/indirizzi diversi')
    result=[]
    for key in ('MemFree','MemAvailable'):
        a=before['memory_kib'].get(key);b=after['memory_kib'].get(key)
        result.append(f'{key}: '+('non disponibile' if a is None or b is None else f'{a/1024:.1f} → {b/1024:.1f} MiB (differenza {(b-a)/1024:+.1f})'))
    return '\n'.join(result)+'\nSono istantanee: cache e app aperte influenzano i risultati. Non provano da sole un aumento di velocità.'

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial',help='Seriale esplicito da adb devices; nessuna connessione automatica')
    parser.add_argument('--adb',default=os.environ.get('ADB','adb'))
    sub=parser.add_subparsers(dest='command',required=True)
    p=sub.add_parser('inspect',help='Sola lettura; dati privati locali');p.add_argument('--output',default='.local/device/before.json')
    p=sub.add_parser('plan');p.add_argument('--snapshot',default='.local/device/before.json');p.add_argument('--output',default='.local/device/plan.json');p.add_argument('--animations-off',action='store_true');p.add_argument('--sleep-five-minutes',action='store_true')
    p=sub.add_parser('apply');p.add_argument('--plan',default='.local/device/plan.json');p.add_argument('--journal',default='.local/device/changes.json')
    p=sub.add_parser('undo');p.add_argument('--journal',default='.local/device/changes.json')
    p=sub.add_parser('compare');p.add_argument('before');p.add_argument('after')
    args=parser.parse_args()
    try:
        if args.command=='plan':
            plan=make_plan(read_json(args.snapshot),args.animations_off,args.sleep_five_minutes);validate_plan(plan);atomic_json(args.output,plan)
            print(json.dumps(plan['changes'],indent=2,ensure_ascii=False));print('Piano locale salvato. Nessuna modifica al dispositivo.')
        elif args.command=='compare':print(compare(read_json(args.before),read_json(args.after)))
        else:
            if not args.serial:raise DeviceError('Indica --serial SERIAL prima del comando')
            device=Adb(args.serial,args.adb)
            if args.command=='inspect':
                snapshot=inspect(device,args.output);print('Ispezione salvata localmente:',args.output);print('RAM disponibile (KiB):',snapshot['memory_kib'].get('MemAvailable','n/d'))
            elif args.command=='apply':apply(device,read_json(args.plan),args.journal)
            elif args.command=='undo':undo(device,args.journal)
            print('Nessuna app disabilitata o eliminata. Screensaver e Home non modificati. Spegni il debugging al termine delle verifiche.')
    except (DeviceError,OSError,ValueError,KeyError,TypeError) as e:parser.exit(1,'STOP: '+str(e)+'\n')
if __name__=='__main__':main()
