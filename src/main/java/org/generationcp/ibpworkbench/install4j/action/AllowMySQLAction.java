package org.generationcp.ibpworkbench.install4j.action;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

public class AllowMySQLAction extends AbstractInstallAction {
    private static final long serialVersionUID = 1L;

    public boolean install(InstallerContext context) throws UserCanceledException {
        Util.showMessage(context.getMessage("allow_mysql_message"));
        
        return true;
    }

}
