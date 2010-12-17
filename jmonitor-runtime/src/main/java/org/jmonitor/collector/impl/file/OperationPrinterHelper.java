/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jmonitor.collector.impl.file;

import java.io.PrintWriter;
import java.lang.Thread.State;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jmonitor.api.probe.ProbeExecutionContext;
import org.jmonitor.collector.service.model.MetricDataItem;
import org.jmonitor.collector.service.model.Operation;
import org.jmonitor.collector.service.model.SampledHotspotTreeNode;
import org.jmonitor.collector.service.model.TraceEvent;
import org.jmonitor.configuration.service.model.AgentConfiguration;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// package protected
class OperationPrinterHelper { // NOPMD for too many methods

    private static final String DOT_DOT_DOT = "...";

    private static final double NANOSECONDS_PER_MILLISECOND = 1000000.0;

    private static final String TOTAL_HEADER = "total";
    private static final String AVERAGE_HEADER = "average";
    private static final String MINIMUM_HEADER = "minimum";
    private static final String MAXIMUM_HEADER = "maximum";
    private static final String COUNT_HEADER = "count";

    private static final Ordering<SampledHotspotTreeNode> SAMPLED_CALL_TREE_NODE_ORDERING =
            new Ordering<SampledHotspotTreeNode>() {
                public int compare(SampledHotspotTreeNode node1, SampledHotspotTreeNode node2) {
                    // reverse sort
                    return node2.getSampleCount() - node1.getSampleCount();
                }
            };

    private static final Ordering<MetricDataItem> METRIC_DATA_ITEM_ORDERING =
            new Ordering<MetricDataItem>() {
                public int compare(MetricDataItem item1, MetricDataItem item2) {
                    return item1.getName().compareToIgnoreCase(item2.getName());
                }
            };

    private static final Ordering<Map.Entry<State, Integer>> THREAD_STATE_ORDERING =
            new Ordering<Map.Entry<State, Integer>>() {
                public int compare(Map.Entry<State, Integer> entry1,
                        Map.Entry<State, Integer> entry2) {
                    return entry1.getValue() - entry2.getValue();
                }
            };

    private final Operation operation;
    private final PrintWriter out;
    private long logNanoTime;

    private int totalSampleCount;

    private int percentageColumnWidth;

    public OperationPrinterHelper(Operation operation, PrintWriter out) {
        this.operation = operation;
        this.out = out;
    }

    public void logOperation(int maxTraceEvents) {

        // we use logNanoTime to make sure we get a consistent snapshot of the timing data in the
        // operation this is really only needed for operations which have not yet completed
        logNanoTime = System.nanoTime();
        Date logTime = new Date();

        boolean completedAsOfLogNanoTime = operation.isCompleted();

        out.println(OperationPrinter.HEADING1);

        if (operation.isStuck()) {
            if (operation.isCompleted()) {
                out.println("UNSTUCK");
            } else {
                out.println("STUCK");
            }
        }

        if (operation.getUniqueId() != 0) {
            // only need unique id for matching up log entries for operations that are flushed prior
            // to completion
            out.print("unique id:     ");
            out.println(operation.getUniqueId());
        }

        out.print("start time:    ");
        out.println(FormatUtils.formatWithMilliseconds(operation.getStartTime()));
        out.print("end time:      ");
        if (completedAsOfLogNanoTime) {
            long durationInMilliseconds =
                    TimeUnit.NANOSECONDS.toMillis(operation.getDurationInNanoseconds());
            Date endTimeDate =
                    new Date(operation.getStartTime().getTime() + durationInMilliseconds);
            out.println(FormatUtils.formatWithMilliseconds(endTimeDate));
        } else {
            out.print(FormatUtils.formatWithMilliseconds(logTime));
            out.println("..");
        }
        out.print("duration:      ");
        out.print(formatDurationInSeconds());
        out.println(" seconds");
        if (operation.getUsername() != null) {
            out.print("username:      ");
            out.println(operation.getUsername());
        }

        List<String> threadNames = Lists.newArrayList(operation.getThreadNames());
        if (threadNames.size() > 1) {
            out.print("thread names:  ");
        } else {
            out.print("thread name:   ");
        }
        for (Iterator<String> i = threadNames.iterator(); i.hasNext();) {
            out.print("\"");
            out.print(i.next());
            out.print("\"");
            if (i.hasNext()) {
                out.print(", ");
            }
        }
        out.println();
        out.println(OperationPrinter.HEADING1);
        out.println();

        // write root element data
        writeTraceRootElement();

        // write metric data items
        writeMetricDataItems(operation.getMetricData().getItems());

        // write contextual trace elements
        writeTrace(maxTraceEvents);

        // write sampled hotspot tree
        writeSampledHotspotTree();
    }

