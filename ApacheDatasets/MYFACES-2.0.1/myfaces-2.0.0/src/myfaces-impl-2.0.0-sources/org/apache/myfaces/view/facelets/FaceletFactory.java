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
package org.apache.myfaces.view.facelets;

import java.io.IOException;
import java.net.URL;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.view.facelets.FaceletException;

/**
 * FaceletFactory for producing Facelets relative to the context of the underlying implementation.
 * 
 * @author Jacob Hookom
 * @version $Id: FaceletFactory.java,v 1.4 2008/07/13 19:01:39 rlubke Exp $
 */
public abstract class FaceletFactory
{

    private static ThreadLocal<FaceletFactory> instance = new ThreadLocal<FaceletFactory>();

    /**
     * Return a Facelet instance as specified by the file at the passed URI.
     * 
     * @param uri
     * @return
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    public abstract Facelet getFacelet(String uri) throws IOException;
    
    /**
     * Create a Facelet from the passed URL. This method checks if the cached Facelet needs to be refreshed before
     * returning. If so, uses the passed URL to build a new instance;
     * 
     * @param url
     *            source url
     * @return Facelet instance
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    public abstract Facelet getFacelet(URL url) throws IOException, FaceletException, FacesException, ELException;
    
    /**
     * Return a Facelet instance as specified by the file at the passed URI. The returned facelet is used
     * to create view metadata in this form: 
     * <p>
     * UIViewRoot(in facet javax_faces_metadata(one or many UIViewParameter instances))
     * </p>
     * <p>
     * This method should be called from FaceletViewMetadata.createMetadataView(FacesContext context)  
     * </p>
     * 
     * @since 2.0
     * @param uri
     * @return
     * @throws IOException
     */
    public abstract Facelet getViewMetadataFacelet(String uri) throws IOException;
    
    /**
     * Create a Facelet used to create view metadata from the passed URL. This method checks if the 
     * cached Facelet needs to be refreshed before returning. If so, uses the passed URL to build a new instance;
     * 
     * @since 2.0
     * @param url source url
     * @return Facelet instance
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    public abstract Facelet getViewMetadataFacelet(URL url) throws IOException, FaceletException, FacesException, ELException;
    
    /**
     * Set the static instance
     * 
     * @param factory
     */
    public static final void setInstance(FaceletFactory factory)
    {
        instance.set(factory);
    }

    /**
     * Get the static instance
     * 
     * @return
     */
    public static final FaceletFactory getInstance()
    {
        return instance.get();
    }
}
