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
package org.apache.camel.model.dataformat;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.camel.CamelContext;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.spi.DataFormat;

/**
 * Represents a <a href="http://camel.apache.org/castor.html">Castor</a> {@link org.apache.camel.spi.DataFormat}.
 *
 * @version 
 */
@XmlRootElement(name = "castor")
@XmlAccessorType(XmlAccessType.FIELD)
public class CastorDataFormat extends DataFormatDefinition {
    @XmlAttribute
    private String mappingFile;
    @XmlAttribute
    private Boolean validation;
    @XmlAttribute
    private String encoding;
    @XmlAttribute
    private String[] packages;
    @XmlAttribute
    private String[] classes;

    public CastorDataFormat() {
        super("castor");
    }

    public boolean isValidation() {
        // defaults to true if not configured
        return validation != null ? validation : true;
    }

    public Boolean getValidation() {
        return validation;
    }

    public void setValidation(Boolean validation) {
        this.validation = validation;
    }

    public String getMappingFile() {
        return mappingFile;
    }

    public void setMappingFile(String mappingFile) {
        this.mappingFile = mappingFile;
    }

    public String[] getPackages() {
        return packages;
    }

    public void setPackages(String[] packages) {
        this.packages = packages;
    }

    public String[] getClasses() {
        return classes;
    }

    public void setClasses(String[] classes) {
        this.classes = classes;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    protected void configureDataFormat(DataFormat dataFormat, CamelContext camelContext) {
        if (mappingFile != null) {
            setProperty(camelContext, dataFormat, "mappingFile", mappingFile);
        }
        setProperty(camelContext, dataFormat, "validation", isValidation());

        if (encoding != null) {
            setProperty(camelContext, dataFormat, "encoding", encoding);
        }
        if (packages != null) {
            setProperty(camelContext, dataFormat, "packages", packages);
        }
        if (classes != null) {
            setProperty(camelContext, dataFormat, "classes", classes);
        }
    }

}