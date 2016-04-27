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
package org.apache.myfaces.shared_impl.taglib.html;

import org.apache.myfaces.shared_impl.renderkit.html.HTML;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;

/**
 * @author Manfred Geiler (latest modification by $Author: cagatay $)
 * @author Martin Marinschek
 * @version $Revision: 606793 $ $Date: 2007-12-25 10:20:46 -0500 (Tue, 25 Dec 2007) $
 */
public abstract class HtmlCommandLinkELTagBase extends HtmlComponentELTagBase
{
    //private static final Log log = LogFactory.getLog(HtmlCommandLinkTag.class);

    // UIComponent attributes --> already implemented in UIComponentTagBase

    // user role attributes --> already implemented in UIComponentTagBase

    // HTML universal attributes --> already implemented in HtmlComponentTagBase

    // HTML event handler attributes --> already implemented in HtmlComponentTagBase

    // HTML anchor attributes relevant for command link
    private ValueExpression _accesskey;
    private ValueExpression _charset;
    private ValueExpression _coords;
    private ValueExpression _disabled;
    private ValueExpression _hreflang;
    private ValueExpression _rel;
    private ValueExpression _rev;
    private ValueExpression _shape;
    private ValueExpression _tabindex;
    private ValueExpression _type;
    private ValueExpression _target;
    //HtmlCommandLink Attributes
    //FIXME: is mentioned in JSF API, but is no official anchor-attribute of HTML 4.0... what to do?
    private ValueExpression _onblur;
    //FIXME: is mentioned in JSF API, but is no official anchor-attribute of HTML 4.0... what to do?
    private ValueExpression _onfocus;

    // UICommand attributes
    private MethodExpression _action;
    private ValueExpression _immediate;
    private MethodExpression _actionListener;

    public void release() {
        super.release();
        _accesskey=null;
        _charset=null;
        _coords=null;
        _disabled=null;
        _hreflang=null;
        _rel=null;
        _rev=null;
        _shape=null;
        _tabindex=null;
        _type=null;
        _target=null;
        _onblur=null;
        _onfocus=null;
        _action=null;
        _immediate=null;
        _actionListener=null;
    }

    protected void setProperties(UIComponent component)
    {
        super.setProperties(component);

        setStringProperty(component, org.apache.myfaces.shared_impl.renderkit.html.HTML.ACCESSKEY_ATTR, _accesskey);
        setStringProperty(component, org.apache.myfaces.shared_impl.renderkit.html.HTML.CHARSET_ATTR, _charset);
        setStringProperty(component, HTML.COORDS_ATTR, _coords);
        setBooleanProperty(component, org.apache.myfaces.shared_impl.renderkit.html.HTML.DISABLED_ATTR, _disabled);
        setStringProperty(component, HTML.HREFLANG_ATTR, _hreflang);
        setStringProperty(component, org.apache.myfaces.shared_impl.renderkit.html.HTML.REL_ATTR, _rel);
        setStringProperty(component, org.apache.myfaces.shared_impl.renderkit.html.HTML.REV_ATTR, _rev);
        setStringProperty(component, HTML.SHAPE_ATTR, _shape);
        setStringProperty(component, HTML.TABINDEX_ATTR, _tabindex);
        setStringProperty(component, HTML.TYPE_ATTR, _type);
        setStringProperty(component, org.apache.myfaces.shared_impl.renderkit.html.HTML.ONBLUR_ATTR, _onblur);
        setStringProperty(component, HTML.ONFOCUS_ATTR, _onfocus);
        setStringProperty(component, HTML.TARGET_ATTR, _target);
        setActionProperty(component, _action);
        setActionListenerProperty(component, _actionListener);
        setBooleanProperty(component, org.apache.myfaces.shared_impl.renderkit.JSFAttr.IMMEDIATE_ATTR, _immediate);
   }

    public void setAccesskey(ValueExpression accesskey)
    {
        _accesskey = accesskey;
    }

    public void setCharset(ValueExpression charset)
    {
        _charset = charset;
    }

    public void setCoords(ValueExpression coords)
    {
        _coords = coords;
    }
    
    public void setDisabled(ValueExpression disabled)
    {
        _disabled = disabled;
    }    

    public void setHreflang(ValueExpression hreflang)
    {
        _hreflang = hreflang;
    }

    public void setOnblur(ValueExpression onblur)
    {
        _onblur = onblur;
    }

    public void setOnfocus(ValueExpression onfocus)
    {
        _onfocus = onfocus;
    }

    public void setRel(ValueExpression rel)
    {
        _rel = rel;
    }

    public void setRev(ValueExpression rev)
    {
        _rev = rev;
    }

    public void setShape(ValueExpression shape)
    {
        _shape = shape;
    }

    public void setTabindex(ValueExpression tabindex)
    {
        _tabindex = tabindex;
    }

    public void setType(ValueExpression type)
    {
        _type = type;
    }

    public void setAction(MethodExpression action)
    {
        _action = action;
    }

    public void setImmediate(ValueExpression immediate)
    {
        _immediate = immediate;
    }

    public void setActionListener(MethodExpression actionListener)
    {
        _actionListener = actionListener;
    }


    public void setTarget(ValueExpression target)
    {
        this._target = target;
    }
}
