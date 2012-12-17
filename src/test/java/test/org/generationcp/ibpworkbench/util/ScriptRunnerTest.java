package test.org.generationcp.ibpworkbench.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.Assert;

import org.generationcp.ibpworkbench.util.ScriptRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ScriptRunnerTest {
    
    private Connection connection;
    
    private final static String SCRIPT_CASSAVA = "crop_databases/central/cassava/01_icass_ibdb_06072012.sql";
    private final static String SCRIPT_CHICKPEA = "crop_databases/central/chickpea/01_ichis_ibdb_05252012.sql";
    private final static String SCRIPT_COWPEA = "crop_databases/central/cowpea/01_ibdbv1_ivis_innodb_20120530.sql";
    private final static String SCRIPT_MAIZE = "crop_databases/central/maize/01_imiscentral_innodb_20120524.sql";

    @Before
    public void beforeTest() {
        connection = connectToMySQL();
        
        Assert.assertNotNull(connection);
    }
    
    @After
    public void afterTest() {
        try {
            connection.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    protected Connection connectToMySQL() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        
        // retry until we can connect to mysql
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:13306/?user=root");
            return conn;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    @Test
    public void testCassava() {
        testCrop("cassava", SCRIPT_CASSAVA);
    }
    
    @Test
    public void testChickpea() {
        testCrop("chickpea", SCRIPT_CHICKPEA);
    }
    
    @Test
    public void testCowpea() {
        testCrop("cowpea", SCRIPT_COWPEA);
    }
    
    @Test
    public void testMaize() {
        testCrop("maize", SCRIPT_MAIZE);
    }

    private void testCrop(String cropType, String script) {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("DROP DATABASE IF EXISTS ibdb_" + cropType + "_central;");
            stmt.executeUpdate("CREATE DATABASE ibdb_" + cropType + "_central;");
            stmt.execute("USE ibdb_" + cropType + "_central");
            
            testScriptRunner(script);
        }
        catch (SQLException e) {
            Assert.fail(e.getMessage());
        }
        finally {
            if (stmt != null) {
                try {
                    stmt.close();
                }
                catch (SQLException e) {
                }
            }
        }
    }
    
    private void testScriptRunner(String script) {
            System.out.println("Running " + script);
            
            BufferedReader br = null;
            Exception exception = null;
            
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(script)));

                ScriptRunner runner = new ScriptRunner(connection, false, true);
                runner.runScript(br);

                br.close();
            }
            catch (IOException e) {
                exception = e;
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
            
            if (exception != null) {
                exception.printStackTrace();
                Assert.fail(exception.getMessage());
            }
    }
}
