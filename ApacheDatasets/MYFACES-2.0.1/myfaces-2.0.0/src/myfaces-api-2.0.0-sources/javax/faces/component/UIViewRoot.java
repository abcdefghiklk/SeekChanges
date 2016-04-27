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
package javax.faces.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FactoryFinder;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.faces.event.PostConstructViewMapEvent;
import javax.faces.event.PostRestoreStateEvent;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewMetadata;
import javax.faces.webapp.FacesServlet;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * Creates a JSF View, which is a container that holds all of the components that are part of the view.
 * <p>
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * </p>
 * <p>
 * See the javadoc for this class in the <a href="http://java.sun.com/j2ee/javaserverfaces/1.2/docs/api/index.html">JSF
 * Specification</a> for further details.
 * </p>
 */
@JSFComponent(name = "f:view", bodyContent = "JSP", tagClass = "org.apache.myfaces.taglib.core.ViewTag")
@JSFJspProperty(name = "binding", returnType = "java.lang.String", tagExcluded = true)
public class UIViewRoot extends UIComponentBase implements UniqueIdVendor
{
    public static final String COMPONENT_FAMILY = "javax.faces.ViewRoot";
    public static final String COMPONENT_TYPE = "javax.faces.ViewRoot";
    public static final String METADATA_FACET_NAME = "javax_faces_metadata";
    public static final String UNIQUE_ID_PREFIX = "j_id";
    public static final String VIEW_PARAMETERS_KEY = "javax.faces.component.VIEW_PARAMETERS_KEY";

    private final Logger logger = Logger.getLogger(UIViewRoot.class.getName());

    private static final PhaseProcessor APPLY_REQUEST_VALUES_PROCESSOR = new ApplyRequestValuesPhaseProcessor();
    private static final PhaseProcessor PROCESS_VALIDATORS_PROCESSOR = new ProcessValidatorPhaseProcessor();
    private static final PhaseProcessor UPDATE_MODEL_PROCESSOR = new UpdateModelPhaseProcessor();

    /**
     * The counter which will ensure a unique component id for every component instance in the tree that doesn't have an
     * id attribute set.
     */
    //private long _uniqueIdCounter = 0;

    // todo: is it right to save the state of _events and _phaseListeners?
    private List<FacesEvent> _events;

    /**
     * Map containing view scope objects. 
     * 
     * It is not expected this map hold PartialStateHolder instances,
     * so we can use saveAttachedState and restoreAttachedState methods.
     */
    private Map<String, Object> _viewScope;

    private transient Lifecycle _lifecycle = null;
    
    private HashMap<Class<? extends SystemEvent>, List<SystemEventListener>> _systemEventListeners;
    
    // Tracks success in the beforePhase. Listeners that threw an exception
    // in beforePhase or were never called, because a previous listener threw
    // an exception, should not have their afterPhase method called
    private transient Map<PhaseId, boolean[]> listenerSuccessMap = new HashMap<PhaseId, boolean[]>();
    
    /**
     * Construct an instance of the UIViewRoot.
     */
    public UIViewRoot()
    {
        setRendererType(null);
        
        _systemEventListeners = new HashMap<Class<? extends SystemEvent>, List<SystemEventListener>>();
    }

    /**
     * @since 2.0
     */
    public void addComponentResource(FacesContext context, UIComponent componentResource)
    {
        addComponentResource(context, componentResource, null);
    }

    /**
     * @since 2.0
     */
    public void addComponentResource(FacesContext context, UIComponent componentResource, String target)
    {
        // If the target argument is null
        if (target == null)
        {
            // Look for a target attribute on the component
            target = (String)componentResource.getAttributes().get("target");

            // If there is no target attribute, set target to be the default value head
            if (target == null)
            {
                target = "head";
            }
        }

        // Call getComponentResources to obtain the child list for the given target
        List<UIComponent> componentResources = _getComponentResources(context, target);

        // If the component ID of componentResource matches the ID of a resource that has already been added, remove the old resource.
        String componentId = componentResource.getId();
        
        // This var helps to handle the case when we try to add a component that already is
        // on the resource list, because PostAddToViewEvent also is sent to components 
        // backing resources. The problem start when a component is already inside
        // componentResources list and we try to relocate it again. This leads to a StackOverflowException
        // so we need to check if a component is and prevent remove and add it again. Note
        // that remove and then add a component trigger another PostAddToViewEvent. The right
        // point to prevent this StackOverflowException is here, because this method is 
        // responsible to traverse the componentResources list and add when necessary.
        boolean alreadyAdded = false;
        
        if (componentId != null)
        {
            for(Iterator<UIComponent> it = componentResources.iterator(); it.hasNext();)
            {
                UIComponent component = it.next();
                if(componentId.equals(component.getId()) && componentResource != component)
                {
                    it.remove();
                }
                else if (componentResource == component)
                {
                    alreadyAdded = true;
                }
            }
        }
        
        // Add the component resource to the list
        if (!alreadyAdded)
        {
            componentResources.add(componentResource);
        }
    }

