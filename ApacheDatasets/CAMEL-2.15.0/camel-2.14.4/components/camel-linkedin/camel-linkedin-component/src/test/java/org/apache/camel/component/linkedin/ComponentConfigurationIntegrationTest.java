/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Camel Api Route test generated by camel-component-util-maven-plugin
 * Generated on: Wed Jul 09 19:57:10 PDT 2014
 */
package org.apache.camel.component.linkedin;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.linkedin.internal.CommentsResourceApiMethod;
import org.apache.camel.component.linkedin.internal.LinkedInApiCollection;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for component configuration validation.
 */
public class ComponentConfigurationIntegrationTest extends AbstractLinkedInTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentConfigurationIntegrationTest.class);
    private static final String PATH_PREFIX = LinkedInApiCollection.getCollection().getApiName(CommentsResourceApiMethod.class).getName();

    @Override
    protected CamelContext createCamelContext() throws Exception {
        final CamelContext camelContext = super.createCamelContext();
        // replace client id with invalid value
        camelContext.getComponent("linkedin", LinkedInComponent.class).getConfiguration().setClientId("bad_client_id");
        return camelContext;
    }

    @Test
    public void testGetComment() throws Exception {
        final Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is String
        headers.put("CamelLinkedIn.comment_id", "123");
        // parameter type is String
        headers.put("CamelLinkedIn.fields", "");

        try {
            requestBodyAndHeaders("direct://GETCOMMENT", null, headers);
            fail("Bad client Id must cause an exception on first message");
        } catch (CamelExecutionException e) {
            Throwable t = e;
            while (t.getCause() != null && t.getCause() != t) {
                t = t.getCause();
            }
            if (!(t instanceof IllegalArgumentException)) {
                throw e;
            }
        }
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                // dummy test route for getComment
                from("direct://GETCOMMENT")
                    .to("linkedin://" + PATH_PREFIX + "/getComment");
            }
        };
    }
}
