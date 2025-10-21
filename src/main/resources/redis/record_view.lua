
redis.call('SADD', KEYS[1], ARGV[1])

redis.call('EXPIRE', KEYS[1], ARGV[2])

return 1