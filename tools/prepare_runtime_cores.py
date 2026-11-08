#!/usr/bin/env python3
"""Use the source-built, pinned core release; require all seven libraries per ABI."""
import hashlib,json,os,pathlib,subprocess,tempfile,zipfile,shutil,sys
root=pathlib.Path(__file__).resolve().parents[1]
lock=json.loads((root/'config/cores.json').read_text())
abis=sys.argv[1:] or ['arm64-v8a','armeabi-v7a','x86','x86_64']
with tempfile.TemporaryDirectory() as temp:
    for abi in abis:
        name='cores-'+abi+'.zip'
        subprocess.run(['gh','release','download',lock['release'],'--repo','Randy3m2/tirsolador','--pattern',name,'--dir',temp],check=True)
        with zipfile.ZipFile(pathlib.Path(temp)/name) as archive:
            actual=json.loads(archive.read('cores.json'))
            if actual!=lock:raise RuntimeError('Core release does not match config/cores.json')
            folder=root/'app/src/main/jniLibs'/abi;folder.mkdir(parents=True,exist_ok=True)
            for key in lock['cores']:
                name='libtirso_'+key+'.so';data=archive.read(name)
                if data[:4]!=b'\x7fELF':raise RuntimeError('Invalid native core '+name)
                (folder/name).write_bytes(data)
shutil.copy2(root/'config/cores.json',root/'app/src/main/assets/cores.json')
