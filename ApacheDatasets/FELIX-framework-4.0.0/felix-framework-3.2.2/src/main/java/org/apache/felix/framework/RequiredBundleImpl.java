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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.felix.framework.resolver.Module;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.RequiredBundle;

class RequiredBundleImpl implements RequiredBundle
{
    private final Felix m_felix;
    private final BundleImpl m_bundle;
    private volatile String m_toString = null;
    private volatile String m_versionString = null;

    public RequiredBundleImpl(Felix felix, BundleImpl bundle)
    {
        m_felix = felix;
        m_bundle = bundle;
    }

    public String getSymbolicName()
    {
        return m_bundle.getSymbolicName();
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    public Bundle[] getRequiringBundles()
    {
        // Spec says to return null for stale bundles.
        if (m_bundle.isStale())
        {
            return null;
        }

        // We need to find all modules that require any of the modules
        // associated with this bundle and save the associated bundle
        // of the dependent modules.
        Set bundleSet = new HashSet();
        // Loop through all of this bundle's modules.
        List<Module> modules = m_bundle.getModules();
        for (int modIdx = 0; (modules != null) && (modIdx < modules.size()); modIdx++)
        {
            // For each of this bundle's modules, loop through all of the
            // modules that require it and add them to the module list.
            List<Module> dependents = ((ModuleImpl) modules.get(modIdx)).getDependentRequirers();
            for (int depIdx = 0; (dependents != null) && (depIdx < dependents.size()); depIdx++)
            {
                if (dependents.get(depIdx).getBundle() != null)
                {
                    bundleSet.add(dependents.get(depIdx).getBundle());
                }
            }
        }
        // Convert to an array.
        return (Bundle[]) bundleSet.toArray(new Bundle[bundleSet.size()]);
    }

    public Version getVersion()
    {
        return m_bundle.getCurrentModule().getVersion();
    }

    public boolean isRemovalPending()
    {
        return m_bundle.isRemovalPending();
    }

    public String toString()
    {
        if (m_toString == null)
        {
            m_toString = m_bundle.getSymbolicName()
                + "; version=" + m_bundle.getCurrentModule().getVersion();
        }
        return m_toString;
    }
}