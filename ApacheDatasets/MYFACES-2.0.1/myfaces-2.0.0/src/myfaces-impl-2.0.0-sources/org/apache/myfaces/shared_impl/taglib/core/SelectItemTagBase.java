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
package org.apache.myfaces.shared_impl.taglib.core;

import org.apache.myfaces.shared_impl.renderkit.JSFAttr;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;

/**
 * @author Thomas Spiegl (latest modification by $Author: lu4242 $)
 * @version $Revision: 684924 $ $Date: 2008-08-11 15:59:26 -0500 (Mon, 11 Aug 2008) $
 */
public class SelectItemTagBase
    extends org.apache.myfaces.shared_impl.taglib.UIComponentELTagBase
{
    //private static final Log log = LogFactory.getLog(SelectItemTag.class);

    public String getComponentType()
    {
        return "javax.faces.SelectItem";
    }

    public String getRendererType()
    {
        return null;
    }

    // UISelectItem attributes
    private ValueExpression _itemDisabled;
    private ValueExpression _itemDescription;
    private ValueExpression _itemLabel;
    private ValueExpression _itemValue;
    private ValueExpression _escape;

    protected void setProperties(UIComponent component)
    {
        super.setProperties(component);

        setBooleanProperty(component, JSFAttr.ITEM_DISABLED_ATTR, _itemDisabled);
        setStringProperty(component, JSFAttr.ITEM_DESCRIPTION_ATTR, _itemDescription);
        setStringProperty(component, org.apache.myfaces.shared_impl.renderkit.JSFAttr.ITEM_LABEL_ATTR, _itemLabel);
        setStringProperty(component, JSFAttr.ITEM_VALUE_ATTR, _itemValue);
        setBooleanProperty(component, JSFAttr.ITEM_ESCAPED_ATTR, _escape, Boolean.TRUE);
    }

    public void setItemDisabled(ValueExpression itemDisabled)
    {
        _itemDisabled = itemDisabled;
    }

    public void setItemDescription(ValueExpression itemDescription)
    {
        _itemDescription = itemDescription;
    }

    public void setItemLabel(ValueExpression itemLabel)
    {
        _itemLabel = itemLabel;
    }

    @Deprecated
    protected void setItemValue(String itemValue)
    {
        _itemValue = getFacesContext().getApplication().getExpressionFactory().createValueExpression(
                    getFacesContext().getELContext(),itemValue,String.class);
    }

    public void setItemValue(ValueExpression itemValue)
    {
        _itemValue = itemValue;
    }

    public void setEscape(ValueExpression escape)
    {
        _escape = escape;
    }

    protected ValueExpression getItemValue()
    {
        return _itemValue;
    }

}
