package org.generationcp.ibpworkbench.install4j.action;

import com.install4j.api.ApplicationRegistry;
import com.install4j.api.ApplicationRegistry.ApplicationInfo;
import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

public class SetInstallationDirectory extends AbstractInstallAction {
    private static final long serialVersionUID = 1L;

    public boolean install(InstallerContext context) throws UserCanceledException {
        String workbenchCentralApplicationId = context.getCompilerVariable("gcp.application.id");
        if (workbenchCentralApplicationId == null) {
            Util.showErrorMessage("gcp.application.id compiler variable has not been declared. Please request for a new installer");
        }
        
        ApplicationInfo[] appInfoArray = ApplicationRegistry.getApplicationInfoById(workbenchCentralApplicationId);
        
        if (appInfoArray.length == 0) {
            Util.showErrorMessage(context.getMessage("workbench_central_not_installed"));
            return false;
        }
        else if (appInfoArray.length > 1) {
            Object[] params = new Object[]{ appInfoArray[0].getInstallationDirectory().getAbsolutePath() };
            String message = context.getMessage("warning_multiple_workbench_central_installation", params);
            Util.showMessage(message);
        }
        
        context.setInstallationDirectory(appInfoArray[0].getInstallationDirectory());
        
        return true;
    }
}
