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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.servlet.ServletContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.shared_impl.util.serial.SerialFactory;

/**
 * <p>Handles encryption, serialization, compression and encoding.
 *
 * <ul>
 * <li>ISO-8859-1 is the character set used.</li>
 * <li>GZIP is used for all compression/decompression.</li>
 * <li>Base64 is used for all encoding and decoding.</li>
 * <li>DES is the default encryption algorithm</li>
 * <li>ECB is the default mode</li>
 * <li>PKCS5Padding is the default padding</li>
 * <li>The default algorithm can be overridden using the
 * <i>org.apache.myfaces.ALGORITHM</i> parameter</li>
 * <li>The default mode and padding can be overridden using the
 * <i>org.apache.myfaces.ALGORITHM.PARAMETERS</i> parameter</li>
 * <li>This class has not been tested with modes other than ECB and CBC</li>
 * <li>An initialization vector can be specified via the
 * <i>org.apache.myfaces.ALGORITHM.IV</i> parameter</li>
 * </ul>
 *
 * <p>The secret is interpretted as base 64 encoded.  In other
 * words, if your secret is "76543210", you would put "NzY1NDMyMTA=" in
 * the deployment descriptor.  This is needed so that key values are not
 * limited to just values composed of printable characters.</p>
 *
 * <p>If you are using CBC mode encryption, you <b>must</b> specify an
 * initialization vector.</p>
 *
 * <p>If you are using the AES algorithm and getting a SecurityException
 * complaining about keysize, you most likely need to get the unlimited
 * strength jurisdiction policy files from a place like
 * http://java.sun.com/j2se/1.4.2/download.html .</p>
 *
 * @author Dennis C. Byrne
 * @see org.apache.myfaces.webapp.StartupServletContextListener
 */

public final class StateUtils {

    //private static final Log log = LogFactory.getLog(StateUtils.class);
    private static final Logger log = Logger.getLogger(StateUtils.class.getName());

    public static final String ZIP_CHARSET = "ISO-8859-1";

    public static final String DEFAULT_ALGORITHM = "DES";
    public static final String DEFAULT_ALGORITHM_PARAMS = "ECB/PKCS5Padding";

    public static final String INIT_PREFIX = "org.apache.myfaces.";
    
    @JSFWebConfigParam(name="org.apache.myfaces.USE_ENCRYPTION",since="1.1")
    public static final String USE_ENCRYPTION = INIT_PREFIX + "USE_ENCRYPTION";
    
    /**
     * Used for encrypting view state. Only relevant for client side 
     * state saving. See MyFaces wiki/web site documentation for instructions 
     * on how to configure an application for different encryption strengths.
     */
    @JSFWebConfigParam(name="org.apache.myfaces.SECRET",since="1.1")
    public static final String INIT_SECRET = INIT_PREFIX + "SECRET";
    
    @JSFWebConfigParam(name="org.apache.myfaces.ALGORITHM",since="1.1")
    public static final String INIT_ALGORITHM = INIT_PREFIX + "ALGORITHM";
    
    @JSFWebConfigParam(name="org.apache.myfaces.SECRET.CACHE",since="1.1")
    public static final String INIT_SECRET_KEY_CACHE = INIT_SECRET + ".CACHE";
    
    @JSFWebConfigParam(name="org.apache.myfaces.ALGORITHM.IV",since="1.1")
    public static final String INIT_ALGORITHM_IV = INIT_ALGORITHM + ".IV";
    
    @JSFWebConfigParam(name="org.apache.myfaces.ALGORITHM.PARAMETERS",since="1.1")
    public static final String INIT_ALGORITHM_PARAM = INIT_ALGORITHM + ".PARAMETERS";
    
    @JSFWebConfigParam(name="org.apache.myfaces.SERIAL_FACTORY", since="1.1")
    public static final String SERIAL_FACTORY = INIT_PREFIX + "SERIAL_FACTORY";
    
