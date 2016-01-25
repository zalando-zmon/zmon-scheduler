#!/usr/bin/env python3

'''
Helper script to clean up some Redis keys

This should probably become part of the regular scheduler code!
'''

import click
import redis


@click.command()
@click.argument('redis-host')
@click.argument('redis-port')
def main(redis_host, redis_port):
    r = redis.StrictRedis(redis_host, redis_port)

    # delete all Trial Run results
    keys = r.keys('zmon:trial_run:*')
    for key in keys:
        print('Deleting {}..'.format(key))
        r.delete(key)

if __name__ == '__main__':
    main()
