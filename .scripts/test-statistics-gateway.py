#!/usr/bin/env python3
"""Exercise real gateway configurations against disposable Nginx stub upstreams."""
import json
from pathlib import Path
import re
import subprocess
import tempfile
import time
from urllib.request import Request, urlopen
from uuid import uuid4

ROOT = Path(__file__).resolve().parents[1]
FILES = ('dbrepo-gateway-service/dbrepo.conf', 'helm/dbrepo/files/dbrepo.conf',
         '.live/config/includes.chroot_after_packages/etc/skel/config/dbrepo.conf')


def main():
    for source in FILES:
        config = (ROOT/source).read_text()
        for service, port in [('data', 18081), ('metadata', 18082)]:
            config = re.sub(r'(?<![\w-])'+service+r'-service(?::8080)?(?=[/;])', '127.0.0.1:'+str(port), config)
        config = re.sub(r'http://[a-z][a-z-]+(?::[0-9]+)?', 'http://127.0.0.1:18089', config)
        # The variable upstream selector must remain intact.
        for port, service in [(18081, 'data'), (18082, 'metadata')]:
            config += '\nserver { listen '+str(port)+'; return 200 \'{"service":"'+service+'","method":"$request_method","uri":"$request_uri"}\'; }\n'
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)/'default.conf'; path.write_text(config)
            name = 'dbrepo-statistics-check-'+uuid4().hex[:12]
            try:
                subprocess.run(['docker','run','-d','--name',name,'--rm','-p','127.0.0.1::8080',
                    '-v',str(path)+':/etc/nginx/conf.d/default.conf:ro','nginx:1.27.3-alpine3.20-slim'],check=True,stdout=subprocess.DEVNULL)
                host = json.loads(subprocess.check_output(['docker','inspect',name],text=True))[0]['NetworkSettings']['Ports']['8080/tcp'][0]['HostPort']
                base = 'http://127.0.0.1:'+host
                for attempt in range(40):
                    try:
                        with urlopen(base+'/health',timeout=2) as response: assert response.status==200
                        break
                    except OSError: time.sleep(.2)
                database, table = str(uuid4()), str(uuid4())
                prefix='/api/v1/database/'+database+'/table/'+table
                for suffix, method, target in [('statistic','GET','data'),('statistic?sample=1','PUT','metadata'),
                        ('statistic/','PUT','metadata'),('data','PUT','data'),('data','POST','data'),('history','GET','data')]:
                    url=prefix+'/'+suffix
                    with urlopen(Request(base+url,method=method),timeout=5) as response: value=json.load(response)
                    assert value=={'service':target,'method':method,'uri':url}, (source,suffix,method,value)
                analysis='/api/v1/image/'+database+'/analyse/schema/'+table
                with urlopen(base+analysis,timeout=5) as response: value=json.load(response)
                assert value=={'service':'data','method':'GET','uri':analysis}, (source,analysis,value)
            finally:
                subprocess.run(['docker','rm','-f',name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        print('PASS:',source)


if __name__=='__main__':main()
