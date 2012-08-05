package org.generationcp.ibpworkbench.install4j.action;

import java.io.IOException;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

public class StopWorkbenchAction extends AbstractInstallAction {
    private static final long serialVersionUID = 1L;

    public boolean install(InstallerContext context) throws UserCanceledException {
        // taskkill /T /F /IM ibpworkbench.exe
        ProcessBuilder pb = new ProcessBuilder("taskkill", "/T", "/F", "/IM", "ibpworkbench.exe");
        pb.directory(context.getInstallationDirectory());
        
        try {
            pb.start();
        }
        catch (IOException e) {
            Util.showErrorMessage(context.getMessage("cannot_stop_workbench"));
            return false;
        }
        
        return true;
    }
}
