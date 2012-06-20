/*******************************************************************************
 * Copyright (c) 2012, All Rights Reserved.
 * 
 * Generation Challenge Programme (GCP)
 * 
 * 
 * This software is licensed for use under the terms of the GNU General Public
 * License (http://bit.ly/8Ztv8M) and the provisions of Part F of the Generation
 * Challenge Programme Amended Consortium Agreement (http://bit.ly/KQX1nL)
 * 
 *******************************************************************************/

package org.generationcp.ibpworkbench.launcher;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher{

    private Logger log = LoggerFactory.getLogger(Launcher.class);

    private String mysqlBinDir = "mysql/bin";
    private String tomcatDir = "tomcat";

    private String workbenchUrl = "http://localhost:18080/ibpworkbench/";

    private Display display;
    private Shell shell;
    private Tray tray;

    private Menu menu;
    private MenuItem launchWorkbenchItem;
    private MenuItem exitItem;
    private TrayItem trayItem;
    private Process mysqlProcess;
    private TomcatServer tomcatServer;

    private Label splashLabel;
    private Label progressLabel;
    private ProgressBar progressBar;

    private File icisIni;
    private File icisIniBakFile;

    private long mysqlHeadstartMillis = 1000;
    private long tomcatHeadstartMillis = 2000;

    private StartupThread startupThread;

    protected void initializeComponents() {
        display = new Display();
        shell = new Shell(display, SWT.NO_TRIM | SWT.INHERIT_DEFAULT);
        shell.setText("IBP Workbench");
        shell.setSize(640, 480);

        // center shell to primary monitor
        Monitor monitor = display.getPrimaryMonitor();
        Rectangle monitorBounds = monitor.getBounds();

        Rectangle bounds = shell.getBounds();
        int x = monitorBounds.x + monitorBounds.width / 2 - bounds.width / 2;
        int y = monitorBounds.y + monitorBounds.height / 2 - bounds.height / 2;
        shell.setLocation(x, y);

        // add the splash image
        splashLabel = new Label(shell, SWT.NONE);
        splashLabel.setImage(new Image(display, "images/splash.png"));

        // add progress label
        progressLabel = new Label(shell, SWT.NONE);
        progressLabel.moveAbove(splashLabel);
        progressLabel.setText("Loading...");

        // add the progress bar
        progressBar = new ProgressBar(shell, SWT.HORIZONTAL | SWT.INDETERMINATE);
        progressBar.moveAbove(splashLabel);

        // get the System Tray
        tray = display.getSystemTray();
        if (tray == null) {
            throw new RuntimeException("System tray not available");
        }

        // create a System Tray item
        trayItem = new TrayItem(tray, SWT.NONE);
        trayItem.setToolTipText("IBPWorkbench");

        Image image = new Image(display, "images/systray.ico");
        trayItem.setImage(image);

        // create the menu
        menu = new Menu(shell, SWT.POP_UP);

        launchWorkbenchItem = new MenuItem(menu, SWT.PUSH);
        launchWorkbenchItem.setText("Launch IBPWorkbench");

        exitItem = new MenuItem(menu, SWT.PUSH);
        exitItem.setText("Exit");
    }

    protected void initializeLayout() {
        Rectangle shellBounds = shell.getBounds();

        splashLabel.setBounds(0, 0, shellBounds.width, shellBounds.height);

        final int margin = 10;
        // Point progressLabelPreferredSize =
        // progressLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point progressBarPreferredSize = progressBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        // int progressLabelY = shellBounds.height - margin -
        // progressBarPreferredSize.y - margin - progressLabelPreferredSize.y;
        int progressBarY = shellBounds.height - margin - progressBarPreferredSize.y;

        // progressLabel.setBounds(margin, progressLabelY, shellBounds.width -
        // (2* margin), progressLabelPreferredSize.y);
        progressBar.setBounds(margin, progressBarY, shellBounds.width - 2 * margin, progressBarPreferredSize.y);
    }

    protected void initializeActions() {
        // add listeners System Tray Item
        Listener showMenuListener = new Listener() {

            public void handleEvent(Event event) {
                menu.setVisible(true);
            }
        };
        trayItem.addListener(SWT.Selection, showMenuListener);
        trayItem.addListener(SWT.MenuDetect, showMenuListener);

        // add listener to the Launch IBPWorkbench listener
        launchWorkbenchItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    Program.launch(workbenchUrl);
                } catch (Exception ex) {
                    log.error("Cannot launch workbench due to error", ex);
                }
            }
        });

        // add listener to the Exit menu item
        exitItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                log.debug("Exiting workbench launcher");

                try {
                    startupThread.join();
                } catch (InterruptedException e1) {
                    log.error("Interrupted while waiting for startup thread to stop", e);
                }

                shell.dispose();

                shutdownMysql();
                shutdownWebApps();
                revertIcisIni();
            }
        });
    }

    protected void initializeMysql() {
        log.trace("Starting MySQL...");

        File workingDirPath = new File(mysqlBinDir).getAbsoluteFile();
        String mysqldPath = "mysqld.exe";
        String myIniPath = "../my.ini";

        ProcessBuilder pb = new ProcessBuilder(workingDirPath.getAbsolutePath() + File.separator + mysqldPath, "--defaults-file="
                + myIniPath);
        pb.directory(workingDirPath);
        try {
            mysqlProcess = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // give Tomcat a headstart
        try {
            Thread.sleep(mysqlHeadstartMillis);
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for MySQL to start", e);
        }

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            log.error("Error encountered while trying to load JDBC Driver", e);
        }

        Connection conn = null;
        while (true) {
            try {
                conn = DriverManager.getConnection("jdbc:mysql://localhost:13306/?user=root");
                break;
            } catch (SQLException e) {
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Interrupted while trying to connect to MySQL", e);
                break;
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Error encountered while trying to close JDBC connection", e);
            }
        }
    }

    protected void initializeTomcat() {
        log.trace("Starting Tomcat...");

        File tomcatBinPath = new File(tomcatDir).getAbsoluteFile();
        tomcatServer = new TomcatServer();
        tomcatServer.setCatalinaHome(tomcatBinPath.getAbsolutePath());

        tomcatServer.start();

        // give Tomcat a headstart
        try {
            Thread.sleep(tomcatHeadstartMillis);
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for Tomcat to start", e);
        }

        // try connecting to the workbench url
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(workbenchUrl);
        HttpResponse response = null;
        while (true) {
            try {
                response = httpClient.execute(httpGet);
            } catch (ClientProtocolException e) {
            } catch (IOException e) {
            }

            if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Interrupted while trying to connect to Tomcat", e);
                break;
            }
        }
    }

    protected void shutdownMysql() {
        log.trace("Stopping MySQL...");

        if (mysqlProcess != null) {
            mysqlProcess.destroy();
        }

        try {
            mysqlProcess.waitFor();
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for MySQL to stop", e);
        }
    }

    protected void shutdownWebApps() {
        log.trace("Stopping Tomcat...");

        tomcatServer.stopServer();
    }

    protected void renameIcisIni() {
        String tempDir = System.getProperty("java.io.tmpdir");
        icisIni = new File(tempDir + File.separator + "ICIS.ini");
        icisIniBakFile = new File(tempDir + File.separator + "ICIS.ini.bak");

        if (icisIni.exists()) {
            boolean result = icisIni.renameTo(icisIniBakFile);
            log.debug("Renamed ICIS.ini to ICIS.ini.bak: {}", result);
        }
    }

    protected void revertIcisIni() {
        if (icisIniBakFile.exists()) {
            boolean result = icisIniBakFile.renameTo(icisIni);
            log.debug("Renamed ICIS.ini.bak to ICIS.ini: {}", result);
        }
    }

    public void open() {
        // show the splash screen
        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    public void assemble() {
        initializeComponents();
        initializeLayout();
        initializeActions();

        startupThread = new StartupThread();
        startupThread.start();
    }

    private class StartupThread extends Thread{

        @Override
        public void run() {
            // rename %TEMP%/icis.ini
            renameIcisIni();

            // initialize MySQL
            initializeMysql();

            // initialize Tomcat
            initializeTomcat();

            // launch the workbench url
            display.asyncExec(new Runnable() {

                public void run() {
                    try {
                        Program.launch(workbenchUrl);
                    } catch (Exception ex) {
                        log.error("Cannot launch workbench due to error", ex);
                    }

                    shell.setVisible(false);
                }
            });
        }
    }

    public static void main(String[] args) {
        Launcher launcher = new Launcher();
        launcher.assemble();

        launcher.open();
    }
}
