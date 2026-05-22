export const DAILY_AI_RECOMMENDATION_LIMIT = 5;

export type QuotaStatus = {
  limit: number;
  used: number;
  remaining: number;
  isExceeded: boolean;
};

export function currentUtcDateKey(now = new Date()): string {
  return now.toISOString().slice(0, 10);
}

export function quotaStatus(
  rawUsed: unknown,
  limit = DAILY_AI_RECOMMENDATION_LIMIT,
): QuotaStatus {
  const used = normalizeUsageCount(rawUsed);
  const remaining = Math.max(limit - used, 0);

  return {
    limit,
    used,
    remaining,
    isExceeded: used >= limit,
  };
}

function normalizeUsageCount(value: unknown): number {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return 0;
  }

  return Math.max(Math.floor(value), 0);
}

