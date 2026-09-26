"""Generate/reopen the native 13 UML diagrams; export native VP XML via official CLI.
Requires installed Visual Paradigm CE18 and an existing valid seed project.
Usage: python docs/tools/report01/run_vp.py [--verify-only]
Never edits the user's other projects. A temp copy of the delivered project is the seed.
"""
from pathlib import Path
import os,shutil,subprocess,tempfile,json,sys
ROOT=Path(__file__).resolve().parents[3]
D=ROOT/'docs/architecture/diagrams/report-01'
VP=Path(os.environ.get('PCSTORE_VP_HOME',r'C:\Program Files\Visual Paradigm CE 18.0'))
cache=Path(tempfile.gettempdir())/'pcstore-report-tools';cache.mkdir(exist_ok=True)
source=Path(__file__).parent/'vp'
plugin=Path(os.environ['APPDATA'])/'VisualParadigm/plugins/pcstore.report01';plugin.mkdir(parents=True,exist_ok=True)
def run(cmd,cwd=ROOT):
 r=subprocess.run([str(x) for x in cmd],cwd=cwd,capture_output=True,encoding='utf-8',errors='replace',timeout=120)
 if r.returncode: raise RuntimeError(r.stdout+'\n'+r.stderr)
 return r.stdout+'\n'+r.stderr
run(['javac','-encoding','UTF-8','--release','8','-cp',VP/'lib/openapi.jar','-d',cache,source/'ReportPlugin.java'])
run(['jar','cf',cache/'report01.jar','-C',cache,'pcstore'])
shutil.copy2(cache/'report01.jar',plugin/'report01.jar');shutil.copy2(source/'plugin.xml',plugin/'plugin.xml')
java=[VP/'jre/bin/java.exe','-Djava.awt.headless=true','-Dfile.encoding=UTF-8','-cp','..\\lib\\*;..\\ormlib\\*']
project=D/'PC_STORE_REPORT_01.vpp'
logs={}
if '--verify-only' not in sys.argv:
 seed=cache/'regeneration-seed.vpp';shutil.copy2(project,seed)
 logs['generate']=run(java+['com.vp.cmd.Plugin','-project',seed,'-pluginid','pcstore.report01','-pluginargs',str(project)+' '+str(D/'model-input.xml')],VP/'bin')
 assert 'REPORT01_OK' in logs['generate'],logs['generate']
logs['reopen']=run(java+['com.vp.cmd.Plugin','-project',project,'-pluginid','pcstore.report01','-pluginargs',str(project)+' verify'],VP/'bin')
assert 'REPORT01_REOPEN_OK diagrams=13' in logs['reopen'],logs['reopen']
logs['export']=run(java+['com.vp.cmd.ExportXML','-project',project,'-out',D/'vp-export','-noimage'],VP/'bin')
assert (D/'vp-export/project.xml').exists()
(D/'vp-build-log.json').write_text(json.dumps(logs,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('VP generated/reopened: 13 UML diagrams; native XML exported. ERD: PlantUML + PNG.')
