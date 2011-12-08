/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmonitor.agent.impl.model;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jmonitor.collector.service.model.SampledCallTree;
import org.jmonitor.collector.service.model.SampledCallTreeNode;

/**
 * Stack trace tree built from stack traces captured by periodic calls to
 * {@link Thread#getStackTrace()}.
 * 
 * This can be either thread-specific sampled call tree tied to an operation, or it can be a global
 * sampled call tree across all threads.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class SampledCallTreeSafeImpl implements SampledCallTree {

    // this is lazy instantiated since most operations won't exceed the
    // threshold for capturing stack traces
    // and so initializing this would use up memory unnecessarily
    private Collection<SampledCallTreeNodeSafeImpl> rootNodes;

    private final Object lock = new Object();

    // this method returns an iterable with a "weakly consistent" iterator
    // that will never throw ConcurrentModificationException, see
    // ConcurrentLinkedQueue.iterator()
    public Iterable<? extends SampledCallTreeNode> getRootNodes() {
        return rootNodes;
    }

    public void captureStackTrace(Thread thread) {

        // we could reduce the scope of this lock considerably, but it probably only makes sense to
        // capture and build a single stack trace at a time anyways
        synchronized (lock) {

            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            ThreadInfo threadInfo = threadBean.getThreadInfo(thread.getId(), Integer.MAX_VALUE);

            addToStackTree(threadInfo.getStackTrace(), threadInfo.getThreadState());
        }
    }

    private void addToStackTree(StackTraceElement[] stackTraceElements, State threadState) {

        if (rootNodes == null) {
            rootNodes = new ConcurrentLinkedQueue<SampledCallTreeNodeSafeImpl>();
        }

        SampledCallTreeNodeSafeImpl lastMatchedNode = null;
        Iterable<SampledCallTreeNodeSafeImpl> nextChildNodes = rootNodes;
        int nextStackTraceIndex = 0;

        // navigate the stack tree nodes
        // matching the new stack trace as far as possible
        for (nextStackTraceIndex = stackTraceElements.length - 1; nextStackTraceIndex >= 0; nextStackTraceIndex--) {

            // check all child nodes
            boolean matchFound = false;
            for (SampledCallTreeNodeSafeImpl childNode : nextChildNodes) {
                if (stackTraceElements[nextStackTraceIndex]
                        .equals(childNode.getStackTraceElement())) {

                    // match found, update lastMatchedNode and continue
                    childNode.sampleCount++;
                    lastMatchedNode = childNode;
                    nextChildNodes = lastMatchedNode.getChildNodes();
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) {
                break;
            }
        }

        // add remaining stack trace elements
        for (int i = nextStackTraceIndex; i >= 0; i--) {

            SampledCallTreeNodeSafeImpl nextNode = new SampledCallTreeNodeSafeImpl(
                    stackTraceElements[i]);
            nextNode.sampleCount = 1;

            if (lastMatchedNode == null) {
                // new root node
                rootNodes.add(nextNode);
            } else {
                lastMatchedNode.addChildNode(nextNode);
            }
            lastMatchedNode = nextNode;
        }

        // add leaf sampling
        lastMatchedNode.addLeafSampling(threadState);
    }

    // TODO may not need volatile or concurrent data structures if all reads are also performed
    // under same lock as writes
    public static final class SampledCallTreeNodeSafeImpl implements SampledCallTreeNode {

        private final StackTraceElement stackTraceElement;
        private final Collection<SampledCallTreeNodeSafeImpl> childNodes =
                new ConcurrentLinkedQueue<SampledCallTreeNodeSafeImpl>();

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

        private SampledCallTreeNodeSafeImpl(StackTraceElement stackTraceElement) {
            this.stackTraceElement = stackTraceElement;
        }

        public void addChildNode(SampledCallTreeNodeSafeImpl methodTreeElement) {
            childNodes.add(methodTreeElement);
        }

        public void addLeafSampling(State threadState) {

            if (singleLeafState == null) {

                // first leaf sampling for this node
                singleLeafState = threadState;

            } else if (!threadState.equals(singleLeafState) 
                    && leafThreadStateSampleCounts == null) {

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

        public Iterable<SampledCallTreeNodeSafeImpl> getChildNodes() {
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
}
