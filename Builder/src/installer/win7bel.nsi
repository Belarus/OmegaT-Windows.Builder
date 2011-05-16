!include "MUI2.nsh"
!include "Registry.nsh"
!include "StrFunc.nsh"
!include "Library.nsh"

${StrRep}
;--------------------------------
;General

  ;Name and file
  Name "Беларускі пераклад Windows 7"
  OutFile "win7bel.exe"
  SetCompressor /SOLID lzma
  !define MUI_ICON win7i18n.ico
  
  ;Request application privileges for Windows Vista
  RequestExecutionLevel admin

;--------------------------------
;Interface Settings

  !define MUI_ABORTWARNING


;--------------------------------
;Pages
  !insertmacro MUI_PAGE_LICENSE "readme.txt"
  Page custom wrongVersionsCheck

  !insertmacro MUI_PAGE_INSTFILES
  
  !insertmacro MUI_UNPAGE_WELCOME
  !insertmacro MUI_UNPAGE_INSTFILES  

;--------------------------------
;Languages
 
;  !insertmacro MUI_LANGUAGE "Belarusian"
!insertmacro MUI_INSERT
LoadLanguageFile "Belarusian.nlf"
!insertmacro LANGFILE_INCLUDE "Belarusian.nsh"

;--------------------------------

Var /GLOBAL DIR
Var /GLOBAL outFile
Var /GLOBAL wrongVersionsText


Function FindMuiFile
    Pop $0
    ${StrRep} $outFile "$0" "\be-BY\" "\en-US\"
    Push $outFile
    Pop $1
    IfFileExists $outFile go
    ${StrRep} $outFile "$0" "\be-BY\" "\ru-RU\"
    Push $outFile
    Pop $2
    IfFileExists $outFile go
    StrCpy $wrongVersionsText "$wrongVersionsTextНемагчыма ўсталяваць увесь пакунак, бо не існуе ані файла $1, ані файла $2$\r$\n$\r$\n"
go:
FunctionEnd

Function FindMuiFileOptional
    Pop $0
    ${StrRep} $outFile "$0" "\be-BY\" "\en-US\"
    Push $outFile
    Pop $1
    IfFileExists $outFile go
    ${StrRep} $outFile "$0" "\be-BY\" "\ru-RU\"
    Push $outFile
    Pop $2
    IfFileExists $outFile go
go:
FunctionEnd

!macro GetMuiVersion muiFile
    GetDllVersion "${muiFile}" $R0 $R1
    IntOp $R2 $R0 / 0x00010000
    IntOp $R3 $R0 & 0x0000FFFF
    IntOp $R4 $R1 / 0x00010000
    IntOp $R5 $R1 & 0x0000FFFF
    StrCpy $0 "$R2.$R3.$R4.$R5"
!macroend

!macro GetFileVersion file
    Crypto::HashFile "SHA1" "${file}"
    Pop $0
    MessageBox MB_OK "$0 ${file}"
!macroend

Function VersionsCheckFunc
  StrCmp "0.0.0.0" $0 fetchall again

fetchall:
    Pop $1
    IfErrors done
    Goto fetchall

again:
    Pop $1
    IfErrors wrong
    StrCmp $1 $0 done 0
    Goto again
wrong:
;        MessageBox MB_OK "Немагчыма ўсталяваць, бо файл $outFile мае версію $0, але патрабуецца іншая"
;        Abort
    StrCpy $wrongVersionsText "$wrongVersionsTextНемагчыма ўсталяваць увесь пакунак, бо файл $outFile мае версію $0$\r$\n$\r$\n"
done:
FunctionEnd

!macro InstallMuiVersion outPath ver inPath
    Push "${outPath}"
    Call FindMuiFile
    !insertmacro GetMuiVersion "$outFile"
    StrCmp "${ver}" "$0" 0 +2
    File '/oname=${outPath}.new' '${inPath}'
!macroend

!macro InstallFileVersion outPath ver inPath
;    Crypto::HashFile "SHA1" "${outPath}"
;    Pop $0
;    StrCmp "${ver}" "$0" 0 +2
    File '/oname=${outPath}.new' '${inPath}'
!macroend


