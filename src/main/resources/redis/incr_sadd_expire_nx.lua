local counterKey = KEYS[1]
local trackingKey = KEYS[2]
local ttl = ARGV[1]

redis.call('INCR', counterKey)

redis.call('SADD', trackingKey, counterKey)

if redis.call('TTL', trackingKey) == -1 then
    redis.call('EXPIRE', trackingKey, ttl)
end

return nil