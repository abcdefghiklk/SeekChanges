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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.ViewHandler;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutcomeTarget;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHint;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;

import org.apache.myfaces.shared_impl.config.MyfacesConfig;
import org.apache.myfaces.shared_impl.renderkit.ClientBehaviorEvents;
import org.apache.myfaces.shared_impl.renderkit.JSFAttr;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.util.FormInfo;
import org.apache.myfaces.shared_impl.renderkit.html.util.JavascriptUtils;
import org.apache.myfaces.shared_impl.renderkit.html.util.ResourceUtils;
import org.apache.myfaces.shared_impl.util._ComponentUtils;

/**
 * @author Manfred Geiler
 * @version $Revision: 934669 $ $Date: 2010-04-15 20:59:31 -0500 (Thu, 15 Apr 2010) $
 */
public abstract class HtmlLinkRendererBase
    extends HtmlRenderer
{
    /* this one is never used
    public static final String URL_STATE_MARKER      = "JSF_URL_STATE_MARKER=DUMMY";
    public static final int    URL_STATE_MARKER_LEN  = URL_STATE_MARKER.length();
    */

    //private static final Log log = LogFactory.getLog(HtmlLinkRenderer.class);

    public boolean getRendersChildren()
    {
        // We must be able to render the children without a surrounding anchor
        // if the Link is disabled
        return true;
    }

    public void decode(FacesContext facesContext, UIComponent component)
    {
        super.decode(facesContext, component);  //check for NP

        if (component instanceof UICommand)
        {
            String clientId = component.getClientId(facesContext);
            FormInfo formInfo = findNestingForm(component, facesContext);
            if (formInfo != null)
            {
                String reqValue = (String) facesContext.getExternalContext().getRequestParameterMap().get(
                        HtmlRendererUtils.getHiddenCommandLinkFieldName(formInfo));
                if (reqValue != null && reqValue.equals(clientId)
                    || HtmlRendererUtils.isPartialOrBehaviorSubmit(facesContext, clientId)) {
                    component.queueEvent(new ActionEvent(component));

                    RendererUtils.initPartialValidationAndModelUpdate(component, facesContext);
                }
            }
            if (component instanceof ClientBehaviorHolder &&
                    !HtmlRendererUtils.isDisabled(component))
            {
                HtmlRendererUtils.decodeClientBehaviors(facesContext, component);
            }
        }
        else if (component instanceof UIOutput)
        {
            //do nothing
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + component.getClass().getName());
        }
    }


    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
    {
        super.encodeBegin(facesContext, component);  //check for NP

        if (component instanceof UICommand)
        {
            renderCommandLinkStart(facesContext, component,
                                   component.getClientId(facesContext),
                                   ((UICommand)component).getValue(),
                                   getStyle(facesContext, component),
                                   getStyleClass(facesContext, component));
        }
        else if (component instanceof UIOutcomeTarget)
        {
            renderOutcomeLinkStart(facesContext, (UIOutcomeTarget)component);
        }        
        else if (component instanceof UIOutput)
        {
            renderOutputLinkStart(facesContext, (UIOutput)component);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + component.getClass().getName());
        }
    }


    /**
     * Can be overwritten by derived classes to overrule the style to be used.
     */
    protected String getStyle(FacesContext facesContext, UIComponent link)
    {
        if (link instanceof HtmlCommandLink)
        {
            return ((HtmlCommandLink)link).getStyle();
        }

        return (String)link.getAttributes().get(HTML.STYLE_ATTR);

    }

    /**
     * Can be overwritten by derived classes to overrule the style class to be used.
     */
    protected String getStyleClass(FacesContext facesContext, UIComponent link)
    {
        if (link instanceof HtmlCommandLink)
        {
            return ((HtmlCommandLink)link).getStyleClass();
        }

        return (String)link.getAttributes().get(HTML.STYLE_CLASS_ATTR);

    }

    public void encodeChildren(FacesContext facesContext, UIComponent component) throws IOException
    {
        RendererUtils.renderChildren(facesContext, component);
    }

    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
    {
        super.encodeEnd(facesContext, component);  //check for NP

        if (component instanceof UICommand)
        {
            renderCommandLinkEnd(facesContext, component);

            FormInfo formInfo = findNestingForm(component, facesContext);
            
            if (formInfo != null)
            {
                HtmlFormRendererBase.renderScrollHiddenInputIfNecessary(
                        formInfo.getForm(), facesContext, facesContext.getResponseWriter());
            }
        }
        else if (component instanceof UIOutcomeTarget)
        {
            renderOutcomeLinkEnd(facesContext, component);
        }
        else if (component instanceof UIOutput)
        {
            renderOutputLinkEnd(facesContext, component);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + component.getClass().getName());
        }
    }

    protected void renderCommandLinkStart(FacesContext facesContext, UIComponent component,
                                          String clientId,
                                          Object value,
                                          String style,
                                          String styleClass)
            throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        Map<String, List<ClientBehavior>> behaviors = null;

        // h:commandLink can be rendered outside a form, but with warning (jsf 2.0 TCK)
        FormInfo formInfo = findNestingForm(component, facesContext);
        
        if (HtmlRendererUtils.isDisabled(component) || formInfo == null)
        {
            writer.startElement(HTML.SPAN_ELEM, component);
            if (component instanceof ClientBehaviorHolder && JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext()))
            {
                behaviors = ((ClientBehaviorHolder) component).getClientBehaviors();
                if (!behaviors.isEmpty())
                {
                    HtmlRendererUtils.writeIdAndName(writer, component, facesContext);
                }
                else
                {
                    HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
                }
                HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, component, behaviors);
                HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(facesContext, writer, component, behaviors);
                HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES_WITHOUT_EVENTS);
            }
            else
            {
                HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
                HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES);
            }
        }
        else
        {
            String[] anchorAttrsToRender;
            if (JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext()))
            {
                if (component instanceof ClientBehaviorHolder)
                {
                    behaviors = ((ClientBehaviorHolder) component).getClientBehaviors();
                    renderBehaviorizedJavaScriptAnchorStart(facesContext, writer, component, clientId, behaviors, formInfo);
                    if (!behaviors.isEmpty())
                    {
                        HtmlRendererUtils.writeIdAndName(writer, component, facesContext);
                    }
                    else
                    {
                        HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
                    }
                    HtmlRendererUtils.renderBehaviorizedEventHandlersWithoutOnclick(facesContext, writer, component, behaviors);
                    HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(facesContext, writer, component, behaviors);
                    anchorAttrsToRender = HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES_WITHOUT_STYLE_AND_EVENTS;
                }
                else
                {
                    renderJavaScriptAnchorStart(facesContext, writer, component, clientId, formInfo);
                    HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
                    anchorAttrsToRender = HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES_WITHOUT_ONCLICK_WITHOUT_STYLE;
                }
            }
            else
            {
                renderNonJavaScriptAnchorStart(facesContext, writer, component, clientId, formInfo);
                HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
                anchorAttrsToRender = HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES_WITHOUT_STYLE;
            }

            HtmlRendererUtils.renderHTMLAttributes(writer, component,
                                                   anchorAttrsToRender);
            HtmlRendererUtils.renderHTMLAttribute(writer, HTML.STYLE_ATTR, HTML.STYLE_ATTR,
                                                  style);
            HtmlRendererUtils.renderHTMLAttribute(writer, HTML.STYLE_CLASS_ATTR, HTML.STYLE_CLASS_ATTR,
                                                  styleClass);
        }

        // render value as required by JSF 1.1 renderkitdocs
        if(value != null)
        {
            writer.writeText(value.toString(), JSFAttr.VALUE_ATTR);
        }
        
        // render warning message for a h:commandLink with no nesting form
        if (formInfo == null)
        {
            writer.writeText(": This link is deactivated, because it is not embedded in a JSF form.", null);
        }
    }

    protected void renderJavaScriptAnchorStart(FacesContext facesContext,
                                               ResponseWriter writer,
                                               UIComponent component,
                                               String clientId,
                                               FormInfo formInfo)
        throws IOException
    {
        UIComponent nestingForm = formInfo.getForm();
        String formName = formInfo.getFormName();

        StringBuffer onClick = new StringBuffer();

        String commandOnclick;
        if (component instanceof HtmlCommandLink)
        {
            commandOnclick = ((HtmlCommandLink)component).getOnclick();
        }
        else
        {
            commandOnclick = (String)component.getAttributes().get(HTML.ONCLICK_ATTR);
        }
        if (commandOnclick != null)
        {
            onClick.append("var cf = function(){");
            onClick.append(commandOnclick);
            onClick.append('}');
            onClick.append(';');
            onClick.append("var oamSF = function(){");
        }

        if (RendererUtils.isAdfOrTrinidadForm(formInfo.getForm())) {
            onClick.append("submitForm('");
            onClick.append(formInfo.getForm().getClientId(facesContext));
            onClick.append("',1,{source:'");
            onClick.append(component.getClientId(facesContext));
            onClick.append("'});return false;");
        }
        else {
            HtmlRendererUtils.renderFormSubmitScript(facesContext);

            StringBuffer params = addChildParameters(component, nestingForm);

            String target = getTarget(component);

            onClick.append("return ").
                append(HtmlRendererUtils.SUBMIT_FORM_FN_NAME).append("('").
                append(formName).append("','").
                append(clientId).append("'");

            if (params.length() > 2 || target != null) {
                onClick.append(",").
                    append(target == null ? "null" : ("'" + target + "'")).append(",").
                    append(params);
            }
            onClick.append(");");

            //Not necessary since we are using oamSetHiddenInput to create input hidden fields
            //render hidden field - todo: in here for backwards compatibility
            //String hiddenFieldName = HtmlRendererUtils.getHiddenCommandLinkFieldName(formInfo);
            //addHiddenCommandParameter(facesContext, nestingForm, hiddenFieldName);

        }
        
        if (commandOnclick != null)
        {
            onClick.append('}');
            onClick.append(';');
            onClick.append("return (cf()==false)? false : oamSF();");
        }        

        writer.startElement(HTML.ANCHOR_ELEM, component);
        writer.writeURIAttribute(HTML.HREF_ATTR, "#", null);
        writer.writeAttribute(HTML.ONCLICK_ATTR, onClick.toString(), null);
    }

    
    protected void renderBehaviorizedJavaScriptAnchorStart(FacesContext facesContext,
            ResponseWriter writer,
            UIComponent component,
            String clientId,
            Map<String, List<ClientBehavior>> behaviors,
            FormInfo formInfo)
    throws IOException
    {
        String commandOnclick;
        if (component instanceof HtmlCommandLink)
        {
            commandOnclick = ((HtmlCommandLink)component).getOnclick();
        }
        else
        {
            commandOnclick = (String)component.getAttributes().get(HTML.ONCLICK_ATTR);
        }

        //Calculate the script necessary to submit form
        String serverEventCode = buildServerOnclick(facesContext, component, clientId, formInfo);
        
        String onclick = null;
        
        if (commandOnclick == null && (behaviors.isEmpty() || 
            (!behaviors.containsKey(ClientBehaviorEvents.CLICK) && 
             !behaviors.containsKey(ClientBehaviorEvents.ACTION) ) ) )
        {
            //we need to render only the submit script
            onclick = serverEventCode;
        }
        else
        {
            boolean hasSubmittingBehavior = hasSubmittingBehavior(behaviors, ClientBehaviorEvents.CLICK)
                || hasSubmittingBehavior(behaviors, ClientBehaviorEvents.ACTION);
            if (!hasSubmittingBehavior)
            {
                //Ensure required resource javascript is available
                ResourceUtils.writeScriptInline(facesContext, writer, 
                        ResourceUtils.JAVAX_FACES_LIBRARY_NAME, 
                        ResourceUtils.JSF_JS_RESOURCE_NAME);
            }
            
            //render a javascript that chain the related code
            onclick = HtmlRendererUtils.buildBehaviorChain(facesContext, component, behaviors,
                    ClientBehaviorEvents.CLICK, ClientBehaviorEvents.ACTION, 
                    commandOnclick , hasSubmittingBehavior ? null : serverEventCode,
                    HtmlRendererUtils.mapAttachedParamsToStringValues(facesContext, component));
        }
        
        writer.startElement(HTML.ANCHOR_ELEM, component);
        writer.writeURIAttribute(HTML.HREF_ATTR, "#", null);
        writer.writeAttribute(HTML.ONCLICK_ATTR, onclick, null);
    }

    private boolean hasSubmittingBehavior(Map<String, List<ClientBehavior>> clientBehaviors, String eventName)
    {
        List<ClientBehavior> eventBehaviors = clientBehaviors.get(eventName);
        if (eventBehaviors != null && !eventBehaviors.isEmpty()) {
            for (ClientBehavior behavior : eventBehaviors) {
                if (behavior.getHints().contains(ClientBehaviorHint.SUBMITTING)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected String buildServerOnclick(FacesContext facesContext, UIComponent component, 
            String clientId, FormInfo formInfo) throws IOException
    {
        UIComponent nestingForm = formInfo.getForm();
        String formName = formInfo.getFormName();

        StringBuffer onClick = new StringBuffer();

        if (RendererUtils.isAdfOrTrinidadForm(formInfo.getForm())) {
            onClick.append("submitForm('");
            onClick.append(formInfo.getForm().getClientId(facesContext));
            onClick.append("',1,{source:'");
            onClick.append(component.getClientId(facesContext));
            onClick.append("'});return false;");
        }
        else {
            HtmlRendererUtils.renderFormSubmitScript(facesContext);

            StringBuffer params = addChildParameters(component, nestingForm);

            String target = getTarget(component);

            onClick.append("return ").
                append(HtmlRendererUtils.SUBMIT_FORM_FN_NAME).append("('").
                append(formName).append("','").
                append(clientId).append("'");

            if (params.length() > 2 || target != null) {
                onClick.append(",").
                    append(target == null ? "null" : ("'" + target + "'")).append(",").
                    append(params);
            }
            onClick.append(");");

            //Not necessary since we are using oamSetHiddenInput to create input hidden fields
            //render hidden field - todo: in here for backwards compatibility
            //String hiddenFieldName = HtmlRendererUtils.getHiddenCommandLinkFieldName(formInfo);
            //addHiddenCommandParameter(facesContext, nestingForm, hiddenFieldName);

        }
        return onClick.toString();
    }

    private String getTarget(UIComponent component) {
        // for performance reason: double check for the target attribute
        String target;
        if (component instanceof HtmlCommandLink) {
            target = ((HtmlCommandLink) component).getTarget();
        }
        else {
            target = (String) component.getAttributes().get(HTML.TARGET_ATTR);
        }
        return target;
    }

    private StringBuffer addChildParameters(UIComponent component, UIComponent nestingForm) {
        //add child parameters
        StringBuffer params = new StringBuffer();
        params.append("[");
        for (Iterator it = getChildren(component).iterator(); it.hasNext();) {

            UIComponent child = (UIComponent) it.next();
            if (child instanceof UIParameter) {
                // check for the disable attribute (since 2.0)
                if (((UIParameter) child).isDisable())
                {
                    // ignore this UIParameter and continue
                    continue;
                }
                
                String name = ((UIParameter) child).getName();

                if (name == null) {
                    throw new IllegalArgumentException("Unnamed parameter value not allowed within command link.");
                }

                //Not necessary, since we are using oamSetHiddenInput to create hidden fields
                //addHiddenCommandParameter(FacesContext.getCurrentInstance(), nestingForm, name);

                Object value = ((UIParameter) child).getValue();

                //UIParameter is no ValueHolder, so no conversion possible - calling .toString on value....
                // MYFACES-1832 bad charset encoding for f:param
                // if HTMLEncoder.encode is called, then
                // when is called on writer.writeAttribute, encode method
                // is called again so we have a duplicated encode
                // call.
                //String strParamValue = value != null ? org.apache.myfaces.shared_impl.renderkit.html.util.HTMLEncoder.encode(value.toString(), false, false) : "";
                String strParamValue = value != null ? value.toString() : "";

                if (params.length() > 1) {
                    params.append(",");
                }

                params.append("['");
                params.append(name);
                params.append("','");
                params.append(strParamValue);
                params.append("']");
            }
        }
        params.append("]");
        return params;
    }

    /**
     * find nesting form<br />
     * need to be overrideable to deal with dummyForm stuff in tomahawk.
     */
    protected FormInfo findNestingForm(UIComponent uiComponent, FacesContext facesContext)
    {
        return _ComponentUtils.findNestingForm(uiComponent, facesContext);
    }

    protected void addHiddenCommandParameter(FacesContext facesContext, UIComponent nestingForm, String hiddenFieldName)
    {
        if (nestingForm != null)
        {
            HtmlFormRendererBase.addHiddenCommandParameter(facesContext, nestingForm, hiddenFieldName);
        }
    }


    protected void renderNonJavaScriptAnchorStart(FacesContext facesContext,
                                                  ResponseWriter writer,
                                                  UIComponent component,
                                                  String clientId,
                                                  FormInfo formInfo)
        throws IOException
    {
        ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
        String viewId = facesContext.getViewRoot().getViewId();
        String path = viewHandler.getActionURL(facesContext, viewId);

        boolean strictXhtmlLinks
                = MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isStrictXhtmlLinks();

        StringBuffer hrefBuf = new StringBuffer(path);

        //add clientId parameter for decode

        if (path.indexOf('?') == -1)
        {
            hrefBuf.append('?');
        }
        else
        {
            if (strictXhtmlLinks) {
                hrefBuf.append("&amp;");
            } else {
                hrefBuf.append('&');
            }
        }
        String hiddenFieldName = HtmlRendererUtils.getHiddenCommandLinkFieldName(formInfo);
        hrefBuf.append(hiddenFieldName);
        hrefBuf.append('=');
        hrefBuf.append(clientId);

        if (getChildCount(component) > 0)
        {
            addChildParametersToHref(facesContext, component, hrefBuf,
                                     false, //not the first url parameter
                                     writer.getCharacterEncoding());
        }

        String href = facesContext.getExternalContext().encodeActionURL(hrefBuf.toString());
        writer.startElement(HTML.ANCHOR_ELEM, component);
        writer.writeURIAttribute(HTML.HREF_ATTR,
                                 facesContext.getExternalContext().encodeActionURL(href),
                                 null);
    }

    private void addChildParametersToHref(FacesContext facesContext,
                                          UIComponent linkComponent,
                                          StringBuffer hrefBuf,
                                          boolean firstParameter,
                                          String charEncoding)
            throws IOException
    {
        boolean strictXhtmlLinks
                = MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isStrictXhtmlLinks();
        for (Iterator it = getChildren(linkComponent).iterator(); it.hasNext(); )
        {
            UIComponent child = (UIComponent)it.next();
            if (child instanceof UIParameter)
            {
                // check for the disable attribute (since 2.0)
                if (((UIParameter) child).isDisable())
                {
                    // ignore this UIParameter and continue
                    continue;
                }
                String name = ((UIParameter)child).getName();
                Object value = ((UIParameter)child).getValue();
                addParameterToHref(name, value, hrefBuf, firstParameter, charEncoding, strictXhtmlLinks);
                firstParameter = false;
            }
        }
    }

    protected void renderOutputLinkStart(FacesContext facesContext, UIOutput output)
            throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        Map<String, List<ClientBehavior>> behaviors = null;

        if (HtmlRendererUtils.isDisabled(output))
        {
            writer.startElement(HTML.SPAN_ELEM, output);
            if (output instanceof ClientBehaviorHolder && JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext()))
            {
                behaviors = ((ClientBehaviorHolder) output).getClientBehaviors();
                if (!behaviors.isEmpty())
                {
                    HtmlRendererUtils.writeIdAndName(writer, output, facesContext);
                }
                else
                {
                    HtmlRendererUtils.writeIdIfNecessary(writer, output, facesContext);
                }
                HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, output, behaviors);
                HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(facesContext, writer, output, behaviors);
                HtmlRendererUtils.renderHTMLAttributes(writer, output, HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES_WITHOUT_EVENTS);
            }
            else
            {
                HtmlRendererUtils.writeIdIfNecessary(writer, output, facesContext);
                HtmlRendererUtils.renderHTMLAttributes(writer, output, HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES);
            }
        }
        else
        { 
            //calculate href
            String href = org.apache.myfaces.shared_impl.renderkit.RendererUtils.getStringValue(facesContext, output);
            
            //check if there is an anchor # in it
            int index = href.indexOf('#');
            String anchorString = null;
            boolean isAnchorInHref = (index > -1);
            if (isAnchorInHref)
            {
                // remove anchor element and add it again after the parameter are encoded
                anchorString = href.substring(index,href.length());
                href = href.substring(0,index);
            }
            if (getChildCount(output) > 0)
            {
                StringBuffer hrefBuf = new StringBuffer(href);
                addChildParametersToHref(facesContext, output, hrefBuf,
                                     (href.indexOf('?') == -1), //first url parameter?
                                     writer.getCharacterEncoding());
                href = hrefBuf.toString();
            }
            // check for the fragement attribute
            String fragmentAttr = null;
            if (output instanceof HtmlOutputLink)
            {
                fragmentAttr = ((HtmlOutputLink) output).getFragment();
            }
            else
            {
                fragmentAttr = (String) output.getAttributes().get(JSFAttr.FRAGMENT_ATTR);
            }
            if (fragmentAttr != null && !"".equals(fragmentAttr)) 
            {
                href += "#" + fragmentAttr;
            }
            else if (isAnchorInHref)
            {
                href += anchorString;
            }
            href = facesContext.getExternalContext().encodeResourceURL(href);    //TODO: or encodeActionURL ?

            //write anchor
            writer.startElement(HTML.ANCHOR_ELEM, output);
            writer.writeURIAttribute(HTML.HREF_ATTR, href, null);
            if (output instanceof ClientBehaviorHolder && JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext()))
            {
                behaviors = ((ClientBehaviorHolder) output).getClientBehaviors();
                if (!behaviors.isEmpty())
                {
                    HtmlRendererUtils.writeIdAndName(writer, output, facesContext);
                }
                else
                {
                    HtmlRendererUtils.writeIdAndNameIfNecessary(writer, output, facesContext);
                }
                HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, output, behaviors);
                HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(facesContext, writer, output, behaviors);
                HtmlRendererUtils.renderHTMLAttributes(writer, output, HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES_WITHOUT_EVENTS);
            }
            else
            {
                HtmlRendererUtils.writeIdAndNameIfNecessary(writer, output, facesContext);
                HtmlRendererUtils.renderHTMLAttributes(writer, output, HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES);
            }
            writer.flush();
        }
    }
    
    protected void renderOutcomeLinkStart(FacesContext facesContext, UIOutcomeTarget output)
            throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        Map<String, List<ClientBehavior>> behaviors = null;
        
        //calculate href
        String targetHref = HtmlRendererUtils.getOutcomeTargetLinkHref (facesContext, output);
        
        if (HtmlRendererUtils.isDisabled(output) || targetHref == null)
        {
            writer.startElement(HTML.SPAN_ELEM, output);
            if (output instanceof ClientBehaviorHolder && JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext()))
            {
                behaviors = ((ClientBehaviorHolder) output).getClientBehaviors();
                if (!behaviors.isEmpty())
                {
                    HtmlRendererUtils.writeIdAndName(writer, output, facesContext);
                }
                else
                {
                    HtmlRendererUtils.writeIdIfNecessary(writer, output, facesContext);
                }
                HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, output, behaviors);
                HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(facesContext, writer, output, behaviors);
                HtmlRendererUtils.renderHTMLAttributes(writer, output, HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES_WITHOUT_EVENTS);
            }
            else
            {
                HtmlRendererUtils.writeIdIfNecessary(writer, output, facesContext);
                HtmlRendererUtils.renderHTMLAttributes(writer, output, HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES);
            }

            Object value = output.getValue();

            if(value != null)
            {
                writer.writeText(value.toString(), JSFAttr.VALUE_ATTR);
            }
        }
        else
        {
            //write anchor
            writer.startElement(HTML.ANCHOR_ELEM, output);
            writer.writeURIAttribute(HTML.HREF_ATTR, targetHref, null);
            if (output instanceof ClientBehaviorHolder && JavascriptUtils.isJavascriptAllowed(facesContext.getExternalContext()))
            {
                behaviors = ((ClientBehaviorHolder) output).getClientBehaviors();
                if (!behaviors.isEmpty())
                {
                    HtmlRendererUtils.writeIdAndName(writer, output, facesContext);
                }
                else
                {
                    HtmlRendererUtils.writeIdAndNameIfNecessary(writer, output, facesContext);
                }
                HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, output, behaviors);
                HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(facesContext, writer, output, behaviors);
                HtmlRendererUtils.renderHTMLAttributes(writer, output, HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES_WITHOUT_EVENTS);
            }
            else
            {
                HtmlRendererUtils.writeIdAndNameIfNecessary(writer, output, facesContext);
                HtmlRendererUtils.renderHTMLAttributes(writer, output, HTML.ANCHOR_PASSTHROUGH_ATTRIBUTES);
            }

            writer.flush();
        }
    }
    

    private void renderLinkParameter(String name,
                                     Object value,
                                     StringBuffer onClick,
                                     String jsForm,
                                     UIComponent nestingForm)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Unnamed parameter value not allowed within command link.");
        }
        onClick.append(jsForm);
        onClick.append(".elements['").append(name).append("']");
        //UIParameter is no ValueHolder, so no conversion possible
        String strParamValue = value != null ? org.apache.myfaces.shared_impl.renderkit.html.util.HTMLEncoder.encode(value.toString(), false, false) : "";
        onClick.append(".value='").append(strParamValue).append("';");

        addHiddenCommandParameter(FacesContext.getCurrentInstance(), nestingForm, name);
    }

    private static void addParameterToHref(String name,
                                           Object value,
                                           StringBuffer hrefBuf,
                                           boolean firstParameter,
                                           String charEncoding,
                                           boolean strictXhtmlLinks) throws UnsupportedEncodingException {
        if (name == null) {
            throw new IllegalArgumentException("Unnamed parameter value not allowed within command link.");
        }

        if (firstParameter) {
            hrefBuf.append('?');
        } else {
            if (strictXhtmlLinks) {
                hrefBuf.append("&amp;");
            } else {
                hrefBuf.append('&');
            }
        }

        hrefBuf.append(URLEncoder.encode(name, charEncoding));
        hrefBuf.append('=');
        if (value != null)
        {
            //UIParameter is no ConvertibleValueHolder, so no conversion possible
            hrefBuf.append(URLEncoder.encode(value.toString(), charEncoding));
        }
    }

    protected void renderOutcomeLinkEnd(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        
        if (HtmlRendererUtils.isDisabled(component))
        {
            writer.endElement(HTML.SPAN_ELEM);
        }
        else
        {
            writer.writeText (org.apache.myfaces.shared_impl.renderkit.RendererUtils.getStringValue
                 (facesContext, component), null);
            writer.endElement(HTML.ANCHOR_ELEM);
        }
    }
    
    protected void renderOutputLinkEnd(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();

        if (HtmlRendererUtils.isDisabled(component))
        {
            writer.endElement(HTML.SPAN_ELEM);
        }
        else
        {
            // force separate end tag
            writer.writeText("", null);
            writer.endElement(HTML.ANCHOR_ELEM);
        }
    }

    protected void renderCommandLinkEnd(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        FormInfo formInfo = findNestingForm(component, facesContext);
        
        ResponseWriter writer = facesContext.getResponseWriter();
        if (HtmlRendererUtils.isDisabled(component) || formInfo == null)
        {

            writer.endElement(HTML.SPAN_ELEM);
        }
        else
        {
            writer.writeText("", null);
            writer.endElement(HTML.ANCHOR_ELEM);
        }
    }
}
