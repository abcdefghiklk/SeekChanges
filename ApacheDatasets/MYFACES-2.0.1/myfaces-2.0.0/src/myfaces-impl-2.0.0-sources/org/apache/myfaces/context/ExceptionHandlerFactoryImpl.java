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
package org.apache.myfaces.context;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;

import org.apache.myfaces.shared_impl.context.ExceptionHandlerImpl;

/**
 * DOCUMENT ME!
 * 
 * @author Leonardo Uribe (latest modification by $Author: sobryan $)
 * @version $Revision: 929113 $ $Date: 2010-03-30 07:45:12 -0500 (Tue, 30 Mar 2010) $
 * 
 * @since 2.0
 */
public class ExceptionHandlerFactoryImpl extends ExceptionHandlerFactory
{

    @Override
    public ExceptionHandler getExceptionHandler()
    {
        return new ExceptionHandlerImpl();
    }
}
