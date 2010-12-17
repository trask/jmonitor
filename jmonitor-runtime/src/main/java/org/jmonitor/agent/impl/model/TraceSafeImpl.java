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

import java.util.LinkedList;

import org.jmonitor.agent.impl.util.collection.FlushableList;
import org.jmonitor.api.probe.ProbeExecution;
import org.jmonitor.collector.service.model.Trace;
import org.jmonitor.collector.service.model.TraceEvent;
import org.jmonitor.collector.shared.logging.CollectorServiceLoggerFactory;
import org.jmonitor.util.NanoUtils;
import org.slf4j.Logger;

/**
 * Call tree with Contextual trace built from AspectJ pointcut probes.
 * 
 * This implementation also has logic built in to handle intermediate flushing of
 * {@link TraceEvent}s and tracking if any previously flushed {@link TraceEvent}s are
 * subsequently changes and need to be re-flushed at the next opportunity.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class TraceSafeImpl implements Trace {

    private static final Logger LOGGER =
            CollectorServiceLoggerFactory.getLogger(TraceSafeImpl.class);

    private final long startNanoTime;
    private volatile long endNanoTime;

    private volatile boolean completed;

    private final TraceEventSafeImpl rootElement;

    // TODO not thread safe, ok?
    private final LinkedList<TraceEventSafeImpl> elementStack = // NOPMD for not using List interface
            new LinkedList<TraceEventSafeImpl>();

    // this can be flushed to collector from time to time during a long
    // operation
    // it is flushed only by the executing thread
    // however it can be read
    private final FlushableList<TraceEventSafeImpl> elements =
            new FlushableList<TraceEventSafeImpl>(10, 10);

    private volatile int size;

    public TraceSafeImpl(ProbeExecution probeExecution) {

        startNanoTime = System.nanoTime();

        rootElement = new TraceEventSafeImpl(probeExecution, startNanoTime, startNanoTime, 0, -1, 0);

        pushElementInternal(rootElement);
    }

    public TraceEventSafeImpl getRootElement() {
        return rootElement;
    }

    public Iterable<TraceEventSafeImpl> getEvents() {
        return elements;
    }

    public Trace flush() {
        final Iterable<TraceEventSafeImpl> flushedElements = elements.flush();
        return new Trace() {
            public Iterable<TraceEventSafeImpl> getEvents() {
                return flushedElements;
            }
        };
    }

    public TraceEventSafeImpl getCurrentElement() {
        if (elementStack.isEmpty()) {
            return null;
        } else {
            return elementStack.getLast();
        }
    }

    public int getSize() {
        return size;
    }

    // typically pop() methods don't require the element to pop, but for safety we are passing
    // in the element to pop just to make sure it is the one on top
    // (and if not we pop until we find it, preventing any nasty bugs from a forgotten pop)
    //
    // throws NoSuchElementException if element is not found anywhere on stack
    //
    public void popElement(TraceEventSafeImpl element, long elementEndNanoTime) {

        element.setEndNanoTime(elementEndNanoTime);
        element.setCompleted(true);
        elements.justUpdatedPossiblyFlushedElement(element);

        TraceEventSafeImpl pop = elementStack.removeLast();

        if (!pop.equals(element)) {
            // maybe 'pop' didn't pop itself correctly so we log it
            LOGGER.error("found " + pop.getDescription()
                    + " at the top of the stack when expecting " + element.getDescription(),
                    new IllegalStateException());
            while (!elementStack.removeLast().equals(element)) {
            }
        }

        if (elementStack.isEmpty()) {
            endNanoTime = elementEndNanoTime;
            completed = true;
        }
    }

    public TraceEventSafeImpl pushElement(ProbeExecution probeExecution) {

        // pushElement() is only called by a single thread so we don't need to worry about
        // synchronizing updates to the size field

        TraceEventSafeImpl element =
                new TraceEventSafeImpl(probeExecution, startNanoTime, System.nanoTime(), size,
                        elementStack.getLast().getIndex(), elementStack.getLast().getLevel() + 1);

        pushElementInternal(element);

        return element;
    }

    private void pushElementInternal(TraceEventSafeImpl element) {

        elementStack.addLast(element);
        elements.add(element);

        size++;
    }

    public long getStartNanoTime() {
        return startNanoTime;
    }

    public long getEndNanoTime() {
        return endNanoTime;
    }

    public long getDurationInNanoseconds() {
        return endNanoTime - startNanoTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean wasCompletedBy(long nanoTime) {
        return completed && NanoUtils.isLessThan(endNanoTime, nanoTime);
    }

    public boolean wasStartedBy(long nanoTime) {
        return NanoUtils.isLessThan(startNanoTime, nanoTime);
    }

    // this doesn't require synchronization since flush is designed to be called
    // only by same thread
    public void justUpdatedCompletedElement(TraceEventSafeImpl element) {
        elements.justUpdatedPossiblyFlushedElement(element);
    }
}
