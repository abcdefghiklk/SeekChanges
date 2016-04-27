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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.NavigationCase;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutcomeTarget;
import javax.faces.component.UIParameter;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlOutcomeTargetButton;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared_impl.config.MyfacesConfig;
import org.apache.myfaces.shared_impl.renderkit.ClientBehaviorEvents;
import org.apache.myfaces.shared_impl.renderkit.JSFAttr;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.util.JavascriptUtils;

/**
 * @since 2.0
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 816721 $ $Date: 2009-09-18 12:40:15 -0500 (Fri, 18 Sep 2009) $
 */
public class HtmlOutcomeTargetButtonRendererBase extends HtmlRenderer
{

    public boolean getRendersChildren()
    {
        return true;
    }

    public void encodeBegin(FacesContext facesContext, UIComponent uiComponent)
            throws IOException
    {
        super.encodeBegin(facesContext, uiComponent); //check for NP

        String clientId = uiComponent.getClientId(facesContext);

        ResponseWriter writer = facesContext.getResponseWriter();

        writer.startElement(HTML.INPUT_ELEM, uiComponent);

        writer.writeAttribute(HTML.ID_ATTR, clientId,
                org.apache.myfaces.shared_impl.renderkit.JSFAttr.ID_ATTR);
        writer.writeAttribute(HTML.NAME_ATTR, clientId, JSFAttr.ID_ATTR);

        String image = getImage(uiComponent);

        ExternalContext externalContext = facesContext.getExternalContext();

        if (image != null)
        {
            writer.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_IMAGE,
                    org.apache.myfaces.shared_impl.renderkit.JSFAttr.TYPE_ATTR);
            String src = facesContext.getApplication().getViewHandler()
                    .getResourceURL(facesContext, image);
            writer.writeURIAttribute(HTML.SRC_ATTR, externalContext
                    .encodeResourceURL(src),
                    org.apache.myfaces.shared_impl.renderkit.JSFAttr.IMAGE_ATTR);
        }
        else
        {
            writer.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_BUTTON,
                    org.apache.myfaces.shared_impl.renderkit.JSFAttr.TYPE_ATTR);
            Object value = org.apache.myfaces.shared_impl.renderkit.RendererUtils
                    .getStringValue(facesContext, uiComponent);
            if (value != null)
            {
                writer
                        .writeAttribute(
                                org.apache.myfaces.shared_impl.renderkit.html.HTML.VALUE_ATTR,
                                value,
                                org.apache.myfaces.shared_impl.renderkit.JSFAttr.VALUE_ATTR);
            }
        }

        if (HtmlRendererUtils.isDisabled(uiComponent))
        {
            HtmlRendererUtils.renderHTMLAttribute(writer, HTML.DISABLED_ATTR,
                    HTML.DISABLED_ATTR, true);
        }
        else
        {
            String href = facesContext.getExternalContext().encodeResourceURL(
                    HtmlRendererUtils.getOutcomeTargetLinkHref(facesContext,
                            (UIOutcomeTarget) uiComponent));

            String commandOnClick = (String) uiComponent.getAttributes().get(
                    HTML.ONCLICK_ATTR);
            StringBuffer onClick = new StringBuffer();

            if (commandOnClick != null)
            {
                onClick.append(commandOnClick);
                onClick.append(';');
            }

            onClick.append("window.location.href = '" + href + "'");

            writer.writeAttribute(HTML.ONCLICK_ATTR, onClick.toString(), null);
        }

        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder && JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext()))
        {
            behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();            
            HtmlRendererUtils.renderBehaviorizedEventHandlersWithoutOnclick(facesContext, writer, uiComponent, behaviors);
            HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(facesContext, writer, uiComponent, behaviors);
        }
        else
        {
            HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                    HTML.EVENT_HANDLER_ATTRIBUTES_WITHOUT_ONCLICK);
            HtmlRendererUtils
                .renderHTMLAttributes(
                    writer,
                    uiComponent,
                    HTML.COMMON_FIELD_EVENT_ATTRIBUTES_WITHOUT_ONSELECT_AND_ONCHANGE);

        }
        HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                HTML.COMMON_FIELD_PASSTROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
        HtmlRendererUtils.renderHTMLAttribute(writer, uiComponent,
                HTML.ALT_ATTR, HTML.ALT_ATTR);

        writer.flush();
    }

    private String getImage(UIComponent uiComponent)
    {
        if (uiComponent instanceof HtmlOutcomeTargetButton)
        {
            return ((HtmlOutcomeTargetButton) uiComponent).getImage();
        }
        return (String) uiComponent.getAttributes().get(JSFAttr.IMAGE_ATTR);
    }

    public void encodeChildren(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        RendererUtils.renderChildren(facesContext, component);
    }

    public void encodeEnd(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        super.encodeEnd(facesContext, component); //check for NP

        ResponseWriter writer = facesContext.getResponseWriter();

        writer.endElement(HTML.INPUT_ELEM);
    }
}
