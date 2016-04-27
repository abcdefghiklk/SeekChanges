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
package org.apache.myfaces.view;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewDeclarationLanguageFactory;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguageStrategy;
import org.apache.myfaces.view.jsp.JspViewDeclarationLanguageStrategy;

/**
 * This is the default PDL factory used as of JSF 2.0, it tries to use Facelet PDL whenever possible, 
 * but fallback on JSP if required.
 * 
 * @author Simon Lessard (latest modification by $Author: slessard $)
 * @version $Revision: 696523 $ $Date: 2009-03-21 14:55:54 -0400 (mer., 17 sept. 2008) $
 *
 * @since 2.0
 */
public class ViewDeclarationLanguageFactoryImpl extends ViewDeclarationLanguageFactory
{
    /**
     * Disable facelets VDL from the current application project. 
     */
    @JSFWebConfigParam(since="2.0", defaultValue="false", expectedValues="true,false")
    public static final String PARAM_DISABLE_JSF_FACELET = "javax.faces.DISABLE_FACELET_JSF_VIEWHANDLER";

    private static final String FACELETS_1_VIEW_HANDLER = "com.sun.facelets.FaceletViewHandler";

    private static final Logger LOGGER = Logger.getLogger(ViewDeclarationLanguageFactoryImpl.class.getName());
    
    private boolean _initialized;
    private ViewDeclarationLanguageStrategy[] _supportedLanguages;
    
    /**
     * 
     */
    public ViewDeclarationLanguageFactoryImpl()
    {
        _initialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ViewDeclarationLanguage getViewDeclarationLanguage(String viewId)
    {
        //if (viewId == null)
        //{
        //    throw new NullPointerException("viewId");
        //}
        
        // TODO: It would be nice to be able to preinitialize the factory. However, since it requires 
        //       access to the ExternalContext it may not be possible, depending on the loading order 
        //       in the FactoryFinder. Could use ideas here. -= SL =-
        initialize();
        
        for (ViewDeclarationLanguageStrategy strategy : _supportedLanguages)
        {
            if (strategy.handles(viewId))
            {
                return strategy.getViewDeclarationLanguage();
            }
        }
        
        throw new FacesException("Cannot find a valid PDL for view id " + viewId);
    }
    
    /**
     * Initialize the supported view declaration languages.
     */
    private void initialize()
    {
        if (!_initialized)
        {
            FacesContext context = FacesContext.getCurrentInstance();

            if (isFacelets2Enabled(context))
            {
                logWarningIfLegacyFaceletViewHandlerIsPresent(context);

                _supportedLanguages = new ViewDeclarationLanguageStrategy[2];
                _supportedLanguages[0] = new FaceletViewDeclarationLanguageStrategy();
                _supportedLanguages[1] = new JspViewDeclarationLanguageStrategy();
            }
            else
            {
                // Support JSP only
                _supportedLanguages = new ViewDeclarationLanguageStrategy[1];
                _supportedLanguages[0] = new JspViewDeclarationLanguageStrategy();
            }

            _initialized = true;
        }
    }
    
    /**
     * Determines if the current application uses Facelets-2.
     * To accomplish that it looks at the init param javax.faces.DISABLE_FACELET_JSF_VIEWHANDLER,
     * 
     * @param context the <code>FacesContext</code>
     * @return <code>true</code> if the current application uses the built in Facelets-2,
     *         <code>false</code> otherwise (e.g. it uses Facelets-1 or only JSP).
     */
    private boolean isFacelets2Enabled(FacesContext context)
    {
        String param = context.getExternalContext().getInitParameter(PARAM_DISABLE_JSF_FACELET);
        boolean facelets2ParamDisabled = (param != null && Boolean.parseBoolean(param.toLowerCase()));
        
        return !facelets2ParamDisabled;
    }
    
    /**
     * If the Facelets-1 ViewHandler com.sun.facelets.FaceletViewHandler is present <b>AND</b>
     * the new Facelets-2 is <b>NOT</b> disabled, we log a <code>WARNING</code>. 
     * 
     * @param context the <code>FacesContext</code>
     */
    private void logWarningIfLegacyFaceletViewHandlerIsPresent(FacesContext context)
    {
        boolean facelets1ViewHandlerPresent  = context.getApplication().getViewHandler().getClass().getName().equals(FACELETS_1_VIEW_HANDLER);

        if (facelets1ViewHandlerPresent)
        {
            if (LOGGER.isLoggable(Level.WARNING))
            {
                LOGGER.log(Level.WARNING, "Your faces-config.xml contains the " + FACELETS_1_VIEW_HANDLER + " class." + 
                    "\nYou need to remove it since you have not disabled the \"new\" Facelets-2 version with the " + PARAM_DISABLE_JSF_FACELET +
                    " context parameter");
            }
        }
    }
}