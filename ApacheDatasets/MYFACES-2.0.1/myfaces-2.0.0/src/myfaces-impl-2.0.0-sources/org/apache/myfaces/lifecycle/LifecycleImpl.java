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
package org.apache.myfaces.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;

import org.apache.myfaces.config.FacesConfigurator;
import org.apache.myfaces.renderkit.ErrorPageWriter;
import org.apache.myfaces.shared_impl.util.ClassUtils;
import org.apache.myfaces.shared_impl.webapp.webxml.WebXml;
import org.apache.myfaces.util.DebugUtils;

/**
 * Implements the lifecycle as described in Spec. 1.0 PFD Chapter 2
 * 
 * @author Manfred Geiler
 * @author Nikolay Petrov
 */
public class LifecycleImpl extends Lifecycle
{
    //private static final Log log = LogFactory.getLog(LifecycleImpl.class);
    private static final Logger log = Logger.getLogger(LifecycleImpl.class.getName());
    
    private static final String FIRST_REQUEST_EXECUTED_PARAM = "org.apache.myfaces.lifecycle.first.request.processed";
    
    private PhaseExecutor[] lifecycleExecutors;
    private PhaseExecutor renderExecutor;

    private final List<PhaseListener> _phaseListenerList = new ArrayList<PhaseListener>();

    private boolean _firstRequestExecuted = false;
    /**
     * Lazy cache for returning _phaseListenerList as an Array.
     */
    private PhaseListener[] _phaseListenerArray = null;

    public LifecycleImpl()
    {
        // hide from public access
        lifecycleExecutors = new PhaseExecutor[] { new RestoreViewExecutor(), new ApplyRequestValuesExecutor(),
                new ProcessValidationsExecutor(), new UpdateModelValuesExecutor(), new InvokeApplicationExecutor() };

        renderExecutor = new RenderResponseExecutor();
    }

    @Override
    public void execute(FacesContext facesContext) throws FacesException
    {
        try
        {
            // refresh all configuration information if according web-xml parameter is set.
            // TODO: Performance wise, shouldn't the lifecycle be configured with a local boolean attribute
            // specifying if the system should look for file modification because this code seems like
            // a developement facility, but at the cost of scalability. I think a context param like
            // Trinidad is a better idea -= Simon Lessard =-
            WebXml.update(facesContext.getExternalContext());
    
            new FacesConfigurator(facesContext.getExternalContext()).update();

            requestStarted(facesContext);
            
            PhaseListenerManager phaseListenerMgr = new PhaseListenerManager(this, facesContext, getPhaseListeners());
            for (PhaseExecutor executor : lifecycleExecutors)
            {
                if (executePhase(facesContext, executor, phaseListenerMgr))
                {
                    return;
                }
            }
        }
        catch (Throwable ex)
        {
            // handle the Throwable accordingly. Maybe generate an error page.
            ErrorPageWriter.handleThrowable(facesContext, ex);
        }
    }

    private boolean executePhase(FacesContext context, PhaseExecutor executor, PhaseListenerManager phaseListenerMgr)
        throws FacesException
    {
        boolean skipFurtherProcessing = false;

        if (log.isLoggable(Level.FINEST))
        {
            log.finest("entering " + executor.getPhase() + " in " + LifecycleImpl.class.getName());
        }

        PhaseId currentPhaseId = executor.getPhase();
        Flash flash = context.getExternalContext().getFlash();

        try
        {
            /* 
             * Specification, section 2.2
             * The default request lifecycle processing implementation must ensure that the currentPhaseId property 
             * of the FacesContext instance for this request is set with the proper PhaseId constant for the current 
             * phase as the first instruction at the beginning of each phase
             */
            context.setCurrentPhaseId(currentPhaseId);
            
            flash.doPrePhaseActions(context);

            phaseListenerMgr.informPhaseListenersBefore(currentPhaseId);

            if (isResponseComplete(context, currentPhaseId, true))
            {
                // have to return right away
                return true;
            }
            if (shouldRenderResponse(context, currentPhaseId, true))
            {
                skipFurtherProcessing = true;
            }

            if (executor.execute(context))
            {
                return true;
            }
        }
        
        catch (Throwable e) {
            // JSF 2.0: publish the executor's exception (if any).
            
            publishException (e, currentPhaseId, context);
        }
        
        finally
        {
            phaseListenerMgr.informPhaseListenersAfter(currentPhaseId);
            
            flash.doPostPhaseActions(context);
            
        }
        
        context.getExceptionHandler().handle();
        
        if (isResponseComplete(context, currentPhaseId, false) || shouldRenderResponse(context, currentPhaseId, false))
        {
            // since this phase is completed we don't need to return right away even if the response is completed
            skipFurtherProcessing = true;
        }

        if (!skipFurtherProcessing && log.isLoggable(Level.FINEST))
        {
            log.finest("exiting " + executor.getPhase() + " in " + LifecycleImpl.class.getName());
        }

        return skipFurtherProcessing;
    }

