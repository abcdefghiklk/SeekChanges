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
package org.apache.myfaces.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.ResponseStateManager;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewDeclarationLanguageFactory;
import javax.faces.view.ViewMetadata;
import javax.servlet.http.HttpServletResponse;

import org.apache.myfaces.shared_impl.application.DefaultViewHandlerSupport;
import org.apache.myfaces.shared_impl.application.InvalidViewIdException;
import org.apache.myfaces.shared_impl.application.ViewHandlerSupport;
import org.apache.myfaces.shared_impl.config.MyfacesConfig;
import org.apache.myfaces.shared_impl.renderkit.html.util.JavascriptUtils;
import org.apache.myfaces.view.facelets.StateWriter;

/**
 * JSF 2.0 ViewHandler implementation 
 *
 * @since 2.0
 */
public class ViewHandlerImpl extends ViewHandler
{
    //private static final Log log = LogFactory.getLog(ViewHandlerImpl.class);
    private static final Logger log = Logger.getLogger(ViewHandlerImpl.class.getName());
    public static final String FORM_STATE_MARKER = "<!--@@JSF_FORM_STATE_MARKER@@-->";
    private ViewHandlerSupport _viewHandlerSupport;
    private ViewDeclarationLanguageFactory _vdlFactory;

    public ViewHandlerImpl()
    {
        _vdlFactory = (ViewDeclarationLanguageFactory)FactoryFinder.getFactory(FactoryFinder.VIEW_DECLARATION_LANGUAGE_FACTORY);
        if (log.isLoggable(Level.FINEST))
            log.finest("New ViewHandler instance created");
    }

    @Override
    public String deriveViewId(FacesContext context, String input)
    {
        if(input != null){
            try
            {
                //TODO: JSF 2.0 - need to make sure calculateViewId follows the new algorithm from 7.5.2 
                return getViewHandlerSupport().calculateAndCheckViewId(context, input);
            }
            catch (InvalidViewIdException e)
            {
                sendSourceNotFound(context, e.getMessage());
            }
        }
        return input;   // If the argument input is null, return null.
    }

    @Override
    public String getBookmarkableURL(FacesContext context, String viewId,
            Map<String, List<String>> parameters, boolean includeViewParams)
    {
        Map<String, List<String>> viewParameters;
        ExternalContext externalContext = context.getExternalContext();
        if (includeViewParams)
        {
            viewParameters = getViewParameterList(context, viewId, parameters);
        }
        else
        {
            viewParameters = parameters;
        }
        
        String actionEncodedViewId = getActionURL(context, viewId);
        String bookmarkEncodedURL = externalContext.encodeBookmarkableURL(actionEncodedViewId, viewParameters);
        return externalContext.encodeActionURL(bookmarkEncodedURL);
    }

    @Override
    public String getRedirectURL(FacesContext context, String viewId,
            Map<String, List<String>> parameters, boolean includeViewParams)
    {
        Map<String, List<String>> viewParameters;
        ExternalContext externalContext = context.getExternalContext();
        if (includeViewParams)
        {
            viewParameters = getViewParameterList(context, viewId, parameters);
        }
        else
        {
            viewParameters = parameters;
        }
        
        String actionEncodedViewId = getActionURL(context, viewId);
        String redirectEncodedURL = externalContext.encodeRedirectURL(actionEncodedViewId, viewParameters);
        return externalContext.encodeActionURL(redirectEncodedURL);
    }

    @Override
    public ViewDeclarationLanguage getViewDeclarationLanguage(
            FacesContext context, String viewId)
    {
        // return a suitable ViewDeclarationLanguage implementation for the given viewId
        return _vdlFactory.getViewDeclarationLanguage(viewId);
    }

    @Override
    public void initView(FacesContext context) throws FacesException
    {
        if(context.getExternalContext().getRequestCharacterEncoding() == null)
        {
            super.initView(context);    
        }        
    }

    /**
     * Get the locales specified as acceptable by the original request, compare them to the
     * locales supported by this Application and return the best match.
     */
    @Override
    public Locale calculateLocale(FacesContext facesContext)
    {
        Application application = facesContext.getApplication();
        for (Iterator<Locale> requestLocales = facesContext.getExternalContext().getRequestLocales(); requestLocales
                .hasNext();)
        {
            Locale requestLocale = requestLocales.next();
            for (Iterator<Locale> supportedLocales = application.getSupportedLocales(); supportedLocales.hasNext();)
            {
                Locale supportedLocale = supportedLocales.next();
                // higher priority to a language match over an exact match
                // that occurs further down (see JSTL Reference 1.0 8.3.1)
                if (requestLocale.getLanguage().equals(supportedLocale.getLanguage())
                        && (supportedLocale.getCountry() == null || supportedLocale.getCountry().length() == 0))
                {
                    return supportedLocale;
                }
                else if (supportedLocale.equals(requestLocale))
                {
                    return supportedLocale;
                }
            }
        }

        Locale defaultLocale = application.getDefaultLocale();
        return defaultLocale != null ? defaultLocale : Locale.getDefault();
    }

    @Override
    public String calculateRenderKitId(FacesContext facesContext)
    {
        Object renderKitId = facesContext.getExternalContext().getRequestMap().get(
                ResponseStateManager.RENDER_KIT_ID_PARAM);
        if (renderKitId == null)
        {
            renderKitId = facesContext.getApplication().getDefaultRenderKitId();
        }
        if (renderKitId == null)
        {
            renderKitId = RenderKitFactory.HTML_BASIC_RENDER_KIT;
        }
        return renderKitId.toString();
    }
    
