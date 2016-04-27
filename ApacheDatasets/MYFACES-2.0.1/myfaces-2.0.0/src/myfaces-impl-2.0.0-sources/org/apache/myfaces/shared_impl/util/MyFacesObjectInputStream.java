/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.myfaces.shared_impl.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * Tried to deploy v0.4.2 on JBoss 3.2.1 and had a classloading problem again.
 * The problem seemed to be with JspInfo, line 98. We are using an
 * ObjectInputStream Class, which then cannot find the classes to deserialize
 * the input stream.  The solution appears to be to subclass ObjectInputStream
 * (eg. CustomInputStream), and specify a different class-loading mechanism.
 *
 * @author Robert Gothan <robert@funkyjazz.net> (latest modification by $Author: schof $)
 * @version $Revision: 382015 $ $Date: 2006-03-01 08:47:11 -0500 (Wed, 01 Mar 2006) $
 */
public class MyFacesObjectInputStream
    extends ObjectInputStream
{
    public MyFacesObjectInputStream(InputStream in) throws IOException
    {
        super(in);
    }

    protected Class resolveClass(ObjectStreamClass desc)
        throws ClassNotFoundException, IOException
    {
        try
        {
            return ClassUtils.classForName(desc.getName());
        }
        catch (ClassNotFoundException e)
        {
            return super.resolveClass(desc);
        }
    }
}