    private String formatDurationInSeconds() {
        if (operation.isCompleted()) {
            return FormatUtils.formatNanosecondsAsSeconds(operation.getDurationInNanoseconds());
        } else {
            long elapsedNanoTime = logNanoTime - operation.getStartNanoTime();
            return FormatUtils.formatNanosecondsAsSeconds(elapsedNanoTime) + "..";
        }
    }

    private void writeTraceRootElement() {

        FormatUtils.printHeader(out, "operation summary");

        ProbeExecutionContext rootTraceEvent =
                operation.getTrace().getEvents().iterator().next().getContext();

        writeContextMap(rootTraceEvent, 0);
        out.println();
    }

    private void writeContextMap(ProbeExecutionContext contextMap, int level) {

        for (Map.Entry<String, String> entry : contextMap.getMap().entrySet()) {

            FormatUtils.printPadding(out, level * 2);
            out.print(entry.getKey());
            out.print(": ");
            out.print(entry.getValue());
            out.println();
        }

        for (Map.Entry<String, ProbeExecutionContext> entry : contextMap.getNestedMaps().entrySet()) {

            FormatUtils.printPadding(out, level * 2);
            if (!entry.getValue().getMap().isEmpty()) {
                // don't display key if the map is empty
                out.print(entry.getKey());
                out.println();
                writeContextMap(entry.getValue(), level + 1);
            }
        }
    }

    private void writeTrace(int maxTraceEvents) {

        FormatUtils.printHeader(out, "execution trace events", "+offsets are in seconds",
                "(durations) are in milliseconds");

        // calculate display widths for start time offset and duration

        // the total duration is the largest possible offset so this is what we base the width on
        int startTimeOffsetColumnWidth = getLargestPossibleStartTimeOffset();

        // the root duration is the largest and longest so this is
        // what we base the width on
        TraceEvent rootTraceEvent =
                operation.getTrace().getEvents().iterator().next();
        String rootDuration = formatDurationInMilliseconds(rootTraceEvent);
        int durationColumnWidth = rootDuration.length();

        int count = 0;
        int lastIndex = -1;
        for (TraceEvent traceEvent : operation.getTrace().getEvents()) {

            if (maxTraceEvents != AgentConfiguration.TRACE_EVENTS_LIMIT_DISABLED
                    && count++ >= maxTraceEvents) {
                break;
            }

            if (traceEvent.getIndex() != lastIndex + 1) {
                // the +1 accounts for the "+" at the beginning of the offset times
                FormatUtils.printPadding(out, startTimeOffsetColumnWidth - DOT_DOT_DOT.length() + 1);
                out.println(DOT_DOT_DOT);
            }
            lastIndex = traceEvent.getIndex();

            // print offset time (in seconds)
            String offsetTimeText = formatOffsetTimeInSeconds(traceEvent);
            FormatUtils.printPadding(out, startTimeOffsetColumnWidth - offsetTimeText.length());
            out.print("+");
            out.print(offsetTimeText);

            // print duration (in milliseconds)
            out.print("  ");
            String durationText = formatDurationInMilliseconds(traceEvent);
            FormatUtils.printPadding(out, 2 * traceEvent.getLevel() + durationColumnWidth
                    - durationText.length());
            out.print("(");
            out.print(durationText);
            out.print(")");

            // print description
            out.print("  ");
            out.println(traceEvent.getDescription());
        }

        out.println();
    }

