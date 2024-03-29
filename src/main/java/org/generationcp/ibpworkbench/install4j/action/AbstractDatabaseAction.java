package org.generationcp.ibpworkbench.install4j.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.generationcp.ibpworkbench.install4j.DatabaseInfo;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

/**
 * An install action that works with a MySQL database.
 * 
 * @author Glenn
 */
public abstract class AbstractDatabaseAction extends AbstractInstallAction {
    private static final long serialVersionUID = 1L;
    
    private final static String MYSQL_PATH = "mysql";
    private final static String MY_INI_PATH = MYSQL_PATH + "/my.ini";
    private final static String MY_INI_BAK_PATH = MYSQL_PATH + "/my.ini.bak";
    
    private final static long MYSQL_HEADSTART_MILLIS = 1000;
    private final static long MYSQL_WAIT_MILLIS = 10000;
    private final static long MYSQL_CONNECT_TRIES = 6;

    protected DatabaseInfo startMySQL(InstallerContext context) throws UserCanceledException {
        File mysqlBinPath = new File(context.getInstallationDirectory(), "mysql/bin");
        String mysqldExeName = "mysqld.exe";
        String myIniPath =  "../my.ini";
        
        File mysqldFile = new File(mysqlBinPath, mysqldExeName);
        if (!mysqldFile.exists()) {
            Util.showErrorMessage(context.getMessage("mysql_not_installed"));
            return null;
        }
        
        // start MySQL
        String mysqldPath = mysqlBinPath.getAbsolutePath() + File.separator + mysqldExeName;
        ProcessBuilder pb = new ProcessBuilder(mysqldPath, "--defaults-file=" + myIniPath);
        pb.directory(mysqlBinPath);
        
        context.getProgressInterface().setStatusMessage(context.getMessage("starting_mysql"));
        context.getProgressInterface().setDetailMessage(mysqldPath);
        
        Process mysqlProcess = null;
        try {
            mysqlProcess = pb.start();
        }
        catch (IOException e) {
            Util.showErrorMessage(context.getMessage("cannot_start_mysql"));
            return null;
        }
        
        context.getProgressInterface().setStatusMessage(context.getMessage("connecting_to_mysql"));
        context.getProgressInterface().setDetailMessage("");
        
        // give MySQL a headstart
        try {
            Thread.sleep(MYSQL_HEADSTART_MILLIS);
        }
        catch (InterruptedException e) {
            throw new UserCanceledException(e.getMessage());
        }
        
        try {
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            Util.showErrorMessage(context.getMessage("cannot_load_jdbc_driver"));
            return null;
        }
        
        // retry until we can connect to mysql
        Connection conn = null;
        int tryNum = 0;
        while (true) {
            if (tryNum >= MYSQL_CONNECT_TRIES) {
                Util.showErrorMessage(context.getMessage("cannot_connect_to_mysql"));
                return null;
            }
            tryNum++;
            
            try {
                conn = DriverManager.getConnection("jdbc:mysql://localhost:13306/?user=root");
                break;
            }
            catch (SQLException e) {
            }
            
            try {
                Thread.sleep(MYSQL_WAIT_MILLIS);
            }
            catch (InterruptedException e) {
                break;
            }
        }
        
        return new DatabaseInfo(mysqlProcess, conn);
    }
    
    protected boolean stopMySql(InstallerContext context, DatabaseInfo databaseInfo) {
        File mysqlBinPath = new File(context.getInstallationDirectory(), "mysql/bin");
        String mysqlAdminExeName = "mysqladmin.exe";
        String myIniPath =  "../my.ini";
        
        Connection conn = databaseInfo.getConnection();
        Process mysqlProcess = databaseInfo.getProcess();
        
        // close JDBC connection
        if (conn != null) {
            try {
                conn.close();
            }
            catch (SQLException e) {
            }
        }
        
        // stop MySQL using MySQL Admin
        String mysqlAdminPath = mysqlBinPath.getAbsolutePath() + File.separator + mysqlAdminExeName;
        
        ProcessBuilder mysqlAdminPb = new ProcessBuilder(mysqlAdminPath, "--defaults-file=" + myIniPath, "-u", "root", "shutdown");
        mysqlAdminPb.directory(mysqlBinPath);
        
        context.getProgressInterface().setStatusMessage(context.getMessage("stopping_mysql"));
        context.getProgressInterface().setDetailMessage("");
        
        Process stopMySQLProcess = null;
        try {
            stopMySQLProcess = mysqlAdminPb.start();
        }
        catch (IOException e) {
            Util.showErrorMessage(context.getMessage("cannot_stop_mysql"));
            return false;
        }
        
        // wait for the MySQL Admin process to terminate
        try {
            stopMySQLProcess.waitFor();
        }
        catch (InterruptedException e) {
            Util.showErrorMessage(context.getMessage("cannot_stop_mysql"));
            return false;
        }
        
        // wait for the MySQL process to terminate
        try {
            mysqlProcess.waitFor();
        }
        catch (InterruptedException e) {
            Util.showErrorMessage(context.getMessage("cannot_stop_mysql"));
            return false;
        }
        
        return true;
    }
    
