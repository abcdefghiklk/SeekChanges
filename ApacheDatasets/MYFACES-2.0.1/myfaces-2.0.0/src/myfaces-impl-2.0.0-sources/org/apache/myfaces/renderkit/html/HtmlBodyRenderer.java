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
package org.apache.myfaces.renderkit.html;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderer;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlBodyRendererBase;

/**
 * Renderer used by h:body component
 * 
 * @since 2.0
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 816435 $ $Date: 2009-09-17 21:10:12 -0500 (Thu, 17 Sep 2009) $
 */
@JSFRenderer(renderKitId = "HTML_BASIC", family = "javax.faces.Output", type = "javax.faces.Body")
public class HtmlBodyRenderer extends HtmlBodyRendererBase
{
    
}
