#!/usr/bin/env python3
"""Validate the shared project file and generate Java; no third-party dependencies."""
import argparse
import json
import math
import re
from pathlib import Path
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

ROOT = Path(__file__).resolve().parents[1]
KINDS = {"clock", "widget", "app", "source", "control"}
SIMPLE_ACTIONS = {"apps", "tv", "av", "hdmi1", "hdmi2", "sources", "settings", "network"}

def require(condition, message):
    if not condition:
        raise ValueError(message)

def number(value):
    return type(value) in (int, float) and math.isfinite(value)

def validate(c):
    require(type(c) is dict and type(c.get("version")) is int and c.get("version") == 1, "Unsupported project version")
    canvas = c["canvas"]
    require(canvas["width"] == 1920 and canvas["height"] == 1080, "Canvas must be 1920 x 1080")
    margin = canvas["safeMargin"]
    require(number(margin) and 8 <= margin <= 120, "safeMargin must be 8..120")
    theme = c["theme"]
    for k in ("background", "backgroundEnd", "text", "muted", "card", "cardText", "accent"):
        require(bool(re.fullmatch(r"#[0-9a-fA-F]{6}", theme[k])), "Invalid color: " + k)
    require(number(theme["radius"]) and 0 <= theme["radius"] <= 60, "radius must be 0..60")
    require(number(theme["textScale"]) and .75 <= theme["textScale"] <= 1.4, "textScale must be .75..1.4")
    try:
        ZoneInfo(c["clock"]["timeZone"])
    except (ZoneInfoNotFoundError, ValueError, TypeError):
        raise ValueError("Use a valid IANA clock timeZone")
    require(bool(re.fullmatch(r"[a-z]{2,3}(?:-[A-Z]{2})?", c["clock"]["locale"])), "Use a locale such as it-IT or en-US")
    widgets = c["widgets"]
    require(set(widgets) == {"weather", "f1", "football"}, "Register additional widget types in the provider registry and validator first")
    for key, w in widgets.items():
        require(type(w["enabled"]) is bool, "enabled must be boolean: " + key)
        require(type(w["refreshMinutes"]) is int and 10 <= w["refreshMinutes"] <= 1440, "refreshMinutes must be 10..1440: " + key)
        for field in (("city",) if key == "weather" else ("name",)):
            require(type(w[field]) is str and 0 < len(w[field]) <= 60 and not any(ord(x) < 32 for x in w[field]), "Invalid widget label")
    w = widgets["weather"]
    require(number(w["latitude"]) and -90 <= w["latitude"] <= 90, "Invalid latitude")
    require(number(w["longitude"]) and -180 <= w["longitude"] <= 180, "Invalid longitude")
    for key, fields in (("f1", ("constructorId",)), ("football", ("teamId", "leagueId"))):
        for field in fields:
            value = widgets[key][field]
            require(type(value) is str and (value == "" or bool(re.fullmatch(r"[a-zA-Z0-9_.-]{1,60}", value))), "Invalid provider ID: " + field)
            require(not widgets[key]["enabled"] or value != "", "Set " + field + " before enabling " + key)
    tiles = c["tiles"]
    require(type(tiles) is list and 1 <= len(tiles) <= 40, "Use 1..40 tiles")
    ids = set()
    for t in tiles:
        require(type(t["id"]) is str and bool(re.fullmatch(r"[a-z][a-z0-9_-]{0,39}", t["id"])) and t["id"] not in ids, "Invalid or duplicate tile ID")
        ids.add(t["id"])
        require(t["kind"] in KINDS, "Unknown tile kind")
        require(type(t["label"]) is str and 0 < len(t["label"]) <= 60 and not any(ord(x) < 32 for x in t["label"]), "Invalid tile label")
        r = t["rect"]
        require(type(r) is list and len(r) == 4 and all(number(x) for x in r), "rect is [x,y,width,height]")
        x,y,w,h = r
        require(w >= 100 and h >= 70 and x >= margin and y >= margin and x+w <= 1920-margin and y+h <= 1080-margin, "Tile outside safe area or too small: " + t["id"])
        if "color" in t:
            require(bool(re.fullmatch(r"#[0-9a-fA-F]{6}", t["color"])), "Invalid tile color")
        action = t["action"]
        require(type(action) is str and (action in SIMPLE_ACTIONS or action.startswith("widget:") and action[7:] in widgets or bool(re.fullmatch(r"app:(?:[a-zA-Z][a-zA-Z0-9_]*(?:\.[a-zA-Z][a-zA-Z0-9_]*)+)?", action))), "Invalid tile action: " + str(action))
        require((t["kind"] not in ("clock", "widget")) or action.startswith("widget:"), "Clock/widget tiles need a widget action")
        if "asset" in t:
            require(type(t["asset"]) is str and bool(re.fullmatch(r"[a-zA-Z0-9_-]+\.png", t["asset"])), "Assets must be local PNG basenames")
        for prev in tiles[:tiles.index(t)]:
            a,b,cw,ch = prev["rect"]
            # Leave space for the focus ring, as well as checking the content bounds.
            require(x+w+12 <= a or a+cw+12 <= x or y+h+12 <= b or b+ch+12 <= y, "Tiles overlap or leave insufficient focus space: " + t["id"] + "/" + prev["id"])
    graph=neighbors(tiles)
    for start in range(len(tiles)):
        seen={start}; pending=[start]
        while pending:
            for target in graph[pending.pop()]:
                if target not in seen: seen.add(target); pending.append(target)
        require(len(seen)==len(tiles), "Some tiles are unreachable with the remote: adjust their positions")
    return c

