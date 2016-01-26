#!/usr/bin/env python3

'''
Helper script to clean up some Redis keys

This should probably become part of the regular scheduler code!
'''

import click
import json
import redis
import requests
import time


def cleanup_outdated_results(r):
    now = time.time()
    keys = r.keys('zmon:checks:*:*')
    p = r.pipeline()
    for key in keys:
        p.lrange(key, 0, 0)
    results = p.execute()

    cutoff = now - (8 * 24 * 3600)  # 8 days ago
    ages = {}
    for key, res in zip(keys, results):
        if res:
            data = json.loads(res[0].decode('utf-8'))
            if data['ts'] < cutoff:
                ages[key] = now - data['ts']
    for key, age in ages.items():
        _, _, check_id, entity_id = key.decode('utf-8').split(':', 3)
        print('Deleting outdated check results {}/{} ({}d ago)..'.format(check_id, entity_id, int(age/(3600*24))))
        r.delete(key)


@click.command()
@click.argument('zmon_url')
@click.argument('redis-host')
@click.argument('redis-port')
def main(zmon_url, redis_host, redis_port):
    r = redis.StrictRedis(redis_host, redis_port)

    # delete all Trial Run results
    keys = r.keys('zmon:trial_run:*')
    for key in keys:
        print('Deleting {}..'.format(key))
        r.delete(key)

    # get all active checks
    response = requests.get(zmon_url + '/checks/all-active-alert-definitions')
    data = response.json()
    all_active_alert_ids = set()
    all_referenced_check_ids = set()
    for row in data['alert_definitions']:
        all_active_alert_ids.add(row['id'])
        all_referenced_check_ids.add(row['check_definition_id'])

    response = requests.get(zmon_url + '/checks/all-active-check-definitions')
    data = response.json()
    all_active_check_ids = set()
    for row in data['check_definitions']:
        all_active_check_ids.add(row['id'])

    all_active_check_ids = all_active_check_ids & all_referenced_check_ids

    keys = r.keys('zmon:alerts:*')
    keys_to_delete = set()
    for key in keys:
        parts = key.split(b':')
        alert_id = int(parts[2])
        if alert_id not in all_active_alert_ids:
            keys_to_delete.add(key)
    for key in sorted(keys_to_delete):
        print('Deleting {}..'.format(key))
        r.delete(key)

    keys = r.keys('zmon:metrics:*:alerts.*.*')
    keys_to_delete = set()
    for key in keys:
        parts = key.rsplit(b'.')
        alert_id = int(parts[-2])
        if alert_id not in all_active_alert_ids:
            keys_to_delete.add(key)
    for key in sorted(keys_to_delete):
        print('Deleting {}..'.format(key))
        r.delete(key)

    keys = r.keys('zmon:downtimes:*:*')
    keys_to_delete = set()
    for key in keys:
        parts = key.split(b':', 3)
        alert_id = int(parts[2])
        if alert_id not in all_active_alert_ids:
            keys_to_delete.add(key)
    for key in sorted(keys_to_delete):
        print('Deleting {}..'.format(key))
        r.delete(key)

    # delete all non-active checks
    keys = r.keys('zmon:checks:*')
    keys_to_delete = set()
    for key in keys:
        parts = key.split(b':')
        check_id = int(parts[2])
        if check_id not in all_active_check_ids:
            keys_to_delete.add(key)
    for key in sorted(keys_to_delete):
        print('Deleting {}..'.format(key))
        r.delete(key)

    keys = r.keys('zmon:metrics:*:check.*.*')
    keys_to_delete = set()
    for key in keys:
        parts = key.rsplit(b'.')
        check_id = int(parts[-2])
        if check_id not in all_active_check_ids:
            keys_to_delete.add(key)
    for key in sorted(keys_to_delete):
        print('Deleting {}..'.format(key))
        r.delete(key)

    cleanup_outdated_results(r)

    # delete ALL metrics
    # keys = r.keys('zmon:metrics:*')
    # for key in keys:
    #     print('Deleting {}..'.format(key))
    #     r.delete(key)

if __name__ == '__main__':
    main()
