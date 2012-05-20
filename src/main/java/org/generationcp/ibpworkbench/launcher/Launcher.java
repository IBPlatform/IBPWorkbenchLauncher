package org.generationcp.ibpworkbench.launcher;

import java.io.File;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {
    private Logger log = LoggerFactory.getLogger(Launcher.class);
    
    private Display display;
    private Shell shell;
    private Tray tray;
    
    private String mysqlBinDir = "mysql/bin";
    private String tomcatDir = "tomcat";
    
    private Menu menu;
    private MenuItem launchWorkbenchItem;
    private MenuItem exitItem;
    private TrayItem trayItem;
    private Process mysqlProcess;
    private TomcatServer tomcatServer;

    protected void initializeComponents() {
        display = new Display ();
        shell = new Shell(display);
        
        // get the System Tray
        tray = display.getSystemTray ();
        if (tray == null) {
            throw new RuntimeException("System tray not available");
        }
        
        // create a System Tray item
        trayItem = new TrayItem (tray, SWT.NONE);
        trayItem.setToolTipText("IBPWorkbench");
        
        Image image = new Image(display, "images/systray.ico");
        trayItem.setImage(image);
        
        // create the menu
        menu = new Menu (shell, SWT.POP_UP);
        
        launchWorkbenchItem = new MenuItem(menu, SWT.PUSH);
        launchWorkbenchItem.setText("Launch IBPWorkbench");
        
        exitItem = new MenuItem(menu, SWT.PUSH);
        exitItem.setText("Exit");
    }
    
    protected void initializeActions() {
        // add listeners System Tray Item
        Listener showMenuListener = new Listener () {
            public void handleEvent (Event event) {
                menu.setVisible(true);
            }
        };
        trayItem.addListener (SWT.Selection, showMenuListener);
        trayItem.addListener (SWT.MenuDetect, showMenuListener);
        
        // add listener to the Launch IBPWorkbench listener
        launchWorkbenchItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    Program.launch("http://localhost:18080/ibpworkbench/");
                }
                catch (Exception ex) {
                    log.error("Cannot launch workbench due to error", ex);
                }
            }
        });
        
        // add listener to the Exit menu item
        exitItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                log.debug("Exiting workbench launcher");
                
                shell.dispose();
                
                shutdownMysql();
                shutdownWebApps();
            }
        });
    }
    
    protected void initializeMysql() {
        log.trace("Starting MySQL...");
        
        File workingDirPath = new File(mysqlBinDir).getAbsoluteFile();
        String mysqldPath = "mysqld.exe";
        String myIniPath =  "../my.ini";
        
        ProcessBuilder pb = new ProcessBuilder(workingDirPath.getAbsolutePath() + File.separator + mysqldPath, "--defaults-file=" + myIniPath);
        pb.directory(workingDirPath);
        try {
            mysqlProcess = pb.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected void initializeWebApps() {
        log.trace("Starting Tomcat...");
        
        File tomcatBinPath = new File(tomcatDir).getAbsoluteFile();
        tomcatServer = new TomcatServer();
        tomcatServer.setCatalinaHome(tomcatBinPath.getAbsolutePath());
        
        tomcatServer.start();
    }
    
    protected void shutdownMysql() {
        log.trace("Stopping MySQL...");
        
        if (mysqlProcess != null) {
            mysqlProcess.destroy();
        }
        
        try {
            mysqlProcess.waitFor();
        }
        catch (InterruptedException e) {
            log.error("Interrupted while waiting for MySQL to stop", e);
        }
    }
    
    protected void shutdownWebApps() {
        log.trace("Stopping Tomcat...");
        
        tomcatServer.stopServer();
    }
    
    public void open() {
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
    }
    
    public void assemble() {
        initializeComponents();
        initializeActions();
        initializeMysql();
        initializeWebApps();
    }
    
    public static void main(String[] args) {
        Launcher launcher = new Launcher();
        launcher.assemble();
        
        launcher.open();
    }
}
