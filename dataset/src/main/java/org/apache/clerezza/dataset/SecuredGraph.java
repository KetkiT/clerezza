/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.clerezza.dataset;

import org.apache.clerezza.api.*;
import org.apache.clerezza.api.impl.graph.SimpleImmutableGraph;
import org.apache.clerezza.api.impl.graph.WriteBlockedGraph;
import org.apache.clerezza.dataset.security.TcAccessController;

import java.security.AccessControlException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * A Secured triple collection wraps a triple collection checking each access
 * for the rights on a the ImmutableGraph for which the uri is passed to the 
 * constructor.
 *
 * @author mir, hasan
 */
public class SecuredGraph implements Graph {

    private final Graph wrapped;
    private final IRI name;
    private final TcAccessController tcAccessController;

    public SecuredGraph(Graph wrapped, IRI name,
                        TcAccessController tcAccessController) {
        this.wrapped = wrapped;
        this.name = name;
        this.tcAccessController = tcAccessController;
    }

    @Override
    public Iterator<Triple> filter(final BlankNodeOrIRI subject, final IRI predicate, final RDFTerm object) {
        final Iterator<Triple> baseIter = wrapped.filter(subject, predicate, object);
        return new Iterator<Triple>() {

            @Override
            public boolean hasNext() {
                checkRead();
                return baseIter.hasNext();
            }

            @Override
            public Triple next() {
                checkRead();
                return baseIter.next();
            }

            @Override
            public void remove() {
                checkWrite();
                baseIter.remove();
            }
        };
    }

    @Override
    public int size() {
        checkRead();
        return wrapped.size();
    }

    @Override
    public boolean isEmpty() {
        checkRead();
        return wrapped.isEmpty();
    }

    @Override
    public Object[] toArray() {
        checkRead();
        return wrapped.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        checkRead();
        return wrapped.toArray(a);
    }

    @Override
    public boolean add(Triple e) {
        checkWrite();
        return wrapped.add(e);
    }

    @Override
    public boolean remove(Object o) {
        checkWrite();
        return wrapped.remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends Triple> c) {
        checkWrite();
        return wrapped.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        checkWrite();
        return wrapped.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        checkWrite();
        return wrapped.retainAll(c);
    }

    @Override
    public void clear() {
        checkWrite();
        wrapped.clear();
    }

    void checkRead() {
        tcAccessController.checkReadPermission(name);
    }

    void checkWrite() {
        tcAccessController.checkReadWritePermission(name);
    }

    @Override
    public boolean contains(Object o) {
        checkRead();
        return wrapped.contains((Triple) o);
    }


    @Override
    public Iterator<Triple> iterator() {
        return filter(null, null, null);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        checkRead();
        return wrapped.containsAll(c);
    }
    
    @Override
    public ImmutableGraph getImmutableGraph() {
        return new SimpleImmutableGraph(this);
    }

    @Override
    public ReadWriteLock getLock() {
        return wrapped.getLock();
    }
    
    /**
     * Returns the wrapped Graph if the caller has all access rights.
     * If the caller has only the read access right, then a write-blocked
     * Graph is returned. If the caller has neither the read nor the write
     * access right then an AccessControlException is thrown.
     *
     * @return the wrapped Graph or a write-block Graph depending
     *        on the access rights of the caller.
     */
    public Graph getUnsecuredGraph() {
        try {
            checkWrite();
            return wrapped;
        } catch (AccessControlException ex) {
            checkRead();
            return new WriteBlockedGraph(wrapped);
        }
        
    }
}
