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

import java.util.*;
import org.apache.felix.framework.resolver.Module;
import org.apache.felix.framework.resolver.Wire;
import org.apache.felix.framework.util.VersionRange;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;


class PackageAdminImpl implements PackageAdmin, Runnable
{
    private static final Comparator COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2)
        {
            // Reverse arguments to sort in descending order.
            return ((ExportedPackage) o2).getVersion().compareTo(
                ((ExportedPackage) o1).getVersion());
        }
    };

    private Felix m_felix = null;
    private Bundle[][] m_reqBundles = null;
    private Bundle m_systemBundle = null;
    private Thread m_thread = null;

    public PackageAdminImpl(Felix felix)
    {
        m_felix = felix;
        m_systemBundle = m_felix.getBundle(0);

        // Start a thread to perform asynchronous package refreshes.
        m_thread = new Thread(this, "FelixPackageAdmin");
        m_thread.setDaemon(true);
        m_thread.start();
    }

    /**
     * Stops the FelixPackageAdmin thread on system shutdown. Shutting down the
     * thread explicitly is required in the embedded case, where Felix may be
     * stopped without the Java VM being stopped. In this case the
     * FelixPackageAdmin thread must be stopped explicitly.
     * <p>
     * This method is called by the
     * {@link PackageAdminActivator#stop(BundleContext)} method.
     */
    synchronized void stop()
    {
        if (m_thread != null)
        {
            // Null thread variable to signal to the thread that
            // we want it to exit.
            m_thread = null;
            
            // Wake up the thread, if it is currently in the wait() state
            // for more work.
            notifyAll();
        }
    }
    
    /**
     * Returns the bundle associated with this class if the class was
     * loaded from a bundle, otherwise returns null.
     * 
     * @param clazz the class for which to determine its associated bundle.
     * @return the bundle associated with the specified class, otherwise null.
    **/
    public Bundle getBundle(Class clazz)
    {
        return m_felix.getBundle(clazz);
    }

    /**
     * Returns all bundles that have a specified symbolic name and whose
     * version is in the specified version range. If no version range is
     * specified, then all bundles with the specified symbolic name are
     * returned. The array is sorted in descending version order.
     * 
     * @param symbolicName the target symbolic name.
     * @param versionRange the target version range.
     * @return an array of matching bundles sorted in descending version order.
    **/
    public Bundle[] getBundles(String symbolicName, String versionRange)
    {
        VersionRange vr = (versionRange == null) ? null : VersionRange.parse(versionRange);
        Bundle[] bundles = m_felix.getBundles();
        List list = new ArrayList();
        for (int i = 0; (bundles != null) && (i < bundles.length); i++)
        {
            String sym = bundles[i].getSymbolicName();
            if ((sym != null) && sym.equals(symbolicName))
            {
                Version v = ((BundleImpl) bundles[i]).getCurrentModule().getVersion();
                if ((vr == null) || vr.isInRange(v))
                {
                    list.add(bundles[i]);
                }
            }
        }
        if (list.size() == 0)
        {
            return null;
        }
        bundles = (Bundle[]) list.toArray(new Bundle[list.size()]);
        Arrays.sort(bundles,new Comparator() {
            public int compare(Object o1, Object o2)
            {
                Version v1 = ((BundleImpl) o1).getCurrentModule().getVersion();
                Version v2 = ((BundleImpl) o2).getCurrentModule().getVersion();
                // Compare in reverse order to get descending sort.
                return v2.compareTo(v1);
            }
        });
        return bundles;
    }

    public int getBundleType(Bundle bundle)
    {
        Map headerMap = ((BundleImpl) bundle).getCurrentModule().getHeaders();
        if (headerMap.containsKey(Constants.FRAGMENT_HOST))
        {
            return PackageAdmin.BUNDLE_TYPE_FRAGMENT;
        }
        return 0;
    }

    /**
     * Returns the exported package associated with the specified
     * package name. If there are more than one version of the package
     * being exported, then the highest version is returned.
     *
     * @param name the name of the exported package to find.
     * @return the exported package or null if no matching package was found.
    **/
    public ExportedPackage getExportedPackage(String name)
    {
        // Get all versions of the exported package.
        ExportedPackage[] pkgs = m_felix.getExportedPackages(name);
        // If there are no versions exported, then return null.
        if ((pkgs == null) || (pkgs.length == 0))
        {
            return null;
        }
        // Sort the exported versions.
        Arrays.sort(pkgs, COMPARATOR);
        // Return the highest version.
        return pkgs[0];
    }

    public ExportedPackage[] getExportedPackages(String name)
    {
        ExportedPackage[] pkgs = m_felix.getExportedPackages(name);
        return ((pkgs == null) || pkgs.length == 0) ? null : pkgs;
    }

    /**
     * Returns the packages exported by the specified bundle.
     *
     * @param bundle the bundle whose exported packages are to be returned.
     * @return an array of packages exported by the bundle or null if the
     *         bundle does not export any packages.
    **/
    public ExportedPackage[] getExportedPackages(Bundle bundle)
    {
        return m_felix.getExportedPackages(bundle);
    }

    public Bundle[] getFragments(Bundle bundle)
    {
        // If the bundle is not a fragment, then return its fragments.
        if ((getBundleType(bundle) & BUNDLE_TYPE_FRAGMENT) == 0)
        {
            List<Bundle> list = new ArrayList<Bundle>();
            // Iterate through modules
            List<Module> modules = ((BundleImpl) bundle).getModules();
            for (int modIdx = 0; modIdx < modules.size(); modIdx++)
            {
                // Get attached fragments.
                ModuleImpl module = (ModuleImpl) modules.get(modIdx);
                if (module.isResolved())
                {
                    List<Module> fragments = module.getFragments();
                    for (int i = 0; (fragments != null) && (i < fragments.size()); i++)
                    {
                        Bundle b = fragments.get(i).getBundle();
                        if (b != null)
                        {
                            list.add(b);
                        }
                    }
                }
            }
            // Convert list to an array.
            return (list.size() == 0)
                ? null
                : (Bundle[]) list.toArray(new Bundle[list.size()]);
        }
        return null;
    }

    public Bundle[] getHosts(Bundle bundle)
    {
        // If the bundle is a fragment, return its hosts
        if ((getBundleType(bundle) & BUNDLE_TYPE_FRAGMENT) != 0)
        {
            List<Bundle> list = new ArrayList<Bundle>();
            // Iterate through modules
            List<Module> modules = ((BundleImpl) bundle).getModules();
            for (int modIdx = 0; modIdx < modules.size(); modIdx++)
            {
                // Get hosts
                ModuleImpl module = (ModuleImpl) modules.get(modIdx);
                if (module.isResolved())
                {
                    List<Wire> hostWires = module.getWires();
                    for (int i = 0; (hostWires != null) && (i < hostWires.size()); i++)
                    {
                        Bundle b = hostWires.get(i).getExporter().getBundle();
                        if (b != null)
                        {
                            list.add(b);
                        }
                    }
                }
            }
            // Convert list to an array.
            return (list.isEmpty())
                ? null
                : (Bundle[]) list.toArray(new Bundle[list.size()]);
        }
        return null;
    }

    public RequiredBundle[] getRequiredBundles(String symbolicName)
    {
        List list = new ArrayList();
        Bundle[] bundles = m_felix.getBundles();
        for (int i = 0; i < bundles.length; i++)
        {
            BundleImpl impl = (BundleImpl) bundles[i];
            if ((symbolicName == null)
                || (symbolicName.equals(impl.getCurrentModule().getSymbolicName())))
            {
                list.add(new RequiredBundleImpl(m_felix, impl));
            }
        }
        return (list.size() == 0)
            ? null
            : (RequiredBundle[]) list.toArray(new RequiredBundle[list.size()]);
    }

    /**
     * The OSGi specification states that refreshing packages is
     * asynchronous; this method simply notifies the package admin
     * thread to do a refresh.
     * @param bundles array of bundles to refresh or <tt>null</tt> to refresh
     *                any bundles in need of refreshing.
    **/
    public void refreshPackages(Bundle[] bundles)
        throws SecurityException
    {
        Object sm = System.getSecurityManager();
        
        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(
                new AdminPermission(m_systemBundle, AdminPermission.RESOLVE));
        }
        synchronized (this)
        {
            // Save our request parameters and notify all.
            if (m_reqBundles == null)
            {
                m_reqBundles = new Bundle[][] { bundles };
            }
            else
            {
                Bundle[][] newReqBundles = new Bundle[m_reqBundles.length + 1][];
                System.arraycopy(m_reqBundles, 0,
                    newReqBundles, 0, m_reqBundles.length);
                newReqBundles[m_reqBundles.length] = bundles;
                m_reqBundles = newReqBundles;
            }
            notifyAll();
        }
    }

    public boolean resolveBundles(Bundle[] bundles)
    {
        Object sm = System.getSecurityManager();
        
        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(
                new AdminPermission(m_systemBundle, AdminPermission.RESOLVE));
        }
        
        return m_felix.resolveBundles(bundles);
    }

    /**
     * The OSGi specification states that package refreshes happen
     * asynchronously; this is the run() method for the package
     * refreshing thread.
    **/
    public void run()
    {
        // This thread loops forever, thus it should
        // be a daemon thread.
        while (true)
        {
            Bundle[] bundles = null;
            synchronized (this)
            {
                // Wait for a refresh request.
                while (m_reqBundles == null)
                {
                    // Terminate the thread if requested to do so (see stop()).
                    if (m_thread == null)
                    {
                        return;
                    }
                    
                    try
                    {
                        wait();
                    }
                    catch (InterruptedException ex)
                    {
                    }
                }

                // Get the bundles parameter for the current refresh request.
                bundles = m_reqBundles[0];
            }

            // Perform refresh.
            // NOTE: We don't catch any exceptions here, because
            // the invoked method shields us from exceptions by
            // catching Throwables when its invokes callbacks.
            m_felix.refreshPackages(bundles);

            // Remove the first request since it is now completed.
            synchronized (this)
            {
                if (m_reqBundles.length == 1)
                {
                    m_reqBundles = null;
                }
                else
                {
                    Bundle[][] newReqBundles = new Bundle[m_reqBundles.length - 1][];
                    System.arraycopy(m_reqBundles, 1,
                        newReqBundles, 0, m_reqBundles.length - 1);
                    m_reqBundles = newReqBundles;
                }
            }
        }
    }
}