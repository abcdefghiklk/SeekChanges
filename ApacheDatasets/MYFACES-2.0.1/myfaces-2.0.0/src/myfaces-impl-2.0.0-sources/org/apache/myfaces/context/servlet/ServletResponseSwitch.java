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
package org.apache.myfaces.context.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;

/**
 * Implementation of a switching response wrapper to turn 
 * output on and off according to the JSF spec 2.0.
 * <p/>
 * Fall-back implementation of HttpServletResponseSwitch
 * for non HttpServletResponses.
 *
 * @author Werner Punz (latest modification by $Author: lu4242 $)
 * @author Jakob Korherr
 * @version $Revision: 933725 $ $Date: 2010-04-13 13:07:04 -0500 (Tue, 13 Apr 2010) $
 */
public class ServletResponseSwitch extends ServletResponseWrapper implements ResponseSwitch
{

    private PrintWriter _switchableWriter;
    private SwitchableOutputStream _switchableOutputStream;

    public ServletResponseSwitch(ServletResponse response)
    {
        super(response);
    }

    /**
     * Enables or disables the Response's Writer and OutputStream.
     * @param enabled
     */
    public void setEnabled(boolean enabled)
    {
        FacesContext.getCurrentInstance().getAttributes().put(ResponseSwitch.RESPONSE_SWITCH_ENABLED, enabled);
    }
    
    public void setEnabled(FacesContext context, boolean enabled)
    {
        context.getAttributes().put(ResponseSwitch.RESPONSE_SWITCH_ENABLED, enabled);
    }

    /**
     * Are the Response's Writer and OutputStream currently enabled?
     * @return
     */
    public boolean isEnabled()
    {
        Boolean enabled = (Boolean) FacesContext.getCurrentInstance().getAttributes()
                              .get(ResponseSwitch.RESPONSE_SWITCH_ENABLED);
        return enabled == null ? true : enabled;
    }
    
    public boolean isEnabled(FacesContext facesContext)
    {
        Boolean enabled = (Boolean) facesContext.getAttributes()
            .get(ResponseSwitch.RESPONSE_SWITCH_ENABLED);
        return enabled == null ? true : enabled;
    }

    @Override
    public int getBufferSize()
    {
        if (isEnabled())
        {
            return super.getBufferSize();
        }
        return 0;
    }

    @Override
    public boolean isCommitted()
    {
        if (isEnabled())
        {
            return super.isCommitted();
        }
        return false;
    }

    @Override
    public void reset()
    {
        if (isEnabled())
        {
            super.reset();
        }
    }

    @Override
    public void resetBuffer()
    {
        if (isEnabled())
        {
            super.resetBuffer();
        }
    }
    
    @Override
    public void flushBuffer() throws IOException
    {
        if (isEnabled())
        {
            super.flushBuffer();
        }
    }

    @Override
    public void setResponse(ServletResponse response)
    {
        // only change the response if it is not the same object
        if (response != getResponse())
        {
            // our OutputStream and our Writer are not valid for the new response
            _switchableOutputStream = null;
            _switchableWriter = null;
            
            // set the new response
            super.setResponse(response);
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        if (_switchableOutputStream == null)
        {
            _switchableOutputStream = new SwitchableOutputStream(super.getOutputStream(), this);
        }
        return _switchableOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException
    {
        if (_switchableWriter == null)
        {
            _switchableWriter = new PrintWriter(new SwitchableWriter(super.getWriter(), this));
        }
        return _switchableWriter;
    }
    
}
