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
package org.apache.myfaces.view.facelets.tag.composite;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URL;
import java.util.Map;

import javax.faces.FacesWrapper;
import javax.faces.application.Resource;
import javax.faces.context.FacesContext;

/**
 * The value inside composite component attribute map with the key
 * Resource.COMPONENT_RESOURCE_KEY should be a Serializable. This
 * wrapper add serialization to Resource instances, because 
 * ResourceImpl depends from the ResourceLoader used by it.
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 882415 $ $Date: 2009-11-19 23:59:57 -0500 (Thu, 19 Nov 2009) $
 */
public final class CompositeResouceWrapper extends Resource 
    implements FacesWrapper<Resource>, Externalizable
{
    /**
     * 
     */
    private static final long serialVersionUID = 8067930634887545843L;
    
    private transient Resource _delegate;
    
    public CompositeResouceWrapper()
    {
        super();
    }
    
    public CompositeResouceWrapper(Resource _delegate)
    {
        super();
        this._delegate = _delegate;
        setResourceName(_delegate.getResourceName());
        setLibraryName(_delegate.getLibraryName());
        setContentType(_delegate.getContentType());
    }


    public InputStream getInputStream() throws IOException
    {
        return getWrapped().getInputStream();
    }

    public String getRequestPath()
    {
        return getWrapped().getRequestPath();
    }

    public Map<String, String> getResponseHeaders()
    {
        return getWrapped().getResponseHeaders();
    }

    public URL getURL()
    {
        return getWrapped().getURL();
    }

    public boolean userAgentNeedsUpdate(FacesContext context)
    {
        return getWrapped().userAgentNeedsUpdate(context);
    }

    public Resource getWrapped()
    {
        if (_delegate == null)
        {
            _delegate = FacesContext.getCurrentInstance().getApplication().
                            getResourceHandler().createResource(
                                    getResourceName(), 
                                    getLibraryName(),
                                    getContentType());
        }
        return _delegate;
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException
    {
        setResourceName((String) in.readObject());
        setLibraryName((String) in.readObject());
        setContentType((String) in.readObject());
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(getResourceName());
        out.writeObject(getLibraryName());
        out.writeObject(getContentType());
    }
}
