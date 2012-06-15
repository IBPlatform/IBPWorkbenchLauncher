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

public class TomcatServerLauncher {

    public static void main(String[] args) {
        TomcatServer server = new TomcatServer();
        server.start();
        
        try {
            Thread.sleep(1000 * 60);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        server.stopServer();
    }
}
