package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.jiraactions.api.*
import com.atlassian.performance.tools.report.api.*
import com.atlassian.performance.tools.report.api.judge.BaselineComparingJudge
import com.atlassian.performance.tools.virtualusers.api.VirtualUserLoad
import com.atlassian.performance.tools.workspace.api.TestWorkspace
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.Assert.assertThat
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Duration.ofMillis
import java.time.Duration.ofMinutes
import java.time.Instant

class CohortResultTest {

    @Test
    fun shouldCropColdCache() {
        val result = LocalRealResult(Paths.get("JIRA-JPT-9107")).load()

        val metrics = result.prepareForJudgement(
            ColdCachesTimeline()
        ).actionMetrics

        val actualEarliest = metrics
            .filter { it.label == BROWSE_BOARDS.label }
            .sortedBy { it.start }
            .first()
        val earlyAndCold = ActionMetric.Builder(
            label = BROWSE_BOARDS.label,
            start = Instant.parse("2018-04-27T09:22:00.537Z"),
            result = ActionResult.OK,
            duration = ofMillis(15359)
        ).build()
        assertThat(actualEarliest, not(equalTo(earlyAndCold)))
    }

    @Test
    fun shouldCropStragglers() {
        val result = LocalRealResult(Paths.get("JIRA-JPTS1-23")).load()

        val metrics = result.prepareForJudgement(
            TestExecutionTimeline(ofMinutes(20))
        ).actionMetrics

        val actualLatest = metrics
            .sortedByDescending { it.start }
            .first()
        val straggler = ActionMetric.Builder(
            label = SEARCH_WITH_JQL.label,
            start = Instant.parse("2018-04-27T15:12:27.740Z"),
            result = ActionResult.ERROR,
            duration = ofMinutes(5) + ofMillis(10)
        ).build()
        assertThat(actualLatest, not(equalTo(straggler)))
    }

    @Test
    fun shouldNotBeAffectedByOutliers() {
        val anticipatedLoad = VirtualUserLoad(
            virtualUsers = 20,
            flat = Duration.ofMinutes(10)
        )
        val criteria = PerformanceCriteria(
            mapOf(
                CREATE_ISSUE_SUBMIT to Criteria(
                    toleranceRatio = 0.04F,
                    minimumSampleSize = 12,
                    acceptableErrorCount = Int.MAX_VALUE
                )
            ),
            virtualUserLoad = anticipatedLoad,
            maxVirtualUsersImbalance = 18,
            nodes = 2
        )
        val resultsPath = Paths.get("CREATE-ISSUE-WITH-OUTLIERS")
        val workspace = TestWorkspace(Files.createTempDirectory("cr-workspace"))
        val rawAlphaResults = LocalRealResult(resultsPath.resolve("alpha")).loadRaw()
        val rawBetaResults = LocalRealResult(resultsPath.resolve("beta")).loadRaw()
        val timeline: Timeline = FullTimeline()
        val edibleAlphaResults = rawAlphaResults.prepareForJudgement(timeline)
        val edibleBetaResults = rawBetaResults.prepareForJudgement(timeline)
        val alphaStats = edibleAlphaResults.stats
        val betaStats = edibleBetaResults.stats

        val verdict = BaselineComparingJudge().judge(
            baselineStats = betaStats,
            experimentStats = alphaStats,
            performanceCriteria = criteria
        )

        verdict.assertAccepted("dontCare", workspace.directory, expectedReportCount = 2)
    }
}
