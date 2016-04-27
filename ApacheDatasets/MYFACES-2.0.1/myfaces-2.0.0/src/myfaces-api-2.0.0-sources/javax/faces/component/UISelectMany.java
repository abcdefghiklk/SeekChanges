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
package javax.faces.component;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.el.ValueBinding;
import javax.faces.model.SelectItem;
import javax.faces.render.Renderer;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperties;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;

/**
 * Base class for the various component classes that allow a user to select zero or more options from a set.
 * <p>
 * This is not an abstract class; java code can create an instance of this, configure its rendererType appropriately,
 * and add it to a view. However there is no tag class for this component, ie it cannot be added directly to a page when
 * using JSP (and other presentation technologies are expected to behave similarly). Instead, there is a family of
 * subclasses that extend this base functionality, and they do have tag classes.
 * </p>
 * <p>
 * See the javadoc for this class in the <a href="http://java.sun.com/j2ee/javaserverfaces/1.2/docs/api/index.html">JSF
 * Specification</a> for further details.
 * </p>
 */
@JSFComponent(defaultRendererType = "javax.faces.Listbox")
@JSFJspProperties
(properties={
        @JSFJspProperty(name="hideNoSelectionOption", returnType="boolean"),
        @JSFJspProperty(name="collectionType", returnType="java.lang.String")
}
)
public class UISelectMany extends UIInput
{
    public static final String COMPONENT_TYPE = "javax.faces.SelectMany";
    public static final String COMPONENT_FAMILY = "javax.faces.SelectMany";

    public static final String INVALID_MESSAGE_ID = "javax.faces.component.UISelectMany.INVALID";

    public UISelectMany()
    {
        setRendererType("javax.faces.Listbox");
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }
    
    public Object[] getSelectedValues()
    {
        return (Object[]) getValue();
    }

    public void setSelectedValues(Object[] selectedValues)
    {
        setValue(selectedValues);
    }

    /**
     * @deprecated Use getValueExpression instead
     */
    @Override
    public ValueBinding getValueBinding(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        if (name.equals("selectedValues"))
        {
            return super.getValueBinding("value");
        }
        else
        {
            return super.getValueBinding(name);
        }
    }

