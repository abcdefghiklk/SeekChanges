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

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderer;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRendererUtils;

/**
 * Renderer used by h:head component
 * 
 * @since 2.0
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 772811 $ $Date: 2009-05-07 18:28:57 -0500 (Thu, 07 May 2009) $
 */
@JSFRenderer(renderKitId = "HTML_BASIC", family = "javax.faces.Output", type = "javax.faces.Head")
public class HtmlHeadRenderer extends Renderer
{
    //TODO: Move constants to shared HTML class
    private final static String HEAD_ELEM = "head";
    private final static String HEAD_TARGET = HEAD_ELEM;

    private final static String PROFILE_ATTR = "profile";

    private final static String[] HEAD_PASSTHROUGH_ATTRIBUTES = { HTML.DIR_ATTR,
            HTML.LANG_ATTR, PROFILE_ATTR};

    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        super.encodeBegin(facesContext, component); //check for NP

        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement(HEAD_ELEM, component);
        HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
        HtmlRendererUtils.renderHTMLAttributes(writer, component,
                HEAD_PASSTHROUGH_ATTRIBUTES);
    }

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        super.encodeEnd(facesContext, component); //check for NP

        ResponseWriter writer = facesContext.getResponseWriter();
        UIViewRoot root = facesContext.getViewRoot();
        for (UIComponent child : root.getComponentResources(facesContext,
                HEAD_TARGET))
        {
            child.encodeAll(facesContext);
        }
        writer.endElement(HEAD_ELEM);
    }
}
