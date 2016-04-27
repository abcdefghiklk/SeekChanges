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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.NavigationCase;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;

import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.element.NavigationRule;
import org.apache.myfaces.shared_impl.util.HashMapUtils;

/**
 * @author Thomas Spiegl (latest modification by $Author: lu4242 $)
 * @author Anton Koinov
 * @version $Revision: 898159 $ $Date: 2010-01-11 21:39:37 -0500 (Mon, 11 Jan 2010) $
 */
public class NavigationHandlerImpl
    extends ConfigurableNavigationHandler
{
    //private static final Log log = LogFactory.getLog(NavigationHandlerImpl.class);
    private static final Logger log = Logger.getLogger(NavigationHandlerImpl.class.getName());

    private static final String ASTERISK = "*";

    private Map<String, Set<NavigationCase>> _navigationCases = null;
    private List<String> _wildcardKeys = new ArrayList<String>();

    public NavigationHandlerImpl()
    {
        if (log.isLoggable(Level.FINEST)) log.finest("New NavigationHandler instance created");
    }

    @Override
    public void handleNavigation(FacesContext facesContext, String fromAction, String outcome)
    {
        NavigationCase navigationCase = getNavigationCase(facesContext, fromAction, outcome);

        if (navigationCase != null)
        {
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("handleNavigation fromAction=" + fromAction + " outcome=" + outcome +
                          " toViewId =" + navigationCase.getToViewId(facesContext) +
                          " redirect=" + navigationCase.isRedirect());
            }
            if (navigationCase.isRedirect())
            { 
                //&& (!PortletUtil.isPortletRequest(facesContext)))
                // Spec section 7.4.2 says "redirects not possible" in this case for portlets
                //But since the introduction of portlet bridge and the 
                //removal of portlet code in myfaces core 2.0, this condition
                //no longer applies
                
                ExternalContext externalContext = facesContext.getExternalContext();
                ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
                String toViewId = navigationCase.getToViewId(facesContext);
                String redirectPath = viewHandler.getRedirectURL(facesContext, toViewId, navigationCase.getParameters(), navigationCase.isIncludeViewParams());
                
                // JSF 2.0 the javadoc of handleNavigation() says something like this 
                // "...If the view has changed after an application action, call
                // PartialViewContext.setRenderAll(true)...". The effect is that ajax requests
                // are included on navigation.
                PartialViewContext partialViewContext = facesContext.getPartialViewContext();
                if ( partialViewContext.isPartialRequest() && 
                     !partialViewContext.isRenderAll() && 
                     !facesContext.getViewRoot().getViewId().equals(toViewId))
                {
                    partialViewContext.setRenderAll(true);
                }
                
                // JSF 2.0 Spec call Flash.setRedirect(true) to notify Flash scope and take proper actions
                externalContext.getFlash().setRedirect(true);
                try
                {
                    externalContext.redirect(redirectPath);
                    facesContext.responseComplete();
                }
                catch (IOException e)
                {
                    throw new FacesException(e.getMessage(), e);
                }
            }
            else
            {
                ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
                //create new view
                String newViewId = navigationCase.getToViewId(facesContext);
                // JSF 2.0 the javadoc of handleNavigation() says something like this 
                // "...If the view has changed after an application action, call
                // PartialViewContext.setRenderAll(true)...". The effect is that ajax requests
                // are included on navigation.
                PartialViewContext partialViewContext = facesContext.getPartialViewContext();
                if ( partialViewContext.isPartialRequest() && 
                     !partialViewContext.isRenderAll() && 
                     !facesContext.getViewRoot().getViewId().equals(newViewId))
                {
                    partialViewContext.setRenderAll(true);
                }
                
                UIViewRoot viewRoot = viewHandler.createView(facesContext, newViewId);
                facesContext.setViewRoot(viewRoot);
                facesContext.renderResponse();
            }
        }
        else
        {
            // no navigationcase found, stay on current ViewRoot
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("handleNavigation fromAction=" + fromAction + " outcome=" + outcome +
                          " no matching navigation-case found, staying on current ViewRoot");
            }
        }
    }


    /**
     * Returns the navigation case that applies for the given action and outcome
     */
    public NavigationCase getNavigationCase(FacesContext facesContext, String fromAction, String outcome)
    {
        String viewId = facesContext.getViewRoot().getViewId();
        Map<String, Set<NavigationCase>> casesMap = getNavigationCases();
        NavigationCase navigationCase = null;

        Set<? extends NavigationCase> casesSet = casesMap.get(viewId);
        if (casesSet != null)
        {
            // Exact match?
            navigationCase = calcMatchingNavigationCase(facesContext, casesSet, fromAction, outcome);
        }

        if (navigationCase == null)
        {
            // Wildcard match?
            for (String fromViewId : getSortedWildcardKeys())
            {
                if (fromViewId.length() > 2)
                {
                    String prefix = fromViewId.substring(0, fromViewId.length() - 1);
                    if (viewId != null && viewId.startsWith(prefix))
                    {
                        casesSet = casesMap.get(fromViewId);
                        if (casesSet != null)
                        {
                            navigationCase = calcMatchingNavigationCase(facesContext, casesSet, fromAction, outcome);
                            if (navigationCase != null) break;
                        }
                    }
                }
                else
                {
                    casesSet = casesMap.get(fromViewId);
                    if (casesSet != null)
                    {
                        navigationCase = calcMatchingNavigationCase(facesContext, casesSet, fromAction, outcome);
                        if (navigationCase != null) break;
                    }
                }
            }
        }
        
        if (outcome != null && navigationCase == null)
        {
            //if outcome is null, we don't check outcome based nav cases
            //otherwise, if navgiationCase is still null, check outcome-based nav cases
            navigationCase = getOutcomeNavigationCase (facesContext, fromAction, outcome);
        }

        return navigationCase;  //if navigationCase == null, will stay on current view

    }
    
    /**
     * Performs the algorithm specified in 7.4.2 for situations where no navigation cases are defined and instead
     * the navigation case is to be determined from the outcome.
     * 
     * TODO: cache results?
     */
    
    private NavigationCase getOutcomeNavigationCase (FacesContext facesContext, String fromAction, String outcome)
    {
        String implicitViewId = null;
        boolean includeViewParams = false;
        int index;
        boolean isRedirect = false;
        String queryString = null;
        NavigationCase result = null;
        String viewId = facesContext.getViewRoot().getViewId();
        String viewIdToTest = outcome;
        
        // If viewIdToTest contains a query string, remove it and set queryString with that value.
        
        index = viewIdToTest.indexOf ("?");
        
        if (index != -1)
        {
            queryString = viewIdToTest.substring (index + 1);
            viewIdToTest = viewIdToTest.substring (0, index);
            
            // If queryString contains "faces-redirect=true", set isRedirect to true.
            
            if (queryString.indexOf ("faces-redirect=true") != -1)
            {
                isRedirect = true;
            }
            
            // If queryString contains "includeViewParams=true", set includeViewParams to true.
            
            if (queryString.indexOf ("includeViewParams=true") != -1)
            {
                includeViewParams = true;
            }
        }
        
        // FIXME: the spec states that redirection (i.e., isRedirect=true) is implied when preemptive navigation is performed,
        // though I'm not sure how we're supposed to determine that.
        
        // If viewIdToTest does not have a "file extension", use the one from the current viewId.
        // TODO: I don't know exactly what the spec means by "file extension".  I'm assuming everything after "."
        
        index = viewIdToTest.indexOf (".");
        
        if (index == -1)
        {
            index = viewId.indexOf (".");
            
            if (index != -1)
            {
                viewIdToTest += viewId.substring (index);
            }
        }
        
        // If viewIdToTest does not start with "/", look for the last "/" in viewId.  If not found, simply prepend "/".
        // Otherwise, prepend everything before and including the last "/" in viewId.
        
        if (!viewIdToTest.startsWith ("/"))
        {
            index = viewId.lastIndexOf ("/");
            
            if (index == -1)
            {
                viewIdToTest = "/" + viewIdToTest;
            }
            
            else
            {
                viewIdToTest = viewId.substring (0, index + 1) + viewIdToTest;
            }
        }
        
        // Call ViewHandler.deriveViewId() and set the result as implicitViewId.
        
        try
        {
            implicitViewId = facesContext.getApplication().getViewHandler().deriveViewId (facesContext, viewIdToTest);
        }
        
        catch (UnsupportedOperationException e)
        {
            // This is the case when a pre-JSF 2.0 ViewHandler is used.  In this case, the default algorithm must be used.
            // FIXME: I think we're always calling the "default" ViewHandler.deriveViewId() algorithm and we don't
            // distinguish between pre-JSF 2.0 and JSF 2.0 ViewHandlers.  This probably needs to be addressed.
        }
        
        if (implicitViewId != null)
        {
            Map<String, List<String>> params = new HashMap<String, List<String>>();
            
            // Append queryString to implicitViewId if it exists.
            
//            if (queryString != null)
//            {
//                implicitViewId += "?" + queryString;
//            }
            
            // Finally, create the NavigationCase.
            
            // FIXME: the spec doesn't really say how the redirect parameters are supposed to be
            // populated.  Assuming for now that they should stay empty...
            
            result = new NavigationCase (viewId, fromAction, outcome, null, implicitViewId, params,
                isRedirect, includeViewParams);
        }
        
        return result;
    }
    
    /**
     * Returns the view ID that would be created for the given action and outcome
     */
    public String getViewId(FacesContext context, String fromAction, String outcome)
    {
        return this.getNavigationCase(context, fromAction, outcome).getToViewId(context);
    }

    /**
     * TODO
     * Invoked by the navigation handler before the new view component is created.
     * @param viewId The view ID to be created
     * @return The view ID that should be used instead. If null, the view ID passed
     * in will be used without modification.
     */
    public String beforeNavigation(String viewId)
    {
        return null;
    }

    private NavigationCase calcMatchingNavigationCase(FacesContext context, Set<? extends NavigationCase> casesList, String actionRef, 
                                                      String outcome)
    {
        NavigationCase noConditionCase = null;
                        
        for (NavigationCase caze : casesList)
        {
            String cazeOutcome = caze.getFromOutcome();
            String cazeActionRef = caze.getFromAction();
            Boolean cazeIf = caze.getCondition(context);
            boolean ifMatches = (cazeIf == null ? false : cazeIf.booleanValue());
            // JSF 2.0: support conditional navigation via <if>.
            // Use for later cases.
            
            if(outcome == null && (cazeOutcome != null || cazeIf == null) && actionRef == null)
            {
                continue;   //To match an outcome value of null, the <from-outcome> must be absent and the <if> element present.
            }
            
            //If there are no conditions on navigation case save it and return as last resort
            if (cazeOutcome == null && cazeActionRef == null && cazeIf == null && noConditionCase == null && outcome != null)
            {
                noConditionCase = caze;
            }
            
            if (cazeActionRef != null)
            {
                if (cazeOutcome != null)
                {
                    if ((actionRef != null) && (outcome != null) && cazeActionRef.equals (actionRef) &&
                            cazeOutcome.equals (outcome))
                    {
                        // First case: match if <from-action> matches action and <from-outcome> matches outcome.
                        // Caveat: evaluate <if> if available.

                        if (cazeIf != null)
                        {
                            if (ifMatches)
                            {
                                return caze;
                            }

                            continue;
                        }
                        else
                        {
                            return caze;
                        }
                    }
                }
                else
                {
                    if ((actionRef != null) && cazeActionRef.equals (actionRef))
                    {
                        // Third case: if only <from-action> specified, match against action.
                        // Caveat: if <if> is available, evaluate.  If not, only match if outcome is not null.

                        if (cazeIf != null)
                        {
                            if (ifMatches)
                            {
                                return caze;
                            }
                            
                            continue;
                        }
                        else
                        {
                            if (outcome != null)
                            {
                                return caze;
                            }
                            
                            continue;
                        }
                    }
                }
            }
            else
            {
                if (cazeOutcome != null)
                {
                    if ((outcome != null) && cazeOutcome.equals (outcome))
                    {
                        // Second case: if only <from-outcome> specified, match against outcome.
                        // Caveat: if <if> is available, evaluate.

                        if (cazeIf != null)
                        {
                            if (ifMatches)
                            {
                                return caze;
                            }
                            
                            continue;
                        }
                        else
                        {
                            return caze;
                        }
                    }
                }
            }

            // Fourth case: anything else matches if outcome is not null or <if> is specified.

            if (outcome != null)
            {
                // Again, if <if> present, evaluate.
                if (cazeIf != null)
                {
                    if (ifMatches)
                    {
                        return caze;
                    }
                    
                    continue;
                }
            }

            if ((cazeIf != null) && ifMatches)
            {
                return caze;
            }
        }
        
        return noConditionCase;
    }

    private List<String> getSortedWildcardKeys()
    {
        return _wildcardKeys;
    }

    @Override
    public Map<String, Set<NavigationCase>> getNavigationCases()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(externalContext);

        if (_navigationCases == null || runtimeConfig.isNavigationRulesChanged())
        {
            synchronized (this)
            {
                if (_navigationCases == null || runtimeConfig.isNavigationRulesChanged())
                {
                    Collection<? extends NavigationRule> rules = runtimeConfig.getNavigationRules();
                    int rulesSize = rules.size();

                    Map<String, Set<NavigationCase>> cases = new HashMap<String, Set<NavigationCase>>(
                            HashMapUtils.calcCapacity(rulesSize));

                    List<String> wildcardKeys = new ArrayList<String>();

                    for (NavigationRule rule : rules)
                    {
                        String fromViewId = rule.getFromViewId();

                        //specification 7.4.2 footnote 4 - missing fromViewId is allowed:
                        if (fromViewId == null)
                        {
                            fromViewId = ASTERISK;
                        }
                        else
                        {
                            fromViewId = fromViewId.trim();
                        }

                        Set<NavigationCase> set = cases.get(fromViewId);
                        if (set == null)
                        {
                            set = new HashSet<NavigationCase>(convertNavigationCasesToAPI(rule));
                            cases.put(fromViewId, set);
                            if (fromViewId.endsWith(ASTERISK))
                            {
                                wildcardKeys.add(fromViewId);
                            }
                        }
                        else
                        {
                            set.addAll(convertNavigationCasesToAPI(rule));
                        }
                    }

                    Collections.sort(wildcardKeys, new KeyComparator());

                    synchronized (cases)
                    {
                        // We do not really need this sychronization at all, but this
                        // gives us the peace of mind that some good optimizing compiler
                        // will not rearrange the execution of the assignment to an
                        // earlier time, before all init code completes
                        _navigationCases = cases;
                        _wildcardKeys = wildcardKeys;

                        runtimeConfig.setNavigationRulesChanged(false);
                    }
                }
            }
        }
        return _navigationCases;
    }

    private static final class KeyComparator implements Comparator<String>
    {
        public int compare(String s1, String s2)
        {
            return -s1.compareTo(s2);
        }
    }
    
    private Set<NavigationCase> convertNavigationCasesToAPI(NavigationRule rule)
    {
        Collection<? extends org.apache.myfaces.config.element.NavigationCase> configCases = rule.getNavigationCases();
        Set<NavigationCase> apiCases = new HashSet<NavigationCase>(configCases.size());
        
        for(org.apache.myfaces.config.element.NavigationCase configCase : configCases)
        {   
            if(configCase.getRedirect() != null)
            {
                apiCases.add(new NavigationCase(rule.getFromViewId(),configCase.getFromAction(),configCase.getFromOutcome(),configCase.getIf(),configCase.getToViewId(),configCase.getRedirect().getViewParams(),true,configCase.getRedirect().isIncludeViewParams()));
            }
            else
            {
                apiCases.add(new NavigationCase(rule.getFromViewId(),configCase.getFromAction(),configCase.getFromOutcome(),configCase.getIf(),configCase.getToViewId(),null,false,false));
            }
        }
        
        return apiCases;
    }

}