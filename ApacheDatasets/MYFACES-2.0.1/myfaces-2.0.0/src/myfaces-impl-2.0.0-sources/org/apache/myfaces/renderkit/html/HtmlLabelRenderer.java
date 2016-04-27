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
package org.apache.myfaces.renderkit.html;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderer;
import org.apache.myfaces.shared_impl.component.EscapeCapable;
import org.apache.myfaces.shared_impl.renderkit.JSFAttr;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRenderer;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.util.JavascriptUtils;

/**
 * 
 * @author Thomas Spiegl (latest modification by $Author: lu4242 $)
 * @author Anton Koinov
 * @author Martin Marinschek
 * @version $Revision: 882748 $ $Date: 2009-11-20 17:37:18 -0500 (Fri, 20 Nov 2009) $
 */
@JSFRenderer(renderKitId = "HTML_BASIC", family = "javax.faces.Output", type = "javax.faces.Label")
public class HtmlLabelRenderer extends HtmlRenderer
{
    //private static final Log log = LogFactory.getLog(HtmlLabelRenderer.class);
    private static final Logger log = Logger.getLogger(HtmlLabelRenderer.class.getName());

    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent uiComponent) throws IOException
    {
        super.encodeBegin(facesContext, uiComponent); // check for NP

        ResponseWriter writer = facesContext.getResponseWriter();

        encodeBefore(facesContext, writer, uiComponent);

        writer.startElement(HTML.LABEL_ELEM, uiComponent);
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
            }
            HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, uiComponent, behaviors);
            HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(facesContext, writer, uiComponent, behaviors);
            HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, HTML.LABEL_PASSTHROUGH_ATTRIBUTES_WITHOUT_EVENTS);
        }
        else
        {
            HtmlRendererUtils.writeIdIfNecessary(writer, uiComponent, facesContext);
            HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, HTML.LABEL_PASSTHROUGH_ATTRIBUTES);
        }

        String forAttr = getFor(uiComponent);

        if (forAttr != null)
        {
            writer.writeAttribute(HTML.FOR_ATTR, getClientId(facesContext, uiComponent, forAttr), JSFAttr.FOR_ATTR);
        }
        else
        {
            if (log.isLoggable(Level.WARNING))
            {
                log.warning("Attribute 'for' of label component with id " + uiComponent.getClientId(facesContext)
                        + " is not defined");
            }
        }

        // MyFaces extension: Render a label text given by value
        // TODO: Move to extended component
        if (uiComponent instanceof ValueHolder)
        {
            String text = RendererUtils.getStringValue(facesContext, uiComponent);
            if (text != null)
            {
                boolean escape;
                if (uiComponent instanceof HtmlOutputLabel || uiComponent instanceof EscapeCapable)
                {
                    escape = ((HtmlOutputLabel)uiComponent).isEscape();
                }
                else
                {
                    escape = RendererUtils.getBooleanAttribute(uiComponent, org.apache.myfaces.shared_impl.renderkit.JSFAttr.ESCAPE_ATTR,
                                                               true); //default is to escape
                }                
                if (escape)
                {
                    writer.writeText(text, org.apache.myfaces.shared_impl.renderkit.JSFAttr.VALUE_ATTR);
                }
                else
                {
                    writer.write(text);
                }
            }
        }

        writer.flush(); // close start tag

        encodeAfterStart(facesContext, writer, uiComponent);
    }

    /**
     * @throws IOException  
     */
    protected void encodeAfterStart(FacesContext facesContext, ResponseWriter writer, UIComponent uiComponent)
        throws IOException
    {
    }

    /**
     * @throws IOException  
     */
    protected void encodeBefore(FacesContext facesContext, ResponseWriter writer, UIComponent uiComponent)
        throws IOException
    {
    }

    protected String getFor(UIComponent component)
    {
        if (component instanceof HtmlOutputLabel)
        {
            return ((HtmlOutputLabel)component).getFor();
        }

        return (String)component.getAttributes().get(JSFAttr.FOR_ATTR);

    }

    protected String getClientId(FacesContext facesContext, UIComponent uiComponent, String forAttr)
    {
        return RendererUtils.getClientId(facesContext, uiComponent, forAttr);
    }

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent) throws IOException
    {
        super.encodeEnd(facesContext, uiComponent); // check for NP

        ResponseWriter writer = facesContext.getResponseWriter();

        encodeBeforeEnd(facesContext, writer, uiComponent);

        writer.endElement(HTML.LABEL_ELEM);

        encodeAfter(facesContext, writer, uiComponent);
    }

    /**
     * @throws IOException  
     */
    protected void encodeBeforeEnd(FacesContext facesContext, ResponseWriter writer, UIComponent uiComponent)
        throws IOException
    {
    }

    /**
     * @throws IOException  
     */
    protected void encodeAfter(FacesContext facesContext, ResponseWriter writer, UIComponent uiComponent)
        throws IOException
    {
    }
}
