package org.generationcp.ibpworkbench.install4j.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

import javax.swing.JOptionPane;

import org.generationcp.ibpworkbench.install4j.Crop;
import org.generationcp.ibpworkbench.install4j.Install4JUtil;
import org.generationcp.ibpworkbench.util.Install4JScriptRunner;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.Context;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

public class InitializeLightCentralDatabaseAction extends AbstractInstallAction {
    private static final long serialVersionUID = 1L;
    
    private final static String DATABASE_DATA_PATH = "database";
    private final static String DATABASE_CENTRAL_DATA_PATH = "database/central_light";
    private final static String DATABASE_CENTRAL_COMMON_DATA_PATH = "database/central_light/common";
    
    public boolean install(InstallerContext context) throws UserCanceledException {
        context.getProgressInterface().setIndeterminateProgress(true);
        
        // connect to MySQL
        Connection connection = Install4JUtil.connectToMySQL(context);
        if (connection == null) {
            return false;
        }
        
        try {
            // run scripts
            for (Crop crop : Crop.values()) {
                if (Install4JUtil.isComponentSelected(context, crop)) {
                    boolean success = runScriptsForCrop(context, connection, crop);
                    if (!success) {
                        return false;
                    }
                }
            }
            
            connection.commit();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            try {
                connection.close();
            }
            catch (SQLException e) {
            }
        }
        
        context.getProgressInterface().setStatusMessage(context.getMessage("deleting_temporary_files"));
        context.getProgressInterface().setDetailMessage("");
        
        // delete the crop database directory
        File databaseDataPath = new File(context.getInstallationDirectory(), DATABASE_DATA_PATH);
        databaseDataPath.delete();
        
        context.getProgressInterface().setStatusMessage(context.getMessage("done"));
        context.getProgressInterface().setDetailMessage("");
        
        context.getProgressInterface().setIndeterminateProgress(false);
        context.getProgressInterface().setPercentCompleted(100);
        
        return true;
    }
    
    protected boolean runScriptsForCrop(InstallerContext context, Connection conn, Crop crop) {
        String cropName = crop.getCropName();
        
        Object[] cropTitleParam = new Object[]{ context.getMessage(cropName) };
        
        // check if the central database is already installed
        boolean databaseExists = Install4JUtil.useDatabase(conn, "ibdb_" + cropName + "_central");
        if (databaseExists) {
            String cropTitle = context.getMessage(cropName);
            String message = context.getMessage("confirm_light_central_database_update", new Object[] { cropTitle });
            String[] options = new String[]{context.getMessage("yes"), context.getMessage("no")};
            try {
                int option = Util.showOptionDialog(message, options, JOptionPane.YES_NO_OPTION);
                if (option != 0) {
                    return true;
                }
            }
            catch (UserCanceledException e) {
                return true;
            }
        }
        
        // create the database and user
        String databaseName = "ibdbv1_" + cropName + "_central_light";
        String userName = "central";
        String password = "central";
        String[] queries = new String[] {
                                         "DROP DATABASE IF EXISTS " + databaseName
                                         ,"CREATE DATABASE " + databaseName
                                         ,"GRANT ALL ON " + databaseName + ".* TO '" + userName + "'@'localhost' IDENTIFIED BY '" + password + "'"
                                         ,"FLUSH PRIVILEGES"
        };
        if (!Install4JUtil.executeUpdate(context, conn, true, queries)) {
            return false;
        }
        
        if (!Install4JUtil.useDatabase(conn, databaseName)) {
            Util.showErrorMessage(context.getMessage("cannot_initialize_database"));
            return false;
        }
        
        File centralDatabaseDir = new File(context.getInstallationDirectory(), DATABASE_CENTRAL_DATA_PATH);
        
        File centralCropDir = new File(centralDatabaseDir, cropName);
        File centralCropCommonDir = new File(context.getInstallationDirectory(), DATABASE_CENTRAL_COMMON_DATA_PATH);
        if (!runScriptsOnDirectory(context, conn, cropTitleParam, centralCropCommonDir)) {
            return false;
        }
        if (!runScriptsOnDirectory(context, conn, cropTitleParam, centralCropDir)) {
            return false;
        }
        
        return true;
    }
    
    protected boolean runScriptsOnDirectory(Context context, Connection conn, Object[] cropTitleParam, File cropDir) {
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
            BufferedReader br = null;
            
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(sqlFile)));
                
                Install4JScriptRunner runner = new Install4JScriptRunner(context, conn, false, true);
                runner.runScript(br);
            }
            catch (IOException e1) {
                e1.printStackTrace();
                
                Util.showErrorMessage(context.getMessage("cannot_initialize_database"));
                return false;
            }
            finally {
                if (br != null) {
                    try {
                        br.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        return true;
    }
}
