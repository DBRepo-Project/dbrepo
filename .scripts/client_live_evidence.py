"""Capture the inputs needed to compare two release test runs."""
import hashlib
import json
from pathlib import Path
import re


CORE_COMPONENTS = {'metadata-service', 'data-service', 'search-service', 'consumer-service', 'dashboard-service', 'ui', 'replication-service'}


def digest_tree(root, files):
    hashes = {path.relative_to(root).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
              for path in files}
    return hashlib.sha256(json.dumps(hashes, sort_keys=True).encode()).hexdigest()


def toolchain(scripts, package):
    runner_files = [scripts / name for name in ('client-live-test.py', 'client-live-login.py',
                                                 'client_live_evidence.py', 'client-live-endpoints.json')]
    if (scripts / 'client_live_auth.py').is_file():
        runner_files.append(scripts / 'client_live_auth.py')
    return {'runner_bundle_sha256': digest_tree(scripts, runner_files),
            'catalog_sha256': hashlib.sha256((scripts / 'client-live-endpoints.json').read_bytes()).hexdigest(),
            'client_package_sha256': digest_tree(package, package.rglob('*.py'))}


def deployment(value, endpoint):
    if (not isinstance(value, dict) or not isinstance(value.get('endpoint'), str) or not isinstance(endpoint, str)
            or not endpoint or value['endpoint'].rstrip('/') != endpoint.rstrip('/')):
        raise ValueError('Deployment evidence must identify the tested endpoint')
    commit, images = value.get('commit'), value.get('images')
    if not isinstance(commit, str) or not re.fullmatch('[0-9a-f]{40}', commit):
        raise ValueError('Deployment evidence requires the full source commit')
    if not isinstance(images, dict) or not images:
        raise ValueError('Deployment evidence requires running image digests')
    digests = {}
    for name, reference in images.items():
        if not isinstance(name, str) or not re.fullmatch('[a-zA-Z0-9_.-]+', name) or not isinstance(reference, str):
            raise ValueError('Invalid deployment component or digest')
        digest = reference.rsplit('@', 1)[-1]
        if not re.fullmatch('sha256:[0-9a-f]{64}', digest):
            raise ValueError('A deployment image is missing its immutable SHA-256 digest')
        digests[name] = digest
    return {'endpoint': endpoint.rstrip('/'), 'commit': commit, 'images': digests}


def release_check(baseline, candidate, expected_candidate, expected_baseline):
    incomplete = []
    runs = (('baseline', baseline, expected_baseline), ('candidate', candidate, expected_candidate))
    run_ids = [report.get('run_id') for _, report, _ in runs]
    if not all(isinstance(run_id, str) and run_id.strip() for run_id in run_ids) or run_ids[0] == run_ids[1]:
        incomplete.append('Release comparison requires two independent run IDs')
    if not all(report.get('profile') == 'full' and report.get('completed') is True for report in (baseline, candidate)):
        incomplete.append('Both reports must be completed full-profile runs')
    for role, report, expected_commit in runs:
        if not isinstance(expected_commit, str) or not re.fullmatch('[0-9a-f]{40}', expected_commit):
            raise ValueError(f'Release comparison requires the full {role} commit')
        try:
            image_set = deployment(report.get('deployment'), report.get('endpoint', ''))
        except ValueError:
            incomplete.append(f'Missing or invalid deployment evidence for {role}')
        else:
            missing = sorted(CORE_COMPONENTS - image_set['images'].keys())
            if missing:
                incomplete.append(f"Missing {role} deployment components: {', '.join(missing)}")
            if image_set['commit'] != expected_commit:
                incomplete.append(f'The deployed {role} differs from the requested commit')
    for key in ('runner_bundle_sha256', 'catalog_sha256', 'client_package_sha256'):
        a = baseline.get('toolchain', {}).get(key)
        b = candidate.get('toolchain', {}).get(key)
        if not isinstance(a, str) or not re.fullmatch('[0-9a-f]{64}', a) or a != b:
            incomplete.append(f'Cannot verify matching {key}')

    def case_map(report):
        return {case['case']: case.get('status') for case in report.get('cases', [])}

    before, after = case_map(baseline), case_map(candidate)
    if not before or not after or len(before) != len(baseline['cases']) or len(after) != len(candidate['cases']):
        incomplete.append('Missing or duplicate case evidence')
    previously_passing = sorted(name for name, status in before.items()
                                if status == 'passed' and after.get(name) != 'passed')
    missing_cases = sorted((set(before) - set(after)) | {name for name, status in after.items() if status
                           not in ('passed', 'failed')})
    failures = sorted(name for name, status in after.items() if status == 'failed')
    for name in ('identifier.retrieve.external', 'identifier.publish', 'oai.GetRecord'):
        if after.get(name) != 'passed' and name not in missing_cases and name not in failures:
            missing_cases.append(name)

    catalog_file = Path(__file__).with_name('client-live-endpoints.json').read_bytes()
    if hashlib.sha256(catalog_file).hexdigest() != candidate.get('toolchain', {}).get('catalog_sha256'):
        incomplete.append('Use the endpoint catalog shipped with these reports')
    catalog = json.loads(catalog_file)
    expected = {entry['operation']: entry.get('excluded') for entry in catalog['endpoints']}
    coverage = {entry['operation']: entry for entry in candidate.get('endpoint_coverage', [])}
    baseline_ops = {entry['operation'] for entry in baseline.get('endpoint_coverage', [])}
    if set(coverage) != set(expected) or baseline_ops != set(expected):
        incomplete.append('The reports do not contain the complete endpoint inventory')
    def covered(operation):
        entry = coverage.get(operation, {})
        if entry.get('state') == 'passed' and entry.get('passed_cases'):
            return True
        companion = candidate.get('replication_evidence', {})
        count = companion.get('http_successes', {}).get(operation, 0)
        return (companion.get('eligible') is True and entry.get('state') == 'observed_in_replication'
                and count > 0 and count == entry.get('replication_successes'))
    missing_operations = sorted(op for op, excluded in expected.items() if not excluded and not covered(op))
    methods = candidate.get('client_methods', {})
    expected_methods = set(catalog['client_methods'])
    if set(methods) != expected_methods or set(baseline.get('client_methods', {})) != expected_methods:
        incomplete.append('The reports do not contain the complete SDK inventory')
    missing_methods = sorted(name for name in expected_methods if methods.get(name) != 'attempted')
    if not candidate.get('runtime_versions') or baseline.get('runtime_versions') != candidate['runtime_versions']:
        incomplete.append('Runtime library versions are missing or differ')
    state = 'failed' if failures else 'incomplete' if (
        incomplete or previously_passing or missing_cases or missing_operations or missing_methods) else 'passed'
    return {'state': state, 'expected_baseline_commit': expected_baseline,
            'expected_candidate_commit': expected_candidate, 'evidence_gaps': incomplete,
            'candidate_failures': failures, 'previously_passing_unverified': previously_passing,
            'missing_cases': sorted(missing_cases), 'uncovered_operations': missing_operations,
            'uncovered_client_methods': missing_methods}


