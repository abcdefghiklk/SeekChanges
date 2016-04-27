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
package javax.faces.component;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 *
 * @author Manfred Geiler (latest modification by $Author: slessard $)
 * @version $Revision: 690051 $ $Date: 2008-08-28 18:47:56 -0500 (Thu, 28 Aug 2008) $
 */
public interface StateHolder
{
    public java.lang.Object saveState(javax.faces.context.FacesContext context);

    public void restoreState(javax.faces.context.FacesContext context,
                             java.lang.Object state);

    public boolean isTransient();

    public void setTransient(boolean newTransientValue);
}
