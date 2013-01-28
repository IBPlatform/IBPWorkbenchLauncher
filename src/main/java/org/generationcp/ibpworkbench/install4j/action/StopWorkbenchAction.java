package org.generationcp.ibpworkbench.install4j.action;

import java.io.IOException;

import com.install4j.api.actions.AbstractInstallOrUninstallAction;
import com.install4j.api.actions.InstallAction;
import com.install4j.api.actions.UninstallAction;
import com.install4j.api.context.Context;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UninstallerContext;
import com.install4j.api.context.UserCanceledException;

public class StopWorkbenchAction extends AbstractInstallOrUninstallAction implements InstallAction, UninstallAction{
    private static final long serialVersionUID = 1L;

    public boolean install(InstallerContext context) throws UserCanceledException {
        return stopWorkbench(context);
    }

    public boolean uninstall(UninstallerContext context) throws UserCanceledException {
        return stopWorkbench(context);
    }
    
    protected boolean stopWorkbench(Context context) {
        // taskkill /T /F /IM ibworkbench.exe
        ProcessBuilder pb = new ProcessBuilder("taskkill", "/T", "/F", "/IM", "ibworkbench.exe");
        pb.directory(context.getInstallationDirectory());
        
        try {
            pb.start();
        }
        catch (IOException e) {
        }
        
        return true;
    }
}
