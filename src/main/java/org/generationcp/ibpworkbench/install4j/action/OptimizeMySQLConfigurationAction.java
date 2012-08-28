package org.generationcp.ibpworkbench.install4j.action;

import org.generationcp.ibpworkbench.install4j.Install4JUtil;

import com.install4j.api.actions.AbstractInstallOrUninstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UninstallerContext;
import com.install4j.api.context.UserCanceledException;

public class OptimizeMySQLConfigurationAction extends AbstractInstallOrUninstallAction {
private static final long serialVersionUID = 1L;
    
    public boolean install(InstallerContext context) throws UserCanceledException {
        return Install4JUtil.optimizeMySQLConfiguration(context);
    }

    public boolean uninstall(UninstallerContext context) throws UserCanceledException {
        return Install4JUtil.optimizeMySQLConfiguration(context);
    }
    
    @Override
    public void rollback(InstallerContext context) {
        Install4JUtil.revertMySQLConfiguration(context);
    }
}