    @Override
    public void render(FacesContext facesContext) throws FacesException
    {
        try
        {
            // if the response is complete we should not be invoking the phase listeners
            if (isResponseComplete(facesContext, renderExecutor.getPhase(), true))
            {
                return;
            }
            if (log.isLoggable(Level.FINEST))
                log.finest("entering " + renderExecutor.getPhase() + " in " + LifecycleImpl.class.getName());
    
            PhaseListenerManager phaseListenerMgr = new PhaseListenerManager(this, facesContext, getPhaseListeners());
            Flash flash = facesContext.getExternalContext().getFlash();
            
            try
            {
                facesContext.setCurrentPhaseId(renderExecutor.getPhase());
                
                flash.doPrePhaseActions(facesContext);
                boolean renderResponse = phaseListenerMgr.informPhaseListenersBefore(renderExecutor.getPhase());
                // also possible that one of the listeners completed the response
                if (isResponseComplete(facesContext, renderExecutor.getPhase(), true))
                {
                    return;
                }
                if(renderResponse || facesContext.getExceptionHandler().getClass().equals(ClassUtils.classForName("javax.faces.webapp.PreJsf2ExceptionHandlerFactory$PreJsf2ExceptionHandlerImpl")))
                    renderExecutor.execute(facesContext);
            }
            
            catch (Throwable e) {
                // JSF 2.0: publish the executor's exception (if any).
                
                publishException (e, renderExecutor.getPhase(), facesContext);
            }
            
            finally
            {
                phaseListenerMgr.informPhaseListenersAfter(renderExecutor.getPhase());
                flash.doPostPhaseActions(facesContext);
            }
            
            facesContext.getExceptionHandler().handle();
            
            if (log.isLoggable(Level.FINEST))
            {
                // Note: DebugUtils Logger must also be in trace level
                DebugUtils.traceView("View after rendering");
            }
    
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("exiting " + renderExecutor.getPhase() + " in " + LifecycleImpl.class.getName());
            }
        }
        catch (Throwable ex)
        {
            // handle the Throwable accordingly. Maybe generate an error page.
            ErrorPageWriter.handleThrowable(facesContext, ex);
        }
    }

    private boolean isResponseComplete(FacesContext facesContext, PhaseId phase, boolean before)
    {
        boolean flag = false;
        if (facesContext.getResponseComplete())
        {
            if (log.isLoggable(Level.FINE))
            {
                log.fine("exiting from lifecycle.execute in " + phase
                        + " because getResponseComplete is true from one of the " + (before ? "before" : "after")
                        + " listeners");
            }
            flag = true;
        }
        return flag;
    }

    private boolean shouldRenderResponse(FacesContext facesContext, PhaseId phase, boolean before)
    {
        boolean flag = false;
        if (facesContext.getRenderResponse())
        {
            if (log.isLoggable(Level.FINE))
            {
                log.fine("exiting from lifecycle.execute in " + phase
                        + " because getRenderResponse is true from one of the " + (before ? "before" : "after")
                        + " listeners");
            }
            flag = true;
        }
        return flag;
    }

    @Override
    public void addPhaseListener(PhaseListener phaseListener)
    {
        if (phaseListener == null)
        {
            throw new NullPointerException("PhaseListener must not be null.");
        }
        synchronized (_phaseListenerList)
        {
            _phaseListenerList.add(phaseListener);
            _phaseListenerArray = null; // reset lazy cache array
        }
    }

    @Override
    public void removePhaseListener(PhaseListener phaseListener)
    {
        if (phaseListener == null)
        {
            throw new NullPointerException("PhaseListener must not be null.");
        }
        synchronized (_phaseListenerList)
        {
            _phaseListenerList.remove(phaseListener);
            _phaseListenerArray = null; // reset lazy cache array
        }
    }

    @Override
    public PhaseListener[] getPhaseListeners()
    {
        synchronized (_phaseListenerList)
        {
            // (re)build lazy cache array if necessary
            if (_phaseListenerArray == null)
            {
                _phaseListenerArray = _phaseListenerList.toArray(new PhaseListener[_phaseListenerList.size()]);
            }
            return _phaseListenerArray;
        }
    }
    
    private void publishException (Throwable e, PhaseId phaseId, FacesContext facesContext)
    {
        ExceptionQueuedEventContext context = new ExceptionQueuedEventContext (facesContext, e, null, phaseId);
        
        facesContext.getApplication().publishEvent (facesContext, ExceptionQueuedEvent.class, context);
    }
    
    /*
     * this method places an attribiuet on the application map to indicate that the first request has been processed. This
     * attribute is used by several methods in ApplicationImpl to determine whether or not to throw an illegalStateException
     */
    private void requestStarted(FacesContext facesContext)
    {
        if(!_firstRequestExecuted)
        {
            _firstRequestExecuted = true;

            facesContext.getExternalContext().getApplicationMap().put(FIRST_REQUEST_EXECUTED_PARAM, Boolean.TRUE);
        }        
    }

}
