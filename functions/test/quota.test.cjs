const assert = require("node:assert/strict");

const {
  DAILY_AI_RECOMMENDATION_LIMIT,
  currentUtcDateKey,
  quotaStatus,
} = require("../lib/quota.js");

assert.equal(DAILY_AI_RECOMMENDATION_LIMIT, 5);
assert.equal(currentUtcDateKey(new Date("2026-05-22T23:59:59.000Z")), "2026-05-22");

assert.deepEqual(quotaStatus(0), {
  limit: 5,
  used: 0,
  remaining: 5,
  isExceeded: false,
});

assert.deepEqual(quotaStatus(4), {
  limit: 5,
  used: 4,
  remaining: 1,
  isExceeded: false,
});

assert.deepEqual(quotaStatus(5), {
  limit: 5,
  used: 5,
  remaining: 0,
  isExceeded: true,
});

assert.deepEqual(quotaStatus(12), {
  limit: 5,
  used: 12,
  remaining: 0,
  isExceeded: true,
});

