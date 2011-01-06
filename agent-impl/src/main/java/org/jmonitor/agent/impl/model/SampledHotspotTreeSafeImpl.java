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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jmonitor.collector.service.model.SampledHotspotTree;
import org.jmonitor.collector.service.model.SampledHotspotTreeNode;

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
public class SampledHotspotTreeSafeImpl implements SampledHotspotTree {

    // this is lazy instantiated since most operations won't exceed the
    // threshold for capturing stack traces
    // and so initializing this would use up memory unnecessarily
    private Collection<SampledHotspotTreeNodeSafeImpl> rootNodes;

    private final Object lock = new Object();

    // this method returns an iterable with a "weakly consistent" iterator
    // that will never throw ConcurrentModificationException, see
    // ConcurrentLinkedQueue.iterator()
    public Iterable<? extends SampledHotspotTreeNode> getRootNodes() {
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
            rootNodes = new ConcurrentLinkedQueue<SampledHotspotTreeNodeSafeImpl>();
        }

        SampledHotspotTreeNodeSafeImpl lastMatchedNode = null;
        Iterable<SampledHotspotTreeNodeSafeImpl> nextChildNodes = rootNodes;
        int nextStackTraceIndex = 0;

        // navigate the stack tree nodes
        // matching the new stack trace as far as possible
        for (nextStackTraceIndex = stackTraceElements.length - 1; nextStackTraceIndex >= 0; nextStackTraceIndex--) {

            // check all child nodes
            boolean matchFound = false;
            for (SampledHotspotTreeNodeSafeImpl childNode : nextChildNodes) {
                if (stackTraceElements[nextStackTraceIndex].equals(childNode.getStackTraceElement())) {
                    // match found, update lastMatchedNode and continue
                    childNode.incrementSampleCount();
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

            SampledHotspotTreeNodeSafeImpl nextNode =
                    new SampledHotspotTreeNodeSafeImpl(stackTraceElements[i]);

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
}
