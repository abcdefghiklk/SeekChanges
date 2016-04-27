/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.myfaces.shared_impl.util.el;

import java.util.Map;
import java.util.Set;
import java.util.Collection;

/**
 * @author Sylvain Vieujot (latest modification by $Author: skitching $)
 * @version $Revision: 355303 $ $Date: 2005-12-09 02:36:08 +0100 (Fr, 09 Dez 2005) $
 *
 */
public abstract class GenericMap implements Map {

    /**
     * This method should return the result of the test.
     */
    protected abstract Object getValue(Object key);

    public int size() {
        return 1;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean containsKey(Object key) {
        return true;
    }

    public boolean containsValue(Object value) {
        return value instanceof Boolean;
    }

    public Object get(Object key) {
        return getValue(key);
    }

    public Object put(Object key, Object value) {
        return null;
    }

    public Object remove(Object key) {
        return null;
    }

    public void putAll(Map m) {
        // NoOp
    }

    public void clear() {
        // NoOp
    }

    public Set keySet() {
        return null;
    }

    public Collection values() {
        return null;
    }

    public Set entrySet() {
        return null;
    }
}


