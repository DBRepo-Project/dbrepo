#!/usr/bin/env python3
"""Compare two live reports made with the same client and test profile."""
import argparse
import json
import os
from pathlib import Path


def signature(report, case):
    return {'error':case.get('error'),'validation':case.get('validation'),
            'requests':[{k:r[k] for k in ('operation','status','api_code','error') if k in r}
                        for r in report.get('requests',[]) if r['case']==case['case']]}


def compare(baseline, candidate):
    runs_completed=baseline.get('completed') is True and candidate.get('completed') is True
    comparable=(runs_completed and baseline.get('client_sha256')==candidate.get('client_sha256') and
                baseline.get('runner_sha256')==candidate.get('runner_sha256') and
                baseline.get('profile','full')==candidate.get('profile','full'))
    a={c['case']:c for c in baseline['cases']};b={c['case']:c for c in candidate['cases']}
    contracts_match = baseline.get('contracts', {'table_deletion':'physical'}) == candidate.get('contracts', {'table_deletion':'physical'})
    cases=[]
    for name in sorted(a.keys()|b.keys()):
        left,right=a.get(name),b.get(name)
        ls=left.get('status') if left else None;rs=right.get('status') if right else None
        if ls in ('skipped','not_in_profile',None) or rs in ('skipped','not_in_profile',None): state='uncompared'
        elif not contracts_match and name in ('table.delete', 'table.deleted'):state='contract_mismatch'
        elif not comparable:state='client_or_profile_mismatch'
        elif ls==rs=='failed':state='both_failed_same_signature' if signature(baseline,left)==signature(candidate,right) else 'both_failed_different_signature'
        elif ls=='passed' and rs=='failed':state='candidate_only_failure'
        elif ls=='failed' and rs=='passed':state='baseline_only_failure'
        else:state='both_passed'
        item={'case':name,'state':state,'baseline':ls,'candidate':rs}
        if left and ls=='failed':item['baseline_failure']=signature(baseline,left)
        if right and rs=='failed':item['candidate_failure']=signature(candidate,right)
        cases.append(item)
    from collections import Counter
    return {'contracts_match':contracts_match,'runs_completed':runs_completed,'runtime_versions_match':baseline.get('runtime_versions')==candidate.get('runtime_versions'),'comparable_client_and_profile':comparable,
            'baseline':{k:baseline.get(k) for k in ('endpoint','profile','completed','instance','client_sha256','runner_sha256','contracts','runtime_versions')},
            'candidate':{k:candidate.get(k) for k in ('endpoint','profile','completed','instance','client_sha256','runner_sha256','contracts','runtime_versions')},
            'summary':dict(Counter(c['state'] for c in cases)),'cases':cases,
            'limits':['Matching failure signatures are evidence of shared behavior, not proof of the same root cause.',
                      'Candidate-only failures can reflect code, configuration, data, permissions or test differences.',
                      'The advertised frontend commit does not prove the version of every backend service.']}


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('baseline',type=Path);p.add_argument('candidate',type=Path)
    p.add_argument('--output',type=Path,default=Path('client-live-comparison.json'))
    args=p.parse_args()
    if args.output.resolve() in (args.baseline.resolve(),args.candidate.resolve()):p.error('Output must not overwrite an input report')
    result=compare(json.loads(args.baseline.read_text()),json.loads(args.candidate.read_text()))
    with os.fdopen(os.open(args.output,os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600),'w') as stream:
        os.fchmod(stream.fileno(),0o600)
        json.dump(result,stream,indent=2)
    print(json.dumps(result['summary'],indent=2));print('Comparison:',args.output.resolve())
    return 0 if result['comparable_client_and_profile'] else 2


if __name__=='__main__':raise SystemExit(main())
