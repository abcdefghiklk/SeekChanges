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
package org.apache.myfaces.view.facelets.tag.jstl.core;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;

/**
 * Subtag of &lt;choose&gt; that includes its body if its
 * condition evalutes to 'true'
 * 
 * @author Jacob Hookom
 * @version $Id: ChooseWhenHandler.java,v 1.3 2008/07/13 19:01:43 rlubke Exp $
 */
@JSFFaceletTag(name="c:when")
public final class ChooseWhenHandler extends TagHandler
{

    /**
     * The test condition that determines whether or not the
     * body content should be processed. 
     */
    @JSFFaceletAttribute(className="boolean",required=true)
    private final TagAttribute test;

    public ChooseWhenHandler(TagConfig config)
    {
        super(config);
        this.test = this.getRequiredAttribute("test");
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException
    {
        this.nextHandler.apply(ctx, parent);
    }

    public boolean isTestTrue(FaceletContext ctx)
    {
        return this.test.getBoolean(ctx);
    }
}
