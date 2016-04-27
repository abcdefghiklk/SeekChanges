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
package org.apache.myfaces.view.facelets.tag.jsf.core;

import javax.faces.component.UIParameter;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectItems;
import javax.faces.component.UIViewParameter;
import javax.faces.convert.DateTimeConverter;
import javax.faces.convert.NumberConverter;
import javax.faces.validator.BeanValidator;
import javax.faces.validator.DoubleRangeValidator;
import javax.faces.validator.LengthValidator;
import javax.faces.validator.LongRangeValidator;
import javax.faces.validator.RegexValidator;
import javax.faces.validator.RequiredValidator;

import org.apache.myfaces.view.facelets.tag.AbstractTagLibrary;

/**
 * For Tag details, see JSF Core <a target="_new"
 * href="http://java.sun.com/j2ee/javaserverfaces/1.1_01/docs/tlddocs/f/tld-summary.html">taglib documentation</a>.
 *
 * @author Jacob Hookom
 * @version $Id: CoreLibrary.java,v 1.13 2008/07/13 19:01:44 rlubke Exp $
 */
public final class CoreLibrary extends AbstractTagLibrary
{

    public final static String Namespace = "http://java.sun.com/jsf/core";

    public final static CoreLibrary Instance = new CoreLibrary();

    public CoreLibrary()
    {
        super(Namespace);

        this.addTagHandler("actionListener", ActionListenerHandler.class);

        this.addTagHandler("ajax", AjaxHandler.class);
        
        this.addTagHandler("attribute", AttributeHandler.class);

        this.addConverter("convertDateTime", DateTimeConverter.CONVERTER_ID, ConvertDateTimeHandler.class);

        this.addConverter("convertNumber", NumberConverter.CONVERTER_ID, ConvertNumberHandler.class);

        this.addConverter("converter", null, ConvertDelegateHandler.class);
        
        this.addTagHandler ("event", EventHandler.class);
        
        this.addTagHandler("facet", FacetHandler.class);

        this.addTagHandler("loadBundle", LoadBundleHandler.class);

        this.addTagHandler("metadata", ViewMetadataHandler.class);
        
        this.addComponent("param", UIParameter.COMPONENT_TYPE, null);

        this.addTagHandler("phaseListener", PhaseListenerHandler.class);

        this.addComponent("selectItem", UISelectItem.COMPONENT_TYPE, null, SelectItemHandler.class);

        this.addComponent("selectItems", UISelectItems.COMPONENT_TYPE, null);

        this.addTagHandler("setPropertyActionListener", SetPropertyActionListenerHandler.class);

        this.addComponent("subview", "javax.faces.NamingContainer", null);

        this.addValidator("validateBean", BeanValidator.VALIDATOR_ID);
        
        this.addValidator("validateLength", LengthValidator.VALIDATOR_ID);

        this.addValidator("validateLongRange", LongRangeValidator.VALIDATOR_ID);

        this.addValidator("validateDoubleRange", DoubleRangeValidator.VALIDATOR_ID);

        this.addValidator("validateRegex", RegexValidator.VALIDATOR_ID);
        
        this.addValidator("validateRequired", RequiredValidator.VALIDATOR_ID);

        this.addValidator("validator", null, ValidateDelegateHandler.class);

        this.addTagHandler("valueChangeListener", ValueChangeListenerHandler.class);

        this.addTagHandler("view", ViewHandler.class);
        
        this.addComponent("viewParam", UIViewParameter.COMPONENT_TYPE, null);

        this.addComponent("verbatim", "javax.faces.HtmlOutputText", "javax.faces.Text", VerbatimHandler.class);
    }
}