def neighbors(tiles):
    result=[]
    for i,t in enumerate(tiles):
        x,y,w,h=t["rect"]; cx,cy=x+w/2,y+h/2; row=[]
        for direction in ("left","up","right","down"):
            candidates=[]
            for j,o in enumerate(tiles):
                if i==j: continue
                a,b,c,d=o["rect"]; dx,dy=a+c/2-cx,b+d/2-cy
                primary = -dx if direction=="left" else dx if direction=="right" else -dy if direction=="up" else dy
                secondary = abs(dy) if direction in ("left","right") else abs(dx)
                overlap = min(y+h,b+d)-max(y,b) > 0 if direction in ("left","right") else min(x+w,a+c)-max(x,a) > 0
                if primary > 0:
                    candidates.append((0 if overlap else 1, primary+secondary*2, j))
            row.append(min(candidates)[2] if candidates else i)
        result.append(row)
    return result

def java_string(s):
    return json.dumps(s, ensure_ascii=True)

def generate(c, output):
    output.mkdir(parents=True, exist_ok=True)
    w=c["widgets"]; theme=c["theme"]
    fields={"CITY":w["weather"]["city"],"TIME_ZONE":c["clock"]["timeZone"],"LOCALE":c["clock"]["locale"],"LATITUDE":str(w["weather"]["latitude"]),"LONGITUDE":str(w["weather"]["longitude"]),"CONSTRUCTOR_ID":w["f1"]["constructorId"],"CONSTRUCTOR_NAME":w["f1"]["name"],"TEAM_ID":w["football"]["teamId"],"TEAM_NAME":w["football"]["name"],"LEAGUE_ID":w["football"]["leagueId"]}
    s="package dev.sketchtv.home;\n/** Generated from project.json; edit that file, not this class. */\nfinal class Config {\n"
    for k,v in fields.items(): s+=f" static final String {k}={java_string(v)};\n"
    for key in ("weather","f1","football"):
        s+=f' static final boolean {key.upper()}_ENABLED={str(w[key]["enabled"]).lower()};\n'
        s+=f' static final long {key.upper()}_INTERVAL={w[key]["refreshMinutes"]}*60000L;\n'
    for key,v in theme.items():
        if type(v) is str: s+=f' static final int {re.sub(r"(?<!^)(?=[A-Z])", "_", key).upper()}=0xFF{v[1:]};\n'
        else:s+=f' static final float {re.sub(r"(?<!^)(?=[A-Z])", "_", key).upper()}={v}f;\n'
    (output/'Config.java').write_text(s+'}\n')
    s='''package dev.sketchtv.home;
final class LayoutSpec {
 static final class Tile {
  final String id,kind,label,action,asset; final float x,y,w,h; final int color;
  Tile(String id,String kind,String label,String action,String asset,float x,float y,float w,float h,int color){this.id=id;this.kind=kind;this.label=label;this.action=action;this.asset=asset;this.x=x;this.y=y;this.w=w;this.h=h;this.color=color;}
 }
 static final Tile[] TILES={
'''
    for t in c['tiles']:
        args=','.join(java_string(t.get(k,'')) for k in ('id','kind','label','action','asset'))
        args+=','+','.join(str(n)+'f' for n in t['rect'])+',0xFF'+t.get('color',theme['card'])[1:]
        s+='  new Tile('+args+'),\n'
    s+=' };\n static final int[][] NEIGHBORS={'+','.join('{'+','.join(map(str,r))+'}' for r in neighbors(c['tiles']))+'};\n}\n'
    (output/'LayoutSpec.java').write_text(s)

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('project',nargs='?',default=str(ROOT/'project.json'))
    parser.add_argument('--output',default=str(ROOT/'out/gen/dev/sketchtv/home'))
    parser.add_argument('--check',action='store_true')
    args=parser.parse_args()
    try:
        c=validate(json.loads(Path(args.project).read_text()))
        if not args.check: generate(c,Path(args.output))
        print('Project valid: '+str(len(c['tiles']))+' tiles, explicit focus links, widget settings checked')
    except (ValueError,KeyError,TypeError,OSError) as e:
        parser.exit(1,'Configuration error: '+str(e)+'\n')
if __name__=='__main__': main()
