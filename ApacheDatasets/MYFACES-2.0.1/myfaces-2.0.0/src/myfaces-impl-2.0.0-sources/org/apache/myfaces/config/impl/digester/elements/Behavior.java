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
package org.apache.myfaces.config.impl.digester.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * Implementation of model for <behavior> element.
 */

public class Behavior implements org.apache.myfaces.config.element.Behavior
{
    private String behaviorClass;
    private String behaviorId;
    
    private List<Attribute> attributes = new ArrayList<Attribute>();
    private List<Property> properties = new ArrayList<Property>();
    // TODO: what about extensions and descriptionGroup elems?  Not addressed in other
    // config objects either.

    public String getBehaviorClass()
    {
        return this.behaviorClass;
    }

    public String getBehaviorId()
    {
        return this.behaviorId;
    }
    
    public void setBehaviorClass (String behaviorClass)
    {
        this.behaviorClass = behaviorClass;
    }
    
    public void setBehaviorId (String behaviorId)
    {
        this.behaviorId = behaviorId;
    }
    
    public Collection<Attribute> getAttributes ()
    {
        return attributes;
    }

    public void addAttribute (Attribute attribute)
    {
        attributes.add (attribute);
    }

    public Collection<Property> getProperties ()
    {
        return properties;
    }
    
    public void addProperty (Property property)
    {
        properties.add (property);
    }
}
