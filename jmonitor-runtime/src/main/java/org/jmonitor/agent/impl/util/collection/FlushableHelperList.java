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

package org.jmonitor.agent.impl.util.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;

/**
 * It's ok if it returns more than nLastXElements in some cases.
 * 
 * @author Trask Stalnaker
 * @param <E>
 * @since 1.0
 */
class FlushableHelperList<E extends IndexedElement> implements Iterable<E> {

    private static final Ordering<IndexedElement> INDEXED_ELEMENT_ORDERING =
            new Ordering<IndexedElement>() {
                public int compare(IndexedElement element1, IndexedElement element2) {
                    return element1.getIndex() - element2.getIndex();
                }
            };

    private final TerminatableList<E> elements = new TerminatableList<E>();

    // used to pre-fill iterator to return a minimum number of consecutive elements
    private volatile Queue<E> immediatelyPriorElements;
    private final int minConsecutiveElements;

    // only for use by add()
    // cannot be used by iterator() since its value cannot be trusted
    // by another thread since it is incremented after the add
    // still needs to be volatile
    // since this FlushableHelperList could be constructed by another thread
    private volatile int elementsSize;

    // only for use by add()
    // cannot be used by iterator() since its value cannot be trusted
    // by another thread since it is incremented after the add
    // still needs to be volatile
    // since this FlushableHelperList could be constructed by another thread
    private volatile int immediatelyPriorElementsSize;

    // by lazy instantiation we are trading non-volatile access for reduced memory
    private volatile ConcurrentHashMap<E, E> updatedPriorElements;

    public FlushableHelperList(int minConsecutiveElements, Iterator<E> lastXFromPrevious) {

        this.minConsecutiveElements = minConsecutiveElements;

        immediatelyPriorElements = new ConcurrentLinkedQueue<E>();

        if (lastXFromPrevious != null) {
            while (lastXFromPrevious.hasNext()) {
                immediatelyPriorElements.add(lastXFromPrevious.next());
            }
        }
    }

    public boolean isEmpty() {
        return elements.isEmpty()
                && (updatedPriorElements == null || updatedPriorElements.isEmpty());
    }

    public int size() {
        int listSize = 0;
        int extrasSize = 0;
        if (!elements.isEmpty()) {
            listSize = elements.getLast().getIndex() - elements.getFirst().getIndex() + 1;
        }
        if (updatedPriorElements != null) {
            extrasSize = updatedPriorElements.size();
        }
        return listSize + extrasSize;
    }

    public boolean add(E object) {
        if (elements.add(object)) {
            elementsSize++;

            if (immediatelyPriorElementsSize > 0
                    && elementsSize + immediatelyPriorElementsSize > minConsecutiveElements) {

                // safe to remove one of the immediately prior elements (for GC)
                immediatelyPriorElements.poll();
                immediatelyPriorElementsSize--;
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean terminate() {
        return elements.terminate();
    }

    public boolean addPossibleUpdatedPriorElement(E object) {
        E first = null;
        if (!elements.isEmpty()) {
            first = elements.getFirst();
        }
        if (first == null || object.getIndex() < first.getIndex()) {
            if (updatedPriorElements == null) {
                updatedPriorElements = new ConcurrentHashMap<E, E>();
            }
            updatedPriorElements.put(object, object);
        }
        return !elements.isTerminated();
    }

    public Iterator<E> iteratorOverLastXConsecutive(int lastX) {
        if (!elements.isTerminated()) {
            throw new IllegalStateException(
                    "getLastXConsecutiveElements() can only be called after termination");
        }
        List<E> lastXList = new ArrayList<E>();
        Iterator<E> iterator = elements.iteratorOverLastN(lastX);
        while (iterator.hasNext()) {
            lastXList.add(iterator.next());
        }
        if (lastXList.size() == lastX) {
            return lastXList.iterator();
        } else {
            return Iterators.concat(
                    getImmediatelyPriorElementsList(lastX - lastXList.size()).iterator(),
                    lastXList.iterator());
        }
    }

    public Iterator<E> iterator() {
        return iterableWithMinimumConsecutiveElements().iterator();
    }

    // guaranteed to return minimum lastX elements (or back to beginning of flushable list if fewer)
    public Iterable<E> iterableWithMinimumConsecutiveElements() {

        // first lock down immediately prior elements
        List<E> immediatelyPriorElementsList = getImmediatelyPriorElementsList();

        // next calculate updated prior elements
        if (immediatelyPriorElementsList.isEmpty()) {

            if (elements.isEmpty()) {

                // this can only happen if this is the first FlushableHelperList in which case there
                // can be no updated prior elements either
                return Collections.emptySet();

            } else {

                List<E> updatedPriorElementsList =
                        getUpdatedPriorElementsWithIndexLessThan(elements.getFirst().getIndex());
                return Iterables.concat(updatedPriorElementsList, elements);
            }

        } else {

            List<E> updatedPriorElementsList =
                    getUpdatedPriorElementsWithIndexLessThan(immediatelyPriorElementsList.get(0).getIndex());

            return Iterables.concat(updatedPriorElementsList, immediatelyPriorElementsList,
                    elements);
        }
    }

    private List<E> getImmediatelyPriorElementsList() {
        // cannot trust elementsSize or immediatelyPriorElementsSize for reasons above
        Iterator<E> iterator = elements.iterator();
        int count = 0;
        while (iterator.hasNext() && count < minConsecutiveElements) {
            iterator.next();
            count++;
        }
        if (count == minConsecutiveElements) {
            return Collections.emptyList();
        } else {
            return getImmediatelyPriorElementsList(minConsecutiveElements - count);
        }
    }

    private List<E> getImmediatelyPriorElementsList(int lastX) {

        List<E> immediatelyPriorList = new ArrayList<E>(immediatelyPriorElements);
        if (immediatelyPriorList.size() <= lastX) {
            return immediatelyPriorList;
        } else {
            // more than we need in immediatelyPriorList
            return immediatelyPriorList.subList(immediatelyPriorList.size() - lastX,
                    immediatelyPriorList.size());
        }
    }

    private List<E> getUpdatedPriorElementsWithIndexLessThan(int priorToIndex) {

        if (updatedPriorElements == null || updatedPriorElements.isEmpty()) {
            return Collections.emptyList();
        } else {
            // there shouldn't be too many "extras"
            // so it's not bad to make shallow copy of this list
            List<E> list = INDEXED_ELEMENT_ORDERING.sortedCopy(updatedPriorElements.keySet());
            for (Iterator<E> i = list.iterator(); i.hasNext();) {
                if (i.next().getIndex() < priorToIndex) {
                    continue;
                } else {
                    i.remove();
                }
            }
            return list;
        }
    }
}
