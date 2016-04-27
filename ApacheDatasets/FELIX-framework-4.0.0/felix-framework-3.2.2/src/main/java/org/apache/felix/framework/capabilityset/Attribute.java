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
package org.apache.felix.framework.capabilityset;

public class Attribute
{
    private final String m_name;
    private final Object m_value;
    private final boolean m_isMandatory;

    public Attribute(String name, Object value, boolean isMandatory)
    {
        m_name = name;
        m_value = value;
        m_isMandatory = isMandatory;
    }

    public String getName()
    {
        return m_name;
    }

    public Object getValue()
    {
        return m_value;
    }

    public boolean isMandatory()
    {
        return m_isMandatory;
    }

    public String toString()
    {
        return m_name + "=" + m_value;
    }
}