    private int getLargestPossibleStartTimeOffset() {
        if (operation.isCompleted()) {
            return FormatUtils.formatNanosecondsAsSeconds(operation.getDurationInNanoseconds()).length();
        } else {
            return FormatUtils.formatNanosecondsAsSeconds(
                    logNanoTime - operation.getStartNanoTime()).length();
        }
    }

    private String formatOffsetTimeInSeconds(TraceEvent traceEvent) {
        long startNanoTimeOffset = traceEvent.getOffsetInNanoseconds();
        return FormatUtils.formatNanosecondsAsSeconds(startNanoTimeOffset);
    }

    private String formatDurationInMilliseconds(TraceEvent traceEvent) {
        if (traceEvent.isCompleted()) {
            return FormatUtils.formatNanosecondsAsMilliseconds(traceEvent.getDurationInNanoseconds());
        } else {
            return FormatUtils.formatNanosecondsAsMilliseconds(logNanoTime
                    - operation.getStartNanoTime() - traceEvent.getOffsetInNanoseconds())
                    + "..";
        }
    }

    public void writeSampledHotspotTree() {

        Iterable<? extends SampledHotspotTreeNode> rootNodes =
                operation.getSampledHotspotTree().getRootNodes();

        // TODO this conditional may not be needed in the future if SampledHotspotTree itself
        // is lazy instantiated
        if (rootNodes == null) {
            return;
        }

        List<? extends SampledHotspotTreeNode> sortedRootNodes =
                SAMPLED_CALL_TREE_NODE_ORDERING.sortedCopy(rootNodes);

        totalSampleCount = 0;
        for (SampledHotspotTreeNode rootNode : sortedRootNodes) {
            totalSampleCount += rootNode.getSampleCount();
        }
        // the percentage of the first root node is the largest and longest (since we just sorted
        // the list) so this is what we base the width on
        double largestPercentage =
                sortedRootNodes.get(0).getSampleCount() / (double) totalSampleCount;
        percentageColumnWidth = FormatUtils.formatPercentage(largestPercentage).length();

        FormatUtils.printHeader(out, "call tree", "created from " + totalSampleCount
                + " stack trace samples");

        for (SampledHotspotTreeNode rootNode : sortedRootNodes) {
            writeSampledTreeCallNode(rootNode, "", "");
        }

        out.println();
    }

    private void writeSampledTreeCallNode(SampledHotspotTreeNode node, String indent,
            String recurseIndent) {

        printSampledHotspotTreePercentageColumn(node.getSampleCount());
        out.print(indent);
        out.println(node.getStackTraceElement());

        if (!node.getLeafThreadStateSampleCounts().isEmpty()) {
            printLeafNode(node, indent);
        }

        // order nodes by leafTotalCount (descending) so the "hotspots" should be listed first
        List<? extends SampledHotspotTreeNode> sortedChildNodes =
                SAMPLED_CALL_TREE_NODE_ORDERING.sortedCopy(node.getChildNodes());

        if (sortedChildNodes.isEmpty()) {
            return;
        }

        // recurse (depth-first)
        String newIndent;
        String newRecurseIndent;
        String lastNodeRecurseIndent;
        if (sortedChildNodes.size() > 1) {
            // indent an additional level at each split in the tree
            newIndent = recurseIndent + "+- ";
            newRecurseIndent = recurseIndent + "|  ";
            lastNodeRecurseIndent = recurseIndent + "   ";
        } else {
            newIndent = recurseIndent;
            newRecurseIndent = recurseIndent;
            lastNodeRecurseIndent = recurseIndent;
        }

        for (Iterator<? extends SampledHotspotTreeNode> i = sortedChildNodes.iterator(); i.hasNext();) {
            SampledHotspotTreeNode childNode = i.next();
            if (i.hasNext()) {
                // not at the last child node for this parent
                writeSampledTreeCallNode(childNode, newIndent, newRecurseIndent);
            } else {
                // last applicable child node for this parent (based on logNanoTime)
                writeSampledTreeCallNode(childNode, newIndent, lastNodeRecurseIndent);
            }
        }
    }

