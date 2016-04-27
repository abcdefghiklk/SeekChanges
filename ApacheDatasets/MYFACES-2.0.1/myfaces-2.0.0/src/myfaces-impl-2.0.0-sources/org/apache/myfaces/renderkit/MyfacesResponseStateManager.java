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
package org.apache.myfaces.renderkit;

import javax.faces.application.StateManager;
import javax.faces.context.FacesContext;
import javax.faces.render.ResponseStateManager;

/**
 * @author Manfred Geiler (latest modification by $Author: slessard $)
 * @version $Revision: 701829 $ $Date: 2008-10-05 12:06:02 -0500 (Sun, 05 Oct 2008) $
 * 
 */
public abstract class MyfacesResponseStateManager extends ResponseStateManager
{

    /**
     * Writes url parameters with the state info to be saved. {@link org.apache.myfaces.application.MyfacesStateManager}
     * delegates calls to {@link org.apache.myfaces.application.MyfacesStateManager#writeState} to this method.
     * 
     * @deprecated
     */
    @Deprecated
    public void writeStateAsUrlParams(FacesContext facescontext, StateManager.SerializedView serializedview)
    {
        throw new UnsupportedOperationException("long been deprecated...");
    }
}
