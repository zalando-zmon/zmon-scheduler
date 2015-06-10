import redis
import json
import base64
import snappy

r = redis.StrictRedis('monitor03.zalando',6379)
print r.llen('zmon:queue:default')

val = r.blpop('zmon:queue:default', 30)

print snappy.decompress(val[1])
