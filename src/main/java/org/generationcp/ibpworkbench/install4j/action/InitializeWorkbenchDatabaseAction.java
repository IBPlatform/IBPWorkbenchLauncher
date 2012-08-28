package org.generationcp.ibpworkbench.install4j.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.generationcp.ibpworkbench.install4j.Install4JUtil;
import org.generationcp.ibpworkbench.util.ScriptRunner;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

public class InitializeWorkbenchDatabaseAction extends AbstractInstallAction {
    private static final long serialVersionUID = 1L;
    
    private final static String DATABASE_WORKBENCH_DATA_PATH = "database/workbench";

    public boolean install(InstallerContext context) throws UserCanceledException {
        // connect to MySQL
        Connection connection = Install4JUtil.connectToMySQL(context);
        if (connection == null) {
            return false;
        }
        
        // run workbench scripts
        try {
            runWorkbenchScripts(context, connection);
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
    
    protected boolean runWorkbenchScripts(InstallerContext context, Connection conn) {
        File workbenchDatabaseDir = new File(context.getInstallationDirectory(), DATABASE_WORKBENCH_DATA_PATH);
        Object[] workbenchTitleParam = new Object[]{ context.getMessage("workbench") };
        
        // create the database and user
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS workbench");
            stmt.executeUpdate("GRANT ALL ON workbench.* TO 'workbench'@'localhost' IDENTIFIED BY 'workbench'");
            stmt.executeUpdate("USE workbench");
        }
        catch (SQLException e1) {
            Util.showErrorMessage(context.getMessage("cannot_initialize_database"));
            return false;
        }
        
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
