package org.generationcp.ibpworkbench.install4j.action;

import java.io.File;
import java.io.IOException;

import org.generationcp.ibpworkbench.install4j.Install4JUtil;

import com.install4j.api.actions.AbstractUninstallAction;
import com.install4j.api.context.UninstallerContext;
import com.install4j.api.context.UserCanceledException;

public class UninstallMySQLServiceAction extends AbstractUninstallAction {
    private static final long serialVersionUID = 1L;

    public boolean uninstall(UninstallerContext context) throws UserCanceledException {
        String mysqlBinPath = Install4JUtil.getMysqlBinPath(context);
        File mysqldPath = new File(context.getInstallationDirectory(), mysqlBinPath + File.separator + "mysqld.exe");
        
        ProcessBuilder pb = new ProcessBuilder(mysqldPath.getAbsolutePath(), "--remove", "MySQLIBWS");
        try {
            Process process = pb.start();
            if (process != null) {
                process.waitFor();
            }
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
