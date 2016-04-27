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
package org.apache.myfaces.shared_impl.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/**
 * Utility class to support multilingual FacesMessages using ResourceBundles.
 * Standard messages are stored at <code>DEFAULT_BUNDLE</code>.<br>
 * The summary of the message is stored at the requested key value. The detail
 * of the message is stored at &lt;messageId&gt;_detail.
 *
 * @see FacesMessage
 * @see java.util.ResourceBundle
 *
 * @author Thomas Spiegl (latest modification by $Author: lu4242 $)
 * @author Manfred Geiler
 * @author Sean Schofield
 * @author Stpehan Strittmatter
 * @version $Revision: 824859 $ $Date: 2009-10-13 12:42:36 -0500 (Tue, 13 Oct 2009) $
 */
public final class MessageUtils
{
    /** Utility class, do not instatiate */
    private MessageUtils()
    {
        // nope
    }

    /** Default bundle for messages (<code>javax.faces.Messages</code>) */
    private static final String DEFAULT_BUNDLE = "javax.faces.Messages";

    /** Suffix for message details (<code>_detail</code>)*/
    private static final String DETAIL_SUFFIX = "_detail";
    //private static Log log = LogFactory.getLog(MessageUtils.class);
    private static Logger log = Logger.getLogger(MessageUtils.class.getName());

    /**
     * @param severity serverity of message
     * @param messageId id of message
     * @param arg arument of message
     *
     * @return generated FacesMessage
     */
    public static FacesMessage getMessage(FacesMessage.Severity severity,
                                          String messageId,
                                          Object arg)
    {
        return getMessage(severity,
                          messageId,
                          new Object[]{arg},
                          FacesContext.getCurrentInstance());
    }

    /**
     * @param severity serverity of message
     * @param messageId id of message
     * @param args aruments of message
     *
     * @return generated FacesMessage
     */
    public static FacesMessage getMessage(FacesMessage.Severity severity,
                                          String messageId,
                                          Object[] args)
    {
        return getMessage(severity,
                          messageId,
                          args,
                          FacesContext.getCurrentInstance());
    }

    public static FacesMessage getMessage(FacesMessage.Severity severity,
                                          String messageId,
                                          Object[] args,
                                          FacesContext facesContext)
    {
        FacesMessage message = getMessage(facesContext, messageId, args);
        message.setSeverity(severity);

        return message;
    }

    public static void addMessage(FacesMessage.Severity severity,
                                  String messageId,
                                  Object[] args)
    {
        addMessage(severity, messageId, args, null, FacesContext.getCurrentInstance());
    }

    public static void addMessage(FacesMessage.Severity severity,
                                  String messageId,
                                  Object[] args,
                                  FacesContext facesContext)
    {
        addMessage(severity, messageId, args, null, facesContext);
    }

    public static void addMessage(FacesMessage.Severity severity,
                                  String messageId,
                                  Object[] args,
                                  String forClientId)
    {
        addMessage(severity, messageId, args, forClientId, FacesContext.getCurrentInstance());
    }

    public static void addMessage(FacesMessage.Severity severity,
                                  String messageId,
                                  Object[] args,
                                  String forClientId,
                                  FacesContext facesContext)
    {
        if(log.isLoggable(Level.FINEST)) {
          log.finest("adding message " + messageId + " for clientId " + forClientId);
        }
        facesContext.addMessage(forClientId,
                                getMessage(severity, messageId, args, facesContext));
    }

    /**
     * Uses <code>MessageFormat</code> and the supplied parameters to fill in the param placeholders in the String.
     *
     * @param locale The <code>Locale</code> to use when performing the substitution.
     * @param msgtext The original parameterized String.
     * @param params The params to fill in the String with.
     * @return The updated String.
     */
    public static String substituteParams(Locale locale, String msgtext, Object params[])
    {
        String localizedStr = null;
        if(params == null || msgtext == null)
            return msgtext;
        StringBuffer b = new StringBuffer(100);
        if(locale != null)
        {
            MessageFormat mf = new MessageFormat(msgtext,locale);            
            b.append(mf.format(params));
            localizedStr = b.toString();
        }
        return localizedStr;
    }

    public static FacesMessage getMessage(String messageId, Object params[])
    {
        Locale locale = null;
        FacesContext context = FacesContext.getCurrentInstance();
        if(context != null && context.getViewRoot() != null)
        {
            locale = context.getViewRoot().getLocale();
            if(locale == null)
                locale = Locale.getDefault();
        } else
        {
            locale = Locale.getDefault();
        }
        return getMessage(locale, messageId, params);
    }

