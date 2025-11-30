local added = redis.call('SADD', KEYS[1], ARGV[1])

if redis.call('TTL', KEYS[1]) == -1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])
end

return added