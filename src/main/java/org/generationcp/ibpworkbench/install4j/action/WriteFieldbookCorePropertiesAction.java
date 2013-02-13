package org.generationcp.ibpworkbench.install4j.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;

/**
 * Writes the core.properties file for Fieldbook in the %APPDATA%/.ibfb/dev/config/Preferences/ibfb/settings
 * folder.  Used in workbench installer.
 * 
 * @author Kevin L. Manansala
 *
 */
public class WriteFieldbookCorePropertiesAction extends AbstractInstallAction{
    
    private static final long serialVersionUID = -615602937530464608L;
    
    private static final String TEMPLATES_DIR = "\\\\fieldbook\\\\Examples\\\\Templates";
    private static final String GERMPLASM_LIST_DIR = "\\\\fieldbook\\\\Examples\\\\GermplasmLists";
    private static final String CROSSES_DIR = "\\\\fieldbook\\\\Examples\\\\IBFieldbookImportCrossSelection";
    
    private static final String IBFB_APPDATA_FOLDER = "/.ibfb/dev/config/Preferences/ibfb/settings";

    public boolean install(InstallerContext context) throws UserCanceledException {
        String format = "CROSSES_DEFAULT_FOLDER=%s\r\n"
            + "GERMPLASM_LIST_DEFAULT_FOLDER=%s\r\n"
            + "READ_ICIS_INI_FILE=0\r\n"
            + "SELECTION_DEFAULT_FOLDER=%s\r\n"
            + "TEMPLATES_DEFAULT_FOLDER=%s\r\n"
            ;
        
        String toolsPath = new File(context.getInstallationDirectory(), "tool").getAbsolutePath();
        toolsPath = toolsPath.replaceAll("\\", "\\\\");
        
        String crossesDir = toolsPath + CROSSES_DIR;
        String germplasmListDir = toolsPath + GERMPLASM_LIST_DIR;
        String templatesDir = toolsPath + TEMPLATES_DIR;
        
        String configuration = String.format(format, crossesDir, germplasmListDir, crossesDir, templatesDir);
        
        //check if the ibfb folders in APPDATA are present, if not create them
        String appDataDir = System.getenv("APPDATA");
        String ibfbSettingsDir = appDataDir + IBFB_APPDATA_FOLDER;
        
        if(!(new File(ibfbSettingsDir)).mkdirs()){
            Util.showErrorMessage(context.getMessage("cannot_create_fieldbook_core_properties_file_folder"));
            return false;
        }
        
        File corePropertiesFile = new File(ibfbSettingsDir, "core.properties");
        
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(corePropertiesFile);
            fos.write(configuration.getBytes());
            fos.flush();
        }
        catch (IOException e) {
            Util.showErrorMessage(context.getMessage("cannot_create_fieldbook_core_properties_file"));
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
