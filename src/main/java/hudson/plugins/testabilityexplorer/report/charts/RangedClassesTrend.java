package hudson.plugins.testabilityexplorer.report.charts;

import hudson.plugins.testabilityexplorer.report.charts.CostTemplate;
import hudson.plugins.testabilityexplorer.report.costs.Statistic;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import hudson.model.AbstractBuild;

import java.util.List;
import java.util.Collection;

import org.jfree.data.category.CategoryDataset;

/**
 * Helper class that is able to construct a JFree {@link CategoryDataset} which may be used to draw a line chart showing class testability
 * distribution. The testability distribution is classes that {@code need work}, are {@code good} or even {@code excellent}.
 * A {@link RangedClassesTrend} can also calculate the corrent upper bound Integer value which may be used to limit the range
 * axis in size.
 *
 * @author reik.schatz
 */
public class RangedClassesTrend extends RangedTrend
{
    public static final CostTemplate EXCELLENT_COST_TEMPLATE = new CostTemplate()
    {
        public int getCost(Statistic statistic)
        {
            return statistic.getCostSummary().getExcellent();
        }
    };

    public static final CostTemplate GOOD_COST_TEMPLATE = new CostTemplate()
    {
        public int getCost(Statistic statistic)
        {
            return statistic.getCostSummary().getGood();
        }
    };

    public static final CostTemplate POOR_COST_TEMPLATE = new CostTemplate()
    {
        public int getCost(Statistic statistic)
        {
            return statistic.getCostSummary().getNeedsWork();
        }
    };

    public RangedClassesTrend(List<BuildAndResults> items)
    {
        super(items);
    }

    /**
     * Returns an Integer which may be used in a JFree line chart to call {@link org.jfree.chart.axis.NumberAxis#setUpperBound}
     * with a correct value.
     *
     * @return int
     */
    public int getUpperBoundRangeAxis()
    {
        int maxCost = RangedTrend.DEFAULT_RANGE_AXIS;  // default range on the y-axis

        CostTemplate[] costTemplates = new CostTemplate[] {EXCELLENT_COST_TEMPLATE, GOOD_COST_TEMPLATE, POOR_COST_TEMPLATE};
        for (BuildAndResults buildAndResults : getItems())
        {
            Collection<Statistic> results = buildAndResults.getStatistics();
            for (CostTemplate costTemplate : costTemplates)
            {
                int cost = summarizeCost(results, costTemplate);
                maxCost = Math.max(maxCost, cost);
            }
        }

        return maxCost > RangedTrend.DEFAULT_RANGE_AXIS ? maxCost + RangedTrend.RANGE_AXIS_SPACE : maxCost;    // add some space on top
    }

    /**
     * Builds and returns a {@link CategoryDataset} represents the classes testability distribution trend. This
     * {@link CategoryDataset} can be used to draw a JFree line chart.
     *
     * @return CategoryDataset
     */
    public CategoryDataset getCategoryDataset()
    {
        final DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

        Collection<Statistic> previousNonEmptyResults = null; // this is needed so that the graphline will not be interrupted
        for (BuildAndResults buildAndResults : getItems())
        {
            AbstractBuild<?, ?> build = buildAndResults.getBuild();
            Collection<Statistic> results = buildAndResults.getStatistics();
            if (results.isEmpty())
            {
                results = previousNonEmptyResults != null ? previousNonEmptyResults : results;
            }
            else
            {
                previousNonEmptyResults = results;
            }

            ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(build);
            dsb.add(summarizeCost(results, EXCELLENT_COST_TEMPLATE), "excellent", label);
            dsb.add(summarizeCost(results, GOOD_COST_TEMPLATE), "good", label);
            dsb.add(summarizeCost(results, POOR_COST_TEMPLATE), "need work", label);
        }
        return dsb.build();
    }
}
