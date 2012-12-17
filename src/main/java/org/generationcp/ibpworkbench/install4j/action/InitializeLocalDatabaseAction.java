package org.generationcp.ibpworkbench.install4j.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
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

public class InitializeLocalDatabaseAction extends AbstractInstallAction {
    private static final long serialVersionUID = 1L;
    
    private final static String DATABASE_LOCAL_DATA_PATH        = "database/local";
    private final static String DATABASE_LOCAL_COMMON_DATA_PATH = "database/local/common";

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
                    
                    success = createFieldbookConfigurationForCrop(context, crop);
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        finally {
            try {
                connection.close();
            }
            catch (SQLException e) {
            }
        }
        
        return true;
    }
    
    protected boolean runScriptsForCrop(InstallerContext context, Connection conn, Crop crop) {
        String cropName = crop.getCropName();
        
        Object[] cropTitleParam = new Object[]{ context.getMessage(cropName) };
        
        // check if the central database is already installed
        boolean databaseExists = false;
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("USE ibdbv1_" + cropName + "_local");
            databaseExists = true;
        }
        catch (SQLException e1) {
            databaseExists = false;
        }
        
        if (databaseExists) {
            String cropTitle = context.getMessage(cropName);
            String message = context.getMessage("confirm_local_database_update", new Object[] { cropTitle });
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
            String databaseName = "ibdbv1_" + cropName + "_local";
            String userName = "local";
            String password = "local";
            
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
        
        File localDatabaseDir = new File(context.getInstallationDirectory(), DATABASE_LOCAL_DATA_PATH);
        File localCropCommonDir = new File(context.getInstallationDirectory(), DATABASE_LOCAL_COMMON_DATA_PATH);
        File localCropDir = new File(localDatabaseDir, cropName);
        
        if (!runScriptsOnDirectory(context, conn, cropTitleParam, localCropCommonDir)) {
            return false;
        }
        if (!runScriptsOnDirectory(context, conn, cropTitleParam, localCropDir)) {
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
    
    protected boolean createFieldbookConfigurationForCrop(Context context, Crop crop) {
        String format = "dmscentral.hibernateDialect=\r\n"
            + "dmscentral.url=%s\r\n"
            + "dmscentral.driverclassname=com.mysql.jdbc.Driver\r\n"
            + "dmscentral.username=%s\r\n"
            + "dmscentral.password=%s\r\n"
            + "dmscentral.accessType=central\r\n"
            + ""
            + "gmscentral.hibernateDialect=\r\n"
            + "gmscentral.url=%s\r\n"
            + "gmscentral.driverclassname=com.mysql.jdbc.Driver\r\n"
            + "gmscentral.username=%s\r\n"
            + "gmscentral.password=%s\r\n"
            + "gmscentral.accessType=central\r\n"
            + "\r\n"
            + "dmslocal.hibernateDialect=\r\n"
            + "dmslocal.url=%s\r\n"
            + "dmslocal.driverclassname=com.mysql.jdbc.Driver\r\n"
            + "dmslocal.username=%s\r\n"
            + "dmslocal.password=%s\r\n"
            + "dmslocal.accessType=local\r\n"
            + ""
            + "gmslocal.hibernateDialect=\r\n"
            + "gmslocal.url=%s\r\n"
            + "gmslocal.driverclassname=com.mysql.jdbc.Driver\r\n"
            + "gmslocal.username=%s\r\n"
            + "gmslocal.password=%s\r\n"
            + "gmslocal.accessType=local\r\n"
            ;
        
        String jdbcHost         = context.getCompilerVariable("gcp.jdbc.host");
        String jdbcPort         = context.getCompilerVariable("gcp.jdbc.port");
        String centralUser      = context.getCompilerVariable("gcp.jdbc.central.user");
        String centralPassword  = context.getCompilerVariable("gcp.jdbc.central.password");
        String localUser        = context.getCompilerVariable("gcp.jdbc.local.user");
        String localPassword    = context.getCompilerVariable("gcp.jdbc.local.password");
        
        String jdbcFormat = "jdbc:mysql://%s:%s/%s";

        String centralDbName = String.format("ibdb_%s_central", crop.getCropName().toLowerCase());
        String localDbName = String.format("ibdbv1_%s_local", crop.getCropName().toLowerCase());
        
        String centralJdbcString = String.format(jdbcFormat, jdbcHost, jdbcPort, centralDbName);
        String localJdbcString = String.format(jdbcFormat, jdbcHost, jdbcPort, localDbName);

        String configuration = String.format(format, centralJdbcString, centralUser, centralPassword
                                             , centralJdbcString, centralUser, centralPassword
                                             , localJdbcString, localUser, localPassword
                                             , localJdbcString, localUser, localPassword);

        File configurationFile = new File(System.getProperty("java.io.tmpdir"), "databaseconfig.properties");
        
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(configurationFile);
            fos.write(configuration.getBytes());
            fos.flush();
        }
        catch (IOException e) {
            Util.showErrorMessage(context.getMessage("cannot_create_fieldbook_configuration"));
            return false;
        }
        finally {
            if (fos != null) {
                try {
                    fos.close();
                }
                catch (IOException e) {
                    // intentionally empty
                }
            }
        }
        
        return true;
    }
}
