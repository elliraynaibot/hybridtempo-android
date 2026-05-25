import {initializeApp} from "firebase-admin/app";
import {FieldValue, getFirestore} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {setGlobalOptions} from "firebase-functions/v2";
import {currentUtcDateKey, quotaStatus} from "./quota";

initializeApp();

setGlobalOptions({
  region: "us-central1",
  maxInstances: 5,
});

type BreathPhase = {
  label: "Inhale" | "Hold" | "Exhale" | "Rest";
  seconds: number;
  instruction: string;
  scaleTarget: number;
};

type BreathworkProtocol = {
  category: string;
  title: string;
  durationMinutes: number;
  phases: BreathPhase[];
  ambientTrackName: string;
};

type RecommendationResponse = {
  protocol: string;
  durationMinutes: number;
  rationale: string;
  cadence: string;
  source: "deterministicFallback";
  quota: RecommendationQuota;
  breathworkProtocol: BreathworkProtocol;
};

type RecommendationResult = Omit<RecommendationResponse, "quota">;

type RecommendationQuota = {
  limit: number;
  used: number;
  remaining: number;
  resetDate: string;
};

type RecommendationRequest = {
  profile: {
    trainingStyle: string;
    weeklyTrainingFrequency: number;
    goals: string[];
    preferredSessionLength: number;
    raceName: string;
    raceDate: string;
  };
  checkIn: {
    energy: number;
    soreness: number;
    stress: number;
    workoutType: string;
    workoutIntensity: number;
    timeAvailable: number;
    sessionIntent: string;
  };
  recentTrends?: {
    energy?: number[];
    soreness?: number[];
    stress?: number[];
  };
};

export const recommendBreathwork = onCall<RecommendationRequest>(
  {
    enforceAppCheck: false,
    invoker: "public",
  },
  async (request): Promise<RecommendationResponse> => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Sign in before requesting a recommendation.");
    }

    const payload = validateRecommendationRequest(request.data);
    const quota = await reserveDailyRecommendation(request.auth.uid);
    return {
      ...deterministicRecommendation(payload),
      quota,
    };
  },
);

async function reserveDailyRecommendation(uid: string): Promise<RecommendationQuota> {
  const db = getFirestore();
  const resetDate = currentUtcDateKey();
  const usageRef = db.doc(`users/${uid}/aiRecommendationUsage/${resetDate}`);

  return db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(usageRef);
    const currentStatus = quotaStatus(snapshot.get("count"));

    if (currentStatus.isExceeded) {
      throw new HttpsError(
        "resource-exhausted",
        dailyLimitMessage(currentStatus.limit),
        {
          limit: currentStatus.limit,
          used: currentStatus.used,
          remaining: 0,
          resetDate,
        },
      );
    }

    const used = currentStatus.used + 1;
    const quota: RecommendationQuota = {
      limit: currentStatus.limit,
      used,
      remaining: Math.max(currentStatus.limit - used, 0),
      resetDate,
    };
    const update: Record<string, unknown> = {
      count: used,
      resetDate,
      updatedAt: FieldValue.serverTimestamp(),
      lastRequestedAt: FieldValue.serverTimestamp(),
    };

    if (!snapshot.exists) {
      update.createdAt = FieldValue.serverTimestamp();
    }

    transaction.set(usageRef, update, {merge: true});
    return quota;
  });
}

function dailyLimitMessage(limit: number): string {
  return `You have used today's ${limit} AI recommendations. Use the local protocol for now and check back tomorrow.`;
}

function validateRecommendationRequest(data: unknown): RecommendationRequest {
  if (!isRecord(data)) {
    throw new HttpsError("invalid-argument", "Request must be an object.");
  }

  const profile = data.profile;
  const checkIn = data.checkIn;
  const recentTrends = data.recentTrends;

  if (!isRecord(profile) || !isRecord(checkIn)) {
    throw new HttpsError("invalid-argument", "Request must include profile and checkIn objects.");
  }

  const request: RecommendationRequest = {
    profile: {
      trainingStyle: readString(profile.trainingStyle, "profile.trainingStyle"),
      weeklyTrainingFrequency: readRange(profile.weeklyTrainingFrequency, "profile.weeklyTrainingFrequency", 1, 14),
      goals: readStringArray(profile.goals, "profile.goals"),
      preferredSessionLength: readDuration(profile.preferredSessionLength, "profile.preferredSessionLength"),
      raceName: readOptionalString(profile.raceName),
      raceDate: readOptionalString(profile.raceDate),
    },
    checkIn: {
      energy: readRange(checkIn.energy, "checkIn.energy", 1, 10),
      soreness: readRange(checkIn.soreness, "checkIn.soreness", 1, 10),
      stress: readRange(checkIn.stress, "checkIn.stress", 1, 10),
      workoutType: readString(checkIn.workoutType, "checkIn.workoutType"),
      workoutIntensity: readRange(checkIn.workoutIntensity, "checkIn.workoutIntensity", 1, 10),
      timeAvailable: readDuration(checkIn.timeAvailable, "checkIn.timeAvailable"),
      sessionIntent: readOptionalString(checkIn.sessionIntent) || "post_workout",
    },
    recentTrends: isRecord(recentTrends) ? {
      energy: readTrend(recentTrends.energy),
      soreness: readTrend(recentTrends.soreness),
      stress: readTrend(recentTrends.stress),
    } : {},
  };

  return request;
}