    /**
     * @deprecated Use setValueExpression instead
     */
    @Override
    public void setValueBinding(String name, ValueBinding binding)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        if (name.equals("selectedValues"))
        {
            super.setValueBinding("value", binding);
        }
        else
        {
            super.setValueBinding(name, binding);
        }
    }

    @Override
    public ValueExpression getValueExpression(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        if (name.equals("selectedValues"))
        {
            return super.getValueExpression("value");
        }
        else
        {
            return super.getValueExpression(name);
        }
    }

    @Override
    public void setValueExpression(String name, ValueExpression binding)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        if (name.equals("selectedValues"))
        {
            super.setValueExpression("value", binding);
        }
        else
        {
            super.setValueExpression(name, binding);
        }
    }

    /**
     * @return true if Objects are different (!)
     */
    @Override
    protected boolean compareValues(Object previous, Object value)
    {
        if (previous == null)
        {
            // one is null, the other not
            return value != null;
        }
        else if (value == null)
        {
            // one is null, the other not
            return true;
        }
        else
        {
            if (previous instanceof Object[] && value instanceof Object[])
            {
                return compareObjectArrays((Object[]) previous, (Object[]) value);
            }
            else if (previous instanceof Collection && value instanceof Collection)
            {
                return compareCollections((Collection<?>) previous, (Collection<?>) value);
            }
            else if (previous.getClass().isArray() && value.getClass().isArray())
            {
                return comparePrimitiveArrays(previous, value);
            }
            else
            {
                // Objects have different classes
                return true;
            }
        }
    }

    private boolean compareObjectArrays(Object[] previous, Object[] value)
    {
        int length = value.length;
        if (previous.length != length)
        {
            // different length
            return true;
        }

        boolean[] scoreBoard = new boolean[length];
        for (int i = 0; i < length; i++)
        {
            Object p = previous[i];
            boolean found = false;
            for (int j = 0; j < length; j++)
            {
                if (scoreBoard[j] == false)
                {
                    Object v = value[j];
                    if ((p == null && v == null) || (p != null && v != null && p.equals(v)))
                    {
                        scoreBoard[j] = true;
                        found = true;
                        break;
                    }
                }
            }
            if (!found)
            {
                return true; // current element of previous array not found in new array
            }
        }

        return false; // arrays are identical
    }

    private boolean compareCollections(Collection<?> previous, Collection<?> value)
    {
        int length = value.size();
        if (previous.size() != length)
        {
            // different length
            return true;
        }

        boolean[] scoreBoard = new boolean[length];
        for (Iterator<?> itPrevious = previous.iterator(); itPrevious.hasNext();)
        {
            Object p = itPrevious.next();
            boolean found = false;
            int j = 0;
            for (Iterator<?> itValue = value.iterator(); itValue.hasNext(); j++)
            {
                Object v = itValue.next();
                if (scoreBoard[j] == false)
                {
                    if ((p == null && v == null) || (p != null && v != null && p.equals(v)))
                    {
                        scoreBoard[j] = true;
                        found = true;
                        break;
                    }
                }
            }
            if (!found)
            {
                return true; // current element of previous Collection not found in new Collection
            }
        }

        return false; // Collections are identical
    }

    private boolean comparePrimitiveArrays(Object previous, Object value)
    {
        int length = Array.getLength(value);
        if (Array.getLength(previous) != length)
        {
            // different length
            return true;
        }

        boolean[] scoreBoard = new boolean[length];
        for (int i = 0; i < length; i++)
        {
            Object p = Array.get(previous, i);
            boolean found = false;
            for (int j = 0; j < length; j++)
            {
                if (scoreBoard[j] == false)
                {
                    Object v = Array.get(value, j);
                    if ((p == null && v == null) || (p != null && v != null && p.equals(v)))
                    {
                        scoreBoard[j] = true;
                        found = true;
                        break;
                    }
                }
            }
            if (!found)
            {
                return true; // current element of previous array not found in new array
            }
        }

        return false; // arrays are identical
    }

    @Override
    protected void validateValue(FacesContext context, Object convertedValue)
    {
        Iterator<?> itemValues = _createItemValuesIterator(convertedValue);

        // verify that iterator was successfully created for convertedValue type
        if (itemValues == null)
        {
            _MessageUtils.addErrorMessage(context, this, INVALID_MESSAGE_ID, new Object[] { _MessageUtils.getLabel(
                context, this) });
            setValid(false);
            return;
        }

        boolean hasValues = itemValues.hasNext();

        // if UISelectMany is required, then there must be some selected values
        if (isRequired() && !hasValues)
        {
            if (getRequiredMessage() != null)
            {
                String requiredMessage = getRequiredMessage();
                context.addMessage(this.getClientId(context), new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    requiredMessage, requiredMessage));
            }
            else
            {
                _MessageUtils.addErrorMessage(context, this, REQUIRED_MESSAGE_ID,
                    new Object[] { _MessageUtils.getLabel(context, this) });
            }
            setValid(false);
            return;
        }

        // run the validators only if there are item values to validate
        if (hasValues)
        {
            _ComponentUtils.callValidators(context, this, convertedValue);
        }

        if (isValid() && hasValues)
        {
            // all selected values must match to the values of the available options

            _SelectItemsUtil._ValueConverter converter = new _SelectItemsUtil._ValueConverter()
            {
                public Object getConvertedValue(FacesContext context, String value)
                {
                    Object convertedValue = UISelectMany.this.getConvertedValue(context, new String[] { value });
                    if (convertedValue instanceof Collection)
                    {
                        Iterator<?> iter = ((Collection<?>) convertedValue).iterator();
                        if (iter.hasNext())
                        {
                            return iter.next();
                        }
                        return null;
                    }
                    return ((Object[]) convertedValue)[0];
                }
            };

            Collection<SelectItem> items = new ArrayList<SelectItem>();
            for (Iterator<SelectItem> iter = new _SelectItemsIterator(this, context); iter.hasNext();)
            {
                items.add(iter.next());
            }
            while (itemValues.hasNext())
            {
                Object itemValue = itemValues.next();

                // selected value must match to one of the available options
                // and if required is true it must not match an option with noSelectionOption set to true (since 2.0)
                if (!(_SelectItemsUtil.matchValue(context, itemValue, items.iterator(), converter)
                      && (!this.isRequired() 
                          || (this.isRequired() 
                              && !_SelectItemsUtil.isNoSelectionOption(context, itemValue, items.iterator(), converter)))))
                {    
                    _MessageUtils.addErrorMessage(context, this, INVALID_MESSAGE_ID,
                        new Object[] { _MessageUtils.getLabel(context, this) });
                    setValid(false);
                    return;
                }
            }
        }
    }

    /**
     * First part is identical to super.validate except the empty condition. Second part: iterate through UISelectItem
     * and UISelectItems and check current values against these items
     */
    @Override
    public void validate(FacesContext context)
    {
        // TODO : Setting the submitted value to null in the super class causes a bug, if set to
        // null, you'll get the following error :
        // java.lang.NullPointerException at
        //org.apache.myfaces.renderkit._SharedRendererUtils.getConvertedUISelectManyValue(_SharedRendererUtils.java:118)
        super.validate(context);
    }

    @Override
    protected Object getConvertedValue(FacesContext context, Object submittedValue) throws ConverterException
    {
        Renderer renderer = getRenderer(context);
        if (renderer != null)
        {
            return renderer.getConvertedValue(context, this, submittedValue);
        }
        else if (submittedValue == null)
        {
            return null;
        }
        else if (submittedValue instanceof String[])
        {
            return _SharedRendererUtils.getConvertedUISelectManyValue(context, this, (String[]) submittedValue);
        }
        return submittedValue;
    }

    private Iterator<?> _createItemValuesIterator(Object convertedValue)
    {
        if (convertedValue == null)
        {
            return Collections.emptyList().iterator();
        }
        else
        {
            Class<? extends Object> valueClass = convertedValue.getClass();
            if (valueClass.isArray())
            {
                return new _PrimitiveArrayIterator(convertedValue);
            }
            else if (convertedValue instanceof Object[])
            {
                Object[] values = (Object[]) convertedValue;
                return Arrays.asList(values).iterator();
            }
            else if (convertedValue instanceof Collection)
            {
                Collection<?> values = (Collection<?>) convertedValue;
                return values.iterator();
            }
            else
            {
                // unsupported type for iteration
                return null;
            }
        }
    }
}