    /**
     * Adds a The phaseListeners attached to ViewRoot.
     */
    public void addPhaseListener(PhaseListener phaseListener)
    {
        if (phaseListener == null)
            throw new NullPointerException("phaseListener");
        
        getStateHelper().add(PropertyKeys.phaseListeners, phaseListener);
    }

    /**
     * @since 2.0
     */
    public void broadcastEvents(FacesContext context, PhaseId phaseId)
    {
        if (_events == null)
        {
            return;
        }

        // Gather the events and purge the event list to prevent concurrent modification during broadcasting
        List<FacesEvent> anyPhase = new ArrayList<FacesEvent>(_events.size());
        List<FacesEvent> onPhase = new ArrayList<FacesEvent>(_events.size());
        for (Iterator<FacesEvent> iterator = _events.iterator(); iterator.hasNext();)
        {
            FacesEvent event = iterator.next();
            if (event.getPhaseId().equals(PhaseId.ANY_PHASE))
            {
                anyPhase.add(event);
                iterator.remove();
            }
            else if (event.getPhaseId().equals(phaseId))
            {
                onPhase.add(event);
                iterator.remove();
            }
        }

        // First broadcast events that have been queued for PhaseId.ANY_PHASE.
        if (_broadcastAll(context, anyPhase))
        {
            _broadcastAll(context, onPhase);
        }
    }