def replication_sources(scripts):
    return digest_tree(scripts, [scripts / name for name in
                       ('replication-acceptance.py', 'client_live_auth.py', 'client_live_evidence.py')])


def replication_fixture(report, endpoint, deployed, scripts):
    """Read fixture IDs. Only count observations made on this same, identified deployment."""
    from uuid import UUID
    if not isinstance(report, dict):
        raise ValueError('Invalid replication report')
    sites = report.get('sites', {})
    if (report.get('status') != 'PASS' or report.get('protocol') != 'master-timestamp-history-v3'
            or not report.get('checks') or any(c.get('status') != 'PASS' for c in report['checks'])
            or not re.fullmatch(r'replication_acceptance_[0-9a-f]{16}', report.get('run', ''))
            or not isinstance(sites, dict) or len(sites) != 3 or not all(isinstance(url, str) for url in sites.values())
            or len(set(sites.values())) != 3):
        raise ValueError('Supply a completed, passing three-site replication acceptance report')
    local = next((name for name, url in sites.items() if url.rstrip('/') == endpoint.rstrip('/')), None)
    if local is None:
        raise ValueError('The tested endpoint is missing from the replication report')
    databases = {item['site']: item for item in report.get('resources', [])
                 if item.get('kind') == 'database' and item.get('name') == report['run']}
    tables = {item['site']: item for item in report.get('resources', [])
              if item.get('kind') == 'table' and item.get('internal_name') == 'roundtrip'}
    if set(databases) != set(sites) or set(tables) != set(sites):
        raise ValueError('The report must contain the database and roundtrip table on all sites')
    # The acceptance report records the primary database first, before registering peers.
    primary = next(iter(databases))
    peer = primary if local != primary else next(name for name in sites if name != local)
    fixture = {side + '_' + kind: str(UUID(objects[site]['id']))
               for side, site in [('local', local), ('peer', peer)]
               for kind, objects in [('database', databases), ('table', tables)]}

    fixture['receiver'] = local != primary
    eligible = (deployed is not None and report.get('deployments', {}).get(local) == deployed
                and report.get('suite_sha256') == replication_sources(scripts))
    successes = {}
    if eligible:
        for operation, statuses in report.get('http_operations', {}).get(local, {}).items():
            count = sum(n for code, n in statuses.items() if code.isdecimal() and 200 <= int(code) < 300
                        and type(n) is int and n > 0)
            if count:
                successes[operation] = count
    return fixture, {'site': local, 'run': report['run'], 'eligible': eligible, 'http_successes': successes}
