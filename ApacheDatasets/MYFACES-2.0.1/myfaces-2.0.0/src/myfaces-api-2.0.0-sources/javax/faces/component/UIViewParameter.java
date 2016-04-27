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

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.FactoryFinder;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.Renderer;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * 
 * TODO: documentation on jsp and pld are not the same. It appear two
 * params: maxlength and for, but no property getter and setter founded here. 
 * If maxlength is used, we can put something like this: 
 * JSFJspProperty(name = "maxlength", returnType = "java.lang.String")
 * 
 * @author Simon Lessard (latest modification by $Author: jakobk $)
 * @version $Revision: 931567 $ $Date: 2010-04-07 09:40:22 -0500 (Wed, 07 Apr 2010) $
 * 
 * @since 2.0
 */
@JSFComponent(name = "f:viewParam", bodyContent = "JSP", 
        tagClass = "org.apache.myfaces.taglib.core.ParamTag")
@JSFJspProperty(name = "maxlength", returnType = "int", longDesc = "The max number or characters allowed for this param")
public class UIViewParameter extends UIInput
{
    public static final String COMPONENT_FAMILY = "javax.faces.ViewParameter";
    public static final String COMPONENT_TYPE = "javax.faces.ViewParameter";

    private static final String DELEGATE_FAMILY = UIInput.COMPONENT_FAMILY;
    private static final String DELEGATE_RENDERER_TYPE = "javax.faces.Text";
    
    private static Renderer _delegateRenderer;

    public UIViewParameter()
    {
        setRendererType(null);
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }

    @Override
    public void decode(FacesContext context)
    {
        // Override behavior from superclass to pull a value from the incoming request parameter map under the 
        // name given by getName() and store it with a call to UIInput.setSubmittedValue(java.lang.Object).
        String value = context.getExternalContext().getRequestParameterMap().get(getName());
        
        // only apply the value if it is non-null (otherwise postbacks 
        // to a view with view parameters would not work correctly)
        if (value != null)
        {
            setSubmittedValue(value);
        }
    }

    @Override
    public void encodeAll(FacesContext context) throws IOException
    {
        if (context == null) 
        {
            throw new NullPointerException();
        }
        setSubmittedValue(getStringValue(context));
    }

    public String getName()
    {
        return (String) getStateHelper().get(PropertyKeys.name);
    }

    public String getStringValue(FacesContext context)
    {
        if (getValueExpression ("value") != null) 
        {
            // Value specified as an expression, so do the conversion.
            
            return getStringValueFromModel (context);
        }
        
        // Otherwise, just return the local value.
        
        return ((String) this.getLocalValue());
    }

    public String getStringValueFromModel(FacesContext context) throws ConverterException
    {
        ValueExpression ve = getValueExpression ("value");
        Converter converter;
        Object value;
        
        if (ve == null) 
        {
            // No value expression, return null.
            return null;
        }
        
        value = ve.getValue (context.getELContext());
        
        if (value instanceof String) 
        {
            // No need to convert.
            return ((String) value);
        }
        
        converter = getConverter();
        
        if (converter == null) 
        {
            if (value == null) 
            {
                // No converter, no value, return null.
                return null;
            }
            
            // See if we can create the converter from the value type.
            
            converter = context.getApplication().createConverter (value.getClass());
            
            if (converter == null) 
            {
                // Only option is to call toString().
                
                return value.toString();
            }
        }
        
        return converter.getAsString (context, this, value);
    }

    @Override
    public String getSubmittedValue()
    {
        return (String)super.getSubmittedValue();
    }

    @JSFProperty(tagExcluded=true)
    @Override
    public boolean isImmediate()
    {
        return false;
    }
    
    @JSFProperty(tagExcluded=true)
    @Override
    public boolean isRendered()
    {
        return super.isRendered();
    }
    
    @Override
    public void processValidators(FacesContext context)
    {
        if (context == null) 
        {
            throw new NullPointerException ("context");
        }
        
        // If value is null and required is set, validation fails.
        
        if ((getSubmittedValue() == null) && isRequired()) 
        {
            FacesMessage message;
            String required = getRequiredMessage();
            
            if (required != null) 
            {
                message = new FacesMessage (FacesMessage.SEVERITY_ERROR, required, required);
            }
            else 
            {
                String label = _MessageUtils.getLabel (context, this);
                
                message = _MessageUtils.getMessage (context, context.getViewRoot().getLocale(),
                     FacesMessage.SEVERITY_ERROR, REQUIRED_MESSAGE_ID, new Object[] { label });
            }
            
            setValid (false);
            
            context.addMessage (getClientId (context), message);
            context.validationFailed();
            context.renderResponse();
            
            return;
        }
        
        super.processValidators (context);
    }
    
    enum PropertyKeys
    {
        name
    }
    
    public void setName(String name)
    {
        getStateHelper().put(PropertyKeys.name, name );
    }

    @Override
    public void updateModel(FacesContext context)
    {
        super.updateModel(context);
        
        // Put name in request map if value is not a value expression, is valid, and local
        // value was set.
        
        if ((getValueExpression ("value") == null) && isValid() && isLocalValueSet()) 
        {
            context.getExternalContext().getRequestMap().put (getName(), getLocalValue());
        }
    }

    @Override
    protected Object getConvertedValue(FacesContext context, Object submittedValue)
    {
        return getDelegateRenderer(context).getConvertedValue(context, this, submittedValue);
    }

    private static Renderer getDelegateRenderer(FacesContext context)
    {
        if (_delegateRenderer == null)
        {
            RenderKitFactory factory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
            RenderKit kit = factory.getRenderKit(context, RenderKitFactory.HTML_BASIC_RENDER_KIT);

            _delegateRenderer = kit.getRenderer(DELEGATE_FAMILY, DELEGATE_RENDERER_TYPE);
        }

        return _delegateRenderer;
    }

    /**
     * @author Simon Lessard (latest modification by $Author: jakobk $)
     * @version $Revision: 931567 $ $Date: 2010-04-07 09:40:22 -0500 (Wed, 07 Apr 2010) $
     * 
     * @since 2.0
     */
    public static class Reference
    {
        private int _index;
        private UIViewParameter _param;
        private Object _state;
        private String _viewId;

        public Reference(FacesContext context, UIViewParameter param, int indexInParent,
                         String viewIdAtTimeOfConstruction)
        {
            // This constructor cause the StateHolder.saveState(javax.faces.context.FacesContext) method
            // to be called on argument UIViewParameter.
            _param = param;
            _viewId = viewIdAtTimeOfConstruction;
            _index = indexInParent;
            _state = param.saveState(context);
        }

        public UIViewParameter getUIViewParameter(FacesContext context)
        {
            // If the current viewId is the same as the viewId passed to our constructor
            if (context.getViewRoot().getViewId().equals(_viewId))
            {
                // use the index passed to the constructor to find the actual UIViewParameter instance and return it.
                // FIXME: How safe is that when dealing with component trees altered by applications?
                return (UIViewParameter) _param.getParent().getChildren().get(_index);
            }
            else
            {
                // Otherwise, call StateHolder.restoreState(javax.faces.context.FacesContext, java.lang.Object) on
                // the saved state and return the result.
                _param.restoreState(context, _state);

                return _param;
            }
        }
    }
}
