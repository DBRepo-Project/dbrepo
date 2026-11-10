#!/usr/bin/env python3
"""Check Compose credentials and rotation against a disposable persistent broker."""
import base64
import hashlib
import json
import logging
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import time
from urllib.request import Request, urlopen

import pika

ROOT = Path(__file__).resolve().parents[2]
HOOK = '/app/sync-service-password.sh'
logging.getLogger('pika').setLevel(logging.CRITICAL)


def docker(*args, env=None, check=True):
    result = subprocess.run(['docker', *args], env={**os.environ, **(env or {})},
                            capture_output=True, text=True, timeout=120)
    if check and result.returncode:
        raise RuntimeError('Disposable Docker operation failed: ' + args[0])
    return result


def configuration():
    env = {key: os.environ[key] for key in ('PATH', 'HOME') if key in os.environ}
    env.update(BROKER_SERVICE_PASSWORD=secrets.token_urlsafe(30), BROKER_SERVICE_USERNAME='admin',
               SYSTEM_PASSWORD='different-system-password', SYSTEM_USERNAME='different-system-user',
               NUXT_OIDC_TOKEN_KEY='data:;base64,' + 'A'*43 + '=',
               OIDC_AUTH_SESSION_SECRET='synthetic-auth', OIDC_SESSION_SECRET='synthetic-session',
               RABBITMQ_VERSION='3.13.7')
    live = ROOT/'.live/config/includes.chroot_after_packages/etc/skel'
    template = None
    for file in (ROOT/'docker-compose.yml', ROOT/'.docker/docker-compose.yml', live/'docker-compose.yml'):
        result = subprocess.run(['docker', 'compose', '--env-file', '/dev/null', '-f', str(file),
                                 'config', '--format', 'json'], env=env, capture_output=True, text=True)
        assert result.returncode == 0, 'Compose configuration failed'
        services = json.loads(result.stdout)['services']
        broker = services['dbrepo-broker-service']
        if file == ROOT/'docker-compose.yml':
            template = broker
        assert broker['environment']['RABBITMQ_PASSWORD'] == env['BROKER_SERVICE_PASSWORD']
        assert broker['environment']['RABBITMQ_USERNAME'] == env['BROKER_SERVICE_USERNAME']
        assert any(v['target'] == HOOK and v['read_only'] for v in broker['volumes'])
        assert broker['entrypoint'] == ['/bin/bash', HOOK]
        assert broker.get('command') == ['/opt/bitnami/scripts/rabbitmq/run.sh'], 'Compose drops the image command when overriding the entry point'
        assert broker['healthcheck']['test'] == ['CMD', 'bash', HOOK, '--check']
        for service in services.values():
            values = service.get('environment', {})
            if 'BROKER_PASSWORD' in values:
                assert values['BROKER_PASSWORD'] == env['BROKER_SERVICE_PASSWORD']
                assert values['BROKER_USERNAME'] == env['BROKER_SERVICE_USERNAME']
        empty = subprocess.run(['docker', 'compose', '--env-file', '/dev/null', '-f', str(file),
                                'config', '--format', 'json'], env={**env, 'BROKER_SERVICE_PASSWORD': ''},
                               capture_output=True, text=True, check=True)
        empty_services = json.loads(empty.stdout)['services']
        assert empty_services['dbrepo-broker-service']['environment']['RABBITMQ_PASSWORD'] == ''
        for service in empty_services.values():
            values = service.get('environment', {})
            if 'BROKER_PASSWORD' in values:
                assert values['BROKER_PASSWORD'] == ''
    assert (live/'config/sync-service-password.sh').read_bytes() == (ROOT/'dbrepo-broker-service/sync-service-password.sh').read_bytes()
    for file in (ROOT/'dbrepo-broker-service/definitions.json', live/'config/definitions.json'):
        assert next(u for u in json.loads(file.read_text())['users'] if u['name'] == 'admin')['password_hash'] == ''
    assert 'cp ./dbrepo-broker-service/sync-service-password.sh ./.docker/config' in (ROOT/'make/dev.mk').read_text()
    generator = (ROOT/'.scripts/gen-secrets.sh').read_text().split('echo "[🙉]')[0]
    passwords = []
    with tempfile.TemporaryDirectory(prefix='dbrepo-password-generation-') as directory:
        for index in range(2):
            work = Path(directory)/str(index)
            (work/'config').mkdir(parents=True)
            result = subprocess.run(['bash', '-c', generator], cwd=work, env={**env, 'INSTALL_SCRIPT': '1'},
                                    capture_output=True, text=True)
            assert result.returncode == 0, 'Disposable secret generation failed'
            values = dict(line.split('=', 1) for line in (work/'.env').read_text().splitlines() if '=' in line)
            password = values['BROKER_SERVICE_PASSWORD']
            assert len(password) >= 32 and password != 'admin'
            assert password not in result.stdout + result.stderr
            assert next(u for u in json.loads((work/'config/definitions.json').read_text())['users'] if u['name'] == 'admin')['password_hash'] == ''
            passwords.append(password)
        assert passwords[0] != passwords[1]
    return template


