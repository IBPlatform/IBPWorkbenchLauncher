package org.generationcp.ibpworkbench.install4j.action;

import org.generationcp.ibpworkbench.install4j.Install4JUtil;

import com.install4j.api.actions.AbstractInstallOrUninstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UninstallerContext;
import com.install4j.api.context.UserCanceledException;

public class NormalizeMySQLConfigurationAction extends AbstractInstallOrUninstallAction {
private static final long serialVersionUID = 1L;
    
    public boolean install(InstallerContext context) throws UserCanceledException {
        return Install4JUtil.revertMySQLConfiguration(context);
    }

    public boolean uninstall(UninstallerContext context) throws UserCanceledException {
        return true;
    }
}