    /**
     * Provides a unique id for this component instance.
     */
    public String createUniqueId()
    {
        return createUniqueId(FacesContext.getCurrentInstance(), null);
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * @since 2.0
     */
    public String createUniqueId(FacesContext context, String seed)
    {
        ExternalContext extCtx = context.getExternalContext();
        StringBuilder bld = __getSharedStringBuilder();

        Long uniqueIdCounter = (Long) getStateHelper().get(PropertyKeys.uniqueIdCounter);
        uniqueIdCounter = (uniqueIdCounter == null) ? 0 : uniqueIdCounter;
        getStateHelper().put(PropertyKeys.uniqueIdCounter, (uniqueIdCounter+1L));
        // Generate an identifier for a component. The identifier will be prefixed with UNIQUE_ID_PREFIX, and will be unique within this UIViewRoot. 
        if(seed==null)
        {
            return extCtx.encodeNamespace(bld.append(UNIQUE_ID_PREFIX).append(uniqueIdCounter).toString());    
        }
        // Optionally, a unique seed value can be supplied by component creators which should be included in the generated unique id.
        else
        {
            return extCtx.encodeNamespace(bld.append(UNIQUE_ID_PREFIX).append(seed).toString());
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException
    {
        checkNull(context, "context");

        boolean skipPhase = false;

        try
        {
            skipPhase = notifyListeners(context, PhaseId.RENDER_RESPONSE, getBeforePhaseListener(), true);
        }
        catch (Exception e)
        {
            // following the spec we have to swallow the exception
            logger.log(Level.SEVERE, "Exception while processing phase listener: " + e.getMessage(), e);
        }

        if (!skipPhase)
        {
            //prerendering happens, we now publish the prerender view event
            //the specs states that the viewroot as source is about to be rendered
            //hence we issue the event immediately before publish, if the phase is not skipped
            //context.getApplication().publishEvent(context, PreRenderViewEvent.class, this);
            //then the view rendering is about to begin
            super.encodeBegin(context);
        }
        else
        {
            pushComponentToEL(context, this);
        }
    }

    /**
     * @since 2.0
     */
    @Override
    public void encodeChildren(FacesContext context) throws IOException
    {
        if (context.getResponseComplete())
        {
            return;
        }
        PartialViewContext pContext = context.getPartialViewContext();
        
        // If PartialViewContext.isAjaxRequest() returns true
        if (pContext.isAjaxRequest())
        {
            // Perform partial rendering by calling PartialViewContext.processPartial() with PhaseId.RENDER_RESPONSE.
            //sectin 13.4.3 of the jsf2 specification
            pContext.processPartial(PhaseId.RENDER_RESPONSE);
        }
        else
        {
            // If PartialViewContext.isAjaxRequest() returns false
            // delegate to super.encodeChildren(javax.faces.context.FacesContext) method.
            super.encodeChildren(context);
        }
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException
    {
        checkNull(context, "context");

        if (!context.getResponseComplete())
        {
            super.encodeEnd(context);
            
            // the call to encodeAll() on every UIViewParameter here is only necessary
            // if the current request is _not_ an AJAX request, because if it was an
            // AJAX request, the call would already have happened in PartialViewContextImpl and
            // would anyway be too late here, because the state would already have been generated
            PartialViewContext partialContext = context.getPartialViewContext();
            if (!partialContext.isAjaxRequest())
            {
                ViewDeclarationLanguage vdl = context.getApplication().getViewHandler().getViewDeclarationLanguage(context, getViewId());
                if (vdl != null)
                {
                    // If the current view has view parameters, as indicated by a non-empty and non-UnsupportedOperationException throwing 
                    // return from ViewDeclarationLanguage.getViewMetadata(javax.faces.context.FacesContext, String)
                    ViewMetadata metadata = null;
                    try
                    {
                        metadata = vdl.getViewMetadata(context, getViewId());    
                    }
                    catch(UnsupportedOperationException e)
                    {
                        logger.log(Level.SEVERE, "Exception while obtaining the view metadata: " + e.getMessage(), e);
                    }
                    
                    if (metadata != null)
                    {
                        try
                        {
                            Collection<UIViewParameter> viewParams = ViewMetadata.getViewParameters(this);    
                            if(!viewParams.isEmpty())
                            {
                                // call UIViewParameter.encodeAll(javax.faces.context.FacesContext) on each parameter.
                                for(UIViewParameter param : viewParams)
                                {
                                    param.encodeAll(context);
                                }
                            }
                        }
                        catch(UnsupportedOperationException e)
                        {
                            // If calling getViewParameters() causes UnsupportedOperationException to be thrown, the exception must be silently swallowed.
                        }
                    }
                }
            }
        }
        
        try
        {
            notifyListeners(context, PhaseId.RENDER_RESPONSE, getAfterPhaseListener(), false);
        }
        catch (Exception e)
        {
            // following the spec we have to swallow the exception
            logger.log(Level.SEVERE, "Exception while processing phase listener: " + e.getMessage(), e);
        }
    }

    /**
     * MethodBinding pointing to a method that takes a javax.faces.event.PhaseEvent and returns void, called after every
     * phase except for restore view.
     *
     * @return the new afterPhaseListener value
     */
    @JSFProperty(returnSignature = "void", methodSignature = "javax.faces.event.PhaseEvent", jspName = "afterPhase")
    public MethodExpression getAfterPhaseListener()
    {
        return (MethodExpression) getStateHelper().eval(PropertyKeys.afterPhaseListener);
    }

    /**
     * MethodBinding pointing to a method that takes a javax.faces.event.PhaseEvent and returns void, called before
     * every phase except for restore view.
     *
     * @return the new beforePhaseListener value
     */
    @JSFProperty(returnSignature = "void", methodSignature = "javax.faces.event.PhaseEvent", jspName = "beforePhase")
    public MethodExpression getBeforePhaseListener()
    {
        return (MethodExpression) getStateHelper().eval(PropertyKeys.beforePhaseListener);
    }

    /**
     * DO NOT USE.
     * <p>
     * As this component has no "id" property, it has no clientId property either.
     */
    @Override
    public String getClientId(FacesContext context)
    {
        return super.getClientId(context);
        // Call parent method due to TCK problems
        // return null;
    }

    /**
     * @since 2.0
     */
    public List<UIComponent> getComponentResources(FacesContext context, String target)
    {
        // Locate the facet for the component by calling getFacet() using target as the argument
        UIComponent facet = getFacet(target);

        /*
        // If the facet is not found,
        if (facet == null)
        {
            // create the facet by calling context.getApplication().createComponent()  using javax.faces.Panel as the argument
            facet = context.getApplication().createComponent("javax.faces.Panel");

            // Set the id of the facet to be target
            facet.setId(target);

            // Add the facet to the facets Map using target as the key
            getFacets().put(target, facet);
        }

        // Return the children of the facet
        // The API doc indicates that this method should "Return an unmodifiable List of UIComponents for the provided target argument."
        // and also that "If no children are found for the facet, return Collections.emptyList()."
        List<UIComponent> children = facet.getChildren();
        return ( children == null ? Collections.<UIComponent>emptyList() : Collections.unmodifiableList(children) );
        */
        if (facet != null)
        {
            List<UIComponent> children = facet.getChildren();
            return ( children == null ? Collections.<UIComponent>emptyList() : Collections.unmodifiableList(children) );
        }
        return Collections.<UIComponent>emptyList();
    }
    
    private List<UIComponent> _getComponentResources(FacesContext context, String target)
    {
        // Locate the facet for the component by calling getFacet() using target as the argument
        UIComponent facet = getFacet(target);

        // If the facet is not found,
        if (facet == null)
        {
            // create the facet by calling context.getApplication().createComponent()  using javax.faces.Panel as the argument
            facet = context.getApplication().createComponent("javax.faces.Panel");

            // Set the id of the facet to be target
            facet.setId(target);
            
            // From jsr-314-open list it was made clear this facet is transient,
            // because all component resources does not change its inner state between
            // requests
            facet.setTransient(true);

            // Add the facet to the facets Map using target as the key
            getFacets().put(target, facet);
        }
        return facet.getChildren();
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }

    /**
     * The locale for this view.
     * <p>
     * Defaults to the default locale specified in the faces configuration file.
     * </p>
     */
    @JSFProperty
    public Locale getLocale()
    {
        Object locale = getStateHelper().get(PropertyKeys.locale);
        if (locale != null)
        {
            return (Locale)locale;
        }
        ValueExpression expression = getValueExpression(PropertyKeys.locale.toString());
        if (expression != null)
        {
            return (Locale)expression.getValue(getFacesContext().getELContext());
        }
        else
        {
            locale = getFacesContext().getApplication().getViewHandler().calculateLocale(getFacesContext());

            if (locale instanceof Locale)
            {
                return (Locale)locale;
            }
            else if (locale instanceof String)
            {
                return stringToLocale((String)locale);
            }
        }

        return getFacesContext().getApplication().getViewHandler().calculateLocale(getFacesContext());
    }

    /**
     * @since 2.0
     */
    public List<PhaseListener> getPhaseListeners()
    {
        List<PhaseListener> listeners = (List<PhaseListener>) getStateHelper().get(PropertyKeys.phaseListeners);
        if (listeners == null)
        {
            listeners = Collections.emptyList();
        }
        else
        {
            listeners = Collections.unmodifiableList(listeners);
        }

        return listeners;
    }

    /**
     * Defines what renderkit should be used to render this view.
     */
    @JSFProperty
    public String getRenderKitId()
    {
        return (String) getStateHelper().eval(PropertyKeys.renderKitId);
    }

    /**
     * @since 2.0
     */
    @Override
    public boolean getRendersChildren()
    {
        // Call UIComponentBase.getRendersChildren() 
        // If PartialViewContext.isAjaxRequest()  returns true this method must return true.
        PartialViewContext context = FacesContext.getCurrentInstance().getPartialViewContext();

        return (context.isAjaxRequest()) ? true : super.getRendersChildren();
    }

    /**
     * A unique identifier for the "template" from which this view was generated.
     * <p>
     * Typically this is the filesystem path to the template file, but the exact details are the responsibility of the
     * current ViewHandler implementation.
     */
    @JSFProperty(tagExcluded = true)
    public String getViewId()
    {
        return (String) getStateHelper().eval(PropertyKeys.viewId);
    }

    /**
     * @since 2.0
     */
    public Map<String, Object> getViewMap()
    {
        return this.getViewMap(true);
    }

    /**
     * @since 2.0
     */
    public Map<String, Object> getViewMap(boolean create)
    {
        if (_viewScope == null && create)
        {
            _viewScope = new ViewScope();
            FacesContext facesContext = getFacesContext();
            facesContext.getApplication().publishEvent(facesContext, PostConstructViewMapEvent.class, this);
        }

        return _viewScope;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInView()
    {
        return true;
    }

    public void processApplication(final FacesContext context)
    {
        checkNull(context, "context");
        _process(context, PhaseId.INVOKE_APPLICATION, null);
    }

    @Override
    public void processDecodes(FacesContext context)
    {
        checkNull(context, "context");
        _process(context, PhaseId.APPLY_REQUEST_VALUES, APPLY_REQUEST_VALUES_PROCESSOR);
    }

    /**
     * @since 2.0
     */
    @Override
    public void processRestoreState(FacesContext context, Object state)
    {
        // The default implementation must call UIComponentBase.processRestoreState(javax.faces.context.FacesContext,
        // java.lang.Object) from within a try block.
        try
        {
            super.processRestoreState(context, state);
        }
        finally
        {
            // The try block must have a finally block that ensures that no FacesEvents remain in the event queue
            broadcastEvents(context, PhaseId.RESTORE_VIEW);

            // invoke afterPhase MethodExpression
            // Note: In this phase it is not possible to invoke the beforePhase method, because we
            // first have to restore the view to get its attributes.
            //notifyListeners(context, PhaseId.RESTORE_VIEW, getAfterPhaseListener(), false);
            
            visitTree(VisitContext.createVisitContext(context), new RestoreStateCallback());
        }
    }

    @Override
    public void queueEvent(FacesEvent event)
    {
        checkNull(event, "event");
        if (_events == null)
        {
            _events = new ArrayList<FacesEvent>();
        }

        _events.add(event);
    }

    @Override
    public void processValidators(FacesContext context)
    {
        checkNull(context, "context");
        _process(context, PhaseId.PROCESS_VALIDATIONS, PROCESS_VALIDATORS_PROCESSOR);
    }

    @Override
    public void processUpdates(FacesContext context)
    {
        checkNull(context, "context");
        _process(context, PhaseId.UPDATE_MODEL_VALUES, UPDATE_MODEL_PROCESSOR);
    }

    public void setLocale(Locale locale)
    {
        getStateHelper().put(PropertyKeys.locale, locale );
    }

    /**
     * Invoke view-specific phase listeners, plus an optional EL MethodExpression.
     * <p>
     * JSF1.2 adds the ability for PhaseListener objects to be added to a UIViewRoot instance, and for
     * "beforePhaseListener" and "afterPhaseListener" EL expressions to be defined on the viewroot. This method is
     * expected to be called at appropriate times, and will then execute the relevant listener callbacks.
     * <p>
     * Parameter "listener" may be null. If not null, then it is an EL expression pointing to a user method that will be
     * invoked.
     * <p>
     * Note that the global PhaseListeners are invoked via the Lifecycle implementation, not from this method here.
     * <p>
     * These PhaseListeners are processed with the same rules as the globally defined PhaseListeners, except
     * that any Exceptions, which may occur during the execution of the PhaseListeners, will only be logged
     * and not published to the ExceptionHandler.
     */
    private boolean notifyListeners(FacesContext context, PhaseId phaseId, MethodExpression listener,
                                    boolean beforePhase)
    {
        List<PhaseListener> phaseListeners = (List<PhaseListener>) getStateHelper().get(PropertyKeys.phaseListeners);
        if (listener != null || (phaseListeners != null && !phaseListeners.isEmpty()))
        {
            // how many listeners do we have? (the MethodExpression listener is counted in either way)
            // NOTE: beforePhaseSuccess[0] always refers to the MethodExpression listener
            int listenerCount = (phaseListeners != null ? phaseListeners.size() + 1 : 1);
            
            boolean[] beforePhaseSuccess;
            if (beforePhase)
            {
                beforePhaseSuccess = new boolean[listenerCount];
                listenerSuccessMap.put(phaseId, beforePhaseSuccess);
            }
            else {
                // afterPhase - get beforePhaseSuccess from the Map
                beforePhaseSuccess = listenerSuccessMap.get(phaseId);
                if (beforePhaseSuccess == null)
                {
                    // no Map available - assume that everything went well
                    beforePhaseSuccess = new boolean[listenerCount];
                    Arrays.fill(beforePhaseSuccess, true);
                }
            }
            
            PhaseEvent event = createEvent(context, phaseId);

            // only invoke the listener if we are in beforePhase
            // or if the related before PhaseListener finished without an Exception
            if (listener != null && (beforePhase || beforePhaseSuccess[0]))
            {
                try
                {
                    listener.invoke(context.getELContext(), new Object[] { event });
                    beforePhaseSuccess[0] = true;
                }
                catch (Throwable t) 
                {
                    beforePhaseSuccess[0] = false; // redundant - for clarity
                    logger.log(Level.SEVERE, "An Exception occured while processing " +
                                             listener.getExpressionString() + 
                                             " in Phase " + phaseId, t);
                    if (beforePhase)
                    {
                        return context.getResponseComplete() || (context.getRenderResponse() && !PhaseId.RENDER_RESPONSE.equals(phaseId));
                    }
                }
            }
            else if (beforePhase)
            {
                // there is no beforePhase MethodExpression listener
                beforePhaseSuccess[0] = true;
            }

            if (phaseListeners != null && !phaseListeners.isEmpty())
            {
                if (beforePhase)
                {
                    // process listeners in ascending order
                    for (int i = 0; i < beforePhaseSuccess.length - 1; i++)
                    {
                        PhaseListener phaseListener;
                        try 
                        {
                            phaseListener = phaseListeners.get(i);
                        }
                        catch (IndexOutOfBoundsException e)
                        {
                            // happens when a PhaseListener removes another PhaseListener 
                            // from UIViewRoot in its beforePhase method
                            throw new IllegalStateException("A PhaseListener must not remove " +
                                    "PhaseListeners from UIViewRoot.");
                        }
                        PhaseId listenerPhaseId = phaseListener.getPhaseId();
                        if (phaseId.equals(listenerPhaseId) || PhaseId.ANY_PHASE.equals(listenerPhaseId))
                        {
                            try
                            {
                                phaseListener.beforePhase(event);
                                beforePhaseSuccess[i + 1] = true;
                            }
                            catch (Throwable t) 
                            {
                                beforePhaseSuccess[i + 1] = false; // redundant - for clarity
                                logger.log(Level.SEVERE, "An Exception occured while processing the " +
                                                         "beforePhase method of PhaseListener " + phaseListener +
                                                         " in Phase " + phaseId, t);
                                return context.getResponseComplete() || (context.getRenderResponse() && !PhaseId.RENDER_RESPONSE.equals(phaseId));
                            }
                        }
                    }
                }
                else
                {
                    // afterPhase
                    // process listeners in descending order
                    for (int i = beforePhaseSuccess.length - 1; i > 0; i--)
                    {
                        PhaseListener phaseListener;
                        try 
                        {
                            phaseListener = phaseListeners.get(i - 1);
                        }
                        catch (IndexOutOfBoundsException e)
                        {
                            // happens when a PhaseListener removes another PhaseListener 
                            // from UIViewRoot in its beforePhase or afterPhase method
                            throw new IllegalStateException("A PhaseListener must not remove " +
                                    "PhaseListeners from UIViewRoot.");
                        }
                        PhaseId listenerPhaseId = phaseListener.getPhaseId();
                        if ((phaseId.equals(listenerPhaseId) || PhaseId.ANY_PHASE.equals(listenerPhaseId))
                                && beforePhaseSuccess[i])
                        {
                            try
                            {
                                phaseListener.afterPhase(event);
                            }
                            catch (Throwable t) 
                            {
                                logger.log(Level.SEVERE, "An Exception occured while processing the " +
                                                         "afterPhase method of PhaseListener " + phaseListener +
                                                         " in Phase " + phaseId, t);
                            }
                        }
                    }
                }
            }
        }

        if (beforePhase)
        {
            return context.getResponseComplete() || (context.getRenderResponse() && !PhaseId.RENDER_RESPONSE.equals(phaseId));
        }
        else
        {
            return context.getResponseComplete() || context.getRenderResponse();
        }
    }

    private PhaseEvent createEvent(FacesContext context, PhaseId phaseId)
    {
        if (_lifecycle == null)
        {
            LifecycleFactory factory = (LifecycleFactory)FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
            String id = context.getExternalContext().getInitParameter(FacesServlet.LIFECYCLE_ID_ATTR);
            if (id == null)
            {
                id = LifecycleFactory.DEFAULT_LIFECYCLE;
            }
            _lifecycle = factory.getLifecycle(id);
        }
        return new PhaseEvent(context, phaseId, _lifecycle);
    }

    /**
     * Broadcast all events in the specified collection, stopping the at any time an AbortProcessingException
     * is thrown.
     *
     * @param context the current JSF context
     * @param events the events to broadcast
     *
     * @return <code>true</code> if the broadcast was completed without abortion, <code>false</code> otherwise
     */
    private boolean _broadcastAll(FacesContext context, Collection<? extends FacesEvent> events)
    {
        assert events != null;

        for (FacesEvent event : events)
        {
            UIComponent source = event.getComponent();
            UIComponent compositeParent = UIComponent.getCompositeComponentParent(source);
            if (compositeParent != null)
            {
                pushComponentToEL(context, compositeParent);
            }
            // Push the source as the current component
            pushComponentToEL(context, source);

            try
            {
                // Actual event broadcasting
                source.broadcast(event);
            }
            catch (AbortProcessingException e)
            {
                // publish the Exception to be handled by the ExceptionHandler
                ExceptionQueuedEventContext exceptionContext 
                        = new ExceptionQueuedEventContext(context, e, source, context.getCurrentPhaseId());
                context.getApplication().publishEvent(context, ExceptionQueuedEvent.class, exceptionContext);
                
                // Abortion
                return false;
            }
            finally
            {
                // Restore the current component
                popComponentFromEL(context);
                if (compositeParent != null)
                {
                    popComponentFromEL(context);
                }
            }
        }

        return true;
    }

    private void clearEvents()
    {
        _events = null;
    }

    private void checkNull(Object value, String valueLabel)
    {
        if (value == null)
        {
            throw new NullPointerException(valueLabel + " is null");
        }
    }

    private Locale stringToLocale(String localeStr)
    {
        // locale expr: \[a-z]{2}((-|_)[A-Z]{2})?

        if (localeStr.contains("_") || localeStr.contains("-"))
        {
            if (localeStr.length() == 2)
            {
                // localeStr is the lang
                return new Locale(localeStr);
            }
        }
        else
        {
            if (localeStr.length() == 5)
            {
                String lang = localeStr.substring(0, 1);
                String country = localeStr.substring(3, 4);
                return new Locale(lang, country);
            }
        }

        return Locale.getDefault();
    }

    public void setRenderKitId(String renderKitId)
    {
        getStateHelper().put(PropertyKeys.renderKitId, renderKitId );
    }

    /**
     * DO NOT USE.
     * <p>
     * This inherited property is disabled. Although this class extends a base-class that defines a read/write rendered
     * property, this particular subclass does not support setting it. Yes, this is broken OO design: direct all
     * complaints to the JSF spec group.
     */
    @Override
    @JSFProperty(tagExcluded = true)
    public void setRendered(boolean state)
    {
        // Call parent method due to TCK problems
        super.setRendered(state);
        // throw new UnsupportedOperationException();
    }

    /**
     * DO NOT USE.
     * <p>
     * Although this class extends a base-class that defines a read/write id property, it makes no sense for this
     * particular subclass to support it. The tag library does not export this property for use, but there is no way to
     * "undeclare" a java method. Yes, this is broken OO design: direct all complaints to the JSF spec group.
     * <p>
     * This property should be disabled (ie throw an exception if invoked). However there are currently several places
     * that call this method (eg during restoreState) so it just does the normal thing for the moment. TODO: fix callers
     * then make this throw an exception.
     *
     * @JSFProperty tagExcluded="true"
     */
    @Override
    public void setId(String id)
    {
        // throw new UnsupportedOperationException();

        // Leave enabled for now. Things like the TreeStructureManager call this,
        // even though they probably should not.
        super.setId(id);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setInView(boolean isInView)
    {
        // no-op view root is always in view
    }

    public void removeComponentResource(FacesContext context, UIComponent componentResource)
    {
        removeComponentResource(context, componentResource, null);
    }

    public void removeComponentResource(FacesContext context, UIComponent componentResource, String target)
    {
        // If the target argument is null
        if (target == null)
        {
            // Look for a target attribute on the component
            target = (String)componentResource.getAttributes().get("target");

            // If there is no target attribute
            if (target == null)
            {
                // Set target to be the default value head
                target = "head";
            }
        }


        // Call getComponentResources to obtain the child list for the given target.
        //List<UIComponent> componentResources = getComponentResources(context, target);
        UIComponent facet = getFacet(target);
        if (facet != null)
        {
            //Only if the facet is found it is possible to remove the resource,
            //otherwise nothing should happen (call to getComponentResource trigger
            //creation of facet)
            List<UIComponent> componentResources = facet.getChildren();
            // Remove the component resource from the child list
            componentResources.remove(componentResource);
        }
    }

    public void setViewId(String viewId)
    {
        // It really doesn't make much sense to allow null here.
        // However the TCK does not check for it, and sun's implementation
        // allows it so here we allow it too.
        getStateHelper().put(PropertyKeys.viewId, viewId );
    }

    /**
     * Removes a The phaseListeners attached to ViewRoot.
     */
    public void removePhaseListener(PhaseListener phaseListener)
    {
        if (phaseListener == null)
            return;

        getStateHelper().remove(PropertyKeys.phaseListeners, phaseListener);
    }

    /**
     * Sets
     *
     * @param beforePhaseListener
     *            the new beforePhaseListener value
     */
    public void setBeforePhaseListener(MethodExpression beforePhaseListener)
    {
        getStateHelper().put(PropertyKeys.beforePhaseListener, beforePhaseListener);
    }

    /**
     * Sets
     *
     * @param afterPhaseListener
     *            the new afterPhaseListener value
     */
    public void setAfterPhaseListener(MethodExpression afterPhaseListener)
    {
        getStateHelper().put(PropertyKeys.afterPhaseListener, afterPhaseListener);
    }
    
    enum PropertyKeys
    {
         afterPhaseListener
        , beforePhaseListener
        , phaseListeners
        , locale
        , renderKitId
        , viewId
        , uniqueIdCounter
    }
    
    @Override
    public Object saveState(FacesContext facesContext)
    {
        if (initialStateMarked())
        {
            Object parentSaved = super.saveState(facesContext);
            if (parentSaved == null && _viewScope == null)
            {
                //No values
                return null;
            }
            
            Object[] values = new Object[2];
            values[0] = parentSaved;
            values[1] = saveAttachedState(facesContext,_viewScope);
            return values;
        }
        else
        {
            Object[] values = new Object[2];
            values[0] = super.saveState(facesContext);
            values[1] = saveAttachedState(facesContext,_viewScope);
            return values;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void restoreState(FacesContext facesContext, Object state)
    {
        if (state == null)
        {
            return;
        }
        
        Object[] values = (Object[])state;
        super.restoreState(facesContext,values[0]);
        _viewScope = (Map<String, Object>) restoreAttachedState(facesContext, values[1]);
    }
    
    public List<SystemEventListener> getViewListenersForEventClass(Class<? extends SystemEvent> systemEvent)
    {
        checkNull (systemEvent, "systemEvent");
        
        return _systemEventListeners.get (systemEvent);
    }
    
    public void subscribeToViewEvent(Class<? extends SystemEvent> systemEvent,
            SystemEventListener listener)
    {
        List<SystemEventListener> listeners;
        
        checkNull (systemEvent, "systemEvent");
        checkNull (listener, "listener");
        
        listeners = _systemEventListeners.get (systemEvent);
        
        if (listeners == null) {
            listeners = new ArrayList<SystemEventListener>();
            
            _systemEventListeners.put (systemEvent, listeners);
        }
        
        listeners.add (listener);
    }
    
    public void unsubscribeFromViewEvent(Class<? extends SystemEvent> systemEvent,
            SystemEventListener listener)
    {
        List<SystemEventListener> listeners;
        
        checkNull (systemEvent, "systemEvent");
        checkNull (listener, "listener");
        
        listeners = _systemEventListeners.get (systemEvent);
        
        if (listeners != null) {
            listeners.remove (listener);
        }
    }

    /**
     * Process the specified phase by calling PhaseListener.beforePhase for every phase listeners defined on this
     * view root, then calling the process method of the processor, broadcasting relevant events and finally
     * notifying the afterPhase method of every phase listeners registered on this view root.
     *
     * @param context
     * @param phaseId
     * @param processor
     * @param broadcast
     *
     * @return
     */
    private boolean _process(FacesContext context, PhaseId phaseId, PhaseProcessor processor)
    {
        RuntimeException processingException = null;
        try
        {
            if (!notifyListeners(context, phaseId, getBeforePhaseListener(), true))
            {
                try
                {
                    if (processor != null)
                    {
                        processor.process(context, this);
                    }
        
                    broadcastEvents(context, phaseId);
                }
                catch (RuntimeException re)
                {
                    // catch any Exception that occures while processing the phase
                    // to ensure invocation of the afterPhase methods
                    processingException = re;
                }
            }
        }
        finally
        {
            if (context.getRenderResponse() || context.getResponseComplete())
            {
                clearEvents();
            }            
        }

        boolean retVal = notifyListeners(context, phaseId, getAfterPhaseListener(), false);
        if (processingException == null) 
        {
            return retVal;   
        }
        else
        {
            throw processingException;
        }
    }

    private void _processDecodesDefault(FacesContext context)
    {
        super.processDecodes(context);
    }

    private void _processUpdatesDefault(FacesContext context)
    {
        super.processUpdates(context);
    }

    private void _processValidatorsDefault(FacesContext context)
    {
        super.processValidators(context);
    }

    private static interface PhaseProcessor
    {
        public void process(FacesContext context, UIViewRoot root);
    }

    private static class ApplyRequestValuesPhaseProcessor implements PhaseProcessor
    {
        public void process(FacesContext context, UIViewRoot root)
        {
            PartialViewContext pvc = context.getPartialViewContext();
            // Perform partial processing by calling PartialViewContext.processPartial(javax.faces.event.PhaseId) with PhaseId.UPDATE_MODEL_VALUES if:
            //   * PartialViewContext.isPartialRequest() returns true and we don't have a request to process all components in the view (PartialViewContext.isExecuteAll() returns false)
            //section 13.4.2 from the  JSF2  spec also see https://issues.apache.org/jira/browse/MYFACES-2119
            if (pvc.isPartialRequest() && !pvc.isExecuteAll())
            {
                pvc.processPartial(PhaseId.APPLY_REQUEST_VALUES);
            }
            // Perform full processing by calling UIComponentBase.processUpdates(javax.faces.context.FacesContext) if one of the following conditions are met:
            // *   PartialViewContext.isPartialRequest() returns true and we have a request to process all components in the view (PartialViewContext.isExecuteAll() returns true)
            // *   PartialViewContext.isPartialRequest() returns false
            else
            {
                root._processDecodesDefault(context);
            }
        }
    }

    private static class ProcessValidatorPhaseProcessor implements PhaseProcessor
    {
        public void process(FacesContext context, UIViewRoot root)
        {
            PartialViewContext pvc = context.getPartialViewContext();
            // Perform partial processing by calling PartialViewContext.processPartial(javax.faces.event.PhaseId) with PhaseId.UPDATE_MODEL_VALUES if:
            // PartialViewContext.isPartialRequest() returns true and we don't have a request to process all components in the view (PartialViewContext.isExecuteAll() returns false)
            //section 13.4.2 from the  JSF2  spec also see https://issues.apache.org/jira/browse/MYFACES-2119
            if (pvc.isPartialRequest() && !pvc.isExecuteAll())
            {
                pvc.processPartial(PhaseId.PROCESS_VALIDATIONS);
            }
            // Perform full processing by calling UIComponentBase.processUpdates(javax.faces.context.FacesContext) if one of the following conditions are met:
            // *   PartialViewContext.isPartialRequest() returns true and we have a request to process all components in the view (PartialViewContext.isExecuteAll() returns true)
            // *   PartialViewContext.isPartialRequest() returns false
            else
            {
                root._processValidatorsDefault(context);
            }
        }
    }

    private static class UpdateModelPhaseProcessor implements PhaseProcessor
    {
        public void process(FacesContext context, UIViewRoot root)
        {
            PartialViewContext pvc = context.getPartialViewContext();
            // Perform partial processing by calling PartialViewContext.processPartial(javax.faces.event.PhaseId) with PhaseId.UPDATE_MODEL_VALUES if:
            //   * PartialViewContext.isPartialRequest() returns true and we don't have a request to process all components in the view (PartialViewContext.isExecuteAll() returns false)
            //section 13.4.2 from the JSF2 spec also see https://issues.apache.org/jira/browse/MYFACES-2119
            if (pvc.isPartialRequest() && !pvc.isExecuteAll())
            {
                pvc.processPartial(PhaseId.UPDATE_MODEL_VALUES);
            }
            // Perform full processing by calling UIComponentBase.processUpdates(javax.faces.context.FacesContext) if one of the following conditions are met:
            // *   PartialViewContext.isPartialRequest() returns true and we have a request to process all components in the view (PartialViewContext.isExecuteAll() returns true)
            // *   PartialViewContext.isPartialRequest() returns false
            else
            {
                root._processUpdatesDefault(context);
            }
        }
    }

    private static class RestoreStateCallback implements VisitCallback
    {
        private PostRestoreStateEvent event;

        public VisitResult visit(VisitContext context, UIComponent target)
        {
            if (event == null)
            {
                event = new PostRestoreStateEvent(target);
            }
            else
            {
                event.setComponent(target);
            }

            // call the processEvent method of the current component.
            // The argument event must be an instance of AfterRestoreStateEvent whose component
            // property is the current component in the traversal.
            target.processEvent(event);
            
            return VisitResult.ACCEPT;
        }
    }

    // we cannot make this class a inner class, because the 
    // enclosing class (UIViewRoot) would also have to be serialized.
    private static class ViewScope extends HashMap<String, Object>
    {
        
        private static final long serialVersionUID = -1088893802269478164L;
        
        @Override
        public void clear()
        {
            /*
             * The returned Map must be implemented such that calling clear() on the Map causes
             * Application.publishEvent(java.lang.Class, java.lang.Object) to be called, passing
             * ViewMapDestroyedEvent.class as the first argument and this UIViewRoot instance as the second argument.
             */
            FacesContext facesContext = FacesContext.getCurrentInstance();
            facesContext.getApplication().publishEvent(facesContext, 
                    PreDestroyViewMapEvent.class, facesContext.getViewRoot());
            
            super.clear();
        }
        
    }
}
