package org.generationcp.ibpworkbench.install4j.action;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.generationcp.ibpworkbench.install4j.DatabaseInfo;

import com.install4j.api.Util;
import com.install4j.api.context.InstallationComponentSetup;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

public class InitializeCentralDatabaseAction extends AbstractDatabaseAction {
    private static final long serialVersionUID = 1L;
    
    private final static String DATABASE_DATA_PATH = "database";
    private final static String DATABASE_CENTRAL_DATA_PATH = "database/central";
    
    private final static String CROP_CASSAVA = "cassava";
    private final static String CROP_CHICKPEA = "chickpea";
    private final static String CROP_COWPEA = "cowpea";
    private final static String CROP_MAIZE = "maize";
    private final static String CROP_RICE = "rice";
    private final static String CROP_WHEAT = "wheat";
    
    protected boolean isComponentSelected(InstallerContext context, String componentId) {
        InstallationComponentSetup component = context.getInstallationComponentById(componentId);
        return component == null ? false : component.isSelected();
    }
    
    public boolean install(InstallerContext context) throws UserCanceledException {
        // update the MySQL my.ini file
        if (!optimizeMySQLConfiguration(context)) {
            return false;
        }
        
        // start MySQL
        DatabaseInfo databaseInfo = startMySQL(context);
        if (databaseInfo == null) {
            return false;
        }
        
        // run scripts
        boolean cassava = isComponentSelected(context, CROP_CASSAVA);
        boolean chickpea = isComponentSelected(context, CROP_CHICKPEA);
        boolean cowpea = isComponentSelected(context, CROP_COWPEA);
        boolean maize = isComponentSelected(context, CROP_MAIZE);
        boolean rice = isComponentSelected(context, CROP_RICE);
        boolean wheat = isComponentSelected(context, CROP_WHEAT);
        
        if (cassava) {
            runScriptsForCrop(context, databaseInfo.getConnection(), CROP_CASSAVA);
        }
        if (chickpea) {
            runScriptsForCrop(context, databaseInfo.getConnection(), CROP_CHICKPEA);
        }
        if (cowpea) {
            runScriptsForCrop(context, databaseInfo.getConnection(), CROP_COWPEA);
        }
        if (maize) {
            runScriptsForCrop(context, databaseInfo.getConnection(), CROP_MAIZE);
        }
        if (rice) {
            runScriptsForCrop(context, databaseInfo.getConnection(), CROP_RICE);
        }
        if (wheat) {
            runScriptsForCrop(context, databaseInfo.getConnection(), CROP_WHEAT);
        }
        
        // stop MySQL
        if (!stopMySql(context, databaseInfo)) {
            return false;
        }
        
        // revert to original my.ini
        if (!revertMySQLConfiguration(context)) {
            return false;
        }
        
        context.getProgressInterface().setStatusMessage(context.getMessage("deleting_temporary_files"));
        context.getProgressInterface().setDetailMessage("");
        
        // delete the crop database directory
        File databaseDataPath = new File(context.getInstallationDirectory(), DATABASE_DATA_PATH);
        databaseDataPath.delete();
        
        context.getProgressInterface().setStatusMessage(context.getMessage("done"));
        context.getProgressInterface().setDetailMessage("");
        
        return true;
    }
    
    protected boolean runScriptsForCrop(InstallerContext context, Connection conn, String cropName) {
        File databaseDataPath = new File(context.getInstallationDirectory(), DATABASE_DATA_PATH);
        File centralDatabaseDir = new File(context.getInstallationDirectory(), DATABASE_CENTRAL_DATA_PATH);
        File cropDir = new File(centralDatabaseDir, cropName);
        
        Object[] cropTitleParam = new Object[]{ context.getMessage(cropName) };
        
        // create the database and user
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DROP DATABASE IF EXISTS ibdb_" + cropName + "_central");
            stmt.executeUpdate("CREATE DATABASE ibdb_" + cropName + "_central");
            stmt.executeUpdate("GRANT ALL ON ibdb_" + cropName + "_central.* TO 'central'@'localhost' IDENTIFIED BY 'central'");
            stmt.executeUpdate("FLUSH PRIVILEGES");
        }
        catch (SQLException e1) {
            Util.showErrorMessage(context.getMessage("cannot_initialize_database"));
            return false;
        }
        
        // get the run_script's path
        String runScriptName = "run_script.bat";
        String runScriptPath = databaseDataPath.getAbsolutePath() + File.separator + runScriptName;
        
        // show installation error if the crop database directory does not exist
        if (!cropDir.exists()) {
            String errorMessage = context.getMessage("cannot_find_database_files", cropTitleParam);
            Util.showErrorMessage(errorMessage);
            return false;
        }
        
        // get the sql files
        File[] sqlFiles = cropDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".sql");
            }
        });
        
        String statusMessage = context.getMessage("importing_database", cropTitleParam);
        context.getProgressInterface().setStatusMessage(statusMessage);

        for (File sqlFile : sqlFiles) {
            String detailMessage = sqlFile.getAbsolutePath();
            context.getProgressInterface().setDetailMessage(detailMessage);
            
            ProcessBuilder runScriptProcessBuilder = new ProcessBuilder(runScriptPath, "13306", "ibdb_" + cropName + "_central", String.format("\"%s\"", sqlFile.getAbsolutePath()));
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
