local reactionsKey = KEYS[1]
local counterKey = KEYS[2]
local trackingKey = KEYS[3]
local rankingKey = KEYS[4]

local memberId = ARGV[1]
local reactionType = ARGV[2]
local targetType = ARGV[3]
local targetId = ARGV[4]
local ttl = tonumber(ARGV[5])

redis.call('HDEL', reactionsKey, memberId)

local count = redis.call('GET', counterKey)
local changed = false
if count and tonumber(count) > 0 then
    redis.call('DECR', counterKey)
    changed = true
elseif not count then
    changed = false
else
    redis.call('SET', counterKey, '0', 'KEEPTTL')
    changed = true
end

if changed then
    redis.call('SADD', trackingKey, counterKey)
    if redis.call('TTL', trackingKey) == -1 then
        redis.call('EXPIRE', trackingKey, ttl)
    end
end

if targetType == 'COMMENT' and reactionType == 'LIKE' and rankingKey and rankingKey ~= '' then
    redis.call('ZINCRBY', rankingKey, -1, targetId)
end

return 1