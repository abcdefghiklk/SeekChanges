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
package org.apache.myfaces.shared_impl.view;

import java.beans.BeanInfo;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FactoryFinder;
import javax.faces.application.Resource;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.view.StateManagementStrategy;
import javax.faces.view.ViewMetadata;

import org.apache.myfaces.shared_impl.application.DefaultViewHandlerSupport;
import org.apache.myfaces.shared_impl.application.ViewHandlerSupport;
import org.apache.myfaces.shared_impl.config.MyfacesConfig;
import org.apache.myfaces.shared_impl.renderkit.html.util.JavascriptUtils;


public abstract class JspViewDeclarationLanguageBase extends ViewDeclarationLanguageBase
{
  private static final Logger log = Logger.getLogger(JspViewDeclarationLanguageBase.class.getName());
  
  private static final String FORM_STATE_MARKER = "<!--@@JSF_FORM_STATE_MARKER@@-->";
  private static final String AFTER_VIEW_TAG_CONTENT_PARAM = JspViewDeclarationLanguageBase.class
              + ".AFTER_VIEW_TAG_CONTENT";
  private static final int FORM_STATE_MARKER_LEN = FORM_STATE_MARKER.length();

  private ViewHandlerSupport _cachedViewHandlerSupport;
  /**
   * {@inheritDoc}
   */
  @Override
  public BeanInfo getComponentMetadata(FacesContext context, Resource componentResource)
  {
      throw new UnsupportedOperationException();
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public Resource getScriptComponentResource(FacesContext context, Resource componentResource)
  {
      throw new UnsupportedOperationException();
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public void renderView(FacesContext context, UIViewRoot view) throws IOException
  {
      //Try not to use native objects in this class.  Both MyFaces and the bridge
      //provide implementations of buildView but they do not override this class.
      checkNull(context, "context");
      checkNull(view, "view");
  
      // do not render the view if the rendered attribute for the view is false
      if (!view.isRendered())
      {
          if (log.isLoggable(Level.FINEST))
              log.finest("View is not rendered");
          return;
      }
  
      ExternalContext externalContext = context.getExternalContext();
  
      String viewId = context.getViewRoot().getViewId();
  
      if (log.isLoggable(Level.FINEST))
          log.finest("Rendering JSP view: " + viewId);
  
  
      // handle character encoding as of section 2.5.2.2 of JSF 1.1
      if(null != externalContext.getSession(false))
      {
        externalContext.getSessionMap().put(ViewHandler.CHARACTER_ENCODING_KEY, externalContext.getResponseCharacterEncoding());
      }
  
      // render the view in this method (since JSF 1.2)
      RenderKitFactory renderFactory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
      RenderKit renderKit = renderFactory.getRenderKit(context, view.getRenderKitId());
  
      ResponseWriter responseWriter = context.getResponseWriter();
      if (responseWriter == null)
      {
          responseWriter = renderKit.createResponseWriter(externalContext.getResponseOutputWriter(), null, externalContext.getRequestCharacterEncoding());
          context.setResponseWriter(responseWriter);
      }
  
      ResponseWriter oldResponseWriter = responseWriter;
      StringWriter stateAwareWriter = null;
  
      StateManager stateManager = context.getApplication().getStateManager();
      if (stateManager.isSavingStateInClient(context))
      {
          stateAwareWriter = new StringWriter();
  
          // Create a new response-writer using as an underlying writer the stateAwareWriter
          // Effectively, all output will be buffered in the stateAwareWriter so that later
          // this writer can replace the state-markers with the actual state.
          responseWriter = oldResponseWriter.cloneWithWriter(stateAwareWriter);
          context.setResponseWriter(responseWriter);
      }
  
      actuallyRenderView(context, view);
      
      if(oldResponseWriter != null)
      {
          context.setResponseWriter(oldResponseWriter);    
      }
      
  
      // We're done with the document - now we can write all content
      // to the response, properly replacing the state-markers on the way out
      // by using the stateAwareWriter
      if (stateManager.isSavingStateInClient(context))
      {
          flushBufferToWriter(stateAwareWriter.getBuffer(), externalContext.getResponseOutputWriter());
      }
      else
      {
          stateManager.saveView(context);
      }
  
      // Final step - we output any content in the wrappedResponse response from above to the response,
      // removing the wrappedResponse response from the request, we don't need it anymore
      ViewResponseWrapper afterViewTagResponse = (ViewResponseWrapper) externalContext.getRequestMap()
              .get(AFTER_VIEW_TAG_CONTENT_PARAM);
      externalContext.getRequestMap().remove(AFTER_VIEW_TAG_CONTENT_PARAM);
  
      if (afterViewTagResponse != null)
      {
          afterViewTagResponse.flushToWriter(externalContext.getResponseOutputWriter(), externalContext.getResponseCharacterEncoding());
      }
  
      //TODO sobryan: Is this right?
      context.getResponseWriter().flush();
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public ViewMetadata getViewMetadata(FacesContext context, String viewId)
  {
      // Not necessary given that this method always returns null, but staying true to
      // the spec.
  
      checkNull(context, "context");
      //checkNull(viewId, "viewId");
  
      // JSP impl must return null.
  
      return null;
  }
  
  protected void setAfterViewTagResponseWrapper(ExternalContext ec, ViewResponseWrapper wrapper)
  {
    ec.getRequestMap().put(AFTER_VIEW_TAG_CONTENT_PARAM, wrapper);
  }
  
  protected void flushBufferToWriter(StringBuffer buff, Writer writer) throws IOException
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    StateManager stateManager = facesContext.getApplication().getStateManager();

    StringWriter stateWriter = new StringWriter();
    ResponseWriter realWriter = facesContext.getResponseWriter();
    facesContext.setResponseWriter(realWriter.cloneWithWriter(stateWriter));

    Object serializedView = stateManager.saveView(facesContext);

    stateManager.writeState(facesContext, serializedView);
    facesContext.setResponseWriter(realWriter);

    String state = stateWriter.getBuffer().toString();

    ExternalContext extContext = facesContext.getExternalContext();
    if (JavascriptUtils.isJavascriptAllowed(extContext)
            && MyfacesConfig.getCurrentInstance(extContext).isViewStateJavascript())
    {
      // If javascript viewstate is enabled no state markers were written
      writePartialBuffer(buff, 0, buff.length(), writer);
      writer.write(state);
    }
    else
    {
      // If javascript viewstate is disabled state markers must be replaced
      int lastFormMarkerPos = 0;
      int formMarkerPos = 0;
      // Find all state markers and write out actual state instead
      while ((formMarkerPos = buff.indexOf(JspViewDeclarationLanguageBase.FORM_STATE_MARKER, formMarkerPos)) > -1)
      {
        // Write content before state marker
        writePartialBuffer(buff, lastFormMarkerPos, formMarkerPos, writer);
        // Write state and move position in buffer after marker
        writer.write(state);
        formMarkerPos += JspViewDeclarationLanguageBase.FORM_STATE_MARKER_LEN;
        lastFormMarkerPos = formMarkerPos;
      }
      
      // Write content after last state marker
      if (lastFormMarkerPos < buff.length())
      {
        writePartialBuffer(buff, lastFormMarkerPos, buff.length(), writer);
      }
    }
  }
  
  protected void writePartialBuffer(StringBuffer contentBuffer, int beginIndex, int endIndex, Writer writer) throws IOException
  {
    int index = beginIndex;
    int bufferSize = 2048;
    char[] bufToWrite = new char[bufferSize];

    while (index < endIndex)
    {
      int maxSize = Math.min(bufferSize, endIndex - index);

      contentBuffer.getChars(index, index + maxSize, bufToWrite, 0);
      writer.write(bufToWrite, 0, maxSize);

      index += bufferSize;
    }
  }

  /**
   * Render the view now - properly setting and resetting the response writer
   */
  private void actuallyRenderView(FacesContext facesContext, UIViewRoot viewToRender)
      throws IOException
  {
      // Set the new ResponseWriter into the FacesContext, saving the old one aside.
      ResponseWriter responseWriter = facesContext.getResponseWriter();
  
      // Now we actually render the document
      // Call startDocument() on the ResponseWriter.
      responseWriter.startDocument();
  
      // Call encodeAll() on the UIViewRoot
      viewToRender.encodeAll(facesContext);
  
      // Call endDocument() on the ResponseWriter
      responseWriter.endDocument();
  
      responseWriter.flush();
  }
  
  @Override
  public StateManagementStrategy getStateManagementStrategy(FacesContext context, String viewId)
  {
      return null;
  }

  @Override
  protected String calculateViewId(FacesContext context, String viewId)
  {
      if (_cachedViewHandlerSupport == null)
      {
          _cachedViewHandlerSupport = new DefaultViewHandlerSupport();
      }
  
      return _cachedViewHandlerSupport.calculateViewId(context, viewId);
  }

  /**
   * Writes the response and replaces the state marker tags with the state information for the current context
   */
/*  private static class StateMarkerAwareWriter extends Writer
  {
    private StringBuilder buf;

    public StateMarkerAwareWriter()
    {
        this.buf = new StringBuilder();
    }

    @Override
    public void close() throws IOException
    {
    }

    @Override
    public void flush() throws IOException
    {
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
      if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0))
      {
        throw new IndexOutOfBoundsException();
      }
      else if (len == 0)
      {
        return;
      }
      buf.append(cbuf, off, len);
    }

    public StringBuilder getStringBuilder()
    {
      return buf;
    }
  }*/
}