function deterministicRecommendation(request: RecommendationRequest): RecommendationResult {
  const checkIn = request.checkIn;
  const goals = request.profile.goals;
  const highLoad = checkIn.workoutIntensity >= 7 || checkIn.soreness >= 7;
  const highStress = checkIn.stress >= 7;
  const lowEnergy = checkIn.energy <= 4;
  const wantsSleepSupport = goals.includes("sleep support");
  const risingStress = isRising(request.recentTrends?.stress ?? []);

  if (checkIn.sessionIntent === "pre_workout") {
    return {
      protocol: "Activation",
      durationMinutes: checkIn.timeAvailable,
      rationale: "You chose pre-workout breathwork, so this session is designed to sharpen focus and controlled arousal before training.",
      cadence: "3 second inhale · 3 second exhale",
      source: "deterministicFallback",
      breathworkProtocol: activation(checkIn.timeAvailable),
    };
  }

  if (checkIn.sessionIntent === "evening_downshift") {
    return {
      protocol: "Sleep transition",
      durationMinutes: checkIn.timeAvailable,
      rationale: "You chose evening breathwork, so this session emphasizes longer exhales to help shift toward sleep and recovery.",
      cadence: "4 second inhale · 7 second exhale",
      source: "deterministicFallback",
      breathworkProtocol: sleepTransition(checkIn.timeAvailable),
    };
  }

  if (checkIn.sessionIntent === "general_reset") {
    return {
      protocol: "Recovery reset",
      durationMinutes: checkIn.timeAvailable,
      rationale: "You chose a general reset, so this keeps the cadence balanced and restorative without assuming a workout just happened.",
      cadence: "4 second inhale · 4 second exhale",
      source: "deterministicFallback",
      breathworkProtocol: recoveryReset(checkIn.timeAvailable),
    };
  }

  if (checkIn.sessionIntent === "post_workout" && highLoad && highStress) {
    return {
      protocol: "Downregulation",
      durationMinutes: checkIn.timeAvailable,
      rationale: "You chose post-workout breathwork and your load/stress is high, so this uses extended exhales to downshift.",
      cadence: "4 second inhale · 6 second exhale",
      source: "deterministicFallback",
      breathworkProtocol: downregulation(checkIn.timeAvailable),
    };
  }

  if (checkIn.sessionIntent === "post_workout") {
    return {
      protocol: "Post-training recovery",
      durationMinutes: checkIn.timeAvailable,
      rationale: "You chose post-workout breathwork, so this focuses on a steady transition into recovery.",
      cadence: "4 second inhale · 5 second exhale",
      source: "deterministicFallback",
      breathworkProtocol: postTrainingRecovery(checkIn.timeAvailable),
    };
  }

  if (wantsSleepSupport && (highStress || risingStress)) {
    return {
      protocol: "Sleep transition",
      durationMinutes: checkIn.timeAvailable,
      rationale: "Your goals include sleep support and stress is elevated, so this shifts the body toward a calmer night state.",
      cadence: "4 second inhale · 7 second exhale",
      source: "deterministicFallback",
      breathworkProtocol: sleepTransition(checkIn.timeAvailable),
    };
  }

  if (highLoad && highStress) {
    return {
      protocol: "Downregulation",
      durationMinutes: checkIn.timeAvailable,
      rationale: "High training load plus stress calls for extended exhales and a fast shift out of sympathetic drive.",
      cadence: "4 second inhale · 6 second exhale",
      source: "deterministicFallback",
      breathworkProtocol: downregulation(checkIn.timeAvailable),
    };
  }

  if (lowEnergy && checkIn.workoutType === "Recovery") {
    return {
      protocol: "Recovery reset",
      durationMinutes: checkIn.timeAvailable,
      rationale: "Low energy on a lighter day points to a calm reset instead of more stimulation.",
      cadence: "4 second inhale · 4 second exhale",
      source: "deterministicFallback",
      breathworkProtocol: recoveryReset(checkIn.timeAvailable),
    };
  }

  if (checkIn.workoutIntensity <= 4 && checkIn.energy >= 7) {
    return {
      protocol: "Activation",
      durationMinutes: checkIn.timeAvailable,
      rationale: "Your recovery cost is low and energy is available, so the session can sharpen focus without overloading you.",
      cadence: "3 second inhale · 3 second exhale",
      source: "deterministicFallback",
      breathworkProtocol: activation(checkIn.timeAvailable),
    };
  }

  return {
    protocol: "Post-training recovery",
    durationMinutes: checkIn.timeAvailable,
    rationale: "Your check-in suggests moderate load. This keeps the protocol steady, controlled, and recovery-oriented.",
    cadence: "4 second inhale · 5 second exhale",
    source: "deterministicFallback",
    breathworkProtocol: postTrainingRecovery(checkIn.timeAvailable),
  };
}

