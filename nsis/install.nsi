# This installs the following:
#   - workbench
#   - workbench tools,
#   - start menu shortcuts
#   - an uninstaller, and
#   - uninstall information to the registry for Add/Remove Programs

# To create the installer, run the build-installer.xml Ant script,
# and run makensis on the script on the dist directory.

# If you change the names "ibpworkbench.exe", "installer.ico", or "license.rtf" you should do a search and replace - they
# show up in a few places.
# All the other settings can be tweaked by editing the !defines at the top of this script
!define APPNAME "IBPWorkbench"
!define COMPANYNAME ""
!define DESCRIPTION "IBPWorkbench installation including workbench tools."

# These three must be integers
!define VERSIONMAJOR 1
!define VERSIONMINOR 0
!define VERSIONBUILD 0

# These will be displayed by the "Click here for support information" link in "Add/Remove Programs"
# It is possible to use "mailto:" links in here to open the email client
!define HELPURL     "http://..." # "Support Information" link
!define UPDATEURL   "http://..." # "Product Updates" link
!define ABOUTURL    "http://..." # "Publisher" link

# This is the size (in kB) of all the files copied into "Program Files"
!define INSTALLSIZE 7233

#SetCompressor /FINAL /SOLID lzma
#SetCompressorDictSize 128

RequestExecutionLevel admin ;Require admin rights on NT6+ (When UAC is turned on)
 
#InstallDir "$PROGRAMFILES\${COMPANYNAME}\${APPNAME}"
InstallDir "C:\${APPNAME}"
 
# rtf or txt file - remember if it is txt, it must be in the DOS text format (\r\n)
LicenseData "license.rtf"

# This will be in the installer/uninstaller's title bar
Name "${APPNAME}"
Icon "installer.ico"
OutFile "ibpworkbench-win32.exe"
 
!include LogicLib.nsh
 
# Just three pages - license agreement, install location, and installation
Page license
Page directory
Page instfiles
 
!macro VerifyUserIsAdmin
UserInfo::GetAccountType
Pop $0
${If} $0 != "admin" ;Require admin rights on NT4+
        MessageBox mb_iconstop "Administrator rights required!"
        SetErrorLevel 740 ;ERROR_ELEVATION_REQUIRED
        Quit
${EndIf}
!macroend
 
Function .onInit
    SetShellVarContext all
    !insertmacro VerifyUserIsAdmin
FunctionEnd
 
Section "install"
    # Files for the install directory - to build the installer, these should be in the same directory as the install script (this file)
    SetOutPath $INSTDIR
    
    # Files added here should be removed by the uninstaller (see section "uninstall")
    File "ibpworkbench.exe"
    File "installer.ico"
    File "ibpworkbench_launcher.jar"
    File /r "documentation"
    File /r "demo_scripts"
    File /r "images"
    File /r "jre"
    File /r "mysql"
    File /r "tomcat"
    File /r "tools"
    
    # Add any other files for the install directory (license files, app data, etc) here
 
    # Uninstaller - See function un.onInit and section "uninstall" for configuration
    WriteUninstaller "$INSTDIR\uninstall.exe"
 
    # Start Menu
    CreateDirectory "$SMPROGRAMS\${APPNAME}"
    CreateShortCut "$SMPROGRAMS\${APPNAME}\${APPNAME}.lnk" "$INSTDIR\ibpworkbench.exe" "" "$INSTDIR\installer.ico"
    CreateShortCut "$DESKTOP\${APPNAME}.lnk" "$INSTDIR\ibpworkbench.exe"
    
    # Create shortcuts for demo scripts
    SetOutPath "$INSTDIR\demo_scripts\"
    CreateShortCut "$DESKTOP\Initialize Workbench Tutorial.lnk" "$INSTDIR\demo_scripts\01_ibdb_cowpea_local-1-Start.bat"
    CreateShortCut "$DESKTOP\Initialize Fieldbook Tutorial.lnk" "$INSTDIR\demo_scripts\02_ibdb_cowpea_local-5-F3_Nursery-arlett.bat"
    CreateShortCut "$DESKTOP\Initialize Genotyping database Tutorial.lnk" "$INSTDIR\demo_scripts\03_ibdb_cowpea_local-gdms.bat"
    
    # Registry information for add/remove programs
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${APPNAME} - ${DESCRIPTION}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$\"$INSTDIR\uninstall.exe$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "QuietUninstallString" "$\"$INSTDIR\uninstall.exe$\" /S"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "InstallLocation" "$\"$INSTDIR$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayIcon" "$\"$INSTDIR\installer.ico$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "Publisher" "$\"${COMPANYNAME}$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "HelpLink" "$\"${HELPURL}$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "URLUpdateInfo" "$\"${UPDATEURL}$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "URLInfoAbout" "$\"${ABOUTURL}$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayVersion" "$\"${VERSIONMAJOR}.${VERSIONMINOR}.${VERSIONBUILD}$\""
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMajor" ${VERSIONMAJOR}
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMinor" ${VERSIONMINOR}
    # There is no option for modifying or repairing the install
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoRepair" 1
    # Set the INSTALLSIZE constant (!defined at the top of this script) so Add/Remove Programs can accurately report the size
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "EstimatedSize" ${INSTALLSIZE}
SectionEnd
 
# Uninstaller
 
Function un.onInit
    SetShellVarContext all
 
    #Verify the uninstaller - last chance to back out
    MessageBox MB_OKCANCEL "Permanantly remove ${APPNAME}?" IDOK next
        Abort
    next:
    !insertmacro VerifyUserIsAdmin
FunctionEnd
 
Section "uninstall"
 
    # Remove Start Menu launcher
    Delete "$SMPROGRAMS\${APPNAME}\${APPNAME}.lnk"
    # Try to remove the Start Menu folder - this will only happen if it is empty
    Rmdir "$SMPROGRAMS\${APPNAME}"
    
    # Remove the desktop shortcut
    Delete "$DESKTOP\${APPNAME}.lnk"
    
    # Create shortcuts for demo scripts
    Delete "$DESKTOP\Initialize Workbench Tutorial.lnk"
    Delete "$DESKTOP\Initialize Fieldbook Tutorial.lnk"
    Delete "$DESKTOP\Initialize Genotyping database Tutorial.lnk"
    
    # Remove files
    #Delete "ibpworkbench.exe"
    #Delete "installer.ico"
    #Delete "ibpworkbench_launcher.jar"
    #Rmdir "images"
    #Rmdir "jre"
    #Rmdir "mysql"
    #Rmdir "tomcat"
    Rmdir /r $INSTDIR

    # Always delete uninstaller as the last action
    Delete $INSTDIR\uninstall.exe
 
    # Try to remove the install directory - this will only happen if it is empty
    Rmdir $INSTDIR
    Delete $INSTDIR
 
    # Remove uninstaller information from the registry
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
SectionEnd