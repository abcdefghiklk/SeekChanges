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
package org.apache.camel.model.loadbalancer;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.camel.model.LoadBalancerDefinition;
import org.apache.camel.processor.loadbalancer.LoadBalancer;
import org.apache.camel.processor.loadbalancer.WeightedLoadBalancer;
import org.apache.camel.processor.loadbalancer.WeightedRandomLoadBalancer;
import org.apache.camel.processor.loadbalancer.WeightedRoundRobinLoadBalancer;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.util.ObjectHelper;

/**
 * Represents an XML &lt;weighted/&gt; element
 */
@XmlRootElement(name = "weighted")
@XmlAccessorType(XmlAccessType.FIELD)
public class WeightedLoadBalancerDefinition extends LoadBalancerDefinition {
    @XmlAttribute
    private Boolean roundRobin;
    @XmlAttribute(required = true)
    private String distributionRatio;
    @XmlAttribute
    private String distributionRatioDelimiter;

    public WeightedLoadBalancerDefinition() {
    }

    @Override
    protected LoadBalancer createLoadBalancer(RouteContext routeContext) {
        WeightedLoadBalancer loadBalancer;
        List<Integer> distributionRatioList = new ArrayList<Integer>();
        
        try {
            if (distributionRatioDelimiter == null) {
                distributionRatioDelimiter = ",";
            }
            
            String[] ratios = distributionRatio.split(distributionRatioDelimiter);
            for (String ratio : ratios) {
                distributionRatioList.add(new Integer(ratio.trim()));
            }
            
            if (!isRoundRobin()) {
                loadBalancer = new WeightedRandomLoadBalancer(distributionRatioList);
            } else {
                loadBalancer = new WeightedRoundRobinLoadBalancer(distributionRatioList);
            }
        } catch (Exception e) {
            throw ObjectHelper.wrapRuntimeCamelException(e);
        }

        return loadBalancer;
    }

    public Boolean getRoundRobin() {
        return roundRobin;
    }

    public void setRoundRobin(Boolean roundRobin) {
        this.roundRobin = roundRobin;
    }

    public boolean isRoundRobin() {
        return roundRobin != null && roundRobin;
    }

    public String getDistributionRatio() {
        return distributionRatio;
    }

    public void setDistributionRatio(String distributionRatio) {
        this.distributionRatio = distributionRatio;
    }

    public String getDistributionRatioDelimiter() {
        return distributionRatioDelimiter;
    }

    public void setDistributionRatioDelimiter(String distributionRatioDelimiter) {
        this.distributionRatioDelimiter = distributionRatioDelimiter;
    }

    @Override
    public String toString() {
        if (!isRoundRobin()) {
            return "WeightedRandomLoadBalancer[" + distributionRatio + "]";
        } else {
            return "WeightedRoundRobinLoadBalancer[" + distributionRatio + "]";
        }
    }
}
