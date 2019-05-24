package com.atlassian.performance.tools.report.api

import com.atlassian.performance.tools.jiraactions.api.CREATE_ISSUE_SUBMIT
import com.atlassian.performance.tools.jiraactions.api.VIEW_ISSUE
import com.atlassian.performance.tools.jiraactions.api.parser.ActionMetricsParser
import com.atlassian.performance.tools.report.api.result.LocalRealResult
import com.atlassian.performance.tools.workspace.api.RootWorkspace
import org.junit.Test
import java.nio.file.Paths
import java.time.Duration

class WaterfallHighlightReportTest {

    @Test
    fun shouldReportMedian() {
        val metricsStream = javaClass.getResourceAsStream("/real-action-metrics-with-drilldown.jpt")
        val actionMetrics = ActionMetricsParser().parse(metricsStream)
        val workspace = RootWorkspace(Paths.get("build/jpt-workspace")).currentTask.isolateTest("WaterfallHighlight")
        val highlight = WaterfallHighlightReport()

        highlight.report(actionMetrics, workspace)
    }

    @Test
    fun aa() {
        val beta = LocalRealResult(Paths.get("tt")).load()
        val edibleBetaResults = beta.prepareForJudgement(FullTimeline())

        val actionMetrics = edibleBetaResults.actionMetrics
        val fast = actionMetrics
            .filter { it.label == VIEW_ISSUE.label }
            .filter { it.duration  < Duration.ofMillis(1100) }
            .toList()
        val slow = actionMetrics
            .filter { it.label == VIEW_ISSUE.label }
            .filter { it.duration >= Duration.ofMillis(1200) }
            .toList()

        val workspace1 = RootWorkspace(Paths.get("build/jpt-workspace")).currentTask.isolateTest("WaterfallHighlightFast")
        val workspace2 = RootWorkspace(Paths.get("build/jpt-workspace")).currentTask.isolateTest("WaterfallHighlightSlow")
        val highlight = WaterfallHighlightReport()

        highlight.report(fast, workspace1)
        highlight.report(slow, workspace2)

    }
}
