package org.generationcp.ibpworkbench.install4j.action;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.generationcp.ibpworkbench.install4j.DatabaseInfo;

import com.install4j.api.Util;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

public class InitializeWorkbenchDatabaseAction extends AbstractDatabaseAction {
    private static final long serialVersionUID = 1L;
    
    private final static String DATABASE_DATA_PATH = "database";
    private final static String DATABASE_WORKBENCH_DATA_PATH = "database/workbench";

    public boolean install(InstallerContext context) throws UserCanceledException {
        // start MySQL
        DatabaseInfo databaseInfo = startMySQL(context);
        if (databaseInfo == null) {
            return false;
        }
        
        // run workbench scripts
        runWorkbenchScripts(context, databaseInfo.getConnection());
        
        // stop MySQL
        if (!stopMySql(context, databaseInfo)) {
            return false;
        }
        
        return true;
    }
    
    protected boolean runWorkbenchScripts(InstallerContext context, Connection conn) {
        File databaseDataPath = new File(context.getInstallationDirectory(), DATABASE_DATA_PATH);
        File workbenchDatabaseDir = new File(context.getInstallationDirectory(), DATABASE_WORKBENCH_DATA_PATH);
        Object[] workbenchTitleParam = new Object[]{ context.getMessage("workbench") };
        
        // create the database and user
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS workbench");
            stmt.executeUpdate("GRANT ALL ON workbench.* TO 'workbench'@'localhost' IDENTIFIED BY 'workbench'");
        }
        catch (SQLException e1) {
            Util.showErrorMessage(context.getMessage("cannot_initialize_database"));
            return false;
        }
        
        // get the run_script's path
        String runScriptName = "run_script.bat";
        String runScriptPath = databaseDataPath.getAbsolutePath() + File.separator + runScriptName;
        
        // show installation error if the crop database directory does not exist
        if (!workbenchDatabaseDir.exists()) {
            String errorMessage = context.getMessage("cannot_find_database_files", workbenchTitleParam);
            Util.showErrorMessage(errorMessage);
            return false;
        }
        
        // get the sql files
        File[] sqlFiles = workbenchDatabaseDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".sql");
            }
        });
        
        String statusMessage = context.getMessage("initializing_database", workbenchTitleParam);
        context.getProgressInterface().setStatusMessage(statusMessage);

        for (File sqlFile : sqlFiles) {
            String detailMessage = sqlFile.getAbsolutePath();
            context.getProgressInterface().setDetailMessage(detailMessage);
            
            ProcessBuilder runScriptProcessBuilder = new ProcessBuilder(runScriptPath, "13306", "workbench", String.format("\"%s\"", sqlFile.getAbsolutePath()));
            runScriptProcessBuilder.directory(databaseDataPath);
            
            Process runScriptProcess = null;
            try {
                runScriptProcess = runScriptProcessBuilder.start();
            }
            catch (IOException e) {
                Util.showErrorMessage(context.getMessage("cannot_initialize_database"));
                return false;
            }

            try {
                runScriptProcess.waitFor();
            }
            catch (InterruptedException e) {
                return false;
            }
        }
        
        return true;
    }
}
