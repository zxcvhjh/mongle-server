local reactionsKey = KEYS[1]
local counterKey = KEYS[2]
local trackingKey = KEYS[3]
local rankingKey = KEYS[4]

local memberId = ARGV[1]
local reactionType = ARGV[2]
local targetType = ARGV[3]
local targetId = ARGV[4]

redis.call('HDEL', reactionsKey, memberId)

local count = redis.call('GET', counterKey)
if count and tonumber(count) > 0 then
    redis.call('DECR', counterKey)
elseif not count then
    return 0
else
    redis.call('SET', counterKey, '0', 'KEEPTTL')
end

redis.call('SREM', trackingKey, counterKey)

if targetType == 'COMMENT' and reactionType == 'LIKE' and rankingKey and rankingKey ~= '' then
    redis.call('ZINCRBY', rankingKey, -1, targetId)
end

return 1