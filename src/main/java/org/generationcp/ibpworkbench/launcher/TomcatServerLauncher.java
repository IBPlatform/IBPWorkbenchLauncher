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