    public static FacesMessage getMessage(Locale locale, String messageId, Object params[])
    {
        String summary = null;
        String detail = null;
        String bundleName = getApplication().getMessageBundle();
        ResourceBundle bundle = null;

        if (bundleName != null)
        {
            try
            {
                bundle = ResourceBundle.getBundle(bundleName, locale, org.apache.myfaces.shared_impl.util.ClassUtils.getCurrentLoader(bundleName));
                summary = bundle.getString(messageId);
            }
            catch (MissingResourceException e) {
                // NoOp
            }
        }

        if (summary == null)
        {
            try
            {
                bundle = ResourceBundle.getBundle(DEFAULT_BUNDLE, locale, org.apache.myfaces.shared_impl.util.ClassUtils.getCurrentLoader(DEFAULT_BUNDLE));
                if(bundle == null)
                {
                    throw new NullPointerException();
                }
                summary = bundle.getString(messageId);
            }
            catch(MissingResourceException e) {
                // NoOp
            }
        }

        if(summary == null)
        {
            summary = messageId;
        }

        if (bundle == null)
        {
            throw new NullPointerException(
                "Unable to locate ResrouceBundle: bundle is null");
        }
        summary = substituteParams(locale, summary, params);

        try
        {
            detail = substituteParams(locale,
                bundle.getString(messageId + DETAIL_SUFFIX), params);
        }
        catch(MissingResourceException e) {
            // NoOp
        }

        return new FacesMessage(summary, detail);
    }

    /**
     * @param bundleBaseName baseName of ResourceBundle to load localized messages
     * @param messageId id of message
     * @param params parameters to set at localized message
     * @return generated FacesMessage
     */
    public static FacesMessage getMessage(String bundleBaseName, String messageId, Object params[])
    {
        return getMessage(bundleBaseName, getCurrentLocale(), messageId, params);
    }

    /**
     *
     * @return  currently applicable Locale for this request.
     */
    public static Locale getCurrentLocale() {
        Locale locale;
        FacesContext context = FacesContext.getCurrentInstance();
        if(context != null && context.getViewRoot() != null)
        {
            locale = context.getViewRoot().getLocale();
            if(locale == null)
                locale = Locale.getDefault();
        } else
        {
            locale = Locale.getDefault();
        }

        return locale;
    }

    /**
     * @param severity severity of message
     * @param bundleBaseName baseName of ResourceBundle to load localized messages
     * @param messageId id of message
     * @param params parameters to set at localized message
     * @return generated FacesMessage
     */
    public static FacesMessage getMessage(FacesMessage.Severity severity, String bundleBaseName, String messageId, Object params[])
    {
      FacesMessage msg = getMessage(bundleBaseName, messageId, params);
      msg.setSeverity(severity);

      return msg;
    }

    /**
     * @param bundleBaseName baseName of ResourceBundle to load localized messages
     * @param locale current locale
     * @param messageId id of message
     * @param params parameters to set at localized message
     * @return generated FacesMessage
     */
    public static FacesMessage getMessage(String bundleBaseName, Locale locale, String messageId, Object params[])
    {
      if (bundleBaseName == null)
      {
          throw new NullPointerException(
              "Unable to locate ResrouceBundle: bundle is null");
      }

      ResourceBundle bundle = ResourceBundle.getBundle(bundleBaseName, locale);

      return getMessage(bundle, messageId, params);
    }
    /**
     * @param bundle ResourceBundle to load localized messages
     * @param messageId id of message
     * @param params parameters to set at localized message
     * @return generated FacesMessage
     */
    public static FacesMessage getMessage(ResourceBundle bundle, String messageId, Object params[])
    {

      String summary = null;
      String detail = null;

      try
      {
          summary = bundle.getString(messageId);
      }
      catch (MissingResourceException e) {
        // NoOp
      }


      if(summary == null)
      {
          summary = messageId;
      }

      summary = substituteParams(bundle.getLocale(), summary, params);

      try
      {
          detail = substituteParams(bundle.getLocale(),
              bundle.getString(messageId + DETAIL_SUFFIX), params);
      }
      catch(MissingResourceException e) {
        // NoOp
      }

      return new FacesMessage(summary, detail);
    }

    /**
     *
     * @param context
     * @param messageId
     * @return generated FacesMessage
     */
    public static FacesMessage getMessage(FacesContext context, String messageId)
    {
        return getMessage(context, messageId, ((Object []) (null)));
    }

    /**
     *
     * @param context
     * @param messageId
     * @param params
     * @return generated FacesMessage
     */
    public static FacesMessage getMessage(FacesContext context, String messageId, Object params[])
    {
        if(context == null || messageId == null)
            throw new NullPointerException(" context " + context + " messageId " + messageId);
        Locale locale = getCurrentLocale();
        if(null == locale)
            throw new NullPointerException(" locale " + locale);
        FacesMessage message = getMessage(locale, messageId, params);
        if(message != null)
        {
            return message;
        } else
        {
            // TODO /FIX:  Note that this has fallback behavior to default Locale for message,
            // but similar behavior above does not.  The methods should probably behave
            locale = Locale.getDefault();
            return getMessage(locale, messageId, params);
        }
    }

    private static Application getApplication()
    {
        FacesContext context = FacesContext.getCurrentInstance();
        if(context != null)
        {
            return FacesContext.getCurrentInstance().getApplication();
        } 
        
        ApplicationFactory afactory = (ApplicationFactory)FactoryFinder.getFactory("javax.faces.application.ApplicationFactory");
        return afactory.getApplication();
        
    }
}
