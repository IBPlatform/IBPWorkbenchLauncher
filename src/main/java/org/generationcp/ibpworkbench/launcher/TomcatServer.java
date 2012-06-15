/***************************************************************
 * Copyright (c) 2012, All Rights Reserved.
 * 
 * Generation Challenge Programme (GCP)
 * 
 * 
 * This software is licensed for use under the terms of the 
 * GNU General Public License (http://bit.ly/8Ztv8M) and the 
 * provisions of Part F of the Generation Challenge Programme 
 * Amended Consortium Agreement (http://bit.ly/KQX1nL)
 * 
 **************************************************************/
package org.generationcp.ibpworkbench.launcher;

import org.apache.catalina.startup.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TomcatServer extends Thread {
    private Logger log = LoggerFactory.getLogger(TomcatServer.class);
    
    private String catalinaHome = "tomcat";

    private Bootstrap bootstrap = null;

    public String getCatalinaHome() {
        return catalinaHome;
    }

    public void setCatalinaHome(String catalinaHome) {
        this.catalinaHome = catalinaHome;
    }

    public void stopServer() {
        if (bootstrap != null) {
            try {
                bootstrap.stop();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        bootstrap = new Bootstrap();
        bootstrap.setCatalinaHome(catalinaHome);

        try {
            bootstrap.start();
        }
        catch (Exception e) {
            log.error("Unable to start tomcat due to error", e);
            
            bootstrap = null;
        }
    }
}
