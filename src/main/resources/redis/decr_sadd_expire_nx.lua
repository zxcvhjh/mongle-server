local counterKey = KEYS[1]
local trackingKey = KEYS[2]
local ttl = ARGV[1]

local finalVal = redis.call('DECR', counterKey)

if finalVal < 0 then
    redis.call('SET', counterKey, '0', 'KEEPTTL')
    finalVal = 0
end

redis.call('SADD', trackingKey, counterKey)

if redis.call('TTL', trackingKey) == -1 then
    redis.call('EXPIRE', trackingKey, ttl)
end

return finalVal