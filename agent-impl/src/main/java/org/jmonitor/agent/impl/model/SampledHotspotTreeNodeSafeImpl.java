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

package org.jmonitor.agent.impl.model;

import java.lang.Thread.State;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jmonitor.collector.service.model.SampledHotspotTreeNode;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO may not need volatile or concurrent data structures if all reads are also performed
// under same lock as writes
public class SampledHotspotTreeNodeSafeImpl implements SampledHotspotTreeNode {

    private final StackTraceElement stackTraceElement;
    private final Collection<SampledHotspotTreeNodeSafeImpl> childNodes =
            new ConcurrentLinkedQueue<SampledHotspotTreeNodeSafeImpl>();

    // these must be volatile since they are updated by one thread (the stack trace sampling
    // thread) and can be read by another thread (the executing thread for flushing or the
    // alerting thread)
    //
    // since we perform the stack traces under a synchronized lock (above)
    // we don't need to worry about concurrent updates which avoids the (slight) overhead
    // of using AtomicInteger
    // 
    // TODO maybe we should perform reads under synchronized lock to avoid volatile
    // and to ensure consistent state of read
    //
    private volatile int sampleCount;
    private volatile Map<State, Integer> leafThreadStateSampleCounts;
    // optimization for common case where there is just a single thread state recorded
    // so we don't have to instantiate map unless necessary
    private volatile State singleLeafState;

    public SampledHotspotTreeNodeSafeImpl(StackTraceElement stackTraceElement) {
        this.stackTraceElement = stackTraceElement;
        sampleCount = 1;
    }

    public void addChildNode(SampledHotspotTreeNodeSafeImpl methodTreeElement) {
        childNodes.add(methodTreeElement);
    }

    public void addLeafSampling(State threadState) {

        if (singleLeafState == null) {

            // first leaf sampling for this node
            singleLeafState = threadState;

        } else if (!threadState.equals(singleLeafState) && leafThreadStateSampleCounts == null) {

            // first leaf sampling of a different state than the single "best guess" state
            // leaving optimized state, now we have to instantiate the map
            leafThreadStateSampleCounts = new ConcurrentHashMap<State, Integer>();
            leafThreadStateSampleCounts.put(singleLeafState, sampleCount);
        }

        if (leafThreadStateSampleCounts != null) {
            Integer count = leafThreadStateSampleCounts.get(threadState);
            if (count == null) {
                leafThreadStateSampleCounts.put(threadState, 1);
            } else {
                leafThreadStateSampleCounts.put(threadState, count + 1);
            }
        }
    }

    // sampleCount is volatile to ensure visibility, but this method still needs to be called under
    // an appropriate lock so that two threads do not try to increment the count at the same time
    public void incrementSampleCount() {
        sampleCount++;
    }

    public Iterable<SampledHotspotTreeNodeSafeImpl> getChildNodes() {
        return childNodes;
    }

    public StackTraceElement getStackTraceElement() {
        return stackTraceElement;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public Map<State, Integer> getLeafThreadStateSampleCounts() {
        if (leafThreadStateSampleCounts == null && singleLeafState == null) {
            return Collections.emptyMap();
        } else if (leafThreadStateSampleCounts == null) {
            // optimized for common case with single "best guess" state
            return Collections.singletonMap(singleLeafState, sampleCount);
        } else {
            return leafThreadStateSampleCounts;
        }
    }
}
