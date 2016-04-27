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
import java.util.List;
import java.util.Map;

import javax.faces.application.ProjectStage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared_impl.renderkit.ClientBehaviorEvents;
import org.apache.myfaces.shared_impl.renderkit.html.util.JavascriptUtils;

/**
 * Renderer used by h:body component
 * 
 * @since 2.0
 * @author Leonardo Uribe (latest modification by $Author: jakobk $)
 * @version $Revision: 930075 $ $Date: 2010-04-01 15:25:28 -0500 (Thu, 01 Apr 2010) $
 */
public class HtmlBodyRendererBase extends HtmlRenderer
{

    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        super.encodeBegin(facesContext, component); //check for NP

        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement(HTML.BODY_ELEM, component);
        HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
        Map<String, List<ClientBehavior>> behaviors = null;
        if (component instanceof ClientBehaviorHolder && JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext()))
        {
            behaviors = ((ClientBehaviorHolder) component).getClientBehaviors();
            HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, component, behaviors);
            HtmlRendererUtils.renderBehaviorizedAttribute(facesContext, writer, component, behaviors,
                    ClientBehaviorEvents.LOAD, HTML.ONLOAD_ATTR, HTML.ONLOAD_ATTR);
            HtmlRendererUtils.renderBehaviorizedAttribute(facesContext, writer, component, behaviors,
                    ClientBehaviorEvents.UNLOAD, HTML.ONUNLOAD_ATTR, HTML.ONUNLOAD_ATTR);
            HtmlRendererUtils.renderHTMLAttributes(writer, component,
                    HTML.BODY_PASSTHROUGH_ATTRIBUTES_WITHOUT_EVENTS);
            
        }
        else
        {
            HtmlRendererUtils.renderHTMLAttributes(writer, component,
                    HTML.BODY_PASSTHROUGH_ATTRIBUTES);
        }
    }

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        super.encodeEnd(facesContext, component); //check for NP

        ResponseWriter writer = facesContext.getResponseWriter();
        UIViewRoot root = facesContext.getViewRoot();
        for (UIComponent child : root.getComponentResources(facesContext,
                HTML.BODY_TARGET))
        {
            child.encodeAll(facesContext);
        }
        
        // render all unhandled FacesMessages when ProjectStage is Development
        if (facesContext.isProjectStage(ProjectStage.Development))
        {
            HtmlRendererUtils.renderUnhandledFacesMessages(facesContext);
        }
        
        writer.endElement(HTML.BODY_ELEM);
    }
}
