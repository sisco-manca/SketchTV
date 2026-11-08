#!/usr/bin/env python3
"""Local preview: whitelisted static files and validation only. Never contacts a TV."""
import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit, unquote
from configure import ROOT, validate, neighbors

class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass
    def respond(self, status, payload, content_type):
        self.send_response(status)
        self.send_header('Content-Type',content_type)
        self.send_header('Content-Length',str(len(payload)))
        self.send_header('Cache-Control','no-store')
        self.send_header('X-Content-Type-Options','nosniff')
        self.end_headers()
        self.wfile.write(payload)
    def do_GET(self):
        path=unquote(urlsplit(self.path).path)
        allowed={'/':'preview/index.html','/index.html':'preview/index.html','/preview.js':'preview/preview.js','/preview.css':'preview/preview.css','/project.json':'project.json'}
        if path in allowed:
            target=ROOT/allowed[path]
        elif path.startswith('/assets/') and '/' not in path[len('/assets/'):]:
            target=ROOT/path.lstrip('/')
            if target.suffix not in ('.png','.ttf'): return self.send_error(404)
        else:
            return self.send_error(404)
        if not target.is_file() or target.is_symlink(): return self.send_error(404)
        types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.json':'application/json; charset=utf-8','.ttf':'font/ttf','.png':'image/png'}
        self.respond(200,target.read_bytes(),types[target.suffix])
    def do_POST(self):
        if self.path!='/validate':return self.send_error(404)
        origin=self.headers.get('Origin')
        if origin!=f'http://127.0.0.1:{self.server.server_port}':return self.send_error(403)
        try:
            size=int(self.headers.get('Content-Length','0'))
            if not 0<size<=131072:raise ValueError('Project must be at most 128 KiB')
            config=validate(json.loads(self.rfile.read(size)))
            result={'valid':True,'neighbors':neighbors(config['tiles'])}
        except (ValueError,KeyError,TypeError) as e:
            result={'valid':False,'error':str(e)}
        self.respond(200,json.dumps(result).encode(),'application/json; charset=utf-8')

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port',type=int,default=8765)
    args=parser.parse_args()
    with ThreadingHTTPServer(('127.0.0.1',args.port),Handler) as server:
        print(f'Preview: http://127.0.0.1:{server.server_port} — Ctrl+C to stop',flush=True)
        server.serve_forever()
if __name__=='__main__':main()
