/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.github.fisher2911.hmcleaves.util.collection;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class UniqueConcurrentLinkedDeque<T> implements Iterable<T> {

    private final ConcurrentLinkedDeque<T> deque;
    private final Set<T> set;

    public UniqueConcurrentLinkedDeque() {
        this(new ConcurrentLinkedDeque<>(), ConcurrentHashMap.newKeySet());
    }

    public UniqueConcurrentLinkedDeque(ConcurrentLinkedDeque<T> deque, Set<T> set) {
        this.deque = deque;
        this.set = set;
    }

    public boolean addFirst(T t) {
        if (this.set.add(t)) {
            this.deque.addFirst(t);
            return true;
        }
        return false;
    }

    public boolean add(T t) {
        if (this.set.add(t)) {
            return this.deque.add(t);
        }
        return false;
    }

    public T poll() {
        final T t = this.deque.poll();
        if (t != null) {
            this.set.remove(t);
        }
        return t;
    }

    public boolean isEmpty() {
        return this.deque.isEmpty();
    }

    public int size() {
        return this.deque.size();
    }

    public void clear() {
        this.deque.clear();
        this.set.clear();
    }

    public ConcurrentLinkedDeque<T> getDeque() {
        return this.deque;
    }

    public Set<T> getSet() {
        return this.set;
    }

    @Override
    public java.util.Iterator<T> iterator() {
        return this.deque.iterator();
    }

}
