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
package org.apache.myfaces.shared_impl.taglib.html;

import org.apache.myfaces.shared_impl.renderkit.JSFAttr;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;

import javax.faces.component.UIComponent;

/**
 * @author Thomas Spiegl (latest modification by $Author: cagatay $)
 * @version $Revision: 606793 $ $Date: 2007-12-25 10:20:46 -0500 (Tue, 25 Dec 2007) $
 * @deprecated use {@link HtmlDataTableELTagBase} instead
 */
public abstract class HtmlDataTableTagBase
        extends org.apache.myfaces.shared_impl.taglib.html.HtmlComponentBodyTagBase
{
    //private static final Log log = LogFactory.getLog(MyfacesHtmlDataTableTag.class);

    // UIComponent attributes --> already implemented in UIComponentTagBase

    // user role attributes --> already implemented in UIComponentTagBase

    // HTML universal attributes --> already implemented in HtmlComponentTagBase

    // HTML event handler attributes --> already implemented in HtmlComponentTagBase

    // HTML table attributes relevant for Table
    private String _align;
    private String _border;
    private String _bgcolor;
    private String _cellpadding;
    private String _cellspacing;
    private String _datafld;
    private String _datasrc;
    private String _dataformatas;
    private String _frame;
    private String _rules;
    private String _summary;
    private String _width;

    // UIPanel attributes
    // value and converterId --> already implemented in UIComponentTagBase

    // HtmlPanelGrid attributes
    private String _columnClasses;
    private String _columns;
    private String _footerClass;
    private String _headerClass;
    private String _rowClasses;

    // UIData attributes
    private String _rows;
    private String _var;
    private String _first;

    public void release() {
        super.release();
        _align=null;
        _border=null;
        _bgcolor=null;
        _cellpadding=null;
        _cellspacing=null;
        _datafld=null;
        _datasrc=null;
        _dataformatas=null;
        _frame=null;
        _rules=null;
        _summary=null;
        _width=null;
        _columnClasses=null;
        _columns=null;
        _footerClass=null;
        _headerClass=null;
        _rowClasses=null;
        _rows=null;
        _var=null;
        _first=null;
    }

    protected void setProperties(UIComponent component)
    {
        super.setProperties(component);

        setStringProperty(component, HTML.ALIGN_ATTR, _align);
        setIntegerProperty(component, HTML.BORDER_ATTR, _border);
        setStringProperty(component, HTML.BGCOLOR_ATTR, _bgcolor);
        setStringProperty(component, org.apache.myfaces.shared_impl.renderkit.html.HTML.CELLPADDING_ATTR, _cellpadding);
        setStringProperty(component, HTML.CELLSPACING_ATTR, _cellspacing);
        setStringProperty(component, org.apache.myfaces.shared_impl.renderkit.html.HTML.FRAME_ATTR, _frame);
        setStringProperty(component, HTML.RULES_ATTR, _rules);
        setStringProperty(component, HTML.SUMMARY_ATTR, _summary);
        setStringProperty(component, HTML.WIDTH_ATTR, _width);

        setStringProperty(component, JSFAttr.COLUMN_CLASSES_ATTR, _columnClasses);
        setIntegerProperty(component, JSFAttr.COLUMNS_ATTR, _columns);
        setStringProperty(component, JSFAttr.FOOTER_CLASS_ATTR, _footerClass);
        setStringProperty(component, org.apache.myfaces.shared_impl.renderkit.JSFAttr.HEADER_CLASS_ATTR, _headerClass);
        setStringProperty(component, JSFAttr.ROW_CLASSES_ATTR, _rowClasses);

        setIntegerProperty(component, JSFAttr.ROWS_ATTR, _rows);
        setStringProperty(component, JSFAttr.VAR_ATTR, _var);
        setIntegerProperty(component, JSFAttr.FIRST_ATTR, _first);
    }



    public void setAlign(String align)
    {
        _align = align;
    }

    public void setBorder(String border)
    {
        _border = border;
    }

    public void setBgcolor(String bgcolor)
    {
        _bgcolor = bgcolor;
    }

    public void setCellpadding(String cellpadding)
    {
        _cellpadding = cellpadding;
    }

    public void setCellspacing(String cellspacing)
    {
        _cellspacing = cellspacing;
    }

    public void setDatafld(String datafld)
    {
        _datafld = datafld;
    }

    public void setDatasrc(String datasrc)
    {
        _datasrc = datasrc;
    }

    public void setDataformatas(String dataformatas)
    {
        _dataformatas = dataformatas;
    }

    public void setFrame(String frame)
    {
        _frame = frame;
    }

    public void setRules(String rules)
    {
        _rules = rules;
    }

    public void setSummary(String summary)
    {
        _summary = summary;
    }

    public void setWidth(String width)
    {
        _width = width;
    }

    public void setColumnClasses(String columnClasses)
    {
        _columnClasses = columnClasses;
    }

    public void setColumns(String columns)
    {
        _columns = columns;
    }

    public void setFooterClass(String footerClass)
    {
        _footerClass = footerClass;
    }

    public void setHeaderClass(String headerClass)
    {
        _headerClass = headerClass;
    }

    public void setRowClasses(String rowClasses)
    {
        _rowClasses = rowClasses;
    }

    public void setRows(String rows)
    {
        _rows = rows;
    }

    public void setVar(String var)
    {
        _var = var;
    }

    public void setFirst(String first)
    {
        _first = first;
    }
}
