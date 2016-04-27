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
package org.apache.myfaces.shared_impl.renderkit.html.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared_impl.config.MyfacesConfig;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;

/**
 * @since 4.0.1
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 824859 $ $Date: 2009-10-13 12:42:36 -0500 (Mar, 13 Oct 2009) $
 */
public class ResourceUtils
{
    public final static String JAVAX_FACES_LIBRARY_NAME = "javax.faces";
    public final static String JSF_JS_RESOURCE_NAME = "jsf.js";
    private final static String RENDERED_STYLESHEET_RESOURCES_SET = "org.apache.myfaces.RENDERED_STYLESHEET_RESOURCES_SET"; 
    private final static String RENDERED_SCRIPT_RESOURCES_SET = "org.apache.myfaces.RENDERED_SCRIPT_RESOURCES_SET";

    /**
     * Return a set of already rendered resources by this renderer on the current
     * request. 
     * 
     * @param facesContext
     * @return
     */
    private static Map<String, Boolean> getRenderedStylesheetResources(FacesContext facesContext)
    {
        Map<String, Boolean> map = (Map<String, Boolean>) facesContext.getAttributes().get(RENDERED_STYLESHEET_RESOURCES_SET);
        if (map == null)
        {
            map = new HashMap<String, Boolean>();
            facesContext.getAttributes().put(RENDERED_STYLESHEET_RESOURCES_SET,map);
        }
        return map;
    }
    
    /**
     * Return a set of already rendered resources by this renderer on the current
     * request. 
     * 
     * @param facesContext
     * @return
     */
    private static Map<String, Boolean> getRenderedScriptResources(FacesContext facesContext)
    {
        Map<String, Boolean> map = (Map<String, Boolean>) facesContext.getAttributes().get(RENDERED_SCRIPT_RESOURCES_SET);
        if (map == null)
        {
            map = new HashMap<String, Boolean>();
            facesContext.getAttributes().put(RENDERED_SCRIPT_RESOURCES_SET,map);
        }
        return map;
    }
    
    public static void markScriptAsRendered(FacesContext facesContext, String libraryName, String resourceName)
    {
        getRenderedScriptResources(facesContext).put(libraryName != null ? libraryName+'/'+resourceName : resourceName, Boolean.TRUE);
    }
    
    public static void markStylesheetAsRendered(FacesContext facesContext, String libraryName, String resourceName)
    {
        getRenderedStylesheetResources(facesContext).put(libraryName != null ? libraryName+'/'+resourceName : resourceName, Boolean.TRUE);
    }
    
    public static boolean isRenderedScript(FacesContext facesContext, String libraryName, String resourceName)
    {
        return getRenderedScriptResources(facesContext).containsKey(libraryName != null ? libraryName+'/'+resourceName : resourceName);
    }
    
    public static boolean isRenderedStylesheet(FacesContext facesContext, String libraryName, String resourceName)
    {
        return getRenderedStylesheetResources(facesContext).containsKey(libraryName != null ? libraryName+'/'+resourceName : resourceName);
    }
    
    public static void writeScriptInline(FacesContext facesContext, ResponseWriter writer, String libraryName, String resourceName) throws IOException
    {
        if (!ResourceUtils.isRenderedScript(facesContext, libraryName, resourceName))
        {
            if (MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isRiImplAvailable())
            {
                //Use more compatible way.
                UIComponent outputScript = facesContext.getApplication().
                    createComponent(facesContext, "javax.faces.Output", "javax.faces.resource.Script");
                outputScript.getAttributes().put("name", resourceName);
                outputScript.getAttributes().put("library", libraryName);
                outputScript.encodeAll(facesContext);
            }
            else
            {
                //Fast shortcut, don't create component instance and do what HtmlScriptRenderer do.
                Resource resource = facesContext.getApplication().getResourceHandler().createResource(resourceName, libraryName);
                markScriptAsRendered(facesContext, libraryName, resourceName);
                writer.startElement(HTML.SCRIPT_ELEM, null);
                writer.writeAttribute(HTML.SCRIPT_TYPE_ATTR, HTML.SCRIPT_TYPE_TEXT_JAVASCRIPT , null);
                writer.writeURIAttribute(HTML.SRC_ATTR, resource.getRequestPath(), null);
                writer.endElement(HTML.SCRIPT_ELEM);
            }
        }
    }
}
