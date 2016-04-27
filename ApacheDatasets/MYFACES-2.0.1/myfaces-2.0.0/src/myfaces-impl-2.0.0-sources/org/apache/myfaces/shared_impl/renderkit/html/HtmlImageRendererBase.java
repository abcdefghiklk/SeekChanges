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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIGraphic;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared_impl.renderkit.JSFAttr;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.util.JavascriptUtils;


/**
 * @author Manfred Geiler (latest modification by $Author: grantsmith $)
 * @author Thomas Spiegl
 * @author Anton Koinov
 * @version $Revision$ $Date: 2005-05-11 18:45:06 +0200 (Wed, 11 May 2005) $
 */
public class HtmlImageRendererBase
        extends HtmlRenderer
{
    //private static final Log log = LogFactory.getLog(HtmlImageRendererBase.class);
    private static final Logger log = Logger.getLogger(HtmlImageRendererBase.class.getName());

    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent)
            throws IOException
    {
        org.apache.myfaces.shared_impl.renderkit.RendererUtils.checkParamValidity(facesContext, uiComponent, UIGraphic.class);

        ResponseWriter writer = facesContext.getResponseWriter();

        writer.startElement(HTML.IMG_ELEM, uiComponent);

        if (uiComponent instanceof ClientBehaviorHolder 
                && JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext())
                && !((ClientBehaviorHolder) uiComponent).getClientBehaviors().isEmpty())
        {
            HtmlRendererUtils.writeIdAndName(writer, uiComponent, facesContext);
        }
        else
        {
            HtmlRendererUtils.writeIdIfNecessary(writer, uiComponent, facesContext);
        }

        final String url = RendererUtils.getIconSrc(facesContext, uiComponent, JSFAttr.URL_ATTR);
        if (url != null)
        {
            writer.writeURIAttribute(HTML.SRC_ATTR, url,JSFAttr.VALUE_ATTR);
        }
        else
        {
          if (log.isLoggable(Level.WARNING)) log.warning("Graphic with id " + uiComponent.getClientId(facesContext) + " has no value (url or name).");
        }

        /* 
         * Warn the user if the ALT attribute is missing.
         */                
        if (uiComponent.getAttributes().get(HTML.ALT_ATTR) == null) 
        {
            //we don't want to flood logs with warnings in production and system test environments
            ProjectStage projectStage = facesContext.getApplication().getProjectStage();
            if(projectStage.equals(ProjectStage.Development) || projectStage.equals(ProjectStage.UnitTest))
                log.warning("ALT attribute is missing for : " + uiComponent.getId());
        }

        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder && JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext()))
        {
            behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
            HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, uiComponent, behaviors);
            HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, HTML.IMG_PASSTHROUGH_ATTRIBUTES_WITHOUT_EVENTS);
        }
        else
        {
            HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, HTML.IMG_PASSTHROUGH_ATTRIBUTES);
        }

        writer.endElement(org.apache.myfaces.shared_impl.renderkit.html.HTML.IMG_ELEM);

    }

}
