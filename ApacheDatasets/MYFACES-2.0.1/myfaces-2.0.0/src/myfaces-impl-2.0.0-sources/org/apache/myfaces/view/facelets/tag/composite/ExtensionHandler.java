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
package org.apache.myfaces.view.facelets.tag.composite;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;

/**
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 881558 $ $Date: 2009-11-17 16:55:58 -0500 (Tue, 17 Nov 2009) $
 */
@JSFFaceletTag(name="composite:extension")
public class ExtensionHandler extends TagHandler
{

    //private static final Log log = LogFactory.getLog(ExtensionHandler.class);
    private static final Logger log = Logger.getLogger(ExtensionHandler.class.getName());
    
    public ExtensionHandler(TagConfig config)
    {
        super(config);
    }

    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException
    {
        // TODO: In theory the xml data inside this tag should be saved,
        // but the spec does not say where and how this should be done.
        // For now we just prevent execute any handler inside this tag.
        // As soon JSR-276 is available, some behavior for this tag
        // should be added.
        CompositeComponentBeanInfo beanInfo = 
            (CompositeComponentBeanInfo) parent.getAttributes()
            .get(UIComponent.BEANINFO_KEY);
        
        if (beanInfo == null)
        {
            if (log.isLoggable(Level.SEVERE))
            {
                log.severe("Cannot found composite bean descriptor UIComponent.BEANINFO_KEY ");
            }
            return;
        }
        
        //BeanDescriptor beanDescriptor = beanInfo.getBeanDescriptor();
    }

}
