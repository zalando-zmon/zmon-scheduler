import redis
import json
import base64

r = redis.StrictRedis('monitor03',6379)
print r.llen('zmon:queue:default2')
val = r.blpop('zmon:queue:default2',30)
val = val[1]
command = base64.b64decode(json.loads(val)["body"])
com = json.loads(command)

print com
