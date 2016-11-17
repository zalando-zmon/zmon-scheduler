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

    keys = r.keys('zmon:downtimes:*:*')
    p = r.pipeline()
    for key in keys:
        print("Deleting ... {}".format(key))
        p.delete(key)
    results = p.execute()

if __name__ == '__main__':
    main()
