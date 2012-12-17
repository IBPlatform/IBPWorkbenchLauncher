package org.generationcp.ibpworkbench.install4j.action;

import java.io.File;
import java.io.IOException;

import org.generationcp.ibpworkbench.install4j.Install4JUtil;

import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

public class InstallMySQLServiceAction extends AbstractInstallAction {
    private static final long serialVersionUID = 1L;

    public boolean install(InstallerContext context) throws UserCanceledException {
        String mysqlBinPath = Install4JUtil.getMysqlBinPath(context);
        File mysqldPath = new File(context.getInstallationDirectory(), mysqlBinPath + File.separator + "mysqld.exe");
        
        String mysqlConfPath = Install4JUtil.getMySqlConfPath(context);
        
        ProcessBuilder pb = new ProcessBuilder(mysqldPath.getAbsolutePath(), "--install", "MySQLIBWS", "--defaults-file=\"" + mysqlConfPath + "\"");
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
