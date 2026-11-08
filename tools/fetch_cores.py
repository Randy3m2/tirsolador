#!/usr/bin/env python3
"""Fetch exact source revisions; never follow a floating branch during a build."""
import json,pathlib,subprocess,sys,concurrent.futures
ROOT=pathlib.Path(__file__).resolve().parents[1]
LOCK=json.loads((ROOT/'config/cores.json').read_text())
def fetch(item):
    name,core=item;dest=ROOT/'vendor'/name
    if not (dest/'.git').exists():
        dest.mkdir(parents=True,exist_ok=True)
        subprocess.run(['git','init',str(dest)],check=True,stdout=subprocess.DEVNULL)
        subprocess.run(['git','-C',str(dest),'remote','add','origin','https://github.com/'+core['repo']+'.git'],check=True)
    current=subprocess.run(['git','-C',str(dest),'rev-parse','HEAD'],capture_output=True,text=True)
    if current.stdout.strip()!=core['rev']:
        subprocess.run(['git','-C',str(dest),'fetch','--depth','1','origin',core['rev']],check=True)
        subprocess.run(['git','-C',str(dest),'checkout','--detach',core['rev']],check=True)
    if (dest/'.gitmodules').exists():subprocess.run(['git','-C',str(dest),'submodule','update','--init','--recursive','--depth','1'],check=True)
    assert subprocess.check_output(['git','-C',str(dest),'rev-parse','HEAD'],text=True).strip()==core['rev']
    print(name,core['rev'],flush=True)
if __name__=='__main__':
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:list(executor.map(fetch,LOCK['cores'].items()))
