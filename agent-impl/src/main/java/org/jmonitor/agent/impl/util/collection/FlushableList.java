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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import com.google.common.collect.Iterables;

/**
 * Primarily for use by a single thread, but can be iterated over by another thread and can be
 * terminated by another thread.
 * 
 * @author Trask Stalnaker
 * @param <E>
 * @since 1.0
 */
public class FlushableList<E extends IndexedElement> implements Iterable<E> {

    private final AtomicReference<FlushableHelperList<E>> listReference =
            new AtomicReference<FlushableHelperList<E>>();

    private final Object flushLock = new Object();

    // keep permanent hold of first X and last X elements
    private final int nFirstXElements;
    private final int nLastXElements;
    private final AtomicReferenceArray<E> firstXElements;

    public FlushableList(int nFirstXElements, int nLastXElements) {
        this.nFirstXElements = nFirstXElements;
        this.nLastXElements = nLastXElements;
        listReference.set(new FlushableHelperList<E>(nLastXElements, null));
        firstXElements = new AtomicReferenceArray<E>(nFirstXElements);
    }

    public void add(E object) {

        // regardless of flushing, we store the nFirstXElements
        if (object.getIndex() < nFirstXElements) {
            firstXElements.set(object.getIndex(), object);
        }

        while (true) {
            FlushableHelperList<E> list = listReference.get();
            boolean success = list.add(object);
            if (success) {
                break;
            } else {
                // list has been terminated but not yet reset
                casNewListUsingCompareValue(list);
            }
        }
    }

    public void justUpdatedPossiblyFlushedElement(E object) {
        while (true) {
            FlushableHelperList<E> list = listReference.get();
            boolean success = list.addPossibleUpdatedPriorElement(object);
            if (success) {
                break;
            } else {
                casNewListUsingCompareValue(list);
            }
        }
    }

    public Iterator<E> iterator() {
        return iterablePrependedWithFirstX(listReference.get()).iterator();
    }

    public Iterable<E> flush() {

        synchronized (flushLock) {

            final FlushableHelperList<E> previousList = listReference.get();

            // don't need to check return value since we know this will succeed
            // since we are inside the synchronized block
            // (in which we terminate and re-create each time)
            previousList.terminate();

            casNewListUsingCompareValue(previousList);

            return iterablePrependedWithFirstX(previousList);
        }
    }

    private Iterable<E> iterablePrependedWithFirstX(FlushableHelperList<E> previousList) {

        // lock down iterable so we can calculate first index
        Iterable<E> iterable = previousList.iterableWithMinimumConsecutiveElements();

        // join firstX elements on to the beginning of previousList.iterator()
        int index = iterable.iterator().next().getIndex();

        // get firstXElements up to but not including the first index we are currently
        // flushing
        List<E> firstX = getFirstXElementsWithIndexLessThan(index);
        return Iterables.concat(firstX, iterable);
    }

    private List<E> getFirstXElementsWithIndexLessThan(int index) {
        List<E> firstX = new ArrayList<E>();
        for (int i = 0; i < nFirstXElements; i++) {
            if (firstXElements.get(i).getIndex() < index) {
                firstX.add(firstXElements.get(i));
            } else {
                // since the indexes are incrementing we can break out now
                break;
            }
        }
        return firstX;
    }

    private void casNewListUsingCompareValue(FlushableHelperList<E> list) {
        listReference.compareAndSet(list, new FlushableHelperList<E>(nLastXElements,
                list.iteratorOverLastXConsecutive(nLastXElements)));
    }
}
