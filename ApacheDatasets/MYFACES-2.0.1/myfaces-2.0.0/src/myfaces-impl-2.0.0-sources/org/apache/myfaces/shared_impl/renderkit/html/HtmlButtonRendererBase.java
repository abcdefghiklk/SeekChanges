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
package org.apache.myfaces.shared_impl.renderkit.html;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.ValueHolder;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlInputHidden;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;
import javax.faces.application.ResourceHandler;

import org.apache.myfaces.shared_impl.config.MyfacesConfig;
import org.apache.myfaces.shared_impl.renderkit.ClientBehaviorEvents;
import org.apache.myfaces.shared_impl.renderkit.JSFAttr;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.util.FormInfo;
import org.apache.myfaces.shared_impl.renderkit.html.util.JavascriptUtils;

/**
 * @author Manfred Geiler (latest modification by $Author: bommel $)
 * @author Thomas Spiegl
 * @author Anton Koinov
 * @version $Revision: 916249 $ $Date: 2010-02-25 06:05:12 -0500 (Thu, 25 Feb 2010) $
 */
public class HtmlButtonRendererBase
    extends HtmlRenderer
{
    private static final String IMAGE_BUTTON_SUFFIX_X = ".x";
    private static final String IMAGE_BUTTON_SUFFIX_Y = ".y";

    public static final String ACTION_FOR_LIST = "org.apache.myfaces.ActionForList";

    public void decode(FacesContext facesContext, UIComponent uiComponent)
    {
        org.apache.myfaces.shared_impl.renderkit.RendererUtils.checkParamValidity(facesContext, uiComponent, UICommand.class);

        //super.decode must not be called, because value is handled here
        if (!isReset(uiComponent) && isSubmitted(facesContext, uiComponent))
        {
            uiComponent.queueEvent(new ActionEvent(uiComponent));

            org.apache.myfaces.shared_impl.renderkit.RendererUtils.initPartialValidationAndModelUpdate(uiComponent, facesContext);
            
            if (uiComponent instanceof ClientBehaviorHolder &&
                    !HtmlRendererUtils.isDisabled(uiComponent))
            {
                HtmlRendererUtils.decodeClientBehaviors(facesContext, uiComponent);
            }
        }
    }

    private static boolean isReset(UIComponent uiComponent)
    {
        return "reset".equals((String) uiComponent.getAttributes().get(HTML.TYPE_ATTR));
    }
    
    private static boolean isButton(UIComponent uiComponent)
    {
        return "button".equals((String) uiComponent.getAttributes().get(HTML.TYPE_ATTR));
    }

    private static boolean isSubmitted(FacesContext facesContext, UIComponent uiComponent)
    {
        String clientId = uiComponent.getClientId(facesContext);
        Map paramMap = facesContext.getExternalContext().getRequestParameterMap();
        return paramMap.containsKey(clientId) || paramMap.containsKey(clientId + IMAGE_BUTTON_SUFFIX_X) 
            || paramMap.containsKey(clientId + IMAGE_BUTTON_SUFFIX_Y)
            || HtmlRendererUtils.isPartialOrBehaviorSubmit(facesContext, clientId);
    }

    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent)
            throws IOException
    {
        org.apache.myfaces.shared_impl.renderkit.RendererUtils.checkParamValidity(facesContext, uiComponent, UICommand.class);

        String clientId = uiComponent.getClientId(facesContext);

        ResponseWriter writer = facesContext.getResponseWriter();
        
        // commandButton does not need to be nested in a form since JSF 2.0
        FormInfo formInfo = findNestingForm(uiComponent, facesContext);
        
        // If we are nested in a form, and we have javascript enabled, and autoscroll is enabled, 
        // we should write the form submit script
        // (define oamSetHiddenInput, oamClearHiddenInput, oamSubmitForm)
        // because oamSetHiddenInput is called on onclick function
        if (formInfo != null && JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext())
                && MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isAutoScroll())
        {        
            HtmlRendererUtils.renderFormSubmitScript(facesContext);
        }

        writer.startElement(HTML.INPUT_ELEM, uiComponent);

        writer.writeAttribute(HTML.ID_ATTR, clientId, org.apache.myfaces.shared_impl.renderkit.JSFAttr.ID_ATTR);
        writer.writeAttribute(HTML.NAME_ATTR, clientId, JSFAttr.ID_ATTR);

        

        ExternalContext externalContext = facesContext.getExternalContext();

        String image = RendererUtils.getIconSrc(facesContext, uiComponent, JSFAttr.IMAGE_ATTR);
        if (image != null)
        {
            writer.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_IMAGE, org.apache.myfaces.shared_impl.renderkit.JSFAttr.TYPE_ATTR);
            writer.writeURIAttribute(HTML.SRC_ATTR, image, org.apache.myfaces.shared_impl.renderkit.JSFAttr.IMAGE_ATTR);
        }
        else
        {
            String type = getType(uiComponent);

            if (type == null || (!isReset(uiComponent) && !isButton(uiComponent)))
            {
                type = HTML.INPUT_TYPE_SUBMIT;
            }
            writer.writeAttribute(HTML.TYPE_ATTR, type, org.apache.myfaces.shared_impl.renderkit.JSFAttr.TYPE_ATTR);
            Object value = getValue(uiComponent);
            if (value != null)
            {
                writer.writeAttribute(org.apache.myfaces.shared_impl.renderkit.html.HTML.VALUE_ATTR, value, org.apache.myfaces.shared_impl.renderkit.JSFAttr.VALUE_ATTR);
            }
        }
        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
        }
        
        if (JavascriptUtils.isJavascriptAllowed(externalContext) &&
            (HtmlRendererUtils.hasClientBehavior(ClientBehaviorEvents.CLICK, behaviors, facesContext) ||
             HtmlRendererUtils.hasClientBehavior(ClientBehaviorEvents.ACTION, behaviors, facesContext)))
        {
            //TODO add the behavior attched rendering here
            String onClick = buildBehaviorizedOnClick(uiComponent, behaviors, facesContext, writer, formInfo);
            if (onClick.length() != 0) {
                writer.writeAttribute(HTML.ONCLICK_ATTR, onClick.toString(), null);
            }
            
            Map<String, Object> attributes = uiComponent.getAttributes(); 
            
            HtmlRendererUtils.buildBehaviorChain(
                    facesContext, uiComponent, behaviors, ClientBehaviorEvents.DBLCLICK,   
                        (String) attributes.get(HTML.ONDBLCLICK_ATTR), "",null);
            
            HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                                                   HTML.BUTTON_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
        }
        //fallback into the pre 2.0 code to keep backwards comptability with libraries which rely on internals
        else if (JavascriptUtils.isJavascriptAllowed(externalContext)) {
            StringBuffer onClick = buildOnClick(uiComponent, facesContext, writer);
            if (onClick.length() != 0) {
                writer.writeAttribute(HTML.ONCLICK_ATTR, onClick.toString(), null);
            }
            HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                                                   HTML.BUTTON_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
        } else {
            HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                                                   HTML.BUTTON_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
        }
        
        HtmlRendererUtils.renderBehaviorizedEventHandlersWithoutOnclick(facesContext, writer, uiComponent, behaviors);
        HtmlRendererUtils.renderBehaviorizedFieldEventHandlers(facesContext, writer, uiComponent, behaviors);

        if (isDisabled(facesContext, uiComponent))
        {
            writer.writeAttribute(HTML.DISABLED_ATTR, Boolean.TRUE, org.apache.myfaces.shared_impl.renderkit.JSFAttr.DISABLED_ATTR);
        }
        
        if (isReadonly(facesContext, uiComponent))
        {
            writer.writeAttribute(HTML.READONLY_ATTR, Boolean.TRUE, org.apache.myfaces.shared_impl.renderkit.JSFAttr.READONLY_ATTR);
        }

        writer.endElement(HTML.INPUT_ELEM);
        
        if (formInfo != null)
        {
            HtmlFormRendererBase.renderScrollHiddenInputIfNecessary(formInfo.getForm(), facesContext, writer);
        }
        
        // render the UIParameter children of the commandButton (since 2.0)
        for (UIComponent child : uiComponent.getChildren())
        {
            if (child.getClass().equals(UIParameter.class))
            {
                UIParameter parameter = (UIParameter) child;
                // check for the disable attribute
                if (parameter.isDisable())
                {
                    continue;
                }
                HtmlInputHidden parameterComponent = new HtmlInputHidden();
                parameterComponent.setId(parameter.getName());
                parameterComponent.setValue(parameter.getValue());
                parameterComponent.encodeAll(facesContext);
            }
        }
    }

    protected String buildBehaviorizedOnClick(UIComponent uiComponent, Map<String, List<ClientBehavior>> behaviors, 
                                              FacesContext facesContext, ResponseWriter writer, FormInfo nestedFormInfo) {
        //TODO fetch parameters from the button

        //we can omit autoscroll here for now maybe we should check if it is an ajax behavior and omit it only in this case
        StringBuilder userOnClick = new StringBuilder();
        //user onclick part 
        String commandOnClick = (String) uiComponent.getAttributes().get(HTML.ONCLICK_ATTR);

        if (commandOnClick != null) {
            userOnClick.append(commandOnClick);
            userOnClick.append(';');
        }

        StringBuffer rendererOnClick = new StringBuffer();
        
        if (nestedFormInfo != null) 
        {
            String formName = nestedFormInfo.getFormName();
            if (JavascriptUtils.isRenderClearJavascriptOnButton(facesContext.getExternalContext())) {
                //call the script to clear the form (clearFormHiddenParams_<formName>) method
                HtmlRendererUtils.appendClearHiddenCommandFormParamsFunctionCall(rendererOnClick, formName);
            }
    
            if (MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isAutoScroll()) {
                HtmlRendererUtils.appendAutoScrollAssignment(rendererOnClick, formName);
            }
        }

        //TODO make parameter resolution here

        //according to the specification in jsf.util.chain jdocs and the spec document we have to use
        //jsf.util.chain to chain the functions and
        return HtmlRendererUtils.buildBehaviorChain(facesContext, uiComponent, behaviors,
                ClientBehaviorEvents.CLICK, ClientBehaviorEvents.ACTION, 
                userOnClick.toString() , rendererOnClick.toString(),
                HtmlRendererUtils.mapAttachedParamsToStringValues(facesContext, uiComponent));

    }

    protected StringBuffer buildOnClick(UIComponent uiComponent, FacesContext facesContext,
                                        ResponseWriter writer)
        throws IOException
    {
        /* DUMMY STUFF
        //Find form
        UIComponent parent = uiComponent.getParent();
        while (parent != null && !(parent instanceof UIForm))
        {
            parent = parent.getParent();
        }

        UIForm nestingForm = null;
        String formName;

        if (parent != null)
        {
            //link is nested inside a form
            nestingForm = (UIForm)parent;
            formName = nestingForm.getClientId(facesContext);

        }
        else
        {
            //not nested in form, we must add a dummy form at the end of the document
            formName = DummyFormUtils.DUMMY_FORM_NAME;
            //dummyFormResponseWriter = DummyFormUtils.getDummyFormResponseWriter(facesContext);
            //dummyFormResponseWriter.setWriteDummyForm(true);
            DummyFormUtils.setWriteDummyForm(facesContext, true);
        }
        */
        StringBuffer onClick = new StringBuffer();
        String commandOnClick = (String) uiComponent.getAttributes().get(HTML.ONCLICK_ATTR);

        if (commandOnClick != null)
        {
            onClick.append(commandOnClick);
            onClick.append(';');
        }
        
        FormInfo nestedFormInfo = findNestingForm(uiComponent, facesContext);
        
        if (nestedFormInfo != null)
        {
            String formName = nestedFormInfo.getFormName();
    
            if (JavascriptUtils.isRenderClearJavascriptOnButton(facesContext.getExternalContext()))
            {
                //call the script to clear the form (clearFormHiddenParams_<formName>) method
                HtmlRendererUtils.appendClearHiddenCommandFormParamsFunctionCall(onClick, formName);
            }
    
            if (MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isAutoScroll()) {
                HtmlRendererUtils.appendAutoScrollAssignment(onClick, formName);
            }
        }

        //The hidden field has only sense if isRenderClearJavascriptOnButton is
        //set to true. In other case, this hidden field should not be rendered.
        //if (JavascriptUtils.isRenderClearJavascriptOnButton(facesContext.getExternalContext()))
        //{
            //add hidden field for the case there is no commandLink in the form
            //String hiddenFieldName = HtmlRendererUtils.getHiddenCommandLinkFieldName(formInfo);
            //addHiddenCommandParameter(facesContext, nestingForm, hiddenFieldName);
        //}

        return onClick;
    }

    protected void addHiddenCommandParameter(FacesContext facesContext, UIComponent nestingForm, String hiddenFieldName)
    {
        if (nestingForm != null)
        {
            HtmlFormRendererBase.addHiddenCommandParameter(facesContext, nestingForm, hiddenFieldName);
        }
    }

    /**
     * find nesting form<br />
     * need to be overrideable to deal with dummyForm stuff in tomahawk.
     */
    protected FormInfo findNestingForm(UIComponent uiComponent, FacesContext facesContext)
    {
        return RendererUtils.findNestingForm(uiComponent, facesContext);
    }

    protected boolean isDisabled(FacesContext facesContext, UIComponent uiComponent)
    {
        //TODO: overwrite in extended HtmlButtonRenderer and check for enabledOnUserRole
        if (uiComponent instanceof HtmlCommandButton)
        {
            return ((HtmlCommandButton)uiComponent).isDisabled();
        }

        return org.apache.myfaces.shared_impl.renderkit.RendererUtils.getBooleanAttribute(uiComponent, HTML.DISABLED_ATTR, false);
        
    }

    protected boolean isReadonly(FacesContext facesContext, UIComponent uiComponent)
    {
        if (uiComponent instanceof HtmlCommandButton)
        {
            return ((HtmlCommandButton)uiComponent).isReadonly();
        }
        return org.apache.myfaces.shared_impl.renderkit.RendererUtils.getBooleanAttribute(uiComponent, HTML.READONLY_ATTR, false);
    }

    private String getImage(UIComponent uiComponent)
    {
        if (uiComponent instanceof HtmlCommandButton)
        {
            return ((HtmlCommandButton)uiComponent).getImage();
        }
        return (String)uiComponent.getAttributes().get(JSFAttr.IMAGE_ATTR);
    }

    private String getType(UIComponent uiComponent)
    {
        if (uiComponent instanceof HtmlCommandButton)
        {
            return ((HtmlCommandButton)uiComponent).getType();
        }
        return (String)uiComponent.getAttributes().get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.TYPE_ATTR);
    }

    private Object getValue(UIComponent uiComponent)
    {
        if (uiComponent instanceof ValueHolder)
        {
            return ((ValueHolder)uiComponent).getValue();
        }
        return uiComponent.getAttributes().get(JSFAttr.VALUE_ATTR);
    }
}
