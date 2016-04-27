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
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIMessages;
import javax.faces.component.html.HtmlMessages;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared_impl.renderkit.JSFAttr;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.util.NullIterator;

/**
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @version $Revision: 824859 $ $Date: 2009-10-13 12:42:36 -0500 (Tue, 13 Oct 2009) $
 */
public abstract class HtmlMessagesRendererBase
        extends HtmlMessageRendererBase
{
    //private static final Log log = LogFactory.getLog(HtmlMessagesRendererBase.class);
    private static final Logger log = Logger.getLogger(HtmlMessagesRendererBase.class.getName());

    protected static final String LAYOUT_LIST  = "list";
    protected static final String LAYOUT_TABLE = "table";


    protected void renderMessages(FacesContext facesContext,
                                  UIComponent messages)
            throws IOException
    {
        // check the for attribute
        String forAttr = getFor(messages);
        UIComponent forComponent = null;
        if(forAttr != null && !"".equals(forAttr))
        {
            forComponent = messages.findComponent(forAttr);
            if (forComponent == null)
            {
                log.severe("Could not render Messages. Unable to find component '" + forAttr + "' (calling findComponent on component '" + messages.getClientId(facesContext) + "'). If the provided id was correct, wrap the message and its component into an h:panelGroup or h:panelGrid.");
                return;
            }
        }
        
        MessagesIterator messagesIterator = new MessagesIterator(facesContext,
                isGlobalOnly(messages), isRedisplay(messages), forComponent);

        if (messagesIterator.hasNext())
        {
            String layout = getLayout(messages);
            if (layout == null)
            {
                if (log.isLoggable(Level.FINE))
                {
                    log.fine("No messages layout given, using default layout 'list'.");
                }
                renderList(facesContext, messages, messagesIterator);
            }
            else if (layout.equalsIgnoreCase(LAYOUT_TABLE))
            {
                renderTable(facesContext, messages, messagesIterator);
            }
            else
            {
                if (log.isLoggable(Level.WARNING) && !layout.equalsIgnoreCase(LAYOUT_LIST))
                {
                    log.warning("Unsupported messages layout '" + layout + "' - using default layout 'list'.");
                }
                renderList(facesContext, messages, messagesIterator);
            }
        }
    }


    private void renderList(FacesContext facesContext,
                            UIComponent messages,
                            MessagesIterator messagesIterator)
            throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();

        writer.startElement(HTML.UL_ELEM, messages);
        HtmlRendererUtils.writeIdIfNecessary(writer, messages, facesContext);
        HtmlRendererUtils.renderHTMLAttributes(writer, messages, HTML.UNIVERSAL_ATTRIBUTES_WITHOUT_STYLE);

        HtmlRendererUtils.renderHTMLAttribute(writer, HTML.STYLE_ATTR, HTML.STYLE_ATTR, getComponentStyle(messages));
        HtmlRendererUtils.renderHTMLAttribute(writer, HTML.STYLE_CLASS_ATTR, HTML.STYLE_CLASS_ATTR, getComponentStyleClass(messages));

        while(messagesIterator.hasNext())
        {
            writer.startElement(org.apache.myfaces.shared_impl.renderkit.html.HTML.LI_ELEM, messages);
            
            FacesMessage facesMessage = (FacesMessage)messagesIterator.next();
            // determine style and style class
            String[] styleAndClass = getStyleAndStyleClass(messages, facesMessage.getSeverity());
            String style = styleAndClass[0];
            String styleClass = styleAndClass[1];
            
            HtmlRendererUtils.renderHTMLAttribute(writer, HTML.STYLE_ATTR, HTML.STYLE_ATTR, style);
            HtmlRendererUtils.renderHTMLAttribute(writer, HTML.STYLE_CLASS_ATTR, HTML.STYLE_CLASS_ATTR, styleClass);
            
            renderSingleFacesMessage(facesContext,
                    messages,
                    facesMessage,
                    messagesIterator.getClientId(),false,false);
            writer.endElement(HTML.LI_ELEM);
        }

        writer.endElement(HTML.UL_ELEM);
    }


    private void renderTable(FacesContext facesContext,
                             UIComponent messages,
                             MessagesIterator messagesIterator)
            throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();

        writer.startElement(HTML.TABLE_ELEM, messages);
        HtmlRendererUtils.writeIdIfNecessary(writer, messages, facesContext);
        HtmlRendererUtils.renderHTMLAttributes(writer, messages, HTML.UNIVERSAL_ATTRIBUTES_WITHOUT_STYLE);
        
        HtmlRendererUtils.renderHTMLAttribute(writer, HTML.STYLE_ATTR, HTML.STYLE_ATTR, getComponentStyle(messages));
        HtmlRendererUtils.renderHTMLAttribute(writer, HTML.STYLE_CLASS_ATTR, HTML.STYLE_CLASS_ATTR, getComponentStyleClass(messages));
        
        while(messagesIterator.hasNext())
        {
            writer.startElement(HTML.TR_ELEM, messages);
            writer.startElement(HTML.TD_ELEM, messages);
            
            FacesMessage facesMessage = (FacesMessage)messagesIterator.next();
            // determine style and style class
            String[] styleAndClass = getStyleAndStyleClass(messages, facesMessage.getSeverity());
            String style = styleAndClass[0];
            String styleClass = styleAndClass[1];
            
            HtmlRendererUtils.renderHTMLAttribute(writer, HTML.STYLE_ATTR, HTML.STYLE_ATTR, style);
            HtmlRendererUtils.renderHTMLAttribute(writer, HTML.STYLE_CLASS_ATTR, HTML.STYLE_CLASS_ATTR, styleClass);
            
            renderSingleFacesMessage(facesContext,
                    messages,
                    facesMessage,
                    messagesIterator.getClientId(),false,false);

            writer.endElement(HTML.TD_ELEM);
            writer.endElement(HTML.TR_ELEM);
        }

        writer.endElement(HTML.TABLE_ELEM);
    }


    public static String[] getStyleAndStyleClass(UIComponent messages,
                                             FacesMessage.Severity severity)
    {
        String style = null;
        String styleClass = null;
        if (messages instanceof HtmlMessages)
        {
            if (severity == FacesMessage.SEVERITY_INFO)
            {
                style = ((HtmlMessages)messages).getInfoStyle();
                styleClass = ((HtmlMessages)messages).getInfoClass();
            }
            else if (severity == FacesMessage.SEVERITY_WARN)
            {
                style = ((HtmlMessages)messages).getWarnStyle();
                styleClass = ((HtmlMessages)messages).getWarnClass();
            }
            else if (severity == FacesMessage.SEVERITY_ERROR)
            {
                style = ((HtmlMessages)messages).getErrorStyle();
                styleClass = ((HtmlMessages)messages).getErrorClass();
            }
            else if (severity == FacesMessage.SEVERITY_FATAL)
            {
                style = ((HtmlMessages)messages).getFatalStyle();
                styleClass = ((HtmlMessages)messages).getFatalClass();
            }

            //if (style == null)
            //{
            //    style = ((HtmlMessages)messages).getStyle();
            //}

            //if (styleClass == null)
            //{
            //    styleClass = ((HtmlMessages)messages).getStyleClass();
            //}
        }
        else
        {
            Map attr = messages.getAttributes();
            if (severity == FacesMessage.SEVERITY_INFO)
            {
                style = (String)attr.get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.INFO_STYLE_ATTR);
                styleClass = (String)attr.get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.INFO_CLASS_ATTR);
            }
            else if (severity == FacesMessage.SEVERITY_WARN)
            {
                style = (String)attr.get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.WARN_STYLE_ATTR);
                styleClass = (String)attr.get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.WARN_CLASS_ATTR);
            }
            else if (severity == FacesMessage.SEVERITY_ERROR)
            {
                style = (String)attr.get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.ERROR_STYLE_ATTR);
                styleClass = (String)attr.get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.ERROR_CLASS_ATTR);
            }
            else if (severity == FacesMessage.SEVERITY_FATAL)
            {
                style = (String)attr.get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.FATAL_STYLE_ATTR);
                styleClass = (String)attr.get(JSFAttr.FATAL_CLASS_ATTR);
            }

            //if (style == null)
            //{
            //    style = (String)attr.get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.STYLE_ATTR);
            //}

            //if (styleClass == null)
            //{
            //    styleClass = (String)attr.get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.STYLE_CLASS_ATTR);
            //}
        }

        return new String[] {style, styleClass};
    }
    
    protected String getComponentStyleClass(UIComponent messages)
    {
        String styleClass = null;
        if (messages instanceof HtmlMessages)
        {
            styleClass = ((HtmlMessages)messages).getStyleClass();
        }
        else
        {
            Map attr = messages.getAttributes();
            styleClass = (String)attr.get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.STYLE_CLASS_ATTR);
        }
        return styleClass;
    }
    
    protected String getComponentStyle(UIComponent messages)
    {
        String style = null;
        if (messages instanceof HtmlMessages)
        {
            style = ((HtmlMessages)messages).getStyle();
        }
        else
        {
            Map attr = messages.getAttributes();
            style = (String)attr.get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.STYLE_ATTR);
        }
        return style;
    }

    protected String getTitle(UIComponent component)
    {
        if (component instanceof HtmlMessages)
        {
            return ((HtmlMessages)component).getTitle();
        }
        else
        {
            return (String)component.getAttributes().get(org.apache.myfaces.shared_impl.renderkit.JSFAttr.TITLE_ATTR);
        }
    }

    protected boolean isTooltip(UIComponent component)
    {
        if (component instanceof HtmlMessages)
        {
            return ((HtmlMessages)component).isTooltip();
        }
        else
        {
            return org.apache.myfaces.shared_impl.renderkit.RendererUtils.getBooleanAttribute(component, org.apache.myfaces.shared_impl.renderkit.JSFAttr.TOOLTIP_ATTR, false);
        }
    }

    protected boolean isShowSummary(UIComponent component)
    {
        if (component instanceof UIMessages)
        {
            return ((UIMessages)component).isShowSummary();
        }
        else
        {
            return RendererUtils.getBooleanAttribute(component, JSFAttr.SHOW_SUMMARY_ATTR, false);
        }
    }

    protected boolean isShowDetail(UIComponent component)
    {
        if (component instanceof UIMessages)
        {
            return ((UIMessages)component).isShowDetail();
        }
        else
        {
            return org.apache.myfaces.shared_impl.renderkit.RendererUtils.getBooleanAttribute(component, JSFAttr.SHOW_DETAIL_ATTR, false);
        }
    }

    protected boolean isGlobalOnly(UIComponent component)
    {
        if (component instanceof UIMessages)
        {
            return ((UIMessages)component).isGlobalOnly();
        }
        else
        {
            return org.apache.myfaces.shared_impl.renderkit.RendererUtils.getBooleanAttribute(component, JSFAttr.GLOBAL_ONLY_ATTR, false);
        }
    }

    protected String getLayout(UIComponent component)
    {
        if (component instanceof HtmlMessages)
        {
            return ((HtmlMessages)component).getLayout();
        }
        else
        {
            return (String)component.getAttributes().get(JSFAttr.LAYOUT_ATTR);
        }
    }
    
    protected String getFor(UIComponent component)
    {
        if (component instanceof UIMessages)
        {
            return ((UIMessages) component).getFor();
        }
 
        return (String) component.getAttributes().get(JSFAttr.FOR_ATTR); 
    }

    protected boolean isRedisplay(UIComponent component)
    {
        if (component instanceof UIMessages)
        {
            return ((UIMessages) component).isRedisplay();
        }

        return org.apache.myfaces.shared_impl.renderkit.RendererUtils.getBooleanAttribute(component, org.apache.myfaces.shared_impl.renderkit.JSFAttr.REDISPLAY_ATTR, true);
        
    }

    private static class MessagesIterator implements Iterator
    {
        private FacesContext _facesContext;
        private Iterator _globalMessagesIterator;
        private Iterator _clientIdsWithMessagesIterator;
        private Iterator _componentMessagesIterator = null;
        private String _clientId = null;
        private boolean _redisplay;
        private Object _next;

        public MessagesIterator(FacesContext facesContext, boolean globalOnly, boolean redisplay,  UIComponent forComponent)
        {
            _facesContext = facesContext;
            // The for attribute is mutually exclusive with globalOnly and take precedence if used.
            if(forComponent != null)
            {
                _clientId = forComponent.getClientId();
                _componentMessagesIterator = facesContext.getMessages(_clientId);
                _globalMessagesIterator = org.apache.myfaces.shared_impl.util.NullIterator.instance();
                _clientIdsWithMessagesIterator = org.apache.myfaces.shared_impl.util.NullIterator.instance();
            }
            else 
            {
                if (globalOnly)
                {
                    _globalMessagesIterator = facesContext.getMessages(null);
                    _clientIdsWithMessagesIterator = NullIterator.instance();
                }
                else
                {
                    _globalMessagesIterator = org.apache.myfaces.shared_impl.util.NullIterator.instance();
                    _clientIdsWithMessagesIterator = facesContext.getClientIdsWithMessages();
                }
                _componentMessagesIterator = null;
                _clientId = null;
            }
            
            _redisplay = redisplay;
            _next = null;
        }

        public boolean hasNext()
        {
            if(_next != null)
            {
                return true;
            }
            if(_globalMessagesIterator.hasNext()) 
            {
                do
                {
                    _next = _globalMessagesIterator.next();
                    if(_redisplay || !((FacesMessage)_next).isRendered())
                    {
                        return true;
                    }
                }
                while(_globalMessagesIterator.hasNext());
            }
            if(_componentMessagesIterator != null && _componentMessagesIterator.hasNext()) 
            {
                do
                {
                    _next = _componentMessagesIterator.next();
                    if(_redisplay || !((FacesMessage)_next).isRendered())
                    {
                        return true;
                    }
                }
                while(_componentMessagesIterator.hasNext());
            }
            if(_clientIdsWithMessagesIterator.hasNext()) 
            {
                do
                {
                    _clientId = (String) _clientIdsWithMessagesIterator.next();
                    _componentMessagesIterator = _facesContext.getMessages(_clientId);
                    while(_componentMessagesIterator.hasNext()) 
                    {
                        _next = _componentMessagesIterator.next();
                        if(_redisplay || !((FacesMessage)_next).isRendered())
                        {
                            return true;
                        }
                    }
                }
                while(_clientIdsWithMessagesIterator.hasNext());
                
            }
            _next = null;
            return false;
        }

        public Object next()
        {
            if(this.hasNext()) 
            {
                Object ret = _next;
                _next = null;
                return ret;
            }
            throw new NoSuchElementException();
        }

        public void remove()
        {
            throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
        }

        public String getClientId()
        {
            return _clientId;
        }
    }

}
