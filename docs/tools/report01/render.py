"""Render locally and fail if any PlantUML source is invalid."""
from pathlib import Path
import tempfile,urllib.request,subprocess,hashlib,json
ROOT=Path(__file__).resolve().parents[3]
D=ROOT/'docs/architecture/diagrams/report-01'
cache=Path(tempfile.gettempdir())/'pcstore-report-tools';cache.mkdir(exist_ok=True)
jar=cache/'plantuml.jar'
url='https://repo.maven.apache.org/maven2/net/sourceforge/plantuml/plantuml/1.2025.4/plantuml-1.2025.4.jar'
if not jar.exists(): urllib.request.urlretrieve(url,jar)
cmd=['java','-Djava.awt.headless=true','-DPLANTUML_LIMIT_SIZE=16000','-jar',str(jar),'-charset','UTF-8','-failfast2','-tpng','-o','../png',str(D/'source/*.puml')]
result=subprocess.run(cmd,capture_output=True,text=True,encoding='utf-8',errors='replace')
log=dict(renderer='PlantUML 1.2025.4',source=url,jar_sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),returncode=result.returncode,stdout=result.stdout,stderr=result.stderr)
(D/'render-log.json').write_text(json.dumps(log,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(result.stdout,result.stderr)
if result.returncode: raise SystemExit(result.returncode)
manifest=json.loads((D/'manifest.json').read_text(encoding='utf-8'))
for entry in manifest:
    p=D/'png'/f'{entry["id"]}.png'
    assert p.exists() and p.stat().st_size>1000,p
print('Rendered',len(manifest),'PNG diagrams')
