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
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.util.UnicodeEncoder;

/**
 * @author Manfred Geiler (latest modification by $Author: jakobk $)
 * @author Anton Koinov
 * @version $Revision: 921120 $ $Date: 2010-03-09 15:52:17 -0500 (Tue, 09 Mar 2010) $
 */
public class HtmlResponseWriterImpl
        extends ResponseWriter
{
    //private static final Log log = LogFactory.getLog(HtmlResponseWriterImpl.class);
    private static final Logger log = Logger.getLogger(HtmlResponseWriterImpl.class.getName());

    private static final String DEFAULT_CONTENT_TYPE = "text/html";
    private static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";
    private static final String UTF8 = "UTF-8";

    private boolean _writeDummyForm = false;
    private Set _dummyFormParams = null;

    private Writer _writer;
    private String _contentType;
    private String _characterEncoding;
    private String _startElementName;
    private Boolean _isScript;
    private Boolean _isStyle;
    private Boolean _isTextArea;
    private UIComponent _startElementUIComponent;
    private boolean _startTagOpen;

    private static final Set s_emptyHtmlElements = new HashSet();

    private static final String CDATA_START = "<![CDATA[ \n";
    private static final String COMMENT_START = "<!--\n";
    private static final String CDATA_COMMENT_END = "\n//]]>";
    private static final String CDATA_END = "\n]]>";
    private static final String COMMENT_COMMENT_END = "\n//-->";
    private static final String COMMENT_END = "\n-->";

    static
    {
        s_emptyHtmlElements.add("area");
        s_emptyHtmlElements.add("br");
        s_emptyHtmlElements.add("base");
        s_emptyHtmlElements.add("basefont");
        s_emptyHtmlElements.add("col");
        s_emptyHtmlElements.add("frame");
        s_emptyHtmlElements.add("hr");
        s_emptyHtmlElements.add("img");
        s_emptyHtmlElements.add("input");
        s_emptyHtmlElements.add("isindex");
        s_emptyHtmlElements.add("link");
        s_emptyHtmlElements.add("meta");
        s_emptyHtmlElements.add("param");
    }

    public HtmlResponseWriterImpl(Writer writer, String contentType, String characterEncoding)
    throws FacesException
    {
        _writer = writer;
        _contentType = contentType;
        if (_contentType == null)
        {
            if (log.isLoggable(Level.FINE)) log.fine("No content type given, using default content type " + DEFAULT_CONTENT_TYPE);
            _contentType = DEFAULT_CONTENT_TYPE;
        }
        if (characterEncoding == null)
        {
            if (log.isLoggable(Level.FINE)) log.fine("No character encoding given, using default character encoding " + DEFAULT_CHARACTER_ENCODING);
            _characterEncoding = DEFAULT_CHARACTER_ENCODING;
        }
        else
        {
            // validates the encoding, it will throw an UnsupportedEncodingException if the encoding is invalid
            try
            {
                new String("myfaces".getBytes(), characterEncoding);
            }
            catch (UnsupportedEncodingException e)
            {
                throw new IllegalArgumentException("Unsupported encoding: "+characterEncoding);
            }
            
            // canonize to uppercase, that's the standard format
            _characterEncoding = characterEncoding.toUpperCase();
        }
    }

    public static boolean supportsContentType(String contentType)
    {
        String[] supportedContentTypes = HtmlRendererUtils.getSupportedContentTypes();

        for (int i = 0; i < supportedContentTypes.length; i++)
        {
            String supportedContentType = supportedContentTypes[i];

            if(supportedContentType.indexOf(contentType)!=-1)
                return true;
        }
        return false;
    }

    public String getContentType()
    {
        return _contentType;
    }

    public String getCharacterEncoding()
    {
        return _characterEncoding;
    }

    public void flush() throws IOException
    {
        // API doc says we should not flush the underlying writer
        //_writer.flush();
        // but rather clear any values buffered by this ResponseWriter:
        closeStartTagIfNecessary();
    }

    public void startDocument()
    {
        // do nothing
    }

    public void endDocument() throws IOException
    {
        _writer.flush();
    }

    public void startElement(String name, UIComponent uiComponent) throws IOException
    {
        if (name == null)
        {
            throw new NullPointerException("elementName name must not be null");
        }

        closeStartTagIfNecessary();
        _writer.write('<');
        _writer.write(name);

        resetStartedElement();

        _startElementName = name;
        _startElementUIComponent = uiComponent;
        _startTagOpen = true;
    }

    private void closeStartTagIfNecessary() throws IOException
    {
        if (_startTagOpen)
        {
            if (s_emptyHtmlElements.contains(_startElementName.toLowerCase()))
            {
                _writer.write(" />");
                // make null, this will cause NullPointer in some invalid element nestings
                // (better than doing nothing)
                resetStartedElement();
            }
            else
            {
                _writer.write('>');

                if(isScript())
                {
                    if(HtmlRendererUtils.isXHTMLContentType(_contentType))
                    {
                        if(HtmlRendererUtils.isAllowedCdataSection(FacesContext.getCurrentInstance()))
                        {
                            _writer.write(CDATA_START);
                        }
                    }
                    else
                    {
                        _writer.write(COMMENT_START);
                    }
                }
            }
            _startTagOpen = false;
        }
    }

    private void resetStartedElement()
    {
        _startElementName = null;
        _startElementUIComponent = null;
        _isScript = null;
        _isStyle = null;
        _isTextArea = null;
    }

    public void endElement(String name) throws IOException
    {
        if (name == null)
        {
            throw new NullPointerException("elementName name must not be null");
        }

        if (log.isLoggable(Level.WARNING))
        {
            if (_startElementName != null &&
                !name.equals(_startElementName))
            {
                if (log.isLoggable(Level.WARNING))
                    log.warning("HTML nesting warning on closing " + name + ": element " + _startElementName +
                            (_startElementUIComponent==null?"":(" rendered by component : "+
                            RendererUtils.getPathToComponent(_startElementUIComponent)))+" not explicitly closed");
            }
        }

        if(_startTagOpen)
        {

            // we will get here only if no text or attribute was written after the start element was opened
            // now we close out the started tag - if it is an empty tag, this is then fully closed
            closeStartTagIfNecessary();

            //tag was no empty tag - it has no accompanying end tag now.
            if(_startElementName!=null)
            {
                //write closing tag
                writeEndTag(name);
            }
        }
        else
        {
            if (s_emptyHtmlElements.contains(name.toLowerCase()))
            {
           /*
           Should this be here?  It warns even when you have an x:htmlTag value="br", it should just close.

                if (log.isWarnEnabled())
                    log.warn("HTML nesting warning on closing " + name + ": This element must not contain nested elements or text in HTML");
                    */
            }
            else
            {
                writeEndTag(name);
            }
        }

        resetStartedElement();
    }

    private void writeEndTag(String name)
        throws IOException
    {
        if(isScript())
        {
            if(HtmlRendererUtils.isXHTMLContentType(_contentType))
            {
                if(HtmlRendererUtils.isAllowedCdataSection(FacesContext.getCurrentInstance()))
                {
                    _writer.write(CDATA_COMMENT_END);
                }
            }
            else
            {
                _writer.write(COMMENT_COMMENT_END);
            }
        }

        _writer.write("</");
        _writer.write(name);
        _writer.write('>');
    }

    public void writeAttribute(String name, Object value, String componentPropertyName) throws IOException
    {
        if (name == null)
        {
            throw new NullPointerException("attributeName name must not be null");
        }
        if (!_startTagOpen)
        {
            throw new IllegalStateException("Must be called before the start element is closed (attribute '" + name + "')");
        }

        if (value instanceof Boolean)
        {
            if (((Boolean)value).booleanValue())
            {
                // name as value for XHTML compatibility
                _writer.write(' ');
                _writer.write(name);
                _writer.write("=\"");
                _writer.write(name);
                _writer.write('"');
            }
        }
        else
        {
            String strValue = (value==null)?"":value.toString();
            _writer.write(' ');
            _writer.write(name);
            _writer.write("=\"");
            _writer.write(org.apache.myfaces.shared_impl.renderkit.html.util.HTMLEncoder.encode(strValue, false, false, !UTF8.equals(_characterEncoding)));
            _writer.write('"');
        }
    }

    public void writeURIAttribute(String name, Object value, String componentPropertyName) throws IOException
    {
        if (name == null)
        {
            throw new NullPointerException("attributeName name must not be null");
        }
        if (!_startTagOpen)
        {
            throw new IllegalStateException("Must be called before the start element is closed (attribute '" + name + "')");
        }

        String strValue = value.toString();
        _writer.write(' ');
        _writer.write(name);
        _writer.write("=\"");
        if (strValue.toLowerCase().startsWith("javascript:"))
        {
            _writer.write(org.apache.myfaces.shared_impl.renderkit.html.util.HTMLEncoder.encode(strValue, false, false, !UTF8.equals(_characterEncoding)));
        }
        else
        {
            /*
            Todo: what is this section about? still needed?
            client side state saving is now done via javascript...

            if (_startElementName.equalsIgnoreCase(HTML.ANCHOR_ELEM) && //Also support image and button urls?
                name.equalsIgnoreCase(HTML.HREF_ATTR) &&
                !strValue.startsWith("#"))
            {
                FacesContext facesContext = FacesContext.getCurrentInstance();
                if (facesContext.getApplication().getStateManager().isSavingStateInClient(facesContext))
                {
                    // saving state in url depends on the work together
                    // of 3 (theoretically) pluggable components:
                    // ViewHandler, ResponseWriter and ViewTag
                    // We should try to make this HtmlResponseWriterImpl able
                    // to handle this alone!
                    if (strValue.indexOf('?') < 0)
                    {
                        strValue = strValue + '?' + JspViewHandlerImpl.URL_STATE_MARKER;
                    }
                    else
                    {
                        strValue = strValue + '&' + JspViewHandlerImpl.URL_STATE_MARKER;
                    }
                }
            }
            */
            //_writer.write(strValue);
            _writer.write(org.apache.myfaces.shared_impl.renderkit.html.util.HTMLEncoder.encodeURIAtributte(strValue, _characterEncoding));
        }
        _writer.write('"');
    }

    public void writeComment(Object value) throws IOException
    {
        if (value == null)
        {
            throw new NullPointerException("comment name must not be null");
        }

        closeStartTagIfNecessary();
        _writer.write("<!--");
        _writer.write(value.toString());    //TODO: Escaping: must not have "-->" inside!
        _writer.write("-->");
    }

    public void writeText(Object value, String componentPropertyName) throws IOException
    {
        if (value == null)
        {
            throw new NullPointerException("Text must not be null.");
        }

        closeStartTagIfNecessary();

        String strValue = value.toString();

        if (isScriptOrStyle())
        {
            // Don't bother encoding anything if chosen character encoding is UTF-8
            if (UTF8.equals(_characterEncoding)) _writer.write(strValue);
            else _writer.write(UnicodeEncoder.encode(strValue) );
        }
        else
        {
            _writer.write(org.apache.myfaces.shared_impl.renderkit.html.util.HTMLEncoder.encode(strValue, false, false, !UTF8.equals(_characterEncoding)));
        }
    }

    public void writeText(char cbuf[], int off, int len) throws IOException
    {
        if (cbuf == null)
        {
            throw new NullPointerException("cbuf name must not be null");
        }
        if (cbuf.length < off + len)
        {
            throw new IndexOutOfBoundsException((off + len) + " > " + cbuf.length);
        }

        closeStartTagIfNecessary();

        if (isScriptOrStyle())
        {
            String strValue = new String(cbuf, off, len);
            // Don't bother encoding anything if chosen character encoding is UTF-8
            if (UTF8.equals(_characterEncoding)) _writer.write(strValue);
            else _writer.write(UnicodeEncoder.encode(strValue) );
        }
        else if (isTextarea())
        {
            // For textareas we must *not* map successive spaces to &nbsp or Newlines to <br/>
            org.apache.myfaces.shared_impl.renderkit.html.util.HTMLEncoder.encode(cbuf, off, len, false, false, !UTF8.equals(_characterEncoding), _writer);
        }
        else
        {
            // We map successive spaces to &nbsp; and Newlines to <br/>
            org.apache.myfaces.shared_impl.renderkit.html.util.HTMLEncoder.encode(cbuf, off, len, true, true, !UTF8.equals(_characterEncoding), _writer);
        }
    }

    private boolean isScriptOrStyle()
    {
        initializeStartedTagInfo();

        return (_isStyle != null && _isStyle.booleanValue()) ||
                (_isScript != null && _isScript.booleanValue());
    }

    private boolean isScript()
    {
        initializeStartedTagInfo();

        return (_isScript != null && _isScript.booleanValue());
    }

    private boolean isTextarea()
    {
        initializeStartedTagInfo();

        return _isTextArea != null && _isTextArea.booleanValue();
    }

    private void initializeStartedTagInfo()
    {
        if(_startElementName != null)
        {
            if(_isScript==null)
            {
                if(_startElementName.equalsIgnoreCase(HTML.SCRIPT_ELEM))
                {
                    _isScript = Boolean.TRUE;
                    _isStyle = Boolean.FALSE;
                    _isTextArea = Boolean.FALSE;
                }
                else
                {
                    _isScript = Boolean.FALSE;
                }
            }
            if(_isStyle == null)
            {
                if(_startElementName.equalsIgnoreCase(org.apache.myfaces.shared_impl.renderkit.html.HTML.STYLE_ELEM))
                {
                    _isStyle = Boolean.TRUE;
                    _isTextArea = Boolean.FALSE;
                }
                else
                {
                    _isStyle = Boolean.FALSE;
                }
            }

            if(_isTextArea == null)
            {
                if(_startElementName.equalsIgnoreCase(HTML.TEXTAREA_ELEM))
                {
                    _isTextArea = Boolean.TRUE;
                }
                else
                {
                    _isTextArea = Boolean.FALSE;
                }
            }
        }
    }

    public ResponseWriter cloneWithWriter(Writer writer)
    {
        HtmlResponseWriterImpl newWriter
                = new HtmlResponseWriterImpl(writer, getContentType(), getCharacterEncoding());
        newWriter._writeDummyForm = _writeDummyForm;
        newWriter._dummyFormParams = _dummyFormParams;
        return newWriter;
    }


    // Writer methods

    public void close() throws IOException
    {
        closeStartTagIfNecessary();
        _writer.close();
    }

    public void write(char cbuf[], int off, int len) throws IOException
    {
        closeStartTagIfNecessary();
        String strValue = new String(cbuf, off, len);
        // Don't bother encoding anything if chosen character encoding is UTF-8
        if (UTF8.equals(_characterEncoding)) _writer.write(strValue);
        else _writer.write(UnicodeEncoder.encode(strValue) );
    }

    public void write(int c) throws IOException
    {
        closeStartTagIfNecessary();
        _writer.write(c);
    }

    public void write(char cbuf[]) throws IOException
    {
        closeStartTagIfNecessary();
        String strValue = new String(cbuf);
        // Don't bother encoding anything if chosen character encoding is UTF-8
        if (UTF8.equals(_characterEncoding)) _writer.write(strValue);
        else _writer.write(UnicodeEncoder.encode(strValue) );
    }

    public void write(String str) throws IOException
    {
        closeStartTagIfNecessary();
        // empty string commonly used to force the start tag to be closed.
        // in such case, do not call down the writer chain
        if (str.length() > 0)
        {
            // Don't bother encoding anything if chosen character encoding is UTF-8
            if (UTF8.equals(_characterEncoding)) _writer.write(str);
            else _writer.write(UnicodeEncoder.encode(str) );
        }
    }

    public void write(String str, int off, int len) throws IOException
    {
        closeStartTagIfNecessary();
        String strValue = str.substring(off, off+len);
        // Don't bother encoding anything if chosen character encoding is UTF-8
        if (UTF8.equals(_characterEncoding)) _writer.write(strValue);
        else _writer.write(UnicodeEncoder.encode(strValue) );
    }
    
    /**
     * This method ignores the <code>UIComponent</code> provided and simply calls
     * <code>writeText(Object,String)</code>
     * @since 1.2
     */
    public void writeText(Object object, UIComponent component, String string) throws IOException
    {
        writeText(object,string);
    }
}
