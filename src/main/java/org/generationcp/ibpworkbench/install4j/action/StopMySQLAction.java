package org.generationcp.ibpworkbench.install4j.action;

import org.generationcp.ibpworkbench.install4j.Install4JUtil;

import com.install4j.api.actions.AbstractInstallOrUninstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UninstallerContext;
import com.install4j.api.context.UserCanceledException;

public class StopMySQLAction extends AbstractInstallOrUninstallAction {
    private static final long serialVersionUID = 1L;
    
    public boolean install(InstallerContext context) throws UserCanceledException {
        return Install4JUtil.stopMySQL(context);
    }

    public boolean uninstall(UninstallerContext context) throws UserCanceledException {
        return Install4JUtil.stopMySQL(context);
    }
    
    @Override
    public void rollback(InstallerContext context) {
    }
}
