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
package org.jmonitor.agent.impl.util.collection;

import java.util.AbstractQueue;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Singleton empty queue similar to {@link Collections#emptyList()}, {@link Collections#emptyMap()}
 * and {@link Collections#emptySet()}.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class QueueUtils {

    @SuppressWarnings("rawtypes")
    private static final Queue EMPTY_QUEUE = new EmptyQueue();

    // utility class
    private QueueUtils() {}

    @SuppressWarnings("unchecked")
    public static <T> Queue<T> emptyQueue() {
        return EMPTY_QUEUE;
    }

    private static class EmptyQueue<T> extends AbstractQueue<T> {

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {

                public boolean hasNext() {
                    return false;
                }

                public T next() {
                    throw new NoSuchElementException();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public int size() {
            return 0;
        }

        public boolean offer(Object object) {
            return false;
        }

        public T peek() {
            return null;
        }

        public T poll() {
            return null;
        }
    }
}
