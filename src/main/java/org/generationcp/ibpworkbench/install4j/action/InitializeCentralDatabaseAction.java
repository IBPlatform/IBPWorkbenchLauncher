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

import javax.swing.JOptionPane;

import org.generationcp.ibpworkbench.install4j.Crop;
import org.generationcp.ibpworkbench.install4j.Install4JUtil;
import org.generationcp.ibpworkbench.util.ScriptRunner;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.Context;
import com.install4j.api.context.InstallationComponentSetup;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

public class InitializeCentralDatabaseAction extends AbstractInstallAction {
    private static final long serialVersionUID = 1L;
    
    private final static String DATABASE_DATA_PATH = "database";
    private final static String DATABASE_CENTRAL_DATA_PATH = "database/central";
    private final static String DATABASE_CENTRAL_COMMON_DATA_PATH = "database/central/common";
    
    protected boolean isComponentSelected(InstallerContext context, Crop crop) {
        InstallationComponentSetup component = context.getInstallationComponentById(crop.getCropName());
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
            for (Crop crop : Crop.values()) {
                if (isComponentSelected(context, crop)) {
                    boolean success = runScriptsForCrop(context, connection, crop);
                    if (!success) {
                        return false;
                    }
                }
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
    
    /**
     * Register selected crops into workbench.workbench_crop table.
     * We SHOULD AVOID introducing any side effect into any existing workbench_crop table.
     * 
     * @param context
     * @param conn
     * @return
     */
    protected boolean registerCrops(InstallerContext context, Connection conn) {
        if (!executeUpdateOrError(context, conn, "CREATE DATABASE IF NOT EXISTS workbench")) {
            return false;
        }
        
        // create the workbench_crop table if it does not exist
        String createWorkbenchCropIfNotExistSql = "CREATE TABLE IF NOT EXISTS workbench.workbench_crop(\n"
                                                + "    crop_name VARCHAR(32) NOT NULL\n"
                                                + "   ,central_db_name VARCHAR(64)\n"
                                                + "   ,PRIMARY KEY(crop_name)\n"
                                                + ") ENGINE=InnoDB";
        if (!executeUpdateOrError(context, conn, createWorkbenchCropIfNotExistSql)) {
            return false;
        }
        
        // check if the workbench_crop table exists in the correct format
        String workbenchCropCheckSql = "SELECT crop_name, central_db_name FROM workbench.workbench_crop LIMIT 1";
        boolean workbenchCropCorrect = canExecuteQueries(context, conn, workbenchCropCheckSql);
        
        if (!workbenchCropCorrect) {
            // if the workbench_crop table is in an incompatible format, drop and re-create it
            String dropWorkbenchCropSql = "DROP TABLE IF EXISTS workbench.workbench_crop";
            String createWorkbenchCropSql = "CREATE TABLE workbench.workbench_crop(\n"
                                          + "    crop_name VARCHAR(32) NOT NULL\n"
                                          + "   ,central_db_name VARCHAR(64)\n"
                                          + "   ,PRIMARY KEY(crop_name)\n"
                                          + ") ENGINE=InnoDB";
            if (!executeUpdateOrError(context, conn, dropWorkbenchCropSql, createWorkbenchCropSql)) {
                return false;
            }
        }
        
        String insertCropSql = "REPLACE INTO workbench.workbench_crop (crop_name, central_db_name) VALUES (?, ?)";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(insertCropSql);
            if (!workbenchCropCorrect) {
                // if workbench_crop table was recreated,
                // we do our best effort to recreate the workbench_crop table
                for (Crop crop : Crop.values()) {
                    if (!centralDatabaseExists(conn, crop.getCentralDatabaseName())) {
                        continue;
                    }
                    
                    pstmt.setString(1, crop.getCropName());
                    pstmt.setString(2, crop.getCentralDatabaseName());
                    pstmt.executeUpdate();
                }
            }
            else {
                // update workbench_crop with selected crops only
                for (Crop crop : Crop.values()) {
                    if (isComponentSelected(context, crop)) {
                        pstmt.setString(1, crop.getCropName());
                        pstmt.setString(2, crop.getCentralDatabaseName());
                        pstmt.executeUpdate();
                    }
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            Util.showErrorMessage(context.getMessage("cannot_initialize_database"));
            return false;
        }
        finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            }
            catch (SQLException e2) {
                // intentionally empty
            }
        }
        
        return true;
    }
    
    protected boolean executeUpdateOrError(Context context, Connection conn, String... queries) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            for (String query : queries) {
                stmt.executeUpdate(query);
            }
            return true;
        }
        catch (SQLException e) {
            e.printStackTrace();
            Util.showErrorMessage(context.getMessage("cannot_initialize_database"));
            return false;
        }
        finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            }
            catch (SQLException e2) {
                // intentionally empty
            }
        }
    }
    
    protected boolean executeQueryOrError(Context context, Connection conn, String... queries) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            for (String query : queries) {
                stmt.executeQuery(query);
            }
            return true;
        }
        catch (SQLException e) {
            e.printStackTrace();
            Util.showErrorMessage(context.getMessage("cannot_initialize_database"));
            return false;
        }
        finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            }
            catch (SQLException e2) {
                // intentionally empty
            }
        }
    }
    
    protected boolean canExecuteQueries(Context context, Connection conn, String... queries) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            for (String query : queries) {
                stmt.executeQuery(query);
            }
            return true;
        }
        catch (SQLException e) {
            return false;
        }
        finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            }
            catch (SQLException e2) {
                // intentionally empty
            }
        }
    }
    
    protected boolean centralDatabaseExists(Connection conn, String centralDatabaseName) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute("USE " + centralDatabaseName);
            return true;
        }
        catch (SQLException e) {
            return false;
        }
        finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            }
            catch (SQLException e2) {
                // intentionally empty
            }
        }
    }
    
    protected boolean runScriptsForCrop(InstallerContext context, Connection conn, Crop crop) {
        String cropName = crop.getCropName();
        
        Object[] cropTitleParam = new Object[]{ context.getMessage(cropName) };
        
        // check if the central database is already installed
        boolean databaseExists = false;
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("USE ibdb_" + cropName + "_central");
            databaseExists = true;
        }
        catch (SQLException e1) {
            databaseExists = false;
        }
        
        if (databaseExists) {
            String cropTitle = context.getMessage(cropName);
            String message = context.getMessage("confirm_central_database_update", new Object[] { cropTitle });
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
        try {
            Statement stmt = conn.createStatement();
            String databaseName = "ibdb_" + cropName + "_central";
            String userName = "central";
            String password = "central";
            
            stmt.executeUpdate("DROP DATABASE IF EXISTS " + databaseName);
            stmt.executeUpdate("CREATE DATABASE " + databaseName);
            stmt.executeUpdate("GRANT ALL ON " + databaseName + ".* TO '" + userName + "'@'localhost' IDENTIFIED BY '" + password + "'");
            stmt.executeUpdate("FLUSH PRIVILEGES");
            stmt.execute("USE " + databaseName);
        }
        catch (SQLException e1) {
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
                
                ScriptRunner runner = new ScriptRunner(conn, false, true);
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
