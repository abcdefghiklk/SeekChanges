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
package org.apache.myfaces.shared_impl.renderkit.html;

import org.apache.myfaces.shared_impl.renderkit.RendererUtils;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectMany;
import javax.faces.component.UISelectOne;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlSelectManyMenu;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import java.io.IOException;

/**
 * X-CHECKED: tlddoc of h:selectManyListbox
 *
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @author Thomas Spiegl
 * @version $Revision: 812311 $ $Date: 2009-09-07 19:22:15 -0500 (Mon, 07 Sep 2009) $
 */
public class HtmlMenuRendererBase
        extends HtmlRenderer
{
    //private static final Log log = LogFactory.getLog(HtmlMenuRenderer.class);

    public void encodeEnd(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        RendererUtils.checkParamValidity(facesContext, component, null);

        if (component instanceof UISelectMany)
        {
            HtmlRendererUtils.renderMenu(facesContext,
                                         (UISelectMany)component,
                                         isDisabled(facesContext, component));
        }
        else if (component instanceof UISelectOne)
        {
            HtmlRendererUtils.renderMenu(facesContext,
                                         (UISelectOne)component,
                                         isDisabled(facesContext, component));
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + component.getClass().getName());
        }
    }

    protected boolean isDisabled(FacesContext facesContext, UIComponent uiComponent)
    {
        //TODO: overwrite in extended HtmlMenuRenderer and check for enabledOnUserRole
        if (uiComponent instanceof HtmlSelectManyMenu)
        {
            return ((HtmlSelectManyMenu)uiComponent).isDisabled();
        }
        else if (uiComponent instanceof HtmlSelectOneMenu)
        {
            return ((HtmlSelectOneMenu)uiComponent).isDisabled();
        }
        else
        {
            return org.apache.myfaces.shared_impl.renderkit.RendererUtils.getBooleanAttribute(uiComponent, org.apache.myfaces.shared_impl.renderkit.html.HTML.DISABLED_ATTR, false);
        }
    }

    public void decode(FacesContext facesContext, UIComponent uiComponent)
    {
        org.apache.myfaces.shared_impl.renderkit.RendererUtils.checkParamValidity(facesContext, uiComponent, null);

        if (uiComponent instanceof UISelectMany)
        {
            HtmlRendererUtils.decodeUISelectMany(facesContext, uiComponent);
        }
        else if (uiComponent instanceof UISelectOne)
        {
            HtmlRendererUtils.decodeUISelectOne(facesContext, uiComponent);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + uiComponent.getClass().getName());
        }
        if (uiComponent instanceof ClientBehaviorHolder &&
                !HtmlRendererUtils.isDisabled(uiComponent))
        {
            HtmlRendererUtils.decodeClientBehaviors(facesContext, uiComponent);
        }
    }

    public Object getConvertedValue(FacesContext facesContext, UIComponent uiComponent, Object submittedValue) throws ConverterException
    {
        org.apache.myfaces.shared_impl.renderkit.RendererUtils.checkParamValidity(facesContext, uiComponent, null);

        if (uiComponent instanceof UISelectMany)
        {
            return org.apache.myfaces.shared_impl.renderkit.RendererUtils.getConvertedUISelectManyValue(facesContext,
                                                               (UISelectMany)uiComponent,
                                                               submittedValue);
        }
        else if (uiComponent instanceof UISelectOne)
        {
            return org.apache.myfaces.shared_impl.renderkit.RendererUtils.getConvertedUISelectOneValue(facesContext,
                                                           (UISelectOne)uiComponent,
                                                           submittedValue);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + uiComponent.getClass().getName());
        }
    }

}
