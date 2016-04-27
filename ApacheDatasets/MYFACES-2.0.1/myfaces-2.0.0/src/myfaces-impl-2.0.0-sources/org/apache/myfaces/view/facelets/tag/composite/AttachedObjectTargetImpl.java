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
package org.apache.myfaces.view.facelets.tag.composite;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.view.AttachedObjectTarget;

import org.apache.myfaces.shared_impl.util.StringUtils;

/**
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 881558 $ $Date: 2009-11-17 16:55:58 -0500 (Tue, 17 Nov 2009) $
 */
public class AttachedObjectTargetImpl implements AttachedObjectTarget, Serializable
{    
    /**
     * 
     */
    private static final long serialVersionUID = -7214478234269252354L;
    
    protected ValueExpression _name;
    
    protected ValueExpression _targets;

    public AttachedObjectTargetImpl()
    {
    }

    public String getName()
    {
        if (_name != null)
        {
            return (String) _name.getValue(FacesContext.getCurrentInstance().getELContext());
        }        
        return null;
    }

    public List<UIComponent> getTargets(UIComponent topLevelComponent)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String [] targetsArray = getTargets(facesContext);
        
        if (targetsArray.length > 0)
        {
            List<UIComponent> targetsList = new ArrayList<UIComponent>(targetsArray.length);
            for (String target : targetsArray)
            {
                UIComponent innerComponent = topLevelComponent.findComponent(target);
                
                if (innerComponent != null)
                {
                    targetsList.add(innerComponent);
                }
            }
            return targetsList;
        }
        else
        {
            // composite:actionSource/valueHolder/editableValueHolder
            // "name" description says if targets is not set, name is 
            // the component id of the target component where
            // it should be mapped to
            String name = getName();
            if (name != null)
            {
                UIComponent innerComponent = topLevelComponent.findComponent(getName());
                if (innerComponent != null)
                {
                    List<UIComponent> targetsList = new ArrayList<UIComponent>(1);
                    targetsList.add(innerComponent);
                    return targetsList;
                }
            }
            return Collections.emptyList();
        }
    }
    
    public String [] getTargets(FacesContext context)
    {
        if (_targets != null)
        {
            return StringUtils.splitShortString((String) _targets.getValue(context.getELContext()), ' ');
        }
        return org.apache.myfaces.shared_impl.util.ArrayUtils.EMPTY_STRING_ARRAY;
    }
    
    public void setName(ValueExpression ve)
    {
        _name = ve;
    }
    
    public void setTargets(ValueExpression ve)
    {
        _targets = ve;
    }
}
