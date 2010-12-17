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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @param <E>
 * @since 1.0
 */
public class TerminatableList<E> implements Iterable<E> {

    private final Node<E> head = new Node<E>(null, null, null);

    private final AtomicReference<Node<E>> tailReference = new AtomicReference<Node<E>>(head);

    public boolean isTerminated() {
        return tailReference.get().getClass() == TerminalNode.class;
    }

    public boolean isEmpty() {
        Node<E> firstNode = head.next;
        return firstNode == null || firstNode.getClass() == TerminalNode.class;
    }

    public E getFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return head.next.element;
    }

    public E getLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        Node<E> lastNode = tailReference.get();
        if (lastNode.getClass() == TerminalNode.class) {
            return lastNode.previous.element;
        } else {
            return lastNode.element;
        }
    }

    public boolean add(E object) {
        return offer(new Node<E>(object, null, null));
    }

    public boolean terminate() {
        return offer(new TerminalNode<E>(null, null, null));
    }

    // based on non-blocking concurrent enqueue algorithm from
    // "Simple, Fast, and Practical Non-Blocking and Blocking Concurrent Queue Algorithms"
    // by Maged M. Michael and Michael L. Scott
    // see http://www.cs.rochester.edu/u/michael/PODC96.html
    private boolean offer(Node<E> node) {

        Node<E> tail;
        while (true) {
            tail = tailReference.get();
            if (tail.getClass() == TerminalNode.class) {
                return false;
            }
            Node<E> next = tail.next;
            if (tail == tailReference.get()) {
                if (next == null) {
                    node.previous = tail;
                    if (tail.casNext(null, node)) {
                        break;
                    }
                } else {
                    tailReference.compareAndSet(tail, next);
                }
            }
        }

        tailReference.compareAndSet(tail, node);
        return true;
    }

    public Iterator<E> iterator() {
        return new Itr(head.next);
    }

    public Iterator<E> iteratorOverLastN(int n) { // NOPMD for short variable name

        if (!isTerminated()) {
            throw new IllegalStateException(
                    "iteratorOverLastX() can only be called after termination");
        }

        Node<E> previousNode = tailReference.get();
        for (int i = 0; i < n; i++) {
            if (previousNode == head) { // NOPMD for not using equals()
                break;
            }
            previousNode = previousNode.previous;
        }

        return new Itr(previousNode.next);
    }

    private final class Itr implements Iterator<E> {

        // doesn't need to be volatile, this iterator is designed for use by single thread only
        private Node<E> next; // NOPMD for having same name as a method name

        private Itr(Node<E> next) {
            this.next = next;
        }

        public boolean hasNext() {
            return next != null && next.getClass() != TerminalNode.class;
        }

        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Node<E> node = next;
            next = next.next;
            return node.element;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // TODO submit checkstyle bug report, it thinks this should be final but it is subclassed below
    private static class Node<E> { // SUPPRESS CHECKSTYLE

        // not using AtomicReference because that would add memory across all Nodes
        @SuppressWarnings("unchecked")
        private static final AtomicReferenceFieldUpdater<Node, Node> NEXT_UPDATER =
                AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "next");

        private volatile E element;
        private volatile Node<E> previous;
        private volatile Node<E> next;

        private Node(E element, Node<E> previous, Node<E> next) {
            this.element = element;
            this.previous = previous;
            this.next = next;
        }

        protected boolean casNext(Node<E> cmp, Node<E> val) {
            return NEXT_UPDATER.compareAndSet(this, cmp, val);
        }
    }

    // adding boolean to Node structure would increase memory across all nodes
    // this seems more efficient for tagging terminal nodes
    private static final class TerminalNode<E> extends Node<E> {

        private TerminalNode(E element, Node<E> next, Node<E> previous) {
            super(element, previous, next);
        }

        @Override
        protected boolean casNext(Node<E> cmp, Node<E> val) {
            return false;
        }
    }
}
