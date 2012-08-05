package org.generationcp.ibpworkbench.install4j;

import java.sql.Connection;

public class DatabaseInfo {
    private Process process;
    private Connection connection;

    public DatabaseInfo(Process process, Connection connection) {
        this.process = process;
        this.connection = connection;
    }

    public Process getProcess() {
        return process;
    }

    public Connection getConnection() {
        return connection;
    }
}
