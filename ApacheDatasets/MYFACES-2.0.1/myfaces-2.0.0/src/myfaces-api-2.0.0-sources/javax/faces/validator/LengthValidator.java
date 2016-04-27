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
package javax.faces.validator;

import javax.faces.component.PartialStateHolder;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFValidator;

/**
 * Creates a validator and associateds it with the nearest parent
 * UIComponent.  When invoked, the validator ensures that values are
 * valid strings with a length that lies within the minimum and maximum
 * values specified.
 * 
 * Commonly associated with a h:inputText entity.
 * 
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * 
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 *
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @author Thomas Spiegl
 * @version $Revision: 882743 $ $Date: 2009-11-20 17:28:21 -0500 (Fri, 20 Nov 2009) $
 */
@JSFValidator(
    name="f:validateLength",
    bodyContent="empty",
    tagClass="org.apache.myfaces.taglib.core.ValidateLengthTag")
@JSFJspProperty(
    name="binding", 
    returnType = "javax.faces.validator.LengthValidator",
    longDesc = "A ValueExpression that evaluates to a LengthValidator.")
public class LengthValidator
        implements Validator, PartialStateHolder
{
    // FIELDS
    public static final String     MAXIMUM_MESSAGE_ID = "javax.faces.validator.LengthValidator.MAXIMUM";
    public static final String     MINIMUM_MESSAGE_ID = "javax.faces.validator.LengthValidator.MINIMUM";
    public static final String     VALIDATOR_ID        = "javax.faces.Length";

    private Integer _minimum = null;
    private Integer _maximum = null;
    private boolean _transient = false;

    // CONSTRUCTORS
    public LengthValidator()
    {
    }

    public LengthValidator(int maximum)
    {
        _maximum = new Integer(maximum);
    }

    public LengthValidator(int maximum,
                           int minimum)
    {
        _maximum = new Integer(maximum);
        _minimum = new Integer(minimum);
    }

    // VALIDATE
    public void validate(FacesContext facesContext,
                         UIComponent uiComponent,
                         Object value)
            throws ValidatorException
    {
        if (facesContext == null) throw new NullPointerException("facesContext");
        if (uiComponent == null) throw new NullPointerException("uiComponent");

        if (value == null)
        {
            return;
        }

        int length = value instanceof String ?
            ((String)value).length() : value.toString().length();

        if (_minimum != null)
        {
            if (length < _minimum.intValue())
            {
                Object[] args = {_minimum,_MessageUtils.getLabel(facesContext, uiComponent)};
                throw new ValidatorException(_MessageUtils.getErrorMessage(facesContext, MINIMUM_MESSAGE_ID, args));
            }
        }

        if (_maximum != null)
        {
            if (length > _maximum.intValue())
            {
                Object[] args = {_maximum,_MessageUtils.getLabel(facesContext, uiComponent)};
                throw new ValidatorException(_MessageUtils.getErrorMessage(facesContext, MAXIMUM_MESSAGE_ID, args));
            }
        }
    }

    // SETTER & GETTER
    
    /** 
     * The largest value that should be considered valid.
     * 
     */
    @JSFProperty(deferredValueType="java.lang.Integer")
    public int getMaximum()
    {
        return _maximum != null ? _maximum.intValue() : 0;
    }

    public void setMaximum(int maximum)
    {
        _maximum = new Integer(maximum);
        clearInitialState();
    }

    /**
     * The smallest value that should be considered valid.
     *  
     */
    @JSFProperty(deferredValueType="java.lang.Integer")
    public int getMinimum()
    {
        return _minimum != null ? _minimum.intValue() : 0;
    }

    public void setMinimum(int minimum)
    {
        _minimum = new Integer(minimum);
        clearInitialState();
    }

    public boolean isTransient()
    {
        return _transient;
    }

    public void setTransient(boolean transientValue)
    {
        _transient = transientValue;
    }

    // RESTORE & SAVE STATE
    public Object saveState(FacesContext context)
    {
        if (!initialStateMarked())
        {
            Object values[] = new Object[2];
            values[0] = _maximum;
            values[1] = _minimum;
            return values;
        }
        return null;
    }

    public void restoreState(FacesContext context,
                             Object state)
    {
        if (state != null)
        {
            Object values[] = (Object[])state;
            _maximum = (Integer)values[0];
            _minimum = (Integer)values[1];
        }
    }

    // MISC
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LengthValidator)) return false;

        final LengthValidator lengthValidator = (LengthValidator)o;

        if (_maximum != null ? !_maximum.equals(lengthValidator._maximum) : lengthValidator._maximum != null) return false;
        if (_minimum != null ? !_minimum.equals(lengthValidator._minimum) : lengthValidator._minimum != null) return false;

        return true;
    }

    private boolean _initialStateMarked = false;

    public void clearInitialState()
    {
        _initialStateMarked = false;
    }

    public boolean initialStateMarked()
    {
        return _initialStateMarked;
    }

    public void markInitialState()
    {
        _initialStateMarked = true;
    }
}