    protected boolean optimizeMySQLConfiguration(InstallerContext context) {
        File mysqlIniPath = new File(context.getInstallationDirectory(), MY_INI_PATH);
        File mysqlIniBakPath = new File(context.getInstallationDirectory(), MY_INI_BAK_PATH);
        
        Long ram = (Long) context.getVariable("gcp.mysql.ram.size");
        if (ram == null) {
            Util.showErrorMessage(context.getMessage("invalid_installer_configuration"));
            return false;
        }
        
        if (!mysqlIniPath.exists()) {
            Util.showErrorMessage(context.getMessage("invalid_installer_configuration"));
            return false;
        }
        
        if (!mysqlIniPath.renameTo(mysqlIniBakPath)) {
            Util.showErrorMessage(context.getMessage("installation_error"));
            return false;
        }
        
        // long systemMemory = Math.round(SystemInfo.getPhysicalMemory() / (1024 * 1024));
        
        // Size of the Key Buffer, used to cache index blocks for MyISAM tables.
        // Do not set it larger than 30% of your available memory, as some
        // memory is also required by the OS to cache rows. Even if you're not
        // using MyISAM tables, you should still set it to 8-64M as it will also
        // be used for internal temporary disk tables.
        // We set it at 30% of available memory but must be between 8 to 512MB.
        long keyBufferSize = Math.max(Math.min(Math.round(ram * 0.30), 512), 8);
        

        // MyISAM uses special tree-like cache to make bulk inserts (that is,
        // INSERT ... SELECT, INSERT ... VALUES (...), (...), ..., and LOAD DATA
        // INFILE) faster. This variable limits the size of the cache tree in
        // bytes per thread. Setting it to 0 will disable this optimization. Do
        // not set it larger than "key_buffer_size" for optimal performance.
        // This buffer is allocated when a bulk insert is detected.
        // We set it at 20% of available memory but must be between 8 to 512MB.
        long bulkInsertBufferSize = Math.max(Math.min(Math.round(ram * 0.20), 512), 8);
        
        // The maximum size of a query packet the server can handle as well as
        // maximum query size server can process (Important when working with
        // large BLOBs). enlarged dynamically, for each connection.
        long maxAllowedPacket = 16;
        
        // Sort buffer is used to perform sorts for some ORDER BY and GROUP BY
        // queries. If sorted data does not fit into the sort buffer, a disk
        // based merge sort is used instead - See the "Sort_merge_passes" status
        // variable. Allocated per thread if sort is needed.
        long sortBufferSize = 8;
        
        // Size of the buffer used for doing full table scans. Allocated per
        // thread, if a full scan is needed.
        long readBufferSize = 8;
        
        // When reading rows in sorted order after a sort, the rows are read
        // through this buffer to avoid disk seeks. You can improve ORDER BY
        // performance a lot, if set this to a high value. Allocated per thread,
        // when needed
        long readRndBufferSize = 16;
        
        // This buffer is allocated when MySQL needs to rebuild the index in
        // REPAIR, OPTIMIZE, ALTER table statements as well as in LOAD DATA
        // INFILE into an empty table. It is allocated per thread so be careful
        // with large settings.
        long myisamSortBufferSize = 128;
        
        // How many threads we should keep in a cache for reuse. When a client
        // disconnects, the client's threads are put in the cache if there
        // aren't more than thread_cache_size threads from before. This greatly
        // reduces the amount of thread creations needed if you have a lot of
        // new connections. (Normally this doesn't give a notable performance
        // improvement if you have a good thread implementation.)
        long threadCacheSize = 8;
        
        // This permits the application to give the threads system a hint for
        // the desired number of threads that should be run at the same time.
        // This value only makes sense on systems that support the
        // thread_concurrency() function call (Sun Solaris, for example). You
        // should try [number of CPUs]*(2..4) for thread_concurrency
        long threadConcurrency = Runtime.getRuntime().availableProcessors() * 4;

        // Query cache is used to cache SELECT results and later return them
        // without actual executing the same query once again. Having the query
        // cache enabled may result in significant speed improvements, if your
        // have a lot of identical queries and rarely changing tables. See the
        // "Qcache_lowmem_prunes" status variable to check if the current value
        // is high enough for your load. Note: In case your tables change very
        // often or if your queries are textually different every time, the
        // query cache may result in a slowdown instead of a performance
        // improvement.
        long queryCacheSize = 64;
        
        // InnoDB, unlike MyISAM, uses a buffer pool to cache both indexes and
        // row data. The bigger you set this the less disk I/O is needed to
        // access data in tables. On a dedicated database server you may set
        // this parameter up to 80% of the machine physical memory size. Do not
        // set it too large, though, because competition of the physical memory
        // may cause paging in the operating system. Note that on 32bit systems
        // you might be limited to 2-3.5G of user level memory per process, so
        // do not set it too high.
        // We set it at 50% of available memory but must be between 8MB to 1GB
        long innodbBufferPoolSize = Math.max(Math.min(Math.round(ram * 0.50), 1024), 8);

        // Number of IO threads to use for async IO operations. This value is
        // hardcoded to 8 on Unix, but on Windows disk I/O may benefit from a
        // larger number.
        long innodbWriteIoThreads = 8;
        long innodbReadIoThreads = 8;
        
        // The size of the buffer InnoDB uses for buffering log data. As soon as
        // it is full, InnoDB will have to flush it to disk. As it is flushed
        // once per second anyway, it does not make sense to have it very large
        // (even with long transactions). 
        long innodbLogBufferSize = 8;

        // Size of each log file in a log group. You should set the combined
        // size of log files to about 25%-100% of your buffer pool size to avoid
        // unneeded buffer pool flush activity on log file overwrite. However,
        // note that a larger logfile size will increase the time needed for the
        // recovery process.
        // We set it at 50% of InnoDB buffer pool size but must be between 8MB to 256MB
        long innodbLogFileSize = Math.max(Math.min(Math.round(innodbBufferPoolSize * 0.5), 256), 8);
        
        String optimizedMyIni = "[client]\r\n"
                              + "port           = 13306\r\n"
                              + "socket         = /tmp/mysql.sock\r\n"
                              + "[mysqld]\r\n"
                              + "port       = 13306\r\n"
                              + "socket      = /tmp/mysql.sock\r\n"
                              + "skip-external-locking\r\n"
                              + "key_buffer_size = %dM\r\n"
                              + "bulk_insert_buffer_size = %dM\r\n"
                              + "max_allowed_packet = %dM\r\n"
                              + "sort_buffer_size = %dM\r\n"
                              + "read_buffer_size = %dM\r\n"
                              + "read_rnd_buffer_size = %dM\r\n"
                              + "myisam_sort_buffer_size = %dM\r\n"
                              + "thread_cache_size = %d\r\n"
                              + "thread_concurrency = %d\r\n"
                              + "query_cache_size= %dM\r\n"
                              + "innodb_buffer_pool_size = %dM\r\n"
                              + "innodb_data_file_path = ibdata1:10M:autoextend\r\n"
                              + "innodb_write_io_threads = %d\r\n"
                              + "innodb_read_io_threads = %d\r\n"
                              + "innodb_log_buffer_size = %dM\r\n"
                              + "innodb_log_file_size = %dM\r\n"
                              + "innodb_fast_shutdown = 0\r\n"
                              ;
        
        String fileContents = String.format(optimizedMyIni, keyBufferSize
                                                          , bulkInsertBufferSize
                                                          , maxAllowedPacket
                                                          , sortBufferSize
                                                          , readBufferSize
                                                          , readRndBufferSize
                                                          , myisamSortBufferSize
                                                          , threadCacheSize
                                                          , threadConcurrency
                                                          , queryCacheSize
                                                          , innodbBufferPoolSize
                                                          , innodbWriteIoThreads
                                                          , innodbReadIoThreads
                                                          , innodbLogBufferSize
                                                          , innodbLogFileSize
                                            );
        
        try {
            FileOutputStream fos = new FileOutputStream(mysqlIniPath);
            fos.write(fileContents.getBytes());
            fos.flush();
            fos.close();
        }
        catch (IOException e) {
            Util.showErrorMessage(context.getMessage("installation_error") + ": " + e.getMessage());
            return false;
        }
        
        return true;
    }
    
