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
package javax.faces.component;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @version $Revision: 881927 $ $Date: 2009-11-18 15:40:02 -0500 (Wed, 18 Nov 2009) $
 */
class _ComponentChildrenList extends AbstractList<UIComponent> implements Serializable
{
    private static final long serialVersionUID = -6775078929331154224L;
    private UIComponent _component;
    private List<UIComponent> _list = new ArrayList<UIComponent>(4);

    _ComponentChildrenList(UIComponent component)
    {
        _component = component;
    }

    @Override
    public UIComponent get(int index)
    {
        return _list.get(index);
    }

    @Override
    public int size()
    {
        return _list.size();
    }

    @Override
    public UIComponent set(int index, UIComponent value)
    {
        checkValue(value);
        removeChildrenFromParent(value);
        UIComponent child = _list.set(index, value);
        if (child != value)
        {
            updateParent(value);
            if (child != null)
            {
                child.setParent(null);
            }
        }
        
        return child;
    }

    @Override
    public boolean add(UIComponent value)
    {
        checkValue(value);

        removeChildrenFromParent(value);
        boolean res = _list.add(value);
        
        updateParent(value);
        
        return res;
    }

    @Override
    public void add(int index, UIComponent value)
    {
        checkValue(value);
        
        removeChildrenFromParent(value);
        
        _list.add(index, value);
        
        updateParent(value);
    }

    @Override
    public UIComponent remove(int index)
    {
        UIComponent child = _list.remove(index);
        if (child != null)
        {
            childRemoved(child);
        }
        
        return child;
    }

    private void checkValue(Object value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        
        if (!(value instanceof UIComponent))
        {
            throw new ClassCastException("value is not a UIComponent");
        }
    }

    private void childRemoved(UIComponent child)
    {
        child.setParent(null);
    }

    private void updateParent(UIComponent child)
    {
        child.setParent(_component);
    }
    
    private void removeChildrenFromParent(UIComponent child)
    {
        UIComponent oldParent = child.getParent();
        if (oldParent != null)
        {
            oldParent.getChildren().remove(child);
        }
    }

    @Override
    public boolean remove(Object value)
    {
        checkValue(value);
        
        if (_list.remove(value))
        {
            childRemoved((UIComponent)value);
            return true;
        }
        return false;
    }
}
