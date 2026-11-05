import subprocess,time,select
from pathlib import Path
name='dbrepo-routine-migration-test'
root=Path(__file__).resolve().parents[1]
def sql(body,user='root',expect=True):
 p=subprocess.run(['docker','exec','-i',name,'mariadb','-u'+user,'-plocal-test-password','--batch','--skip-column-names'],input=body,text=True,capture_output=True)
 if expect and p.returncode:raise RuntimeError(p.stderr)
 return p
subprocess.run(['docker','run','-d','--name',name,'--memory=512m','-e','MARIADB_ROOT_PASSWORD=local-test-password','mariadb:10.11'],check=True,stdout=subprocess.DEVNULL)
try:
 for _ in range(60):
  if sql('SELECT 1',expect=False).returncode==0:break
  time.sleep(1)
 sql("CREATE DATABASE access_a; CREATE DATABASE access_b; CREATE TABLE access_a.qs_queries(id INT); CREATE TABLE access_b.secret(value INT); INSERT INTO access_b.secret VALUES(42); CREATE USER reader@'%' IDENTIFIED BY 'local-test-password'; GRANT SELECT,EXECUTE ON access_a.* TO reader@'%';")
 for routine in ['store_query','_store_query','hash_query','hash_table']:
  sql(f"CREATE PROCEDURE access_a.{routine}() SELECT * FROM access_b.secret; GRANT EXECUTE ON PROCEDURE access_a.{routine} TO reader@'%';")
 assert sql('CALL access_a.store_query()',user='reader').stdout.strip()=='42'
 retained=subprocess.Popen(['docker','exec','-i',name,'mariadb','-ureader','-plocal-test-password',
                            '--batch','--skip-column-names','--unbuffered','--skip-reconnect','access_a'],
                           stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,text=True)
 def retained_call():
  retained.stdin.write('CALL store_query();\n');retained.stdin.flush()
  assert select.select([retained.stdout],[],[],10)[0], 'Existing session did not respond'
  return retained.stdout.readline().strip()
 assert retained_call()=='42'

 for run in range(2):
  sql('USE access_a;\n'+(root/'.scripts/revoke-query-store-access.sql').read_text())
  for routine in ['store_query','_store_query','hash_query','hash_table']:
   p=sql(f'CALL access_a.{routine}()',user='reader',expect=False);assert p.returncode and '1370' in p.stderr,p.stderr
  sql('SELECT * FROM access_a.qs_queries',user='reader')
  assert sql('CALL access_a.store_query()').stdout.strip()=='42'
  print(f'Migration run {run+1}: user calls denied, SELECT and service calls preserved.',flush=True)
 assert retained_call()=='42', 'The pre-migration connection demonstrates cached grants'
 subprocess.run(['docker','restart',name],check=True,stdout=subprocess.DEVNULL)
 retained.wait(timeout=15)
 for _ in range(60):
  if sql('SELECT 1',expect=False).returncode==0:break
  time.sleep(1)
 assert sql('CALL access_a.store_query()',user='reader',expect=False).returncode != 0
 print('Forced database restart terminated the retained session; reconnect cannot execute routines.',flush=True)
 print('Live migration and idempotence checks passed.',flush=True)
finally:subprocess.run(['docker','rm','-f',name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
