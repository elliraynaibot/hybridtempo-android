package com.hybridtempo.android.recommendation

class BackendRecommendationEngine(
    private val fallback: RecommendationEngine = DeterministicRecommendationEngine(),
) : RecommendationEngine {
    override fun recommend(request: RecommendationRequest): RecommendationResponse {
        // Future integration point:
        // Call Firebase Cloud Functions / Firebase AI Logic with RecommendationRequest,
        // validate the structured response, and fall back to the deterministic engine
        // if the backend is unavailable or returns an unsafe protocol.
        return fallback.recommend(request)
    }
}