Function wrongVersionsCheck

    StrCpy $wrongVersionsText ""
    
    IntOp $9 0 + 0
    
    ${If} ${RunningX64}
        ${DisableX64FSRedirection}

        nxs::Show /NOUNLOAD "Спраўджваем усталяваныя версіі..." /top "" /sub "" /max /end ##FILESCOUNT64##
        ##FILEVERSIONS64##
        ${EnableX64FSRedirection}
    ${Else}
        nxs::Show /NOUNLOAD "Спраўджваем усталяваныя версіі..." /top "" /sub "" /max /end ##FILESCOUNT32##
        ##FILEVERSIONS32##
    ${EndIf}
    
    nxs::Destroy
    
    StrCmp $wrongVersionsText "" 0 +2
        Abort

    nsDialogs::Create 1018
    Pop $0

    ${NSD_CreateLabel}  0u 2u 115u 11u "Дашліце, калі ласка, на адрас:"
    nsDialogs::CreateControl ${__NSD_Text_CLASS} ${__NSD_Text_STYLE}|${ES_READONLY} ${__NSD_Text_EXSTYLE}  115u  0u 185u  13u "i18n-bel-win7@googlegroups.com"
    nsDialogs::CreateControl ${__NSD_Text_CLASS} ${DEFAULT_STYLES}|${WS_TABSTOP}|${WS_VSCROLL}|${ES_MULTILINE}|${ES_WANTRETURN} ${__NSD_Text_EXSTYLE}    0u 17u 300u 123u ""
    Pop $0
    ${NSD_SetText} $0 "$wrongVersionsText"

    !insertmacro MUI_HEADER_TEXT "Памылка ўсталёўкі" "Ня ўсе файлы магчыма ўсталяваць. Дашліце інфармацыю распрацоўшчыкам альбо ўсталюйце апошнія абнаўленні Windows 7 і паспрабуйце зноў."
    nsDialogs::Show
FunctionEnd

;--------------------------------
;Installer Section

Section
    StrCpy $INSTDIR "$PROGRAMFILES\win7bel\"
    
    CreateDirectory  '$INSTDIR'
    
    File "/oname=$TEMP\win7bel-chown.exe" "..\Builder\updater\chown.exe"
    ExecWait '$TEMP\win7bel-chown.exe $WINDIR\servicing $WINDIR\diagnostics\system\DeviceCenter' $0
    IntCmp $0 0 +2
        Abort
    
    ${If} ${RunningX64}
    ${DisableX64FSRedirection}
##DIR_INSTALL64##
    ${EnableX64FSRedirection}
    ${Else}
##DIR_INSTALL32##
    ${EndIf}

    WriteUninstaller "$INSTDIR\Uninstall.exe"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\win7bel" "DisplayName" "$(^NameDA)"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\win7bel" "UninstallString" "$\"$INSTDIR\Uninstall.exe$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\win7bel" "DisplayVersion" "##DATE##"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\win7bel" "HelpLink" "http://mounik.org/w/Windows"

    ${If} ${RunningX64}
    ${DisableX64FSRedirection}
##FILEUNPACK64##
##FILEINSTALL64##
    ${EnableX64FSRedirection}
    ${Else}
##FILEUNPACK32##
##FILEINSTALL32##
    ${EndIf}

; write updater
    File "/oname=$INSTDIR\win7bel-updater.exe" "..\Builder\updater\win7bel-updater.exe"
    FileOpen $0 "$INSTDIR\version.txt" w
    FileWrite $0 "##DATEMARK##"
    FileClose $0
; write scheduler for updater
    File "/oname=$TEMP\win7bel-scheduler.exe" "..\Builder\updater\win7bel-scheduler.exe"
    File "/oname=$TEMP\Interop.TaskScheduler.dll" "..\Builder\updater\Interop.TaskScheduler.dll"
    ExecWait '$TEMP\win7bel-scheduler.exe' $0    

    WriteRegDWORD       HKLM "SYSTEM\CurrentControlSet\Control\MUI\UILanguages\be-BY" "LCID" 0x00000423
    WriteRegStr         HKLM "SYSTEM\CurrentControlSet\Control\MUI\UILanguages\be-BY" "DefaultFallback" "en-US"
    WriteRegDWORD       HKLM "SYSTEM\CurrentControlSet\Control\MUI\UILanguages\be-BY" "Type" 0x00000094
    ${registry::Write} "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\MUI\UILanguages\be-BY" "en-US" "" REG_MULTI_SZ $R0
    ${registry::Write} "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\MUI\UILanguages\be-BY" "ru-RU" "" REG_MULTI_SZ $R0

IfRebootFlag 0 NoReboot
  MessageBox MB_YESNO|MB_ICONQUESTION "Каб скончыць усталёўку, трэба перазапусціць камп'ютар. Зрабіць гэта зараз ?" IDNO NoReboot
    Reboot
NoReboot:
SectionEnd
;--------------------------------
;Uninstaller Section

Section "Uninstall"
    StrCpy $INSTDIR "$PROGRAMFILES\win7bel\"

    ${If} ${RunningX64}
    ${DisableX64FSRedirection}
##DIR_UNINSTALL64##
    ${EnableX64FSRedirection}
    ${Else}
##DIR_UNINSTALL32##
    ${EndIf}

    ExecWait 'schtasks /Delete /tn win7bel-updater /f' $0
    
    RmDir /r /REBOOTOK '$INSTDIR'

  DeleteRegKey  HKLM "SYSTEM\CurrentControlSet\Control\MUI\UILanguages\be-BY"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\win7bel"

IfRebootFlag 0 NoReboot
  MessageBox MB_YESNO|MB_ICONQUESTION "Каб выдаліць некаторыя файлы трэба перазапусціць камп'ютар. Зрабіць гэта зараз ?" IDNO NoReboot
    Reboot
NoReboot:
SectionEnd