    private void printLeafNode(SampledHotspotTreeNode leafNode, String indent) {

        List<Map.Entry<State, Integer>> sortedEntries =
                THREAD_STATE_ORDERING.sortedCopy(leafNode.getLeafThreadStateSampleCounts().entrySet());

        String additionalIndent = "";
        if (sortedEntries.size() > 1) {
            additionalIndent = "   ";
        }

        for (Map.Entry<State, Integer> entry : sortedEntries) {
            // print percentage
            printSampledHotspotTreePercentageColumn(entry.getValue());
            out.print(indent);
            out.print(additionalIndent);
            // print thread state
            out.println(entry.getKey());
        }
    }

    private void printSampledHotspotTreePercentageColumn(int leafCount) {
        String percentageText = FormatUtils.formatPercentage(leafCount / (double) totalSampleCount);
        FormatUtils.printPadding(out, percentageColumnWidth - percentageText.length());
        out.print(percentageText);
        out.print("  ");
    }

    public void writeMetricDataItems(Iterable<? extends MetricDataItem> items) {

        FormatUtils.printHeader(out, "metric data", "times are in milliseconds");

        // sort by name
        List<? extends MetricDataItem> sortedItems = METRIC_DATA_ITEM_ORDERING.sortedCopy(items);

        if (sortedItems.isEmpty()) {
            out.println("<none>");
            out.println();
            return;
        }

        // TODO only print those >= 1% of total

        // calculate display widths for metric data table
        int nameWidth = 0;
        for (MetricDataItem item : sortedItems) {
            nameWidth = Math.max(nameWidth, item.getName().length());
        }

        String[] headers =
                new String[] {TOTAL_HEADER, AVERAGE_HEADER, MINIMUM_HEADER, MAXIMUM_HEADER,
                        COUNT_HEADER};
        // +1 for the number of rows because the first row is the header row
        // +1 for the number of columns because the first column is the metric data item label
        String[][] rows = new String[sortedItems.size() + 1][headers.length + 1];
        // first row is the header row (offset by an empty
        rows[0] = new String[headers.length + 1];
        System.arraycopy(headers, 0, rows[0], 1, headers.length);

        final NumberFormat millisecondsFormat = new DecimalFormat("#,##0.0");

        int i = 1; // NOPMD for short name "i"
        for (MetricDataItem item : sortedItems) {
            int j = 0; // NOPMD for short name "j"
            rows[i][j++] = item.getName() + ":";
            rows[i][j++] =
                    millisecondsFormat.format(item.getTotalTimeInNanoseconds()
                            / NANOSECONDS_PER_MILLISECOND);
            rows[i][j++] =
                    millisecondsFormat.format(item.getAverageTimeInNanoseconds()
                            / NANOSECONDS_PER_MILLISECOND);
            rows[i][j++] =
                    millisecondsFormat.format(item.getMinimumTimeInNanoseconds()
                            / NANOSECONDS_PER_MILLISECOND);
            rows[i][j++] =
                    millisecondsFormat.format(item.getMaximumTimeInNanoseconds()
                            / NANOSECONDS_PER_MILLISECOND);
            rows[i][j++] = Long.toString(item.getCount());
            i++;
        }

        FormatUtils.printLeftPaddedTable(out, rows);
        out.println();
    }
}
