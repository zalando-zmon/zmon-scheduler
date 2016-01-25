#!/usr/bin/env python3

'''
Helper script to clean up some Redis keys

This should probably become part of the regular scheduler code!
'''

import click
import random
import redis
import collections


@click.command()
@click.argument('redis-host')
@click.argument('redis-port')
def main(redis_host, redis_port):
    r = redis.StrictRedis(redis_host, redis_port)

    keys = r.keys('zmon:checks:*')
    sizes = collections.Counter()
    for key in random.sample(keys, 10000):
        print('.', end='')
        try:
            res = r.debug_object(key)
        except:
            pass
        else:
            sizes[key] = res['serializedlength']
    print(sizes.most_common(20))

if __name__ == '__main__':
    main()
