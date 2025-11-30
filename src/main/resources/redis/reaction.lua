local reactionsKey = KEYS[1]
local likesCountKey = KEYS[2]
local dislikesCountKey = KEYS[3]
local rankingZSetKey = KEYS[4]
local likesTrackingKey = KEYS[5]
local dislikesTrackingKey = KEYS[6]

local memberId = ARGV[1]
local newReaction = ARGV[2]
local targetType = ARGV[3]
local targetId = ARGV[4]
local ttl = tonumber(ARGV[5])

local oldReaction = redis.call('HGET', reactionsKey, memberId)

local likesDelta = 0
local dislikesDelta = 0
local shouldDelete = false

if oldReaction == newReaction then
    -- 같은 리액션 클릭 -> 취소
    if newReaction == 'LIKE' then
        likesDelta = -1
    elseif newReaction == 'DISLIKE' then
        dislikesDelta = -1
    end
    shouldDelete = true
elseif newReaction == 'LIKE' then
    -- 새로운 LIKE
    if oldReaction == 'DISLIKE' then
        dislikesDelta = -1
    end
    likesDelta = 1
elseif newReaction == 'DISLIKE' then
    -- 새로운 DISLIKE
    if oldReaction == 'LIKE' then
        likesDelta = -1
    end
    dislikesDelta = 1
elseif newReaction == 'NONE' then
    -- 취소
    if oldReaction == 'LIKE' then
        likesDelta = -1
    elseif oldReaction == 'DISLIKE' then
        dislikesDelta = -1
    end
    shouldDelete = true
end

-- 사용자 기록 업데이트
if shouldDelete then
    redis.call('HDEL', reactionsKey, memberId)
elseif newReaction ~= 'NONE' then
    redis.call('HSET', reactionsKey, memberId, newReaction)
end

-- 카운터 업데이트 및 조회
local finalLikes
local finalDislikes

if likesDelta ~= 0 then
    finalLikes = redis.call('INCRBY', likesCountKey, likesDelta)
else
    finalLikes = redis.call('GET', likesCountKey)
end

if dislikesDelta ~= 0 then
    finalDislikes = redis.call('INCRBY', dislikesCountKey, dislikesDelta)
else
    finalDislikes = redis.call('GET', dislikesCountKey)
end

-- 음수 방지 및 nil 처리
finalLikes = tonumber(finalLikes) or 0
finalDislikes = tonumber(finalDislikes) or 0

if finalLikes < 0 then
    redis.call('SET', likesCountKey, '0', 'KEEPTTL')
    finalLikes = 0
end
if finalDislikes < 0 then
    redis.call('SET', dislikesCountKey, '0', 'KEEPTTL')
    finalDislikes = 0
end

-- 추적 SET 업데이트
if likesDelta ~= 0 then
    redis.call('SADD', likesTrackingKey, likesCountKey)
    if redis.call('TTL', likesTrackingKey) == -1 then
        redis.call('EXPIRE', likesTrackingKey, ttl)
    end
end
if dislikesDelta ~= 0 then
    redis.call('SADD', dislikesTrackingKey, dislikesCountKey)
    if redis.call('TTL', dislikesTrackingKey) == -1 then
        redis.call('EXPIRE', dislikesTrackingKey, ttl)
    end
end

-- 댓글 랭킹 ZSET 업데이트
if targetType == 'COMMENT' then
    if likesDelta ~= 0 and rankingZSetKey and rankingZSetKey ~= '' and targetId and targetId ~= '' then
        redis.call('ZINCRBY', rankingZSetKey, likesDelta, targetId)
    end
end

-- 최종 카운트 반환
return { finalLikes, finalDislikes }