def main():
    template = configuration()
    suffix = secrets.token_hex(5)
    name, volume = 'dbrepo-password-test-' + suffix, 'dbrepo-password-data-' + suffix
    first, second, peer_password = [secrets.token_urlsafe(30) for _ in range(3)]
    second += ' $\'"\\ ü'
    marker = secrets.token_hex(16).encode()
    connection = None
    with tempfile.TemporaryDirectory(prefix='dbrepo-password-test-') as directory:
        work = Path(directory)
        work.chmod(0o755)
        (work/'rabbitmq.conf').write_text('default_vhost = dbrepo\nmanagement.load_definitions = /tmp/dbrepo-definitions.json\n'
                                         'auth_backends.1 = internal\nlog.console.level = warning\n')
        (work/'enabled_plugins').write_text('[rabbitmq_management].\n')
        (work/'definitions.json').write_bytes((ROOT/'dbrepo-broker-service/definitions.json').read_bytes())
        for file in work.iterdir():
            file.chmod(0o644)
        mounts = [(work/'rabbitmq.conf', '/etc/rabbitmq/rabbitmq.conf'),
                  (work/'enabled_plugins', '/etc/rabbitmq/enabled_plugins'),
                  (work/'definitions.json', '/app/definitions.json'),
                  (ROOT/'dbrepo-broker-service/sync-service-password.sh', HOOK)]
        compose = work/'compose.json'

        def compose_run(*args, env=None, check=True):
            return docker('compose', '--project-name', name, '--file', str(compose), *args, env=env, check=check)

        def start(password):
            env = dict(RABBITMQ_USERNAME='admin', RABBITMQ_PASSWORD=password,
                       RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS='+S 2:2 +SDcpu 1 +SDio 1',
                       RABBITMQ_CTL_ERL_ARGS='+S 1:1')
            compose.write_text(json.dumps({'services': {'broker': {
                'image': template['image'], 'entrypoint': template['entrypoint'], 'command': template.get('command'),
                'container_name': name, 'hostname': name,
                'environment': ['RABBITMQ_USERNAME', 'RABBITMQ_PASSWORD',
                                'RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS', 'RABBITMQ_CTL_ERL_ARGS'],
                'ports': ['127.0.0.1:0:5672', '127.0.0.1:0:15672'], 'healthcheck': template['healthcheck'],
                'volumes': [str(source) + ':' + target + ':ro' for source, target in mounts]
                           + [volume + ':/bitnami/rabbitmq/mnesia']}},
                'volumes': {volume: {'external': True}}}))
            compose.chmod(0o600)
            assert password not in compose.read_text(), 'Compose fixture contains a password value'
            compose_run('up', '-d', env=env)

        def ready():
            deadline = time.monotonic() + 120
            while time.monotonic() < deadline:
                if docker('inspect', '--format', '{{.State.Running}}', name).stdout.strip() != 'true':
                    logs = docker('logs', name, check=False)
                    detail = logs.stdout + logs.stderr
                    for password in (first, second, peer_password):
                        detail = detail.replace(password, '<synthetic-password>')
                    raise RuntimeError('Disposable broker stopped during initialization:\n' + detail[-4000:])
                # Skip the temporary server started by Bitnami's setup phase.
                if (docker('exec', name, 'cat', '/proc/1/comm', check=False).stdout.strip() in ('rabbitmq-server', 'beam.smp')
                        and docker('exec', name, 'bash', HOOK, '--check', check=False).returncode == 0):
                    return
                time.sleep(1)
            raise RuntimeError('Disposable broker credentials did not become ready')

        def connect(password, username='admin'):
            port = int(docker('port', name, '5672/tcp').stdout.rsplit(':', 1)[1])
            return pika.BlockingConnection(pika.ConnectionParameters('127.0.0.1', port, 'dbrepo',
                    pika.PlainCredentials(username, password), socket_timeout=10, stack_timeout=15))

        def rejected(password):
            try:
                unexpected = connect(password)
            except pika.exceptions.ProbableAuthenticationError:
                return
            unexpected.close()
            raise AssertionError('An obsolete broker password was accepted')

        def api(method, path, password, body=None):
            port = int(docker('port', name, '15672/tcp').stdout.rsplit(':', 1)[1])
            headers = {'Authorization': 'Basic ' + base64.b64encode(('admin:' + password).encode()).decode(),
                       'Content-Type': 'application/json'}
            data = json.dumps(body).encode() if body is not None else None
            with urlopen(Request(f'http://127.0.0.1:{port}/api/' + path, data=data, headers=headers, method=method), timeout=10) as response:
                value = response.read()
                return json.loads(value) if value else None

        try:
            for username, password in (('missing-service-user', first), ('admin', '')):
                env = dict(RABBITMQ_USERNAME=username, RABBITMQ_PASSWORD=password)
                args = ['run', '--rm', '--name', name, '--entrypoint', '/bin/bash']
                for key in env:
                    args.extend(['--env', key])
                for source, target in mounts:
                    args.extend(['-v', str(source) + ':' + target + ':ro'])
                result = docker(*args, 'bitnamilegacy/rabbitmq:3.13.7', HOOK, '/bin/true', env=env, check=False)
                assert result.returncode != 0, 'Invalid service credentials reached the native entry point'
                assert first not in result.stdout + result.stderr
            docker('volume', 'create', volume)
            start(first)
            ready()
            assert docker('exec', name, 'stat', '-c', '%a', '/tmp/dbrepo-definitions.json').stdout.strip() == '600'
            rejected('admin')
            connection = connect(first)
            channel = connection.channel()
            channel.confirm_delivery()
            for queue in ('dbrepo', 'dbrepo.consumer.errors'):
                channel.basic_publish('', queue, marker, pika.BasicProperties(delivery_mode=2), mandatory=True)
            channel.queue_declare('operator-probe', durable=True)
            api('PUT', 'users/operator-probe', first, {'password': peer_password, 'tags': []})
            api('PUT', 'permissions/dbrepo/operator-probe', first,
                {'configure': '^operator-probe$', 'read': '^operator-probe$', 'write': '^operator-probe$'})
            connection.close()
            connection = None
            docker('restart', name)
            ready()
            connect(first).close()
            connect(peer_password, 'operator-probe').close()
            docker('rm', '-f', name)
            # Upgrade an existing deployment whose imported hash still contains its old password.
            definitions = json.loads((work/'definitions.json').read_text())
            salt = secrets.token_bytes(4)
            next(u for u in definitions['users'] if u['name'] == 'admin')['password_hash'] = base64.b64encode(
                salt + hashlib.sha256(salt + first.encode()).digest()).decode()
            (work/'definitions.json').write_text(json.dumps(definitions))
            start(second)
            ready()
            rejected(first)
            rejected('admin')
            connect(peer_password, 'operator-probe').close()
            connection = connect(second)
            channel = connection.channel()
            for queue in ('dbrepo', 'dbrepo.consumer.errors'):
                method, _, body = channel.basic_get(queue, auto_ack=True)
                assert method is not None and body == marker, 'Rotation lost a retained delivery'
                assert api('GET', 'queues/dbrepo/' + queue, second)['arguments']['x-queue-type'] == 'quorum'
            assert api('GET', 'queues/dbrepo/operator-probe', second)['durable']
            assert api('GET', 'permissions/dbrepo/operator-probe', second)['read'] == '^operator-probe$'
            logs = docker('logs', name)
            assert all(password not in logs.stdout + logs.stderr for password in (first, second, peer_password))
            connection.close()
            connection = None
            docker('rm', '-f', name)
            start(first)
            ready()
            connect(first).close()
            rejected(second)
            connect(peer_password, 'operator-probe').close()
            print('PASS: three Compose variants, fresh password, restart, rotation, rollback, old-password rejection, retained quorum deliveries and unrelated user/queue')
        finally:
            if connection is not None and connection.is_open:
                connection.close()
            if compose.exists():
                compose_run('down', '--remove-orphans', check=False)
            docker('rm', '-f', '-v', name, check=False)
            docker('volume', 'rm', volume, check=False)


if __name__ == '__main__':
    main()
