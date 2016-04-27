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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.felix.framework.Felix.StatefulResolver;
import org.apache.felix.framework.cache.JarContent;
import org.apache.felix.framework.capabilityset.Attribute;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Directive;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.resolver.Content;
import org.apache.felix.framework.resolver.HostedCapability;
import org.apache.felix.framework.resolver.HostedRequirement;
import org.apache.felix.framework.resolver.Module;
import org.apache.felix.framework.resolver.ResolveException;
import org.apache.felix.framework.resolver.ResourceNotFoundException;
import org.apache.felix.framework.resolver.Wire;
import org.apache.felix.framework.util.CompoundEnumeration;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.SecureAction;
import org.apache.felix.framework.util.SecurityManagerEx;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.framework.util.manifestparser.RequirementImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class ModuleImpl implements Module
{
    private final Logger m_logger;
    private final Map m_configMap;
    private final StatefulResolver m_resolver;
    private final String m_id;
    private final Content m_content;
    private final Map m_headerMap;
    private final URLStreamHandler m_streamHandler;

    private final String m_manifestVersion;
    private final boolean m_isExtension;
    private final String m_symbolicName;
    private final Version m_version;

    private final List<Capability> m_capabilities;
    private List<Capability> m_cachedCapabilities = null;
    private final List<Requirement> m_requirements;
    private List<Requirement> m_cachedRequirements = null;
    private final List<Requirement> m_dynamicRequirements;
    private List<Requirement> m_cachedDynamicRequirements = null;
    private final List<R4Library> m_nativeLibraries;
    private final int m_declaredActivationPolicy;
    private final List<String> m_activationIncludes;
    private final List<String> m_activationExcludes;

    private final Bundle m_bundle;

    private List<Module> m_fragments = null;
    private List<Wire> m_wires = null;
    private List<Module> m_dependentImporters = new ArrayList<Module>(0);
    private List<Module> m_dependentRequirers = new ArrayList<Module>(0);
    private volatile boolean m_isResolved = false;

    private Content[] m_contentPath;
    private Content[] m_fragmentContents = null;
    private ModuleClassLoader m_classLoader;
    private boolean m_isActivationTriggered = false;
    private ProtectionDomain m_protectionDomain = null;
    private final static SecureAction m_secureAction = new SecureAction();

    // Bundle-specific class loader for boot delegation.
    private final ClassLoader m_bootClassLoader;
    // Default class loader for boot delegation.
    private final static ClassLoader m_defBootClassLoader;

    // Statically define the default class loader for boot delegation.
    static
    {
        ClassLoader cl = null;
        try
        {
            Constructor ctor = m_secureAction.getDeclaredConstructor(
                SecureClassLoader.class, new Class[] { ClassLoader.class });
            m_secureAction.setAccesssible(ctor);
            cl = (ClassLoader) m_secureAction.invoke(ctor, new Object[] { null });
        }
        catch (Throwable ex)
        {
            // On Android we get an exception if we set the parent class loader
            // to null, so we will work around that case by setting the parent
            // class loader to the system class loader in getClassLoader() below.
            cl = null;
            System.err.println("Problem creating boot delegation class loader: " + ex);
        }
        m_defBootClassLoader = cl;
    }

    // Boot delegation packages.
    private final String[] m_bootPkgs;
    private final boolean[] m_bootPkgWildcards;

    // Boolean flag to enable/disable implicit boot delegation.
    private final boolean m_implicitBootDelegation;
    // Boolean flag to enable/disable local URLs.
    private final boolean m_useLocalURLs;

    // Re-usable security manager for accessing class context.
    private static SecurityManagerEx m_sm = new SecurityManagerEx();

    // Thread local to detect class loading cycles.
    private final ThreadLocal m_cycleCheck = new ThreadLocal();

    // Thread local to keep track of deferred activation.
    private static final ThreadLocal m_deferredActivation = new ThreadLocal();

    // Flag indicating whether we are on an old JVM or not.
    private volatile static boolean m_isPreJava5 = false;

    /**
     * This constructor is used by the extension manager, since it needs
     * a constructor that does not throw an exception.
     * @param logger
     * @param bundle
     * @param id
     * @param bootPkgs
     * @param bootPkgWildcards
     * @throws org.osgi.framework.BundleException
     */
    public ModuleImpl(
        Logger logger, Map configMap, Bundle bundle, String id,
        String[] bootPkgs, boolean[] bootPkgWildcards)
    {
        m_logger = logger;
        m_configMap = configMap;
        m_resolver = null;
        m_bundle = bundle;
        m_id = id;
        m_headerMap = null;
        m_content = null;
        m_streamHandler = null;
        m_bootPkgs = bootPkgs;
        m_bootPkgWildcards = bootPkgWildcards;
        m_manifestVersion = null;
        m_symbolicName = null;
        m_isExtension = false;
        m_version = null;
        m_capabilities = null;
        m_requirements = null;
        m_dynamicRequirements = null;
        m_nativeLibraries = null;
        m_declaredActivationPolicy = EAGER_ACTIVATION;
        m_activationExcludes = null;
        m_activationIncludes = null;
        m_implicitBootDelegation = false;
        m_useLocalURLs =
            (m_configMap.get(FelixConstants.USE_LOCALURLS_PROP) == null)
                ? false : true;
        m_bootClassLoader = m_defBootClassLoader;
    }

    ModuleImpl(
        Logger logger, Map configMap, StatefulResolver resolver,
        Bundle bundle, String id, Map headerMap, Content content,
        URLStreamHandler streamHandler, String[] bootPkgs,
        boolean[] bootPkgWildcards)
        throws BundleException
    {
        m_logger = logger;
        m_configMap = configMap;
        m_resolver = resolver;
        m_bundle = bundle;
        m_id = id;
        m_headerMap = headerMap;
        m_content = content;
        m_streamHandler = streamHandler;
        m_bootPkgs = bootPkgs;
        m_bootPkgWildcards = bootPkgWildcards;

        m_implicitBootDelegation =
            (m_configMap.get(FelixConstants.IMPLICIT_BOOT_DELEGATION_PROP) == null)
            || Boolean.valueOf(
                (String) m_configMap.get(
                    FelixConstants.IMPLICIT_BOOT_DELEGATION_PROP)).booleanValue();

        m_useLocalURLs =
            (m_configMap.get(FelixConstants.USE_LOCALURLS_PROP) == null)
                ? false : true;

        ClassLoader bootLoader = m_defBootClassLoader;
        Object map = m_configMap.get(FelixConstants.BOOT_CLASSLOADERS_PROP);
        if (map instanceof Map)
        {
            Object l = ((Map) map).get(bundle);
            if (l instanceof ClassLoader)
            {
                bootLoader = (ClassLoader) l;
            }
        }
        m_bootClassLoader = bootLoader;

        ManifestParser mp = new ManifestParser(m_logger, m_configMap, this, m_headerMap);

        // Record some of the parsed metadata. Note, if this is an extension
        // bundle it's exports are removed, since they will be added to the
        // system bundle directly later on.
        m_manifestVersion = mp.getManifestVersion();
        m_version = mp.getBundleVersion();
        m_capabilities = mp.isExtension() ? null : mp.getCapabilities();
        m_requirements = mp.getRequirements();
        m_dynamicRequirements = mp.getDynamicRequirements();
        m_nativeLibraries = mp.getLibraries();
        m_declaredActivationPolicy = mp.getActivationPolicy();
        m_activationExcludes = (mp.getActivationExcludeDirective() == null)
            ? null
            : ManifestParser.parseDelimitedString(mp.getActivationExcludeDirective(), ",");
        m_activationIncludes = (mp.getActivationIncludeDirective() == null)
            ? null
            : ManifestParser.parseDelimitedString(mp.getActivationIncludeDirective(), ",");
        m_symbolicName = mp.getSymbolicName();
        m_isExtension = mp.isExtension();
    }

    //
    // Metadata access methods.
    //

    public Map getHeaders()
    {
        return m_headerMap;
    }

    public boolean isExtension()
    {
        return m_isExtension;
    }

    public String getSymbolicName()
    {
        return m_symbolicName;
    }

    public String getManifestVersion()
    {
        return m_manifestVersion;
    }

    public Version getVersion()
    {
        return m_version;
    }

    public synchronized List<Capability> getCapabilities()
    {
        if (m_cachedCapabilities == null)
        {
            List capList = (m_capabilities == null)
                ? new ArrayList<Capability>()
                : new ArrayList<Capability>(m_capabilities);
            for (int fragIdx = 0;
                (m_fragments != null) && (fragIdx < m_fragments.size());
                fragIdx++)
            {
                List<Capability> caps = m_fragments.get(fragIdx).getCapabilities();
                for (int capIdx = 0;
                    (caps != null) && (capIdx < caps.size());
                    capIdx++)
                {
                    if (caps.get(capIdx).getNamespace().equals(Capability.PACKAGE_NAMESPACE))
                    {
                        capList.add(
                            new HostedCapability(this, caps.get(capIdx)));
                    }
                }
            }
            m_cachedCapabilities = Collections.unmodifiableList(capList);
        }
        return m_cachedCapabilities;
    }

    public synchronized List<Requirement> getRequirements()
    {
        if (m_cachedRequirements == null)
        {
            List<Requirement> reqList = (m_requirements == null)
                ? new ArrayList() : new ArrayList(m_requirements);
            for (int fragIdx = 0;
                (m_fragments != null) && (fragIdx < m_fragments.size());
                fragIdx++)
            {
                List<Requirement> reqs = m_fragments.get(fragIdx).getRequirements();
                for (int reqIdx = 0;
                    (reqs != null) && (reqIdx < reqs.size());
                    reqIdx++)
                {
                    if (reqs.get(reqIdx).getNamespace().equals(Capability.PACKAGE_NAMESPACE)
                        || reqs.get(reqIdx).getNamespace().equals(Capability.MODULE_NAMESPACE))
                    {
                        reqList.add(
                            new HostedRequirement(this, reqs.get(reqIdx)));
                    }
                }
            }
            m_cachedRequirements = Collections.unmodifiableList(reqList);
        }
        return m_cachedRequirements;
    }

    public synchronized List<Requirement> getDynamicRequirements()
    {
        if (m_cachedDynamicRequirements == null)
        {
            List<Requirement> reqList = (m_dynamicRequirements == null)
                ? new ArrayList() : new ArrayList(m_dynamicRequirements);
            for (int fragIdx = 0;
                (m_fragments != null) && (fragIdx < m_fragments.size());
                fragIdx++)
            {
                List<Requirement> reqs = m_fragments.get(fragIdx).getDynamicRequirements();
                for (int reqIdx = 0;
                    (reqs != null) && (reqIdx < reqs.size());
                    reqIdx++)
                {
                    if (reqs.get(reqIdx).getNamespace().equals(Capability.PACKAGE_NAMESPACE))
                    {
                        reqList.add(reqs.get(reqIdx));
                    }
                }
            }
            m_cachedDynamicRequirements = Collections.unmodifiableList(reqList);
        }
        return m_cachedDynamicRequirements;
    }

    public synchronized List<R4Library> getNativeLibraries()
    {
        List<R4Library> result = null;
        if (m_isResolved)
        {
            List<R4Library> nativeList = (m_nativeLibraries == null)
                ? new ArrayList() : new ArrayList(m_nativeLibraries);
            for (int fragIdx = 0;
                (m_fragments != null) && (fragIdx < m_fragments.size());
                fragIdx++)
            {
                List<R4Library> libs = m_fragments.get(fragIdx).getNativeLibraries();
                for (int reqIdx = 0;
                    (libs != null) && (reqIdx < libs.size());
                    reqIdx++)
                {
                    nativeList.add(libs.get(reqIdx));
                }
            }

            // We need to return null here if we don't have any libraries, since a
            // zero-length array is used to indicate that matching native libraries
            // could not be found when resolving the bundle.
            result = (nativeList.isEmpty())
                ? null
                : Collections.unmodifiableList(nativeList);
        }
        else
        {
            result = m_nativeLibraries;
        }

        return result;
    }

    public int getDeclaredActivationPolicy()
    {
        return m_declaredActivationPolicy;
    }

    synchronized boolean isActivationTriggered()
    {
        return m_isActivationTriggered;
    }

    boolean isActivationTrigger(String pkgName)
    {
        if ((m_activationIncludes == null) && (m_activationExcludes == null))
        {
            return true;
        }

        // If there are no include filters then all classes are included
        // by default, otherwise try to find one match.
        boolean included = (m_activationIncludes == null);
        for (int i = 0;
            (!included) && (m_activationIncludes != null) && (i < m_activationIncludes.size());
            i++)
        {
            included = m_activationIncludes.get(i).equals(pkgName);
        }

        // If there are no exclude filters then no classes are excluded
        // by default, otherwise try to find one match.
        boolean excluded = false;
        for (int i = 0;
            (!excluded) && (m_activationExcludes != null) && (i < m_activationExcludes.size());
            i++)
        {
            excluded = m_activationExcludes.get(i).equals(pkgName);
        }
        return included && !excluded;
    }

    //
    // Run-time data access.
    //

    public Bundle getBundle()
    {
        return m_bundle;
    }

    public String getId()
    {
        return m_id;
    }

    public synchronized List<Wire> getWires()
    {
        return m_wires;
    }

    public synchronized void setWires(List<Wire> wires)
    {
        // This not only sets the wires for the module, but it also records
        // the dependencies this module has on other modules (i.e., the provider
        // end of the wire) to simplify bookkeeping.

        // For fragments we don't need to capture any additional dependency
        // information, since the wires are sufficient, so just record the
        // new wires. The wires are to the hosts to which the fragment is attached.
        boolean isFragment = Util.isFragment(this);

        // Remove module from old wire modules' dependencies,
        // since we are no longer dependent on any the moduels
        // from the old wires.
        for (int i = 0; !isFragment && (m_wires != null) && (i < m_wires.size()); i++)
        {
            if (m_wires.get(i).getCapability().getNamespace().equals(Capability.MODULE_NAMESPACE))
            {
                ((ModuleImpl) m_wires.get(i).getExporter()).removeDependentRequirer(this);
            }
            else if (m_wires.get(i).getCapability().getNamespace().equals(Capability.PACKAGE_NAMESPACE))
            {
                ((ModuleImpl) m_wires.get(i).getExporter()).removeDependentImporter(this);
            }
        }

        m_wires = wires;

        // Add ourself as a dependent to the new wires' modules.
        for (int i = 0; !isFragment && (m_wires != null) && (i < m_wires.size()); i++)
        {
            if (m_wires.get(i).getCapability().getNamespace().equals(Capability.MODULE_NAMESPACE))
            {
                ((ModuleImpl) m_wires.get(i).getExporter()).addDependentRequirer(this);
            }
            else if (m_wires.get(i).getCapability().getNamespace().equals(Capability.PACKAGE_NAMESPACE))
            {
                ((ModuleImpl) m_wires.get(i).getExporter()).addDependentImporter(this);
            }
        }
    }

    public boolean isResolved()
    {
        return m_isResolved;
    }

    public void setResolved()
    {
        m_isResolved = true;
    }


    public synchronized void setSecurityContext(Object securityContext)
    {
        m_protectionDomain = (ProtectionDomain) securityContext;
    }

    public synchronized Object getSecurityContext()
    {
        return m_protectionDomain;
    }

    // TODO: FRAGMENT RESOLVER - Technically, this is only necessary for fragments.
    //       When we refactoring for the new R4.3 framework API, we'll have to see
    //       if this is still necessary, since the new BundleWirings API will give
    //       us another way to detect it.
    public boolean isRemovalPending()
    {
        return (m_bundle.getState() == Bundle.UNINSTALLED)
            || (this != ((BundleImpl) m_bundle).getCurrentModule());
    }

    //
    // Content access methods.
    //

    public Content getContent()
    {
        return m_content;
    }

    private synchronized Content[] getContentPath()
    {
        if (m_contentPath == null)
        {
            try
            {
                m_contentPath = initializeContentPath();
            }
            catch (Exception ex)
            {
                m_logger.log(
                    m_bundle, Logger.LOG_ERROR, "Unable to get module class path.", ex);
            }
        }
        return m_contentPath;
    }

    private Content[] initializeContentPath() throws Exception
    {
        List contentList = new ArrayList();
        calculateContentPath(this, m_content, contentList, true);
        for (int i = 0; (m_fragmentContents != null) && (i < m_fragmentContents.length); i++)
        {
            calculateContentPath(m_fragments.get(i), m_fragmentContents[i], contentList, false);
        }
        return (Content[]) contentList.toArray(new Content[contentList.size()]);
    }

    private List calculateContentPath(
        Module module, Content content, List contentList, boolean searchFragments)
        throws Exception
    {
        // Creating the content path entails examining the bundle's
        // class path to determine whether the bundle JAR file itself
        // is on the bundle's class path and then creating content
        // objects for everything on the class path.

        // Create a list to contain the content path for the specified content.
        List localContentList = new ArrayList();

        // Find class path meta-data.
        String classPath = (String) module.getHeaders().get(FelixConstants.BUNDLE_CLASSPATH);
        // Parse the class path into strings.
        List<String> classPathStrings = ManifestParser.parseDelimitedString(
            classPath, FelixConstants.CLASS_PATH_SEPARATOR);

        if (classPathStrings == null)
        {
            classPathStrings = new ArrayList<String>(0);
        }

        // Create the bundles class path.
        for (int i = 0; i < classPathStrings.size(); i++)
        {
            // Remove any leading slash, since all bundle class path
            // entries are relative to the root of the bundle.
            classPathStrings.set(i, (classPathStrings.get(i).startsWith("/"))
                ? classPathStrings.get(i).substring(1)
                : classPathStrings.get(i));

            // Check for the bundle itself on the class path.
            if (classPathStrings.get(i).equals(FelixConstants.CLASS_PATH_DOT))
            {
                localContentList.add(content);
            }
            else
            {
                // Try to find the embedded class path entry in the current
                // content.
                Content embeddedContent = content.getEntryAsContent(classPathStrings.get(i));
                // If the embedded class path entry was not found, it might be
                // in one of the fragments if the current content is the bundle,
                // so try to search the fragments if necessary.
                for (int fragIdx = 0;
                    searchFragments && (embeddedContent == null)
                        && (m_fragmentContents != null) && (fragIdx < m_fragmentContents.length);
                    fragIdx++)
                {
                    embeddedContent =
                        m_fragmentContents[fragIdx].getEntryAsContent(classPathStrings.get(i));
                }
                // If we found the embedded content, then add it to the
                // class path content list.
                if (embeddedContent != null)
                {
                    localContentList.add(embeddedContent);
                }
                else
                {
// TODO: FRAMEWORK - Per the spec, this should fire a FrameworkEvent.INFO event;
//       need to create an "Eventer" class like "Logger" perhaps.
                    m_logger.log(m_bundle, Logger.LOG_INFO,
                        "Class path entry not found: "
                        + classPathStrings.get(i));
                }
            }
        }

        // If there is nothing on the class path, then include
        // "." by default, as per the spec.
        if (localContentList.isEmpty())
        {
            localContentList.add(content);
        }

        // Now add the local contents to the global content list and return it.
        contentList.addAll(localContentList);
        return contentList;
    }

    public Class getClassByDelegation(String name) throws ClassNotFoundException
    {
        // We do not call getClassLoader().loadClass() for arrays because
        // it does not correctly handle array types, which is necessary in
        // cases like deserialization using a wrapper class loader.
        if ((name != null) && (name.length() > 0) && (name.charAt(0) == '['))
        {
            return Class.forName(name, false, getClassLoader());
        }
        return getClassLoader().loadClass(name);
    }

    public URL getResourceByDelegation(String name)
    {
        try
        {
            return (URL) findClassOrResourceByDelegation(name, false);
        }
        catch (ClassNotFoundException ex)
        {
            // This should never be thrown because we are loading resources.
        }
        catch (ResourceNotFoundException ex)
        {
            m_logger.log(m_bundle,
                Logger.LOG_DEBUG,
                ex.getMessage());
        }
        return null;
    }

    private Object findClassOrResourceByDelegation(String name, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        Object result = null;

        Set requestSet = (Set) m_cycleCheck.get();
        if (requestSet == null)
        {
            requestSet = new HashSet();
            m_cycleCheck.set(requestSet);
        }
        if (requestSet.add(name))
        {
            try
            {
                // First, try to resolve the originating module.
                m_resolver.resolve(this);

                // Get the package of the target class/resource.
                String pkgName = (isClass)
                    ? Util.getClassPackage(name)
                    : Util.getResourcePackage(name);

                // Delegate any packages listed in the boot delegation
                // property to the parent class loader.
                if (shouldBootDelegate(pkgName))
                {
                    try
                    {
                        // Get the appropriate class loader for delegation.
                        ClassLoader bdcl = getBootDelegationClassLoader();
                        result = (isClass)
                            ? (Object) bdcl.loadClass(name)
                            : (Object) bdcl.getResource(name);
                        // If this is a java.* package, then always terminate the
                        // search; otherwise, continue to look locally if not found.
                        if (pkgName.startsWith("java.") || (result != null))
                        {
                            return result;
                        }
                    }
                    catch (ClassNotFoundException ex)
                    {
                        // If this is a java.* package, then always terminate the
                        // search; otherwise, continue to look locally if not found.
                        if (pkgName.startsWith("java."))
                        {
                            throw ex;
                        }
                    }
                }

                // Look in the module's imports. Note that the search may
                // be aborted if this method throws an exception, otherwise
                // it continues if a null is returned.
                result = searchImports(name, isClass);

                // If not found, try the module's own class path.
                if (result == null)
                {
                    result = (isClass)
                        ? (Object) getClassLoader().findClass(name)
                        : (Object) getResourceLocal(name);

                    // If still not found, then try the module's dynamic imports.
                    if (result == null)
                    {
                        result = searchDynamicImports(name, pkgName, isClass);
                    }
                }
            }
            catch (ResolveException ex)
            {
                if (isClass)
                {
                    // We do not use the resolve exception as the
                    // cause of the exception, since this would
                    // potentially leak internal module information.
                    throw new ClassNotFoundException(
                        name + " not found because "
                        + getBundle()
                        + " cannot resolve: "
                        + ex.getRequirement());
                }
                else
                {
                    // The spec states that if the bundle cannot be resolved, then
                    // only the local bundle's resources should be searched. So we
                    // will ask the module's own class path.
                    URL url = getResourceLocal(name);
                    if (url != null)
                    {
                        return url;
                    }

                    // We need to throw a resource not found exception.
                    throw new ResourceNotFoundException(
                        name + " not found because "
                        + getBundle()
                        + " cannot resolve: "
                        + ex.getRequirement());
                }
            }
            finally
            {
                requestSet.remove(name);
            }
        }
        else
        {
            // If a cycle is detected, we should return null to break the
            // cycle. This should only ever be return to internal class
            // loading code and not to the actual instigator of the class load.
            return null;
        }

        if (result == null)
        {
            if (isClass)
            {
                throw new ClassNotFoundException(
                    name + " not found by " + this.getBundle());
            }
            else
            {
                throw new ResourceNotFoundException(
                    name + " not found by " + this.getBundle());
            }
        }

        return result;
    }

    URL getResourceLocal(String name)
    {
        URL url = null;

        // Remove leading slash, if present, but special case
        // "/" so that it returns a root URL...this isn't very
        // clean or meaninful, but the Spring guys want it.
        if (name.equals("/"))
        {
            // Just pick a class path index since it doesn't really matter.
            url = createURL(1, name);
        }
        else if (name.startsWith("/"))
        {
            name = name.substring(1);
        }

        // Check the module class path.
        Content[] contentPath = getContentPath();
        for (int i = 0;
            (url == null) &&
            (i < contentPath.length); i++)
        {
            if (contentPath[i].hasEntry(name))
            {
                url = createURL(i + 1, name);
            }
        }

        return url;
    }

    public Enumeration getResourcesByDelegation(String name)
    {
        Set requestSet = (Set) m_cycleCheck.get();
        if (requestSet == null)
        {
            requestSet = new HashSet();
            m_cycleCheck.set(requestSet);
        }
        if (!requestSet.contains(name))
        {
            requestSet.add(name);
            try
            {
                return findResourcesByDelegation(name);
            }
            finally
            {
                requestSet.remove(name);
            }
        }

        return null;
    }

    private Enumeration findResourcesByDelegation(String name)
    {
        Enumeration urls = null;
        List completeUrlList = new ArrayList();

        // First, try to resolve the originating module.
        try
        {
            m_resolver.resolve(this);
        }
        catch (ResolveException ex)
        {
            // The spec states that if the bundle cannot be resolved, then
            // only the local bundle's resources should be searched. So we
            // will ask the module's own class path.
            return getResourcesLocal(name);
        }

        // Get the package of the target class/resource.
        String pkgName = Util.getResourcePackage(name);

        // Delegate any packages listed in the boot delegation
        // property to the parent class loader.
        if (shouldBootDelegate(pkgName))
        {
            try
            {
                // Get the appropriate class loader for delegation.
                ClassLoader bdcl = getBootDelegationClassLoader();
                urls = bdcl.getResources(name);
            }
            catch (IOException ex)
            {
                // This shouldn't happen and even if it does, there
                // is nothing we can do, so just ignore it.
            }
            // If this is a java.* package, then always terminate the
            // search; otherwise, continue to look locally.
            if (pkgName.startsWith("java."))
            {
                return urls;
            }

            completeUrlList.add(urls);
        }

        // Look in the module's imports.
        // We delegate to the module's wires for the resources.
        // If any resources are found, this means that the package of these
        // resources is imported, we must not keep looking since we do not
        // support split-packages.

        // Note that the search may be aborted if this method throws an
        // exception, otherwise it continues if a null is returned.
        List<Wire> wires = getWires();
        for (int i = 0; (wires != null) && (i < wires.size()); i++)
        {
            if (wires.get(i).getRequirement().getNamespace()
                .equals(Capability.PACKAGE_NAMESPACE))
            {
                try
                {
                    // If we find the class or resource, then return it.
                    urls = wires.get(i).getResources(name);
                }
                catch (ResourceNotFoundException ex)
                {
                    urls = null;
                }
                if (urls != null)
                {
                    completeUrlList.add(urls);
                    return new CompoundEnumeration((Enumeration[])
                        completeUrlList.toArray(new Enumeration[completeUrlList.size()]));
                }
            }
        }

        // See whether we can get the resource from the required bundles and
        // regardless of whether or not this is the case continue to the next
        // step potentially passing on the result of this search (if any).
        for (int i = 0; (wires != null) && (i < wires.size()); i++)
        {
            if (wires.get(i).getRequirement().getNamespace()
                .equals(Capability.MODULE_NAMESPACE))
            {
                try
                {
                    // If we find the class or resource, then add it.
                    urls = wires.get(i).getResources(name);
                }
                catch (ResourceNotFoundException ex)
                {
                    urls = null;
                }
                if (urls != null)
                {
                    completeUrlList.add(urls);
                }
            }
        }

        // Try the module's own class path. If we can find the resource then
        // return it together with the results from the other searches else
        // try to look into the dynamic imports.
        urls = getResourcesLocal(name);
        if ((urls != null) && (urls.hasMoreElements()))
        {
            completeUrlList.add(urls);
        }
        else
        {
            // If not found, then try the module's dynamic imports.
            // At this point, the module's imports were searched and so was the
            // the module's content. Now we make an attempt to load the
            // class/resource via a dynamic import, if possible.
            Wire wire = null;
            try
            {
                wire = m_resolver.resolve(this, pkgName);
            }
            catch (ResolveException ex)
            {
                // Ignore this since it is likely normal.
            }
            if (wire != null)
            {
                try
                {
                    urls = wire.getResources(name);
                }
                catch (ResourceNotFoundException ex)
                {
                    urls = null;
                }
                if (urls != null)
                {
                    completeUrlList.add(urls);
                }
            }
        }

        return new CompoundEnumeration((Enumeration[])
            completeUrlList.toArray(new Enumeration[completeUrlList.size()]));
    }

    private Enumeration getResourcesLocal(String name)
    {
        List l = new ArrayList();

        // Special case "/" so that it returns a root URLs for
        // each bundle class path entry...this isn't very
        // clean or meaningful, but the Spring guys want it.
        final Content[] contentPath = getContentPath();
        if (name.equals("/"))
        {
            for (int i = 0; i < contentPath.length; i++)
            {
                l.add(createURL(i + 1, name));
            }
        }
        else
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            // Check the module class path.
            for (int i = 0; i < contentPath.length; i++)
            {
                if (contentPath[i].hasEntry(name))
                {
                    // Use the class path index + 1 for creating the path so
                    // that we can differentiate between module content URLs
                    // (where the path will start with 0) and module class
                    // path URLs.
                    l.add(createURL(i + 1, name));
                }
            }
        }

        return Collections.enumeration(l);
    }

    // TODO: API: Investigate how to handle this better, perhaps we need
    // multiple URL policies, one for content -- one for class path.
    public URL getEntry(String name)
    {
        URL url = null;

        // Check for the special case of "/", which represents
        // the root of the bundle according to the spec.
        if (name.equals("/"))
        {
            url = createURL(0, "/");
        }

        if (url == null)
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            // Check the module content.
            if (getContent().hasEntry(name))
            {
                // Module content URLs start with 0, whereas module
                // class path URLs start with the index into the class
                // path + 1.
                url = createURL(0, name);
            }
        }

        return url;
    }

    public boolean hasInputStream(int index, String urlPath)
    {
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }
        if (index == 0)
        {
            return m_content.hasEntry(urlPath);
        }
        return getContentPath()[index - 1].hasEntry(urlPath);
    }

    public InputStream getInputStream(int index, String urlPath)
        throws IOException
    {
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }
        if (index == 0)
        {
            return m_content.getEntryAsStream(urlPath);
        }
        return getContentPath()[index - 1].getEntryAsStream(urlPath);
    }

    public URL getLocalURL(int index, String urlPath)
    {
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }
        if (index == 0)
        {
            return m_content.getEntryAsURL(urlPath);
        }
        return getContentPath()[index - 1].getEntryAsURL(urlPath);
    }

    private URL createURL(int port, String path)
    {
        // Add a slash if there is one already, otherwise
        // the is no slash separating the host from the file
        // in the resulting URL.
        if (!path.startsWith("/"))
        {
            path = "/" + path;
        }

        try
        {
            return m_secureAction.createURL(null,
                FelixConstants.BUNDLE_URL_PROTOCOL + "://" +
                m_id + ":" + port + path, m_streamHandler);
        }
        catch (MalformedURLException ex)
        {
            m_logger.log(m_bundle,
                Logger.LOG_ERROR,
                "Unable to create resource URL.",
                ex);
        }
        return null;
    }

    //
    // Fragment and dependency management methods.
    //

    public synchronized List<Module> getFragments()
    {
        return m_fragments;
    }

    public synchronized void attachFragments(List<Module> fragments) throws Exception
    {
        // Remove the host wires for this module from old fragments.
        // We will generally only remove host wires when we are uninstalling
        // the module.
        for (int i = 0; (m_fragments != null) && (i < m_fragments.size()); i++)
        {
            // If the fragment has no wires, then there is no reason to try to
            // remove ourself from its wires since it has apparently already
            // been refreshed.
            if (m_fragments.get(i).getWires() != null)
            {
                List<Wire> hostWires = new ArrayList<Wire>(m_fragments.get(i).getWires());
                for (Iterator<Wire> it = hostWires.iterator(); it.hasNext(); )
                {
                    Wire hostWire = it.next();
                    if (hostWire.getExporter().equals(this))
                    {
                        it.remove();
                        ((ModuleImpl) m_fragments.get(i)).setWires(hostWires);
                        break;
                    }
                }
            }
        }

        // Close previous fragment contents.
        for (int i = 0; (m_fragmentContents != null) && (i < m_fragmentContents.length); i++)
        {
            m_fragmentContents[i].close();
        }
        m_fragmentContents = null;

        // Close the old content path, since we'll need to recalculate it for
        // for the added (or removed) fragments.
        for (int i = 0; (m_contentPath != null) && (i < m_contentPath.length); i++)
        {
            // Don't close this module's content, if it is on the content path.
            if (m_content != m_contentPath[i])
            {
                m_contentPath[i].close();
            }
        }
        m_contentPath = null;

        // Remove cached capabilities and requirements.
        m_cachedCapabilities = null;
        m_cachedRequirements = null;
        m_cachedDynamicRequirements = null;

        // Update the dependencies on the new fragments.
        m_fragments = fragments;

        // We need to sort the fragments and add ourself as a dependent of each one.
        // We also need to create an array of fragment contents to attach to our
        // content path.
        if (m_fragments != null)
        {
            // Sort fragments according to ID order, if necessary.
            // Note that this sort order isn't 100% correct since
            // it uses a string, but it is likely close enough and
            // avoids having to create more objects.
            if (m_fragments.size() > 1)
            {
                SortedMap<String, Module> sorted = new TreeMap<String, Module>();
                for (Module f : m_fragments)
                {
                    sorted.put(f.getId(), f);
                }
                m_fragments = new ArrayList(sorted.values());
            }
            m_fragmentContents = new Content[m_fragments.size()];
            for (int i = 0; (m_fragments != null) && (i < m_fragments.size()); i++)
            {
                m_fragmentContents[i] =
                    m_fragments.get(i).getContent()
                        .getEntryAsContent(FelixConstants.CLASS_PATH_DOT);
            }
            // Recalculate the content path for the new fragments.
            m_contentPath = initializeContentPath();
        }
    }

    public synchronized List<Module> getDependentImporters()
    {
        return m_dependentImporters;
    }

    public synchronized void addDependentImporter(Module module)
    {
        if (!m_dependentImporters.contains(module))
        {
            m_dependentImporters.add(module);
        }
    }

    public synchronized void removeDependentImporter(Module module)
    {
        m_dependentImporters.remove(module);
    }

    public synchronized List<Module> getDependentRequirers()
    {
        return m_dependentRequirers;
    }

    public synchronized void addDependentRequirer(Module module)
    {
        if (!m_dependentRequirers.contains(module))
        {
            m_dependentRequirers.add(module);
        }
    }

    public synchronized void removeDependentRequirer(Module module)
    {
        m_dependentRequirers.remove(module);
    }

    public synchronized List<Module> getDependents()
    {
        List<Module> dependents;
        if (Util.isFragment(this))
        {
            dependents = new ArrayList<Module>();
            for (int i = 0; (m_wires != null) && (i < m_wires.size()); i++)
            {
                dependents.add(m_wires.get(i).getExporter());
            }
        }
        else
        {
            dependents = new ArrayList<Module>
                (m_dependentImporters.size() + m_dependentRequirers.size());
            dependents.addAll(m_dependentImporters);
            dependents.addAll(m_dependentRequirers);
        }
        return dependents;
    }

    public synchronized void close()
    {
        m_content.close();
        for (int i = 0; (m_contentPath != null) && (i < m_contentPath.length); i++)
        {
            m_contentPath[i].close();
        }
        m_contentPath = null;
        for (int i = 0; (m_fragmentContents != null) && (i < m_fragmentContents.length); i++)
        {
            m_fragmentContents[i].close();
        }
        m_fragmentContents = null;
        m_classLoader = null;
    }

    @Override
    public String toString()
    {
        return m_id;
    }

    private synchronized ModuleClassLoader getClassLoader()
    {
        if (m_classLoader == null)
        {
            // Determine which class loader to use based on which
            // Java platform we are running on.
            Class clazz;
            if (m_isPreJava5)
            {
                clazz = ModuleClassLoader.class;
            }
            else
            {
                try
                {
                    clazz = ModuleClassLoaderJava5.class;
                }
                catch (Throwable th)
                {
                    // If we are on pre-Java5 then we will get a verify error
                    // here since we try to override a getResources() which is
                    // a final method in pre-Java5.
                    m_isPreJava5 = true;
                    clazz = ModuleClassLoader.class;
                }
            }

            // Use SecureAction to create the class loader if security is
            // enabled; otherwise, create it directly.
            try
            {
                Constructor ctor = (Constructor) m_secureAction.getConstructor(
                    clazz, new Class[] { ModuleImpl.class, ClassLoader.class });
                m_classLoader = (ModuleClassLoader)
                    m_secureAction.invoke(ctor,
                    new Object[] { this, determineParentClassLoader() });
            }
            catch (Exception ex)
            {
                throw new RuntimeException("Unable to create module class loader: "
                    + ex.getMessage() + " [" + ex.getClass().getName() + "]");
            }
        }
        return m_classLoader;
    }

    private ClassLoader determineParentClassLoader()
    {
        // Determine the class loader's parent based on the
        // configuration property; use boot class loader by
        // default.
        String cfg = (String) m_configMap.get(Constants.FRAMEWORK_BUNDLE_PARENT);
        cfg = (cfg == null) ? Constants.FRAMEWORK_BUNDLE_PARENT_BOOT : cfg;
        final ClassLoader parent;
        if (cfg.equalsIgnoreCase(Constants.FRAMEWORK_BUNDLE_PARENT_APP))
        {
            parent = m_secureAction.getSystemClassLoader();
        }
        else if (cfg.equalsIgnoreCase(Constants.FRAMEWORK_BUNDLE_PARENT_EXT))
        {
            parent = m_secureAction.getParentClassLoader(m_secureAction.getSystemClassLoader());
        }
        else if (cfg.equalsIgnoreCase(Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK))
        {
            parent = m_secureAction.getClassLoader(ModuleImpl.class);
        }
        // On Android we cannot set the parent class loader to be null, so
        // we special case that situation here and set it to the system
        // class loader by default instead, which is not really spec.
        else if (m_bootClassLoader == null)
        {
            parent = m_secureAction.getSystemClassLoader();
        }
        else
        {
            parent = null;
        }
        return parent;
    }

    private Object searchImports(String name, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // We delegate to the module's wires to find the class or resource.
        List<Wire> wires = getWires();
        for (int i = 0; (wires != null) && (i < wires.size()); i++)
        {
            // If we find the class or resource, then return it.
            Object result = (isClass)
                ? (Object) wires.get(i).getClass(name)
                : (Object) wires.get(i).getResource(name);
            if (result != null)
            {
                return result;
            }
        }

        return null;
    }

    private Object searchDynamicImports(
        final String name, String pkgName, final boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // At this point, the module's imports were searched and so was the
        // the module's content. Now we make an attempt to load the
        // class/resource via a dynamic import, if possible.
        Wire wire = null;
        try
        {
            wire = m_resolver.resolve(this, pkgName);
        }
        catch (ResolveException ex)
        {
            // Ignore this since it is likely normal.
        }

        // If the dynamic import was successful, then this initial
        // time we must directly return the result from dynamically
        // created wire, but subsequent requests for classes/resources
        // in the associated package will be processed as part of
        // normal static imports.
        if (wire != null)
        {
            // Return the class or resource.
            return (isClass)
                ? (Object) wire.getClass(name)
                : (Object) wire.getResource(name);
        }

        // If implicit boot delegation is enabled, then try to guess whether
        // we should boot delegate.
        if (m_implicitBootDelegation)
        {
            // At this point, the class/resource could not be found by the bundle's
            // static or dynamic imports, nor its own content. Before we throw
            // an exception, we will try to determine if the instigator of the
            // class/resource load was a class from a bundle or not. This is necessary
            // because the specification mandates that classes on the class path
            // should be hidden (except for java.*), but it does allow for these
            // classes/resources to be exposed by the system bundle as an export.
            // However, in some situations classes on the class path make the faulty
            // assumption that they can access everything on the class path from
            // every other class loader that they come in contact with. This is
            // not true if the class loader in question is from a bundle. Thus,
            // this code tries to detect that situation. If the class instigating
            // the load request was NOT from a bundle, then we will make the
            // assumption that the caller actually wanted to use the parent class
            // loader and we will delegate to it. If the class was
            // from a bundle, then we will enforce strict class loading rules
            // for the bundle and throw an exception.

            // Get the class context to see the classes on the stack.
            final Class[] classes = m_sm.getClassContext();
            try
            {
                if (System.getSecurityManager() != null)
                {
                    return AccessController
                        .doPrivileged(new PrivilegedExceptionAction()
                        {
                            public Object run() throws Exception
                            {
                                return doImplicitBootDelegation(classes, name,
                                    isClass);
                            }
                        });
                }
                else
                {
                    return doImplicitBootDelegation(classes, name, isClass);
                }
            }
            catch (PrivilegedActionException ex)
            {
                Exception cause = ex.getException();
                if (cause instanceof ClassNotFoundException)
                {
                    throw (ClassNotFoundException) cause;
                }
                else
                {
                    throw (ResourceNotFoundException) cause;
                }
            }
        }
        return null;
    }

    private Object doImplicitBootDelegation(Class[] classes, String name, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // Start from 1 to skip security manager class.
        for (int i = 1; i < classes.length; i++)
        {
            // Find the first class on the call stack that is not from
            // the class loader that loaded the Felix classes or is not
            // a class loader or class itself, because we want to ignore
            // calls to ClassLoader.loadClass() and Class.forName() since
            // we are trying to find out who instigated the class load.
            // Also ignore inner classes of class loaders, since we can
            // assume they are a class loader too.

            // TODO: FRAMEWORK - This check is a hack and we should see if we can think
            // of another way to do it, since it won't necessarily work in all situations.
            // Since Felix uses threads for changing the start level
            // and refreshing packages, it is possible that there is no
            // module classes on the call stack; therefore, as soon as we
            // see Thread on the call stack we exit this loop. Other cases
            // where modules actually use threads are not an issue because
            // the module classes will be on the call stack before the
            // Thread class.
            if (Thread.class.equals(classes[i]))
            {
                break;
            }
            // Break if the current class came from a bundle, since we should
            // not implicitly boot delegate in that case.
            else if (isClassLoadedFromModule(classes[i]))
            {
                break;
            }
            // Break if this goes through BundleImpl because it must be a call
            // to Bundle.loadClass() which should not implicitly boot delegate.
            else if (BundleImpl.class.equals(classes[i]))
            {
                break;
            }
            else if (isClassExternal(classes[i]))
            {
                try
                {
                    // Return the class or resource from the parent class loader.
                    return (isClass)
                        ? (Object) m_secureAction.getClassLoader(this.getClass()).loadClass(name)
                        : (Object) m_secureAction.getClassLoader(this.getClass()).getResource(name);
                }
                catch (NoClassDefFoundError ex)
                {
                    // Ignore, will return null
                }
                break;
            }
        }

        return null;
    }

    private boolean isClassLoadedFromModule(Class clazz)
    {
        // The target class is loaded by a module class loader,
        // then return true.
        if (ModuleClassLoader.class.isInstance(m_secureAction.getClassLoader(clazz)))
        {
            return true;
        }

        // If the target class was loaded from a class loader that
        // came from a module, then return true.
        ClassLoader last = null;
        for (ClassLoader cl = m_secureAction.getClassLoader(clazz);
            (cl != null) && (last != cl);
            cl = m_secureAction.getClassLoader(cl.getClass()))
        {
            last = cl;
            if (ModuleClassLoader.class.isInstance(cl))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Tries to determine whether the given class is part of the framework or not.
     * Framework classes include everything in org.apache.felix.framework.* and
     * org.osgi.framework.*. We also consider ClassLoader and Class to be internal
     * classes, because they are inserted into the stack trace as a result of
     * method overloading. Typically, ClassLoader or Class will be mixed in
     * between framework classes or will be at the point where the class loading
     * request enters the framework class loading mechanism, which will then be
     * followed by either bundle or external code, which will then exit our
     * attempt to determine if we should boot delegate or not. Other standard
     * class loaders, like URLClassLoader, are considered external classes and
     * should trigger boot delegation. This means that bundles can create standard
     * class loaders to get access to boot packages, but this is the standard
     * behavior of class loaders.
     * @param clazz the class to determine if it is external or not.
     * @return <tt>true</tt> if the class is external, otherwise <tt>false</tt>.
     */
    private boolean isClassExternal(Class clazz)
    {
        if (clazz.getName().startsWith("org.apache.felix.framework."))
        {
            return false;
        }
        else if (clazz.getName().startsWith("org.osgi.framework."))
        {
            return false;
        }
        else if (ClassLoader.class.equals(clazz))
        {
            return false;
        }
        else if (Class.class.equals(clazz))
        {
            return false;
        }
        return true;
    }

    boolean shouldBootDelegate(String pkgName)
    {
        // Always boot delegate if the bundle has a configured
        // boot class loader.
        if (m_bootClassLoader != m_defBootClassLoader)
        {
            return true;
        }

        boolean result = false;

        // Only consider delegation if we have a package name, since
        // we don't want to promote the default package. The spec does
        // not take a stand on this issue.
        if (pkgName.length() > 0)
        {
            for (int i = 0; !result && (i < m_bootPkgs.length); i++)
            {
                // Check if the boot package is wildcarded.
                // A wildcarded boot package will be in the form "foo.",
                // so a matching subpackage will start with "foo.", e.g.,
                // "foo.bar".
                if (m_bootPkgWildcards[i] && pkgName.startsWith(m_bootPkgs[i]))
                {
                    return true;
                }
                // If not wildcarded, then check for an exact match.
                else if (m_bootPkgs[i].equals(pkgName))
                {
                    return true;
                }
            }
        }

        return result;
    }

    ClassLoader getBootDelegationClassLoader()
    {
        // Get the appropriate class loader for delegation.
        ClassLoader parent = (m_classLoader == null)
            ? determineParentClassLoader() : m_secureAction.getParentClassLoader(m_classLoader);
        return (parent == null) ? m_bootClassLoader : parent;
    }

    private static final Constructor m_dexFileClassConstructor;
    private static final Method m_dexFileClassLoadDex;
    private static final Method m_dexFileClassLoadClass;

    static
    {
        Constructor dexFileClassConstructor = null;
        Method dexFileClassLoadDex = null;
        Method dexFileClassLoadClass = null;
        try
        {
            Class dexFileClass;
            try
            {
                dexFileClass = Class.forName("dalvik.system.DexFile");
            }
            catch (Exception ex)
            {
                dexFileClass = Class.forName("android.dalvik.DexFile");
            }

            try
            {
                dexFileClassLoadDex = dexFileClass.getMethod("loadDex", 
                    new Class[]{String.class, String.class, Integer.TYPE});
            }
            catch (Exception ex)
            {
                // Nothing we need to do 
            }
            dexFileClassConstructor = dexFileClass.getConstructor(
                new Class[] { java.io.File.class });
            dexFileClassLoadClass = dexFileClass.getMethod("loadClass",
                new Class[] { String.class, ClassLoader.class });
        }
        catch (Throwable ex)
        {
           dexFileClassConstructor = null;
           dexFileClassLoadDex = null;
           dexFileClassLoadClass = null;
        }
        m_dexFileClassConstructor = dexFileClassConstructor;
        m_dexFileClassLoadDex= dexFileClassLoadDex;
        m_dexFileClassLoadClass = dexFileClassLoadClass;
    }

    public class ModuleClassLoaderJava5 extends ModuleClassLoader
    {
        public ModuleClassLoaderJava5(ClassLoader parent)
        {
            super(parent);
        }

        @Override
        public Enumeration getResources(String name)
        {
            Enumeration urls = ModuleImpl.this.getResourcesByDelegation(name);
            if (m_useLocalURLs)
            {
                urls = new ToLocalUrlEnumeration(urls);
            }
            return urls;
        }

        @Override
        protected Enumeration findResources(String name)
        {
            return ModuleImpl.this.getResourcesLocal(name);
        }
    }

    public class ModuleClassLoader extends SecureClassLoader implements BundleReference
    {
        private final Map m_jarContentToDexFile;
        private Object[][] m_cachedLibs = new Object[0][];
        private static final int LIBNAME_IDX = 0;
        private static final int LIBPATH_IDX = 1;

        public ModuleClassLoader(ClassLoader parent)
        {
            super(parent);
            if (m_dexFileClassLoadClass != null)
            {
                m_jarContentToDexFile = new HashMap();
            }
            else
            {
                m_jarContentToDexFile = null;
            }
        }

        public Bundle getBundle()
        {
            return ModuleImpl.this.getBundle();
        }

        @Override
        protected Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException
        {
            Class clazz = null;

            // Make sure the class was not already loaded.
            synchronized (this)
            {
                clazz = findLoadedClass(name);
            }

            if (clazz == null)
            {
                try
                {
                    clazz = (Class) findClassOrResourceByDelegation(name, true);
                }
                catch (ResourceNotFoundException ex)
                {
                    // This should never happen since we are asking for a class,
                    // so just ignore it.
                }
                catch (ClassNotFoundException cnfe)
                {
                    ClassNotFoundException ex = cnfe;
                    String msg = name;
                    if (m_logger.getLogLevel() >= Logger.LOG_DEBUG)
                    {
                        msg = diagnoseClassLoadError(m_resolver, ModuleImpl.this, name);
                        ex = (msg != null)
                            ? new ClassNotFoundException(msg, cnfe)
                            : ex;
                    }
                    throw ex;
                }
            }

            // Resolve the class and return it.
            if (resolve)
            {
                resolveClass(clazz);
            }
            return clazz;
        }

        @Override
        protected Class findClass(String name) throws ClassNotFoundException
        {
            Class clazz = null;

            // Search for class in module.
            if (clazz == null)
            {
                String actual = name.replace('.', '/') + ".class";

                byte[] bytes = null;

                // Check the module class path.
                Content[] contentPath = getContentPath();
                Content content = null;
                for (int i = 0;
                    (bytes == null) &&
                    (i < contentPath.length); i++)
                {
                    bytes = contentPath[i].getEntryAsBytes(actual);
                    content = contentPath[i];
                }

                if (bytes != null)
                {
                    // Get package name.
                    String pkgName = Util.getClassPackage(name);

                    // Before we actually attempt to define the class, grab
                    // the lock for this class loader and make sure than no
                    // other thread has defined this class in the meantime.
                    synchronized (this)
                    {
                        clazz = findLoadedClass(name);

                        if (clazz == null)
                        {
                            int activationPolicy = 
                                ((BundleImpl) getBundle()).isDeclaredActivationPolicyUsed()
                                ? ((BundleImpl) getBundle()).getCurrentModule().getDeclaredActivationPolicy()
                                : Module.EAGER_ACTIVATION;

                            // If the module is using deferred activation, then if
                            // we load this class from this module we need to activate
                            // the module before returning the class. We will short
                            // circuit the trigger matching if the trigger is already
                            // tripped.
                            boolean isTriggerClass = m_isActivationTriggered
                                ? false : isActivationTrigger(pkgName);
                            if (!m_isActivationTriggered
                                && isTriggerClass
                                && (activationPolicy == Module.LAZY_ACTIVATION)
                                && (getBundle().getState() == Bundle.STARTING))
                            {
                                List deferredList = (List) m_deferredActivation.get();
                                if (deferredList == null)
                                {
                                    deferredList = new ArrayList();
                                    m_deferredActivation.set(deferredList);
                                }
                                deferredList.add(new Object[] { name, getBundle() });
                            }
                            // We need to try to define a Package object for the class
                            // before we call defineClass() if we haven't already
                            // created it.
                            if (pkgName.length() > 0)
                            {
                                if (getPackage(pkgName) == null)
                                {
                                    Object[] params = definePackage(pkgName);
                                    if (params != null)
                                    {
                                        definePackage(
                                            pkgName,
                                            (String) params[0],
                                            (String) params[1],
                                            (String) params[2],
                                            (String) params[3],
                                            (String) params[4],
                                            (String) params[5],
                                            null);
                                    }
                                    else
                                    {
                                        definePackage(pkgName, null, null,
                                            null, null, null, null, null);
                                    }
                                }
                            }

                            // If we can load the class from a dex file do so
                            if (content instanceof JarContent)
                            {
                                try
                                {
                                    clazz = getDexFileClass((JarContent) content, name, this);
                                }
                                catch (Exception ex)
                                {
                                    // Looks like we can't
                                }
                            }

                            if (clazz == null)
                            {
                                // If we have a security context, then use it to
                                // define the class with it for security purposes,
                                // otherwise define the class without a protection domain.
                                if (m_protectionDomain != null)
                                {
                                    clazz = defineClass(name, bytes, 0, bytes.length,
                                        m_protectionDomain);
                                }
                                else
                                {
                                    clazz = defineClass(name, bytes, 0, bytes.length);
                                }
                            }

                            // At this point if we have a trigger class, then the deferred
                            // activation trigger has tripped.
                            if (!m_isActivationTriggered && isTriggerClass && (clazz != null))
                            {
                                m_isActivationTriggered = true;
                            }
                        }
                    }

                    // Perform deferred activation without holding the class loader lock,
                    // if the class we are returning is the instigating class.
                    List deferredList = (List) m_deferredActivation.get();
                    if ((deferredList != null)
                        && (deferredList.size() > 0)
                        && ((Object[]) deferredList.get(0))[0].equals(name))
                    {
                        for (int i = deferredList.size() - 1; i >= 0; i--)
                        {
                            try
                            {
                                ((BundleImpl) ((Object[]) deferredList.get(i))[1]).getFramework().activateBundle(
                                    (BundleImpl) ((Object[]) deferredList.get(i))[1], true);
                            }
                            catch (BundleException ex)
                            {
                                ex.printStackTrace();
                            }
                        }
                        deferredList.clear();
                    }
                }
            }

            return clazz;
        }

        private Object[] definePackage(String pkgName)
        {
            String spectitle = (String) m_headerMap.get("Specification-Title");
            String specversion = (String) m_headerMap.get("Specification-Version");
            String specvendor = (String) m_headerMap.get("Specification-Vendor");
            String impltitle = (String) m_headerMap.get("Implementation-Title");
            String implversion = (String) m_headerMap.get("Implementation-Version");
            String implvendor = (String) m_headerMap.get("Implementation-Vendor");
            if ((spectitle != null)
                || (specversion != null)
                || (specvendor != null)
                || (impltitle != null)
                || (implversion != null)
                || (implvendor != null))
            {
                return new Object[] {
                    spectitle, specversion, specvendor, impltitle, implversion, implvendor
                };
            }
            return null;
        }

        private Class getDexFileClass(JarContent content, String name, ClassLoader loader)
            throws Exception
        {
            if (m_jarContentToDexFile == null)
            {
                return null;
            }

            Object dexFile = null;

            if (!m_jarContentToDexFile.containsKey(content))
            {
                try
                {
                    if (m_dexFileClassLoadDex != null)
                    {
                        dexFile = m_dexFileClassLoadDex.invoke(null, 
                            new Object[]{content.getFile().getAbsolutePath(), 
                                content.getFile().getAbsolutePath() + ".dex", new Integer(0)});
                    }
                    else
                    {
                        dexFile = m_dexFileClassConstructor.newInstance(
                            new Object[] { content.getFile() });
                    }
                }
                finally
                {
                    m_jarContentToDexFile.put(content, dexFile);
                }
            }
            else
            {
                dexFile = m_jarContentToDexFile.get(content);
            }

            if (dexFile != null)
            {
                return (Class) m_dexFileClassLoadClass.invoke(dexFile,
                    new Object[] { name.replace('.','/'), loader });
            }
            return null;
        }

        @Override
        public URL getResource(String name)
        {
            URL url = ModuleImpl.this.getResourceByDelegation(name);
            if (m_useLocalURLs)
            {
                url = convertToLocalUrl(url);
            }
            return url;
        }

        @Override
        protected URL findResource(String name)
        {
            return ModuleImpl.this.getResourceLocal(name);
        }

        // The findResources() method should only look at the module itself, but
        // instead it tries to delegate because in Java version prior to 1.5 the
        // getResources() method was final and could not be overridden. We should
        // override getResources() like getResource() to make it delegate, but we
        // can't. As a workaround, we make findResources() delegate instead.
        @Override
        protected Enumeration findResources(String name)
        {
            Enumeration urls = ModuleImpl.this.getResourcesByDelegation(name);
            if (m_useLocalURLs)
            {
                urls = new ToLocalUrlEnumeration(urls);
            }
            return urls;
        }

        @Override
        protected String findLibrary(String name)
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            String result = null;
            // CONCURRENCY: In the long run, we might want to break this
            // sync block in two to avoid manipulating the cache while
            // holding the lock, but for now we will do it the simple way.
            synchronized (this)
            {
                // Check to make sure we haven't already found this library.
                for (int i = 0; (result == null) && (i < m_cachedLibs.length); i++)
                {
                    if (m_cachedLibs[i][LIBNAME_IDX].equals(name))
                    {
                        result = (String) m_cachedLibs[i][LIBPATH_IDX];
                    }
                }

                // If we don't have a cached result, see if we have a matching
                // native library.
                if (result == null)
                {
                    List<R4Library> libs = getNativeLibraries();
                    for (int libIdx = 0; (libs != null) && (libIdx < libs.size()); libIdx++)
                    {
                        if (libs.get(libIdx).match(m_configMap, name))
                        {
                            // Search bundle content first for native library.
                            result = getContent().getEntryAsNativeLibrary(
                                libs.get(libIdx).getEntryName());
                            // If not found, then search fragments in order.
                            for (int i = 0;
                                (result == null) && (m_fragmentContents != null)
                                    && (i < m_fragmentContents.length);
                                i++)
                            {
                                result = m_fragmentContents[i].getEntryAsNativeLibrary(
                                    libs.get(libIdx).getEntryName());
                            }
                        }
                    }

                    // Remember the result for future requests.
                    if (result != null)
                    {
                        Object[][] tmp = new Object[m_cachedLibs.length + 1][];
                        System.arraycopy(m_cachedLibs, 0, tmp, 0, m_cachedLibs.length);
                        tmp[m_cachedLibs.length] = new Object[] { name, result };
                        m_cachedLibs = tmp;
                    }
                }
            }

            return result;
        }

        @Override
        public String toString()
        {
            return ModuleImpl.this.toString();
        }
    }

    static URL convertToLocalUrl(URL url)
    {
        if (url.getProtocol().equals("bundle"))
        {
            try
            {
                url = ((URLHandlersBundleURLConnection)
                    url.openConnection()).getLocalURL();
            }
            catch (IOException ex)
            {
                // Ignore and add original url.
            }
        }
        return url;
    }

    static class ToLocalUrlEnumeration implements Enumeration
    {
        final Enumeration m_enumeration;

        ToLocalUrlEnumeration(Enumeration enumeration)
        {
            m_enumeration = enumeration;
        }

        public boolean hasMoreElements()
        {
            return m_enumeration.hasMoreElements();
        }

        public Object nextElement()
        {
            return convertToLocalUrl((URL) m_enumeration.nextElement());
        }
    }

    private static String diagnoseClassLoadError(
        StatefulResolver resolver, ModuleImpl module, String name)
    {
        // We will try to do some diagnostics here to help the developer
        // deal with this exception.

        // Get package name.
        String pkgName = Util.getClassPackage(name);
        if (pkgName.length() == 0)
        {
            return null;
        }

        // First, get the bundle string of the module doing the class loader.
        String importer = module.getBundle().toString();

        // Next, check to see if the module imports the package.
        List<Wire> wires = module.getWires();
        for (int i = 0; (wires != null) && (i < wires.size()); i++)
        {
            if (wires.get(i).getCapability().getNamespace().equals(Capability.PACKAGE_NAMESPACE) &&
                wires.get(i).getCapability().getAttribute(Capability.PACKAGE_ATTR).getValue().equals(pkgName))
            {
                String exporter = wires.get(i).getExporter().getBundle().toString();

                StringBuffer sb = new StringBuffer("*** Package '");
                sb.append(pkgName);
                sb.append("' is imported by bundle ");
                sb.append(importer);
                sb.append(" from bundle ");
                sb.append(exporter);
                sb.append(", but the exported package from bundle ");
                sb.append(exporter);
                sb.append(" does not contain the requested class '");
                sb.append(name);
                sb.append("'. Please verify that the class name is correct in the importing bundle ");
                sb.append(importer);
                sb.append(" and/or that the exported package is correctly bundled in ");
                sb.append(exporter);
                sb.append(". ***");

                return sb.toString();
            }
        }

        // Next, check to see if the package was optionally imported and
        // whether or not there is an exporter available.
        List<Requirement> reqs = module.getRequirements();
/*
* TODO: RB - Fix diagnostic message for optional imports.
        for (int i = 0; (reqs != null) && (i < reqs.length); i++)
        {
            if (reqs[i].getName().equals(pkgName) && reqs[i].isOptional())
            {
                // Try to see if there is an exporter available.
                IModule[] exporters = getResolvedExporters(reqs[i], true);
                exporters = (exporters.length == 0)
                    ? getUnresolvedExporters(reqs[i], true) : exporters;

                // An exporter might be available, but it may have attributes
                // that do not match the importer's required attributes, so
                // check that case by simply looking for an exporter of the
                // desired package without any attributes.
                if (exporters.length == 0)
                {
                    IRequirement pkgReq = new Requirement(
                        ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")");
                    exporters = getResolvedExporters(pkgReq, true);
                    exporters = (exporters.length == 0)
                        ? getUnresolvedExporters(pkgReq, true) : exporters;
                }

                long expId = (exporters.length == 0)
                    ? -1 : Util.getBundleIdFromModuleId(exporters[0].getId());

                StringBuffer sb = new StringBuffer("*** Class '");
                sb.append(name);
                sb.append("' was not found, but this is likely normal since package '");
                sb.append(pkgName);
                sb.append("' is optionally imported by bundle ");
                sb.append(impId);
                sb.append(".");
                if (exporters.length > 0)
                {
                    sb.append(" However, bundle ");
                    sb.append(expId);
                    if (reqs[i].isSatisfied(
                        Util.getExportPackage(exporters[0], reqs[i].getName())))
                    {
                        sb.append(" does export this package. Bundle ");
                        sb.append(expId);
                        sb.append(" must be installed before bundle ");
                        sb.append(impId);
                        sb.append(" is resolved or else the optional import will be ignored.");
                    }
                    else
                    {
                        sb.append(" does export this package with attributes that do not match.");
                    }
                }
                sb.append(" ***");

                return sb.toString();
            }
        }
*/
        // Next, check to see if the package is dynamically imported by the module.
        if (resolver.isAllowedDynamicImport(module, pkgName))
        {
            // Try to see if there is an exporter available.
            List<Directive> dirs = Collections.EMPTY_LIST;
            List<Attribute> attrs = new ArrayList(1);
            attrs.add(new Attribute(Capability.PACKAGE_ATTR, pkgName, false));
            Requirement req = new RequirementImpl(
                module, Capability.PACKAGE_NAMESPACE, dirs, attrs);
            Set<Capability> exporters = resolver.getCandidates(req, false);

            Wire wire = null;
            try
            {
                wire = resolver.resolve(module, pkgName);
            }
            catch (Exception ex)
            {
                wire = null;
            }

            String exporter = (exporters.isEmpty())
                ? null : exporters.iterator().next().getModule().getBundle().toString();

            StringBuffer sb = new StringBuffer("*** Class '");
            sb.append(name);
            sb.append("' was not found, but this is likely normal since package '");
            sb.append(pkgName);
            sb.append("' is dynamically imported by bundle ");
            sb.append(importer);
            sb.append(".");
            if ((exporters.size() > 0) && (wire == null))
            {
                sb.append(" However, bundle ");
                sb.append(exporter);
                sb.append(" does export this package with attributes that do not match.");
            }
            sb.append(" ***");

            return sb.toString();
        }

        // Next, check to see if there are any exporters for the package at all.
        List<Directive> dirs = Collections.EMPTY_LIST;
        List<Attribute> attrs = new ArrayList(1);
        attrs.add(new Attribute(Capability.PACKAGE_ATTR, pkgName, false));
        Requirement req = new RequirementImpl(
            module, Capability.PACKAGE_NAMESPACE, dirs, attrs);
        Set<Capability> exports = resolver.getCandidates(req, false);
        if (exports.size() > 0)
        {
            boolean classpath = false;
            try
            {
                m_secureAction.getClassLoader(ModuleClassLoader.class).loadClass(name);
                classpath = true;
            }
            catch (NoClassDefFoundError err)
            {
                // Ignore
            }
            catch (Exception ex)
            {
                // Ignore
            }

            String exporter = exports.iterator().next().getModule().getBundle().toString();

            StringBuffer sb = new StringBuffer("*** Class '");
            sb.append(name);
            sb.append("' was not found because bundle ");
            sb.append(importer);
            sb.append(" does not import '");
            sb.append(pkgName);
            sb.append("' even though bundle ");
            sb.append(exporter);
            sb.append(" does export it.");
            if (classpath)
            {
                sb.append(" Additionally, the class is also available from the system class loader. There are two fixes: 1) Add an import for '");
                sb.append(pkgName);
                sb.append("' to bundle ");
                sb.append(importer);
                sb.append("; imports are necessary for each class directly touched by bundle code or indirectly touched, such as super classes if their methods are used. ");
                sb.append("2) Add package '");
                sb.append(pkgName);
                sb.append("' to the '");
                sb.append(Constants.FRAMEWORK_BOOTDELEGATION);
                sb.append("' property; a library or VM bug can cause classes to be loaded by the wrong class loader. The first approach is preferable for preserving modularity.");
            }
            else
            {
                sb.append(" To resolve this issue, add an import for '");
                sb.append(pkgName);
                sb.append("' to bundle ");
                sb.append(importer);
                sb.append(".");
            }
            sb.append(" ***");

            return sb.toString();
        }

        // Next, try to see if the class is available from the system
        // class loader.
        try
        {
            m_secureAction.getClassLoader(ModuleClassLoader.class).loadClass(name);

            StringBuffer sb = new StringBuffer("*** Package '");
            sb.append(pkgName);
            sb.append("' is not imported by bundle ");
            sb.append(importer);
            sb.append(", nor is there any bundle that exports package '");
            sb.append(pkgName);
            sb.append("'. However, the class '");
            sb.append(name);
            sb.append("' is available from the system class loader. There are two fixes: 1) Add package '");
            sb.append(pkgName);
            sb.append("' to the '");
            sb.append(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
            sb.append("' property and modify bundle ");
            sb.append(importer);
            sb.append(" to import this package; this causes the system bundle to export class path packages. 2) Add package '");
            sb.append(pkgName);
            sb.append("' to the '");
            sb.append(Constants.FRAMEWORK_BOOTDELEGATION);
            sb.append("' property; a library or VM bug can cause classes to be loaded by the wrong class loader. The first approach is preferable for preserving modularity.");
            sb.append(" ***");

            return sb.toString();
        }
        catch (Exception ex2)
        {
        }

        // Finally, if there are no imports or exports for the package
        // and it is not available on the system class path, simply
        // log a message saying so.
        StringBuffer sb = new StringBuffer("*** Class '");
        sb.append(name);
        sb.append("' was not found. Bundle ");
        sb.append(importer);
        sb.append(" does not import package '");
        sb.append(pkgName);
        sb.append("', nor is the package exported by any other bundle or available from the system class loader.");
        sb.append(" ***");

        return sb.toString();
    }
}