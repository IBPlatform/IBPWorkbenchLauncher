package org.generationcp.ibpworkbench.util;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;

import com.install4j.api.context.Context;

public class Install4JScriptRunner extends ScriptRunner {
    private Context context;
    
    public Install4JScriptRunner(Context context, Connection connection) {
        super(connection);
        
        this.context = context;
    }
    
    public Install4JScriptRunner(Context context, Connection connection, boolean autoCommit, boolean stopOnError) {
        super(connection, autoCommit, stopOnError);
        
        this.context = context;
    }

    protected void executeStatement(String command) throws SQLException, UnsupportedEncodingException {
        context.getProgressInterface().setDetailMessage(command);
        
        super.executeStatement(command);
    }
}
