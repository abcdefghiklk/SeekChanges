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
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.render.ClientBehaviorRenderer;
import javax.faces.render.RenderKit;
import javax.faces.render.Renderer;
import javax.faces.render.ResponseStateManager;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderKit;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlResponseWriterImpl;

/**
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @version $Revision: 824859 $ $Date: 2009-10-13 12:42:36 -0500 (Tue, 13 Oct 2009) $
 */
@JSFRenderKit(renderKitId = "HTML_BASIC")
public class HtmlRenderKitImpl extends RenderKit
{
    //private static final Log log = LogFactory.getLog(HtmlRenderKitImpl.class);
    private static final Logger log = Logger.getLogger(HtmlRenderKitImpl.class.getName());

    // ~ Instance fields ----------------------------------------------------------------------------

    private Map<String, Map<String, Renderer>> _renderers;
    private ResponseStateManager _responseStateManager;
    private Map<String,Set<String>> _families;
    private Map<String, ClientBehaviorRenderer> _clientBehaviorRenderers;

    // ~ Constructors -------------------------------------------------------------------------------

    public HtmlRenderKitImpl()
    {
        _renderers = new ConcurrentHashMap<String, Map<String, Renderer>>(64, 0.75f, 1);
        _responseStateManager = new HtmlResponseStateManager();
        _families = new HashMap<String, Set<String> >();
        _clientBehaviorRenderers = new HashMap<String, ClientBehaviorRenderer>();
    }

    // ~ Methods ------------------------------------------------------------------------------------

    @Override
    public void addClientBehaviorRenderer(String type, ClientBehaviorRenderer renderer)
    {
        if (type == null)
        {
            throw new NullPointerException("client behavior renderer type must not be null");
        }
        if ( renderer == null)
        {
            throw new NullPointerException("client behavior renderer must not be null");
        }
        
        _clientBehaviorRenderers.put(type, renderer);
    }
    
    @Override
    public ClientBehaviorRenderer getClientBehaviorRenderer(String type)
    {
        if (type == null)
        {
            throw new NullPointerException("client behavior renderer type must not be null");
        }
        
        return _clientBehaviorRenderers.get(type);
    }
    
    @Override
    public Iterator<String> getClientBehaviorRendererTypes()
    {
        return _clientBehaviorRenderers.keySet().iterator();
    }
    
    @Override
    public Renderer getRenderer(String componentFamily, String rendererType)
    {
        if (componentFamily == null)
        {
            throw new NullPointerException("component family must not be null.");
        }
        if (rendererType == null)
        {
            throw new NullPointerException("renderer type must not be null.");
        }
        Map <String,Renderer> familyRendererMap = _renderers.get(componentFamily); 
        Renderer renderer = null;
        if (familyRendererMap != null)
        {
            renderer = familyRendererMap.get(rendererType);
        }
        if (renderer == null)
        {
            log.warning("Unsupported component-family/renderer-type: " + componentFamily + "/" + rendererType);
        }
        return renderer;
    }

    @Override
    public void addRenderer(String componentFamily, String rendererType, Renderer renderer)
    {
        if (componentFamily == null)
        {
            log.severe("addRenderer: componentFamily = null is not allowed");
            throw new NullPointerException("component family must not be null.");
        }
        if (rendererType == null)
        {
            log.severe("addRenderer: rendererType = null is not allowed");
            throw new NullPointerException("renderer type must not be null.");
        }
        if (renderer == null)
        {
            log.severe("addRenderer: renderer = null is not allowed");
            throw new NullPointerException("renderer must not be null.");
        }
        
        _put(componentFamily, rendererType, renderer);

        if (log.isLoggable(Level.FINEST))
            log.finest("add Renderer family = " + componentFamily + " rendererType = " + rendererType
                    + " renderer class = " + renderer.getClass().getName());
    }
    
    /**
     * Put the renderer on the double map
     * 
     * @param componentFamily
     * @param rendererType
     * @param renderer
     */
    synchronized private void _put(String componentFamily, String rendererType, Renderer renderer)
    {
        Map <String,Renderer> familyRendererMap = _renderers.get(componentFamily);
        if (familyRendererMap == null)
        {
            familyRendererMap = new ConcurrentHashMap<String, Renderer>(8, 0.75f, 1);
            _renderers.put(componentFamily, familyRendererMap);
        }
        else
        {
            if (familyRendererMap.get(rendererType) != null) {
                // this is not necessarily an error, but users do need to be
                // very careful about jar processing order when overriding
                // some component's renderer with an alternate renderer.
                log.fine("Overwriting renderer with family = " + componentFamily +
                   " rendererType = " + rendererType +
                   " renderer class = " + renderer.getClass().getName());
            }
        }
        familyRendererMap.put(rendererType, renderer);
    }

    @Override
    public ResponseStateManager getResponseStateManager()
    {
        return _responseStateManager;
    }
    
    /**
     * @since JSF 2.0
     */
    @Override
    public Iterator<String> getComponentFamilies()
    {
        return _families.keySet().iterator();
    }
    
    /**
     * @since JSF 2.0
     */
    @Override
    public Iterator<String> getRendererTypes(String componentFamily)
    {
        //Return an Iterator over the renderer-type entries for the given component-family.
        Set<String> rendererTypes = _families.get(componentFamily);
        if(rendererTypes != null)
        {
            return rendererTypes.iterator();
        }
        //If the specified componentFamily is not known to this RenderKit implementation, return an empty Iterator
        return Collections.<String>emptySet().iterator();
        


    }

    @Override
    public ResponseWriter createResponseWriter(Writer writer, String contentTypeListString, String characterEncoding)
    {
        String selectedContentType = HtmlRendererUtils.selectContentType(contentTypeListString);

        if (characterEncoding == null)
        {
            characterEncoding = HtmlRendererUtils.DEFAULT_CHAR_ENCODING;
        }

        return new HtmlResponseWriterImpl(writer, selectedContentType, characterEncoding);
    }

    @Override
    public ResponseStream createResponseStream(OutputStream outputStream)
    {
        return new MyFacesResponseStream(outputStream);
    }
    
    private void checkNull(Object value, String valueLabel)
    {
        if (value == null)
        {
            throw new NullPointerException(valueLabel + " is null");
        }
    }

    private static class MyFacesResponseStream extends ResponseStream
    {
        private OutputStream output;

        public MyFacesResponseStream(OutputStream output)
        {
            this.output = output;
        }

        @Override
        public void write(int b) throws IOException
        {
            output.write(b);
        }

        @Override
        public void write(byte b[]) throws IOException
        {
            output.write(b);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException
        {
            output.write(b, off, len);
        }

        @Override
        public void flush() throws IOException
        {
            output.flush();
        }

        @Override
        public void close() throws IOException
        {
            output.close();
        }
    }
}
