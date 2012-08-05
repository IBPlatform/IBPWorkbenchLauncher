package org.generationcp.ibpworkbench.install4j.screen;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.install4j.api.SystemInfo;
import com.install4j.api.Util;
import com.install4j.api.context.UserCanceledException;
import com.install4j.api.screens.AbstractInstallerScreen;

public class InputMySQLMemorySize extends AbstractInstallerScreen {
    
    private final static long RECOMMENDED_RAM = Math.round((SystemInfo.getPhysicalMemory() / (1024 * 1024)) * 0.70);
    private final static DecimalFormat INPUT_FORMAT = new DecimalFormat("#,###");
    
    private JTextField tfRam;
    private JLabel lblMb;
    
    public JComponent createComponent() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        
        tfRam = new JFormattedTextField(INPUT_FORMAT);
        tfRam.setHorizontalAlignment(JTextField.RIGHT);
        
        tfRam.setText(INPUT_FORMAT.format(RECOMMENDED_RAM));
        
        Dimension preferredSize = tfRam.getPreferredSize();
        preferredSize.setSize(80, preferredSize.getHeight());
        tfRam.setPreferredSize(preferredSize);
        
        lblMb = new JLabel();
        lblMb.setText("MB");
        
        panel.add(tfRam);
        panel.add(lblMb);
        
        return panel;
    }
    
    @Override
    public boolean next() {
        long ram = RECOMMENDED_RAM;
        try {
            ram = INPUT_FORMAT.parse(tfRam.getText()).longValue();
            if (ram > RECOMMENDED_RAM) {
                ram = RECOMMENDED_RAM;
            }
        }
        catch (ParseException e) {
            int option = 1;
            try {
                String message = getInstallerContext().getMessage("invalid_mysql_memory_input", new Object[]{ RECOMMENDED_RAM });
                option = Util.showOptionDialog(message, new String[]{"Yes", "No"}, 1);
            }
            catch (UserCanceledException e1) {
                option = 1;
            }
            
            if (option == 0) {
                tfRam.setText(INPUT_FORMAT.format(RECOMMENDED_RAM));
            }
            
            return option == 0;
        }
        
        getInstallerContext().setVariable("gcp.mysql.ram.size", new Long(ram));
        
        return super.next();
    }

    public String getTitle() {
        return getInstallerContext().getMessage("adjust_mysql_performance");
    }
    
    public String getSubTitle() {
        return getInstallerContext().getMessage("set_mysql_memory_allocation");
    }

    public boolean isFillHorizontal() {
        return false;
    }

    public boolean isFillVertical() {
        return false;
    }

}