    @Override
    public UIViewRoot createView(FacesContext context, String viewId)
    {
       checkNull(context, "facesContext");
       String calculatedViewId = getViewHandlerSupport().calculateViewId(context, viewId);
       return getViewDeclarationLanguage(context,calculatedViewId).createView(context,calculatedViewId);
    }

    @Override
    public String getActionURL(FacesContext context, String viewId)
    {
        return getViewHandlerSupport().calculateActionURL(context, viewId);
    }

    @Override
    public String getResourceURL(FacesContext facesContext, String path)
    {
        if (path.length() > 0 && path.charAt(0) == '/')
        {
            return facesContext.getExternalContext().getRequestContextPath() + path;
        }

        return path;

    }

    @Override
    public void renderView(FacesContext context, UIViewRoot viewToRender)
            throws IOException, FacesException
    {

        checkNull(context, "context");
        checkNull(viewToRender, "viewToRender");

        getViewDeclarationLanguage(context,viewToRender.getViewId()).renderView(context, viewToRender);
    }

    @Override
    public UIViewRoot restoreView(FacesContext context, String viewId)
    {
        checkNull(context, "context");
    
        String calculatedViewId = getViewHandlerSupport().calculateViewId(context, viewId);
        return getViewDeclarationLanguage(context,calculatedViewId).restoreView(context, calculatedViewId); 
    }
    
    @Override
    public void writeState(FacesContext context) throws IOException
    {
        checkNull(context, "context");

        if(context.getPartialViewContext().isAjaxRequest())
            return;

        // Facelets specific hack:
        // Tell the StateWriter that we're about to write state
        StateWriter stateWriter = StateWriter.getCurrentInstance();
        if (stateWriter != null)
        {
            // Write the STATE_KEY out. Unfortunately, this will
            // be wasteful for pure server-side state managers where nothing
            // is actually written into the output, but this cannot
            // programatically be discovered
            stateWriter.writingState();
        }

        StateManager stateManager = context.getApplication().getStateManager();
        if (stateManager.isSavingStateInClient(context))
        {
            // Only write state marker if javascript view state is disabled
            ExternalContext extContext = context.getExternalContext();
            if (!(JavascriptUtils.isJavascriptAllowed(extContext) && MyfacesConfig.getCurrentInstance(extContext).isViewStateJavascript())) {
                context.getResponseWriter().write(FORM_STATE_MARKER);
            }
        }
        else
        {
            stateManager.writeState(context, new Object[2]);
        }
    }
    
    private Map<String, List<String>> getViewParameterList(FacesContext context,
            String viewId, Map<String, List<String>> parametersFromArg)
    {

        Map<String, List<String>> viewParameters;
        UIViewRoot viewRoot = context.getViewRoot();
        String currentViewId = viewRoot.getViewId();
        Collection<UIViewParameter> toViewParams = null;
        Collection<UIViewParameter> currentViewParams = ViewMetadata.getViewParameters(viewRoot);

        if (currentViewId.equals(viewId))
        {
            toViewParams = currentViewParams;
        }
        else
        {
            String calculatedViewId = getViewHandlerSupport().calculateViewId(context, viewId);            
            ViewDeclarationLanguage vdl = getViewDeclarationLanguage(context,calculatedViewId);
            ViewMetadata viewMetadata = vdl.getViewMetadata(context, viewId);
            // getViewMetadata() returns null on JSP
            if (viewMetadata != null)
            {
                UIViewRoot viewFromMetaData = viewMetadata.createMetadataView(context);
                toViewParams = ViewMetadata.getViewParameters(viewFromMetaData);
            }
        }

        if (toViewParams == null || toViewParams.isEmpty())
        {
            return parametersFromArg;
        }

        for (UIViewParameter viewParameter : toViewParams)
        {
            if (!parametersFromArg.containsKey(viewParameter.getName()))
            {
                String parameterValue = viewParameter.getStringValueFromModel(context);
                if (parameterValue == null)
                {
                    if(currentViewId.equals(viewId))
                    {
                        parameterValue = viewParameter.getStringValue(context);
                    }
                    else
                    {
                        boolean found = false;
                        for (UIViewParameter curParam : currentViewParams) {
                            if (curParam.getName() != null && viewParameter.getName() != null &&
                                    curParam.getName().equals(viewParameter.getName())) 
                            {
                                parameterValue = curParam.getStringValue(context);
                                found = true;
                            }
                            if (found)
                                break;
                        }
                    }
                }
                if (parameterValue != null)
                {
                    List<String> parameterValueList = parametersFromArg.get(viewParameter.getName());
                    if (parameterValueList == null)
                    {
                        parameterValueList = new ArrayList<String>();
                    }
                    parameterValueList.add(parameterValue);
                    parametersFromArg.put(viewParameter.getName(),parameterValueList);
                }
            }
        }        
        return parametersFromArg;
    }
    
    private void checkNull(final Object o, final String param)
    {
        if (o == null)
        {
            throw new NullPointerException(param + " can not be null.");
        }
    }
    
    private void sendSourceNotFound(FacesContext context, String message)
    {
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        try
        {
            context.responseComplete();
            response.sendError(HttpServletResponse.SC_NOT_FOUND, message);
        }
        catch (IOException ioe)
        {
            throw new FacesException(ioe);
        }
    }
    
    public void setViewHandlerSupport(ViewHandlerSupport viewHandlerSupport)
    {
        _viewHandlerSupport = viewHandlerSupport;
    }    
    
    protected ViewHandlerSupport getViewHandlerSupport()
    {
        if (_viewHandlerSupport == null)
        {
            _viewHandlerSupport = new DefaultViewHandlerSupport();
        }
        return _viewHandlerSupport;
    }
}
