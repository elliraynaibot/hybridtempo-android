package com.hybridtempo.android.data.seed

import com.hybridtempo.android.domain.model.BreathSkillCategory
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BreathSkillSeedTest {
    @Test
    fun seedCatalogContainsTheInitialFifteenBreathSkills() {
        assertEquals(15, BreathSkillSeed.skills.size)
    }

    @Test
    fun seedCatalogUsesStableUniqueIds() {
        val ids = BreathSkillSeed.skills.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.isNotBlank() })
        assertTrue(ids.all { it == it.lowercase() })
        assertTrue(ids.all { id -> id.all { it.isLowerCase() || it.isDigit() || it == '-' } })
    }

    @Test
    fun seedCatalogCoversThePrdTrainingMoments() {
        val categories = BreathSkillSeed.skills.map { it.category }.toSet()

        assertTrue(BreathSkillCategory.BEFORE_TRAINING in categories)
        assertTrue(BreathSkillCategory.DURING_TRAINING in categories)
        assertTrue(BreathSkillCategory.AFTER_TRAINING in categories)
        assertTrue(BreathSkillCategory.SKILL_BASICS in categories)
    }

    @Test
    fun seedCatalogIncludesRequiredHeroSkills() {
        val skillById = BreathSkillSeed.skills.associateBy { it.id }

        assertNotNull(skillById["avoid-early-spike"])
        assertNotNull(skillById["between-rep-recovery"])
        assertNotNull(skillById["post-conditioning-downshift"])
        assertNotNull(skillById["evening-training-wind-down"])
        assertNotNull(skillById["cooldown-hr-recovery"])
    }

    @Test
    fun seedSkillsHaveEnoughInformationToPowerRecommendationAndReview() {
        BreathSkillSeed.skills.forEach { skill ->
            assertTrue(skill.title.isNotBlank(), "${skill.id} is missing a title")
            assertTrue(skill.athleteProblem.isNotBlank(), "${skill.id} is missing an athlete problem")
            assertTrue(skill.goal.isNotBlank(), "${skill.id} is missing a goal")
            assertTrue(skill.instructions.isNotEmpty(), "${skill.id} is missing instructions")
            assertTrue(skill.trainingCue.isNotBlank(), "${skill.id} is missing a training cue")
            assertTrue(skill.measurementFocus.isNotBlank(), "${skill.id} is missing a measurement focus")
            assertTrue(skill.safetyNotes.isNotBlank(), "${skill.id} is missing safety notes")
            assertTrue(skill.durationOptionsMinutes.isNotEmpty(), "${skill.id} is missing duration options")
            assertTrue(skill.bestForWorkoutTypes.isNotEmpty(), "${skill.id} is missing workout matching")
        }
    }

    @Test
    fun seedSkillsDoNotIncludeUnsafeBreathworkPatterns() {
        val blockedPhrases = listOf(
            "max breath hold",
            "maximum breath hold",
            "hyperventilation",
            "underwater",
            "push through dizziness",
            "push through chest pain",
        )

        BreathSkillSeed.skills.forEach { skill ->
            val searchableText = buildString {
                appendLine(skill.title)
                appendLine(skill.athleteProblem)
                appendLine(skill.goal)
                appendLine(skill.instructions.joinToString(separator = "\n"))
                appendLine(skill.trainingCue)
                appendLine(skill.measurementFocus)
                appendLine(skill.safetyNotes)
            }.lowercase()

            blockedPhrases.forEach { phrase ->
                assertFalse(searchableText.contains(phrase), "${skill.id} contains unsafe phrase '$phrase'")
            }
        }
    }
}
