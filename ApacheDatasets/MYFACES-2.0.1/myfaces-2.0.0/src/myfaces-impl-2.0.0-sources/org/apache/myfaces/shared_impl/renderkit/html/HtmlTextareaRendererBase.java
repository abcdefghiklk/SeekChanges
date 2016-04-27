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
import org.apache.myfaces.shared_impl.renderkit.html.HTML;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRenderer;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.util.JavascriptUtils;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.ConverterException;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @author Anton Koinov
 * @version $Revision: 819760 $ $Date: 2009-09-28 17:45:53 -0500 (Mon, 28 Sep 2009) $
 */
public class HtmlTextareaRendererBase
        extends HtmlRenderer
{
    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent)
            throws IOException
    {
        RendererUtils.checkParamValidity(facesContext, uiComponent, UIInput.class);

        encodeTextArea(facesContext, uiComponent);

    }

    protected void encodeTextArea(FacesContext facesContext, UIComponent uiComponent) throws IOException {
        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement(HTML.TEXTAREA_ELEM, uiComponent);

        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder && JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext()))
        {
            behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
            if (!behaviors.isEmpty())
            {
                HtmlRendererUtils.writeIdAndName(writer, uiComponent, facesContext);
            }
            else
            {
                HtmlRendererUtils.writeIdIfNecessary(writer, uiComponent, facesContext);
                writer.writeAttribute(HTML.NAME_ATTR, uiComponent.getClientId(facesContext), null);
            }
            HtmlRendererUtils.renderBehaviorizedOnchangeEventHandler(facesContext, writer, uiComponent, behaviors);
            HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, uiComponent, behaviors);
            HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchange(facesContext, writer, uiComponent, behaviors);
            HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, HTML.TEXTAREA_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
        }
        else
        {
            HtmlRendererUtils.writeIdIfNecessary(writer, uiComponent, facesContext);
            writer.writeAttribute(HTML.NAME_ATTR, uiComponent.getClientId(facesContext), null);
            HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, HTML.TEXTAREA_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED);
        }

        if (isDisabled(facesContext, uiComponent))
        {
            writer.writeAttribute(org.apache.myfaces.shared_impl.renderkit.html.HTML.DISABLED_ATTR, Boolean.TRUE, null);
        }

        String strValue = org.apache.myfaces.shared_impl.renderkit.RendererUtils.getStringValue(facesContext, uiComponent);
        writer.writeText(strValue, org.apache.myfaces.shared_impl.renderkit.JSFAttr.VALUE_ATTR);

        writer.endElement(HTML.TEXTAREA_ELEM);
    }

    protected boolean isDisabled(FacesContext facesContext, UIComponent uiComponent)
    {
        //TODO: overwrite in extended HtmlTextareaRenderer and check for enabledOnUserRole
        if (uiComponent instanceof HtmlInputTextarea)
        {
            return ((HtmlInputTextarea)uiComponent).isDisabled();
        }

        return org.apache.myfaces.shared_impl.renderkit.RendererUtils.getBooleanAttribute(uiComponent, HTML.DISABLED_ATTR, false);
        
    }

    public void decode(FacesContext facesContext, UIComponent component)
    {
        RendererUtils.checkParamValidity(facesContext, component, UIInput.class);
        HtmlRendererUtils.decodeUIInput(facesContext, component);
        if (component instanceof ClientBehaviorHolder &&
            !HtmlRendererUtils.isDisabled(component))
        {
            HtmlRendererUtils.decodeClientBehaviors(facesContext, component);
        }
    }

    public Object getConvertedValue(FacesContext facesContext, UIComponent uiComponent, Object submittedValue) throws ConverterException
    {
        RendererUtils.checkParamValidity(facesContext, uiComponent, UIOutput.class);
        return org.apache.myfaces.shared_impl.renderkit.RendererUtils.getConvertedUIOutputValue(facesContext,
                                                       (UIOutput)uiComponent,
                                                       submittedValue);
    }

}
