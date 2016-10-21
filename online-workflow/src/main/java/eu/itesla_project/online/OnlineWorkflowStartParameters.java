/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online;

import eu.itesla_project.commons.config.ModuleConfig;
import eu.itesla_project.commons.config.PlatformConfig;

import java.io.Serializable;

/**
 * @author Quinary <itesla@quinary.com>
 */
public class OnlineWorkflowStartParameters implements Serializable {

	/*
     * example of online-start-parameters.properties file.
	 *

	# number of thread
	threads=20
	# JMX host and port
	jmxHost=127.0.0.1
	jmxPort=6667
	#listenerFactoryClasses=eu.itesla_project.apogee_client.ApogeeOnlineApplicationListenerFactory
	#onlineWorkflowFactoryClass=eu.itesla_project.quinary_utils.online.OnlineWorkFlowFactory

	*/

    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_THREADS = 1;

    private int threads;
    private String jmxHost;
    private int jmxPort;
    private Class<? extends OnlineApplicationListenerFactory> listenerFactoryClasses = null;
    private Class<? extends OnlineWorkflowFactory> onlineWorkflowFactory;

    public static OnlineWorkflowStartParameters loadDefault() {
        ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfig("online-start-parameters");
        int threads = config.getIntProperty("threads", DEFAULT_THREADS);
        if (threads <= 0) {
            threads = DEFAULT_THREADS;
        }
        String jmxHost = config.getStringProperty("jmxHost");
        int jmxPort = config.getIntProperty("jmxPort");

        Class<? extends OnlineWorkflowFactory> onlineWorkflowFactory = config.getClassProperty("onlineWorkflowFactoryClass", OnlineWorkflowFactory.class, OnlineWorkflowFactoryImpl.class);
        String listenerFactoryClassName = config.getStringProperty("listenerFactoryClasses", null);
        if (listenerFactoryClassName != null) {
            Class<? extends OnlineApplicationListenerFactory> listenerFactoryClass = config.getClassProperty("listenerFactoryClasses", OnlineApplicationListenerFactory.class);
            return new OnlineWorkflowStartParameters(threads, jmxHost, jmxPort, listenerFactoryClass, onlineWorkflowFactory);
        }

        return new OnlineWorkflowStartParameters(threads, jmxHost, jmxPort, null, onlineWorkflowFactory);
    }

    public OnlineWorkflowStartParameters(int threads, String jmxHost, int jmxPort, Class<? extends OnlineApplicationListenerFactory> listenerFactoryClass, Class<? extends OnlineWorkflowFactory> onlineWorkflowFactory) {
        this.threads = threads;
        this.jmxHost = jmxHost;
        this.jmxPort = jmxPort;
        this.listenerFactoryClasses = listenerFactoryClass;
        this.onlineWorkflowFactory = onlineWorkflowFactory;
    }

    public int getThreads() {
        return threads;
    }

    public String getJmxHost() {
        return jmxHost;
    }

    public int getJmxPort() {
        return jmxPort;
    }

    @Override
    public String toString() {
        return "OnlineWorkflowStartParameters [threads=" + threads + ", jmxHost=" + jmxHost + ", jmxPort=" + jmxPort
                + ", listenerFactoryClasses=" + listenerFactoryClasses + ", onlineWorkflowFactory="
                + onlineWorkflowFactory + "]";
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public void setJmxHost(String jmxHost) {
        this.jmxHost = jmxHost;
    }

    public void setJmxPort(int jmxPort) {
        this.jmxPort = jmxPort;
    }

    public Class<? extends OnlineApplicationListenerFactory> getOnlineApplicationListenerFactoryClass() {
        return listenerFactoryClasses;
    }

    public Class<? extends OnlineWorkflowFactory> getOnlineWorkflowFactoryClass() {
        return onlineWorkflowFactory;
    }

}