    protected boolean revertMySQLConfiguration(InstallerContext context) {
        File mysqlIniPath = new File(context.getInstallationDirectory(), MY_INI_PATH);
        File mysqlIniBakPath = new File(context.getInstallationDirectory(), MY_INI_BAK_PATH);
        
        if (!mysqlIniPath.delete()) {
            Util.showErrorMessage(context.getMessage("installation_error"));
            return false;
        }
        
        if (!mysqlIniBakPath.exists()) {
            Util.showErrorMessage(context.getMessage("invalid_installer_configuration"));
            return false;
        }
        
        if (!mysqlIniBakPath.renameTo(mysqlIniPath)) {
            Util.showErrorMessage(context.getMessage("installation_error"));
            return false;
        }
        
        // delete all MySQL log files
        File mysqlDataPath = new File(context.getInstallationDirectory(), MYSQL_PATH + "/data");
        String[] logFilenames = mysqlDataPath.list(new FilenameFilter() {
            
            public boolean accept(File dir, String name) {
                return name.startsWith("ib_logfile");
            }
        });
        
        for (String logFilename : logFilenames) {
            File logFile = new File(mysqlDataPath, logFilename);
            if (logFile.exists()) {
                logFile.delete();
            }
        }
        
        return true;
    }
    
    public boolean install(InstallerContext context) throws UserCanceledException {
        return true;
    }
}
