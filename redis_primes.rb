#!/usr/bin/env ruby
#
# Store primes from a file in Redis as a sorted set. Score is same as the prime.
# Primes are stored in a set called primes.
# It will clear any existing set first.

require "redis"

SET_NAME = "euler-46:primes"
redis = Redis.new

redis.zremrangebyscore(SET_NAME, 0, "inf")

ARGF.each_line do |line|
  prime = line.to_i
  redis.zadd(SET_NAME, prime, prime)
end
