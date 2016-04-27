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
package org.apache.felix.framework;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.List;
import org.apache.felix.framework.resolver.Module;

import org.apache.felix.framework.util.Util;

class URLHandlersBundleURLConnection extends URLConnection
{
    private Felix m_framework;
    private Module m_targetModule;
    private int m_classPathIdx = -1;
    private int m_contentLength;
    private long m_contentTime;
    private String m_contentType;
    private InputStream m_is;

    public URLHandlersBundleURLConnection(URL url)
    {
        super(url);
    }

    public URLHandlersBundleURLConnection(URL url, Felix framework)
        throws IOException
    {
        super(url);

        // If this is an attempt to create a connection to the root of
        // the bundle, then throw an exception since this isn't possible.
        // We only allow "/" as a valid URL so it can be used as context
        // for creating other URLs.
        String path = url.getPath();
        if ((path == null) || (path.length() == 0) || path.equals("/"))
        {
            throw new IOException("Resource does not exist: " + url);
        }

        m_framework = framework;

        // If we don't have a framework instance, try to find
        // one from the call context.
        if (m_framework == null)
        {
            Object tmp = URLHandlers.getFrameworkFromContext();
            if (tmp instanceof Felix)
            {
                m_framework = (Felix) tmp;
            }
        }

        // If there is still no framework, then error.
        if (m_framework == null)
        {
            throw new IOException("Unable to find framework for URL: " + url);
        }
        // Verify that the resource pointed to by the URL exists.
        // The URL is constructed like this:
        //     bundle://<module-id>:<bundle-classpath-index>/<resource-path>
        // Where <module-id> = <bundle-id>.<revision>
        long bundleId = Util.getBundleIdFromModuleId(url.getHost());
        BundleImpl bundle = (BundleImpl) m_framework.getBundle(bundleId);
        if (bundle == null)
        {
            throw new IOException("No bundle associated with resource: " + url);
        }
        m_contentTime = bundle.getLastModified();

        // Get the bundle's modules to find the target module.
        List<Module> modules = bundle.getModules();
        if ((modules == null) || modules.isEmpty())
        {
            throw new IOException("Resource does not exist: " + url);
        }

        // Search for matching module name.
        for (Module m : modules)
        {
            if (m.getId().equals(url.getHost()))
            {
                m_targetModule = m;
                break;
            }
        }

        // If not found, assume the current module.
        if (m_targetModule == null)
        {
            m_targetModule = modules.get(modules.size() - 1);
        }

        // If the resource cannot be found at the current class path index,
        // then search them all in order to see if it can be found. This is
        // necessary since the user might create a resource URL from another
        // resource URL and not realize they have the wrong class path entry.
        // Of course, this approach won't work in cases where there are multiple
        // resources with the same path, since it will always find the first
        // one on the class path.
        m_classPathIdx = url.getPort();
        if (m_classPathIdx < 0)
        {
            m_classPathIdx = 0;
        }
        if (!m_targetModule.hasInputStream(m_classPathIdx, url.getPath()))
        {
            URL newurl = m_targetModule.getResourceByDelegation(url.getPath());
            if (newurl == null)
            {
                throw new IOException("Resource does not exist: " + url);
            }
            m_classPathIdx = newurl.getPort();
        }
    }

    public synchronized void connect() throws IOException
    {
        if (!connected)
        {
            if ((m_targetModule == null) || (m_classPathIdx < 0))
            {
                throw new IOException("Resource does not exist: " + url);
            }
            m_is = m_targetModule.getInputStream(m_classPathIdx, url.getPath());
            m_contentLength = (m_is == null) ? 0 : m_is.available();
            m_contentType = URLConnection.guessContentTypeFromName(url.getFile());
            connected = true;
        }
    }

    public InputStream getInputStream()
        throws IOException
    {
        connect();

        return m_is;
    }

    public int getContentLength()
    {
        try
        {
            connect();
        }
        catch(IOException ex)
        {
            return -1;
        }

        return m_contentLength;
    }

    public long getLastModified()
    {
        try
        {
            connect();
        }
        catch(IOException ex)
        {
            return 0;
        }

        if (m_contentTime != -1L)
        {
            return m_contentTime;
        }
        else
        {
            return 0L;
        }
    }

    public String getContentType()
    {
        try
        {
            connect();
        }
        catch (IOException ex)
        {
            return null;
        }

        return m_contentType;
    }

    public Permission getPermission()
    {
        // TODO: SECURITY - This should probably return a FilePermission
        // to access the bundle JAR file, but we don't have the
        // necessary information here to construct the absolute
        // path of the JAR file...so it would take some
        // re-arranging to get this to work.
        return null;
    }

    /**
     * Retrieve the entry as a URL using standard protocols such as file: and jar:
     *
     * @return the local URL
     */
    URL getLocalURL()
    {
        if ((m_targetModule == null) || (m_classPathIdx < 0))
        {
            return url;
        }
        return m_targetModule.getLocalURL(m_classPathIdx, url.getPath());
    }
}