    @JSFWebConfigParam(name="org.apache.myfaces.COMPRESS_STATE_IN_CLIENT",since="1.1")
    private static final String COMPRESS_STATE_IN_CLIENT = INIT_PREFIX + "COMPRESS_STATE_IN_CLIENT";
    
    /** Utility class, do not instatiate */
    private StateUtils()
    {
        //nope
    }

    private static void testConfiguration(ExternalContext ctx){

        String algorithmParams = ctx.getInitParameter(INIT_ALGORITHM_PARAM);
        String iv = ctx.getInitParameter(INIT_ALGORITHM_IV);
        
        if (algorithmParams != null && algorithmParams.startsWith("CBC") )
        {
            if(iv == null)
                throw new FacesException(INIT_ALGORITHM_PARAM +
                                    " parameter has been set with CBC mode," +
                                    " but no initialization vector has been set " +
                                    " with " + INIT_ALGORITHM_IV);
        }
        
    }
    
    public static boolean enableCompression(ExternalContext ctx)
    {
        if(ctx == null)
            throw new NullPointerException("ExternalContext ctx");
    
        return "true".equals(ctx.getInitParameter(COMPRESS_STATE_IN_CLIENT));
    }
    
    public static boolean isSecure(ExternalContext ctx)
    {
        
        if(ctx == null)
            throw new NullPointerException("ExternalContext ctx");
        
        return ! "false".equals(ctx.getInitParameter(USE_ENCRYPTION));
    }

    /**
     * This fires during the Render Response phase, saving state.
     */

    public static final String construct(Object object, ExternalContext ctx){
        byte[] bytes = getAsByteArray(object, ctx);
        if( enableCompression(ctx) )
                bytes = compress(bytes);
        if(isSecure(ctx))
                bytes = encrypt(bytes, ctx);
        bytes = encode(bytes);
        try
        {
            return new String(bytes, ZIP_CHARSET);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new FacesException(e);
        }
    }

    /**
     * Performs serialization with the serialization provider created by the 
     * SerialFactory.  
     * 
     * @param object
     * @param ctx
     * @return
     */
    
    public static final byte[] getAsByteArray(Object object, ExternalContext ctx)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // get the Factory that was instantiated @ startup
        SerialFactory serialFactory = (SerialFactory) ctx.getApplicationMap().get(SERIAL_FACTORY);
        
        if(serialFactory == null)
            throw new NullPointerException("serialFactory");
        
