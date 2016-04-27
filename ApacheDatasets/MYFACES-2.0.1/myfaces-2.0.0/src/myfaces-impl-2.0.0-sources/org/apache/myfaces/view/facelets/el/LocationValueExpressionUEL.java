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
package org.apache.myfaces.view.facelets.el;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.faces.context.FacesContext;
import javax.faces.view.Location;

/**
 * LocationValueExpression for el-api 2.2
 * 
 * @author Jakob Korherr (latest modification by $Author: jakobk $)
 * @version $Revision: 928752 $ $Date: 2010-03-29 07:28:20 -0500 (Mon, 29 Mar 2010) $
 */
public class LocationValueExpressionUEL extends LocationValueExpression
{

    private static final long serialVersionUID = 1824869909994211424L;

    public LocationValueExpressionUEL(Location location, ValueExpression delegate)
    {
        super(location, delegate);
    }
    
    @Override
    public ValueReference getValueReference(ELContext context)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        CompositeComponentELUtils.saveCompositeComponentForResolver(facesContext, location);
        try
        {
            return delegate.getValueReference(context);
        }
        finally
        {
            CompositeComponentELUtils.removeCompositeComponentForResolver(facesContext);
        }
    }

}
