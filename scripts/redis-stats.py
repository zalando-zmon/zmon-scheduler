#!/usr/bin/env python3

'''
Helper script to clean up some Redis keys

This should probably become part of the regular scheduler code!
'''

import click
import clickclick
import json
import redis
import collections
import time


@click.command()
@click.argument('redis-host')
@click.argument('redis-port')
def main(redis_host, redis_port):
    r = redis.StrictRedis(redis_host, redis_port)

    now = time.time()
    keys = r.keys('zmon:checks:*:*')
    p = r.pipeline()
    for key in keys:
        p.lrange(key, 0, 1)
    results = p.execute()

    cutoff = now - (2 * 3600)  # two hours ago
    durations = collections.Counter()
    for key, res in zip(keys, results):
        if len(res) >= 2:
            data = json.loads(res[0].decode('utf-8'))
            # only consider "recent" results
            if data['ts'] > cutoff:
                _, _, check_id, entity_id = key.decode('utf-8').split(':', 3)
                check_id = int(check_id)
                data_before = json.loads(res[1].decode('utf-8'))
                interval_seconds = data['ts'] - data_before['ts']
                checks_per_second = 1. / interval_seconds
                durations.update({check_id: data['td'] * checks_per_second})

    # print "worst" checks
    # i.e. checks using the most worker time
    rows = []
    for key, duration in durations.most_common(20):
        rows.append({'check_id': key, 'duration': round(duration, 1)})
    clickclick.print_table(['check_id', 'duration'], rows)

    total_duration = sum(durations.values())
    print('Total duration: {:.2f}s'.format(total_duration))

if __name__ == '__main__':
    main()