function downregulation(durationMinutes: number): BreathworkProtocol {
  return protocol("downregulation", "Downregulation", durationMinutes, [
    phase("Inhale", 4, "Draw air in through the nose", 1.12),
    phase("Exhale", 6, "Let the exhale do the work", 0.72),
  ]);
}

function sleepTransition(durationMinutes: number): BreathworkProtocol {
  return protocol("sleep_transition", "Sleep transition", durationMinutes, [
    phase("Inhale", 4, "Slow nasal inhale", 1.08),
    phase("Exhale", 7, "Long quiet exhale", 0.68),
    phase("Rest", 1, "Soften the jaw", 0.68),
  ]);
}

function recoveryReset(durationMinutes: number): BreathworkProtocol {
  return protocol("recovery", "Recovery reset", durationMinutes, [
    phase("Inhale", 4, "Expand the ribs", 1.08),
    phase("Exhale", 4, "Relax the shoulders", 0.78),
  ]);
}

function activation(durationMinutes: number): BreathworkProtocol {
  return protocol("activation", "Activation", durationMinutes, [
    phase("Inhale", 3, "Crisp controlled inhale", 1.14),
    phase("Exhale", 3, "Controlled reset", 0.82),
  ]);
}

function postTrainingRecovery(durationMinutes: number): BreathworkProtocol {
  return protocol("post_training_recovery", "Post-training recovery", durationMinutes, [
    phase("Inhale", 4, "Breathe into the low ribs", 1.1),
    phase("Exhale", 5, "Drop the heart rate down", 0.74),
  ]);
}

function protocol(
  category: string,
  title: string,
  durationMinutes: number,
  phases: BreathPhase[],
): BreathworkProtocol {
  return {
    category,
    title,
    durationMinutes,
    phases,
    ambientTrackName: "ambient_loop",
  };
}

function phase(
  label: BreathPhase["label"],
  seconds: number,
  instruction: string,
  scaleTarget: number,
): BreathPhase {
  return {label, seconds, instruction, scaleTarget};
}

function readString(value: unknown, field: string): string {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new HttpsError("invalid-argument", `${field} must be a non-empty string.`);
  }
  return value.trim();
}

function readOptionalString(value: unknown): string {
  return typeof value === "string" ? value.trim() : "";
}

function readStringArray(value: unknown, field: string): string[] {
  if (!Array.isArray(value)) {
    throw new HttpsError("invalid-argument", `${field} must be an array.`);
  }
  return value.filter((item): item is string => typeof item === "string" && item.trim().length > 0);
}

function readRange(value: unknown, field: string, min: number, max: number): number {
  if (typeof value !== "number" || !Number.isFinite(value) || value < min || value > max) {
    throw new HttpsError("invalid-argument", `${field} must be a number from ${min} to ${max}.`);
  }
  return Math.round(value);
}

function readDuration(value: unknown, field: string): number {
  const duration = readRange(value, field, 1, 15);
  return [3, 5, 10].includes(duration) ? duration : 5;
}

function readTrend(value: unknown): number[] {
  if (!Array.isArray(value)) return [];
  return value
    .filter((item): item is number => typeof item === "number" && Number.isFinite(item))
    .map((item) => Math.round(Math.max(1, Math.min(10, item))))
    .slice(0, 7);
}

function isRising(values: number[]): boolean {
  const trend = values.filter((value) => value > 0).slice(0, 4);
  if (trend.length < 3) return false;

  const baseline = trend.slice(1).reduce((sum, value) => sum + value, 0) / (trend.length - 1);
  return trend[0] > baseline;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