        try
        {
            ObjectOutputStream writer = serialFactory.getObjectOutputStream(outputStream);
            writer.writeObject(object);
            byte[] bytes = outputStream.toByteArray();
            writer.close();
            outputStream.close();
            writer = null;
            outputStream = null;
            return bytes;
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }
    }

    public static byte[] encrypt(byte[] insecure, ExternalContext ctx)
    {
        return symmetric(insecure, ctx, Cipher.ENCRYPT_MODE);
    }

    public static final byte[] compress(byte[] bytes)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            GZIPOutputStream gzip = new GZIPOutputStream(baos);
            gzip.write(bytes, 0, bytes.length);
            gzip.finish();
            byte[] fewerBytes = baos.toByteArray();
            gzip.close();
            baos.close();
            gzip = null;
            baos = null;
            return fewerBytes;
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }
    }

    public static final byte[] encode(byte[] bytes)
    {
          return new Base64().encode(bytes);
    }

    /**
     * This fires during the Restore View phase, restoring state.
     */

    public static final Object reconstruct(String string, ExternalContext ctx){
        byte[] bytes;
        try
        {
            bytes = string.getBytes(ZIP_CHARSET);
            bytes = decode(bytes);
            if(isSecure(ctx))
                bytes = decrypt(bytes, ctx);
            if( enableCompression(ctx) )
                bytes = decompress(bytes);
            return getAsObject(bytes, ctx);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new FacesException(e);
        }
    }

    public static final byte[] decode(byte[] bytes)
    {
          return new Base64().decode(bytes);
    }

    public static final byte[] decompress(byte[] bytes)
    {
        if(bytes == null)
            throw new NullPointerException("byte[] bytes");
        
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[bytes.length];
        int length;

        try
        {
            GZIPInputStream gis = new GZIPInputStream(bais);
            while ((length = gis.read(buffer)) != -1)
            {
                baos.write(buffer, 0, length);
            }

            byte[] moreBytes = baos.toByteArray();
            baos.close();
            bais.close();
            gis.close();
            baos = null;
            bais = null;
            gis = null;
            return moreBytes;
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }
    }
    
    public static byte[] decrypt(byte[] secure, ExternalContext ctx)
    {
        return symmetric(secure, ctx, Cipher.DECRYPT_MODE); 
    }

    /**
     * Performs deserialization with the serialization provider created from the
     * SerialFactory.
     * 
     * @param bytes
     * @param ctx
     * @return
     */
    
    public static final Object getAsObject(byte[] bytes, ExternalContext ctx)
    {
        ByteArrayInputStream input = null;

        try
        {
            input = new ByteArrayInputStream(bytes);

            // get the Factory that was instantiated @ startup
            SerialFactory serialFactory = (SerialFactory) ctx.getApplicationMap().get(SERIAL_FACTORY);
            
            if(serialFactory == null)
                throw new NullPointerException("serialFactory");
            
            ObjectInputStream s = null;
            Exception pendingException = null;
            try
            {
                s = serialFactory.getObjectInputStream(input); 
                Object object = null;
                if (System.getSecurityManager() != null)
                {
                    final ObjectInputStream ois = s;
                    object = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>()
                    {
                        //Put IOException and ClassNotFoundException as "checked" exceptions,
                        //so AccessController wrap them in a PrivilegedActionException
                        public Object run() throws  PrivilegedActionException, IOException, ClassNotFoundException
                        {
                            return ois.readObject();
                        }
                    });
                    // Since s has the same instance as ois,
                    // we don't need to close it here, rather
                    // close it on the finally block related to s
                    // and avoid duplicate close exceptions
                    // finally
                    // {
                    //    ois.close();
                    // }
                }
                else
                {
                    object = s.readObject();
                }
                return object;
            }
            catch (Exception e)
            {
                pendingException = e;
                throw new FacesException(e);
            }
            finally
            {
                if (s != null)
                {
                    try
                    {
                        s.close();
                    }
                    catch (IOException e)
                    {
                        // If a previous exception is thrown 
                        // ignore this, but if not, wrap it in a
                        // FacesException and throw it. In this way
                        // we preserve the original semantic of this
                        // method, but we handle correctly the case
                        // when we close a stream. Obviously, the 
                        // information about this exception is lost,
                        // but note that the interesting information 
                        // is always on pendingException, since we
                        // only do a readObject() on the outer try block.
                        if (pendingException == null)
                        {
                            throw new FacesException(e);
                        }                        
                    }
                    finally
                    {
                        s = null;
                    }
                }
            }
        }
        finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (IOException e)
                {
                    //ignore it, because ByteArrayInputStream.close has
                    //no effect, but it is better to call close and preserve
                    //semantic from previous code.
                }
                finally
                {
                    input = null;
                }
            }
        }
    }

    /**
     * Utility method for generating base 63 encoded strings.
     * 
     * @param args
     * @throws UnsupportedEncodingException
     */
    
    public static void main (String[] args) throws UnsupportedEncodingException
    {
        byte[] bytes = encode(args[0].getBytes(ZIP_CHARSET));
          System.out.println(new String(bytes, ZIP_CHARSET));
    }
    
    private static byte[] symmetric(byte[] data, SecretKey secretKey,
            String algorithm, String algorithmParams, byte[] iv, int mode){
    
        try
        {
            // keep local to avoid threading issue
            Cipher cipher = Cipher.getInstance(algorithm + "/"
                    + algorithmParams);
            if (iv != null)
            {
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(mode, secretKey, ivSpec);
            }
            else
            {
                cipher.init(mode, secretKey);
            }

            if (log.isLoggable(Level.FINE))
            {

                String action = mode == Cipher.ENCRYPT_MODE ? "encrypting"
                        : "decrypting";

                log.fine(action + " w/ " + algorithm + "/" + algorithmParams);
            }

            return cipher.doFinal(data);
        }
        catch (Exception e)
        {
            throw new FacesException(e);
        }
    
    }

    /**
     * Pulls configuration data from the context and performs symmetric encryption
     * or decryption.  If a SecretKey is not located in an application scope level
     * cache, it is created.
     * 
     * @param data
     * @param ctx
     * @param mode
     * @return
     */
    
    private static byte[] symmetric(byte[] data, ExternalContext ctx, int mode)
    {

        if (ctx == null)
            throw new NullPointerException("ExternalContext ctx");

        testConfiguration(ctx);
        
        String _algorithm = findAlgorithm(ctx);
        String _algorithmParams = findAlgorithmParams(ctx);
        byte[] iv = findInitializationVector(ctx);
            
        Object object = ctx.getApplicationMap().get(INIT_SECRET_KEY_CACHE);
        
        if( object == null )
            throw new NullPointerException("Could not find SecretKey in application scope using key '" 
                    + INIT_SECRET_KEY_CACHE + "'");
        
        if( ! ( object instanceof SecretKey ) )
            throw new ClassCastException("Did not find an instance of SecretKey "
                    + "in application scope using the key '" + INIT_SECRET_KEY_CACHE + "'");
        
        return symmetric(data, (SecretKey)object, _algorithm, _algorithmParams, iv, mode);
            
    }

    private static byte[] findInitializationVector(ExternalContext ctx) {
        
        byte[] iv = null;
        String _iv = ctx.getInitParameter(INIT_ALGORITHM_IV);
        
        if (_iv != null)
            iv = new Base64().decode(_iv.getBytes());
        
        return iv;
    }

    private static String findAlgorithmParams(ExternalContext ctx) {
        
        String algorithmParams = ctx.getInitParameter(INIT_ALGORITHM_PARAM);
        
        if (algorithmParams == null)
        {
            algorithmParams = DEFAULT_ALGORITHM_PARAMS;
        }
        
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Using algorithm paramaters " + algorithmParams);
        }
        
        return algorithmParams;
    }

    private static String findAlgorithm(ExternalContext ctx) {
        
        return findAlgorithm( ctx.getInitParameter(INIT_ALGORITHM) );

    }
    
    private static String findAlgorithm(ServletContext ctx) {
        
        return findAlgorithm( ctx.getInitParameter(INIT_ALGORITHM) );
        
    }
    
    private static String findAlgorithm(String initParam) {
        
        if (initParam == null)
        {
            initParam = DEFAULT_ALGORITHM;
        }
        
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Using algorithm " + initParam);
        }
        
        return initParam;
        
    }

    /**
     * Does nothing if the user has disabled the SecretKey cache. This is
     * useful when dealing with a JCA provider whose SecretKey 
     * implementation is not thread safe.
     * 
     * Instantiates a SecretKey instance based upon what the user has 
     * specified in the deployment descriptor.  The SecretKey is then 
     * stored in application scope where it can be used for all requests.
     */
    
    public static void initSecret(ServletContext ctx){
        
        if(ctx == null)
            throw new NullPointerException("ServletContext ctx");
        
        if (log.isLoggable(Level.FINE))
            log.fine("Storing SecretKey @ " + INIT_SECRET_KEY_CACHE);
        
        // you want to create this as few times as possible
        ctx.setAttribute(INIT_SECRET_KEY_CACHE, new SecretKeySpec(findSecret(ctx), findAlgorithm(ctx)));
        
    }

    private static byte[] findSecret(ServletContext ctx) {
        String _secret = ctx.getInitParameter(INIT_SECRET);
        byte[] bytes = null;
        
        if(_secret == null)
        {
            int length = 8;
            bytes = new byte[length];
            new Random().nextBytes(bytes);
            
            if(log.isLoggable(Level.FINE))
                log.fine("generated random password of length " + length);
        }
        else 
        {
            bytes = new Base64().decode(_secret.getBytes());
        }
        
        return bytes;
    }

}
