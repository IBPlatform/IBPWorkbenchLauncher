package org.generationcp.ibpworkbench.install4j.action;

import java.io.File;
import java.io.IOException;

import org.generationcp.ibpworkbench.install4j.Install4JUtil;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

/**
 * This custom action copies <code>&lt;installation dir&gt;/install/mysql</code>
 * to <code>&lt;installation dir&gt;/infrastructure/mysql</code> if
 * <code>&lt;installation dir&gt;/infrastructure/mysql</code> does not exists
 * yet.
 * 
 * @author Glenn Marintes
 */
public class InstallMySQLAction extends AbstractInstallAction {
    private static final long serialVersionUID = 1L;

    public boolean install(InstallerContext context) throws UserCanceledException {
        File installationDirectory = context.getInstallationDirectory();
        
        // copy MySQL files to actual installation directory
        String mysqlPath = context.getCompilerVariable("gcp.mysql.dir");
        File mysqlDir = new File(installationDirectory, mysqlPath);
        
        File mysqlSource = new File(installationDirectory, "install/mysql");
        if (!mysqlDir.exists()) {
            mysqlSource.renameTo(mysqlDir);
        }
        
        // update the datadir in my.ini
        try {
            File myIniFile = new File(mysqlDir, "my.ini");
            
            String myIniStr = Install4JUtil.stringWithContentsOFile(myIniFile);
            String dataDir = "datadir=\"" + Install4JUtil.getMySqlDataPath(context) + "\"";
            
            myIniStr = myIniStr.replace("datadir=../../../data/", dataDir);
            
            Install4JUtil.writeStringToFile(myIniStr, myIniFile);
            
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            Util.showErrorMessage(context.getMessage("installation_error"));
        }
        
        return false;
    }
}
