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
package org.apache.myfaces.view.facelets.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.myfaces.shared_impl.util.ClassUtils;

/**
 * @author Jacob Hookom
 * @author Roland Huss
 * @author Ales Justin (ales.justin@jboss.org)
 * @version $Id: Classpath.java,v 1.10 2008/07/13 19:01:34 rlubke Exp $
 */
public final class Classpath
{
    private Classpath()
    {
    }

    public static URL[] search(String prefix, String suffix) throws IOException
    {
        return search(ClassUtils.getContextClassLoader(), prefix, suffix);
    }

    public static URL[] search(ClassLoader loader, String prefix, String suffix) throws IOException
    {
        Set<URL> all = new LinkedHashSet<URL>();

        _searchResource(all, loader, prefix, prefix, suffix);
        _searchResource(all, loader, prefix + "MANIFEST.MF", prefix, suffix);

        URL[] urlArray = (URL[]) all.toArray(new URL[all.size()]);

        return urlArray;
    }

    private static void _searchResource(Set<URL> result, ClassLoader loader, String resource, String prefix,
                                        String suffix) throws IOException
    {
        for (Enumeration<URL> urls = loader.getResources(resource); urls.hasMoreElements();)
        {
            URL url = urls.nextElement();
            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);

            JarFile jar;
            if (conn instanceof JarURLConnection)
            {
                jar = ((JarURLConnection) conn).getJarFile();
            }
            else
            {
                jar = _getAlternativeJarFile(url);
            }

            if (jar != null)
            {
                _searchJar(loader, result, jar, prefix, suffix);
            }
            else
            {
                if (!_searchDir(result, new File(URLDecoder.decode(url.getFile(), "UTF-8")), suffix))
                {
                    _searchFromURL(result, prefix, suffix, url);
                }
            }
        }
    }

    private static boolean _searchDir(Set<URL> result, File dir, String suffix) throws IOException
    {
        if (dir.exists() && dir.isDirectory())
        {
            for (File file : dir.listFiles())
            {
                String path = file.getAbsolutePath();
                if (file.isDirectory())
                {
                    _searchDir(result, file, suffix);
                }
                else if (path.endsWith(suffix))
                {
                    result.add(file.toURI().toURL());
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Search from URL. Fall back on prefix tokens if not able to read from original url param.
     * 
     * @param result
     *            the result urls
     * @param prefix
     *            the current prefix
     * @param suffix
     *            the suffix to match
     * @param url
     *            the current url to start search
     * @throws IOException
     *             for any error
     */
    private static void _searchFromURL(Set<URL> result, String prefix, String suffix, URL url) throws IOException
    {
        boolean done = false;

        InputStream is = _getInputStream(url);
        if (is != null)
        {
            try
            {
                ZipInputStream zis;
                if (is instanceof ZipInputStream)
                {
                    zis = (ZipInputStream) is;
                }
                else
                {
                    zis = new ZipInputStream(is);
                }

                try
                {
                    ZipEntry entry = zis.getNextEntry();
                    // initial entry should not be null
                    // if we assume this is some inner jar
                    done = entry != null;

                    while (entry != null)
                    {
                        String entryName = entry.getName();
                        if (entryName.endsWith(suffix))
                        {
                            result.add(new URL(url.toExternalForm() + entryName));
                        }

                        entry = zis.getNextEntry();
                    }
                }
                finally
                {
                    zis.close();
                }
            }
            catch (Exception ignore)
            {
            }
        }

        if (!done && prefix.length() > 0)
        {
            // we add '/' at the end since join adds it as well
            String urlString = url.toExternalForm() + "/";

            String[] split = prefix.split("/");

            prefix = _join(split, true);

            String end = _join(split, false);

            url = new URL(urlString.substring(0, urlString.lastIndexOf(end)));

            _searchFromURL(result, prefix, suffix, url);
        }
    }

    /**
     * Join tokens, exlude last if param equals true.
     * 
     * @param tokens
     *            the tokens
     * @param excludeLast
     *            do we exclude last token
     * @return joined tokens
     */
    private static String _join(String[] tokens, boolean excludeLast)
    {
        StringBuilder join = new StringBuilder();
        int length = tokens.length - (excludeLast ? 1 : 0);
        for (int i = 0; i < length; i++)
        {
            join.append(tokens[i]).append("/");
        }

        return join.toString();
    }

    /**
     * Open input stream from url. Ignore any errors.
     * 
     * @param url
     *            the url to open
     * @return input stream or null if not possible
     */
    private static InputStream _getInputStream(URL url)
    {
        try
        {
            return url.openStream();
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    /**
     * For URLs to JARs that do not use JarURLConnection - allowed by the servlet spec - attempt to produce a JarFile
     * object all the same. Known servlet engines that function like this include Weblogic and OC4J. This is not a full
     * solution, since an unpacked WAR or EAR will not have JAR "files" as such.
     */
    private static JarFile _getAlternativeJarFile(URL url) throws IOException
    {
        String urlFile = url.getFile();

        // Trim off any suffix - which is prefixed by "!/" on Weblogic
        int separatorIndex = urlFile.indexOf("!/");

        // OK, didn't find that. Try the less safe "!", used on OC4J
        if (separatorIndex == -1)
        {
            separatorIndex = urlFile.indexOf('!');
        }

        if (separatorIndex != -1)
        {
            String jarFileUrl = urlFile.substring(0, separatorIndex);
            // And trim off any "file:" prefix.
            if (jarFileUrl.startsWith("file:"))
            {
                jarFileUrl = jarFileUrl.substring("file:".length());
            }

            return new JarFile(jarFileUrl);
        }

        return null;
    }

    private static void _searchJar(ClassLoader loader, Set<URL> result, JarFile file, String prefix, String suffix)
            throws IOException
    {
        Enumeration<JarEntry> e = file.entries();
        while (e.hasMoreElements())
        {
            try
            {
                String name = e.nextElement().getName();
                if (name.startsWith(prefix) && name.endsWith(suffix))
                {
                    Enumeration<URL> e2 = loader.getResources(name);
                    while (e2.hasMoreElements())
                    {
                        result.add(e2.nextElement());
                    }
                }
            }
            catch (Throwable t)
            {
                // shallow
            }
        }
    }

}
