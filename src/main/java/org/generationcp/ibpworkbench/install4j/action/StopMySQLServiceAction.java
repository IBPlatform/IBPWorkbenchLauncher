package org.generationcp.ibpworkbench.install4j.action;

import java.io.IOException;

import com.install4j.api.actions.AbstractInstallOrUninstallAction;
import com.install4j.api.context.Context;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UninstallerContext;
import com.install4j.api.context.UserCanceledException;

public class StopMySQLServiceAction extends AbstractInstallOrUninstallAction {
    private static final long serialVersionUID = 1L;

    public boolean install(InstallerContext context) throws UserCanceledException {
        return doAction(context);
    }

    public boolean uninstall(UninstallerContext context) throws UserCanceledException {
        return doAction(context);
    }

    public boolean doAction(Context context) {
        ProcessBuilder pb = new ProcessBuilder("NET", "stop", "MySQLIBWS");
        try {
            Process process = pb.start();
            process.waitFor();
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        return false;
    }
}
