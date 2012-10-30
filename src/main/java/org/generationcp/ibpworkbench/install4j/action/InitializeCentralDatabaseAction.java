package org.generationcp.ibpworkbench.install4j.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.generationcp.ibpworkbench.install4j.Install4JUtil;
import org.generationcp.ibpworkbench.util.ScriptRunner;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallationComponentSetup;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

public class InitializeCentralDatabaseAction extends AbstractInstallAction {
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
        // connect to MySQL
        Connection connection = Install4JUtil.connectToMySQL(context);
        if (connection == null) {
            return false;
        }
        
        try {
            // run scripts
            boolean cassava = isComponentSelected(context, CROP_CASSAVA);
            boolean chickpea = isComponentSelected(context, CROP_CHICKPEA);
            boolean cowpea = isComponentSelected(context, CROP_COWPEA);
            boolean maize = isComponentSelected(context, CROP_MAIZE);
            boolean rice = isComponentSelected(context, CROP_RICE);
            boolean wheat = isComponentSelected(context, CROP_WHEAT);

            if (cassava) {
                runScriptsForCrop(context, connection, CROP_CASSAVA);
            }
            if (chickpea) {
                runScriptsForCrop(context, connection, CROP_CHICKPEA);
            }
            if (cowpea) {
                runScriptsForCrop(context, connection, CROP_COWPEA);
            }
            if (maize) {
                runScriptsForCrop(context, connection, CROP_MAIZE);
            }
            if (rice) {
                runScriptsForCrop(context, connection, CROP_RICE);
            }
            if (wheat) {
                runScriptsForCrop(context, connection, CROP_WHEAT);
            }

            // register installed crop types
            if (!registerCrops(context, connection)) {
                return false;
            }
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
        
        return true;
    }
    
    protected boolean registerCrops(InstallerContext context, Connection conn) {
        String dropWorkbenchCropSql = "DROP TABLE IF EXISTS workbench.workbench_crop";
        String createWorkbenchCropSql = "CREATE TABLE workbench.workbench_crop(\n"
                                      + "    crop_name VARCHAR(32) NOT NULL\n"
                                      + "   ,central_db_name VARCHAR(64)\n"
                                      + "   ,PRIMARY KEY(crop_name)\n"
                                      + ") ENGINE=InnoDB";
        String insertCropSql = "INSERT INTO workbench.workbench_crop (crop_name, central_db_name) VALUES (?, ?)";
        
        boolean cassava = isComponentSelected(context, CROP_CASSAVA);
        boolean chickpea = isComponentSelected(context, CROP_CHICKPEA);
        boolean cowpea = isComponentSelected(context, CROP_COWPEA);
        boolean maize = isComponentSelected(context, CROP_MAIZE);
        boolean rice = isComponentSelected(context, CROP_RICE);
        boolean wheat = isComponentSelected(context, CROP_WHEAT);
        
        // create the database and user
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS workbench");
            stmt.executeUpdate(dropWorkbenchCropSql);
            stmt.executeUpdate(createWorkbenchCropSql);
            stmt.close();
            
            PreparedStatement pstmt = conn.prepareStatement(insertCropSql);
            if (cassava) {
                pstmt.setString(1, "CASSAVA");
                pstmt.setString(2, "ibdb_cassava_central");
                pstmt.executeUpdate();
            }
            if (chickpea) {
                pstmt.setString(1, "CHICKPEA");
                pstmt.setString(2, "ibdb_chickpea_central");
                pstmt.executeUpdate();
            }
            if (cowpea) {
                pstmt.setString(1, "COWPEA");
                pstmt.setString(2, "ibdb_cowpea_central");
                pstmt.executeUpdate();
            }
            if (maize) {
                pstmt.setString(1, "MAIZE");
                pstmt.setString(2, "ibdb_maize_central");
                pstmt.executeUpdate();
            }
            if (rice) {
                pstmt.setString(1, "RICE");
                pstmt.setString(2, "ibdb_rice_central");
                pstmt.executeUpdate();
            }
            if (wheat) {
                pstmt.setString(1, "WHEAT");
                pstmt.setString(2, "ibdb_wheat_central");
                pstmt.executeUpdate();
            }
            pstmt.close();
            
            return true;
        }
        catch (SQLException e) {
            e.printStackTrace();
            Util.showErrorMessage(context.getMessage("cannot_initialize_database"));
            return false;
        }
    }
    
    protected boolean runScriptsForCrop(InstallerContext context, Connection conn, String cropName) {
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
            stmt.execute("USE ibdb_" + cropName + "_central");
        }
        catch (SQLException e1) {
            Util.showErrorMessage(context.getMessage("cannot_initialize_database"));
            return false;
        }
        
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
                
                ScriptRunner runner = new ScriptRunner(conn, false, true);
                runner.runScript(br);
            }
            catch (IOException e1) {
                e1.printStackTrace();
                
                Util.showErrorMessage(context.getMessage("cannot_initialize_database"));
                return false;
            }
            catch (SQLException e1) {
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
