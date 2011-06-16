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

  !insertmacro MUI_PAGE_INSTFILES
  Page custom wrongVersionsCheck
  
  !insertmacro MUI_UNPAGE_WELCOME
  !insertmacro MUI_UNPAGE_INSTFILES  

;--------------------------------
;Languages
 
;  !insertmacro MUI_LANGUAGE "Belarusian"
!insertmacro MUI_INSERT
LoadLanguageFile "Belarusian.nlf"
!insertmacro LANGFILE_INCLUDE "Belarusian.nsh"

;--------------------------------

Var /GLOBAL wrongVersionsText
Var /GLOBAL FirstInstall ; Усталёўваем першы раз - прапанаваць reboot


Function GetMuiVersion
    ${StrRep} $1 $0 '\be-BY\' '\en-US\'
    IfFileExists $1 gmvgo
    ${StrRep} $1 $0 '\be-BY\' '\ru-RU\'
    IfFileExists $1 gmvgo
    StrCpy $0 ""
    Goto gmvend
gmvgo:
    GetDllVersion $1 $R0 $R1
    IntOp $R2 $R0 / 0x00010000
    IntOp $R3 $R0 & 0x0000FFFF
    IntOp $R4 $R1 / 0x00010000
    IntOp $R5 $R1 & 0x0000FFFF
    StrCpy $0 "$R2.$R3.$R4.$R5"
    
    FileOpen $2 $1 r
    FileSeek $2 0x3C
    nsisFile::FileReadBytes $2 1
    nsisFile::FileReadBytes $2 1
    nsisFile::FileReadBytes $2 1
    nsisFile::FileReadBytes $2 1
    Pop $4
    Pop $5
    Pop $6
    Pop $7
    
    StrCpy $4 "0x$4"
    StrCpy $5 "0x$5"
    StrCpy $6 "0x$6"
    StrCpy $7 "0x$7"

    IntOp $4 $4 << 24 
    IntOp $5 $5 << 16 
    IntOp $6 $6 << 8

    IntOp $3 $7 + 0 
    IntOp $3 $3 | $6 
    IntOp $3 $3 | $5 
    IntOp $3 $3 | $4 
    
    FileSeek $2 $3
    nsisFile::FileReadBytes $2 4
    Pop $3
    StrCmp $3 "50450000" 0 gmvclose
    
    nsisFile::FileReadBytes $2 2
    Pop $3
    StrCpy $3 "0x$3"
    IntCmp $3 0x4C01 gmv32 gmv64
    
gmv32:
    StrCpy $0 "$0_x32"
    Goto gmvclose
gmv64:
    StrCpy $0 "$0_x64"
    Goto gmvclose
    
gmvclose:
    FileClose $2
gmvend:
FunctionEnd


Function wrongVersionsCheck

    StrCmp $wrongVersionsText "" 0 +2
        Abort

    nsDialogs::Create 1018
    Pop $0

    ${NSD_CreateLabel}  0u 2u 115u 11u "Дашліце, калі ласка, на адрас:"
    nsDialogs::CreateControl ${__NSD_Text_CLASS} ${__NSD_Text_STYLE}|${ES_READONLY} ${__NSD_Text_EXSTYLE}  115u  0u 185u  13u "i18n-bel-win7@googlegroups.com"
    nsDialogs::CreateControl ${__NSD_Text_CLASS} ${DEFAULT_STYLES}|${WS_TABSTOP}|${WS_VSCROLL}|${ES_MULTILINE}|${ES_WANTRETURN} ${__NSD_Text_EXSTYLE}    0u 17u 300u 123u ""
    Pop $0
    ${NSD_SetText} $0 "$wrongVersionsText"

    !insertmacro MUI_HEADER_TEXT "Памылка ўсталявання" "Не ўсе файлы магчыма ўсталяваць. Дашліце інфармацыю распрацоўнікам альбо ўсталюйце апошнія абнаўленні Windows 7 і паспрабуйце зноў."
    nsDialogs::Show
FunctionEnd

;--------------------------------
;Installer Section

Section
    StrCpy $INSTDIR "$PROGRAMFILES\win7bel\"
    
    CreateDirectory  '$INSTDIR'
    
    File "/oname=$TEMP\win7bel-chown.exe" "..\Builder\updater\chown.exe"
    ExecWait '$TEMP\win7bel-chown.exe $WINDIR\servicing $WINDIR\diagnostics\system\DeviceCenter $WINDIR\diagnostics\system\HomeGroup' $0
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

    StrCpy $wrongVersionsText ""

    ${If} ${RunningX64}
    ${DisableX64FSRedirection}
##FILEINSTALL64##
##FILEINSTALL64OTHER##
    ${EnableX64FSRedirection}
    ${Else}
##FILEINSTALL32##
##FILEINSTALL32OTHER##
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

; first install ?
    ${registry::KeyExists} "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\MUI\UILanguages\be-BY" $FirstInstall
; remove mui cache
    DeleteRegKey        HKCR "Local Settings\MuiCache"
; write language to regitry
    WriteRegDWORD       HKLM "SYSTEM\CurrentControlSet\Control\MUI\UILanguages\be-BY" "LCID" 0x00000423
    WriteRegStr         HKLM "SYSTEM\CurrentControlSet\Control\MUI\UILanguages\be-BY" "DefaultFallback" "en-US"
    WriteRegDWORD       HKLM "SYSTEM\CurrentControlSet\Control\MUI\UILanguages\be-BY" "Type" 0x00000094
    ${registry::Write} "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\MUI\UILanguages\be-BY" "en-US" "" REG_MULTI_SZ $R0
    ${registry::Write} "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\MUI\UILanguages\be-BY" "ru-RU" "" REG_MULTI_SZ $R0

IfRebootFlag 0 NoReboot
  MessageBox MB_YESNO|MB_ICONQUESTION "Каб скончыць усталяванне, трэба перазапусціць камп'ютар. Зрабіць гэта зараз?" IDNO NoReboot
    Reboot
NoReboot:


  IntCmp $FirstInstall 0 NoRebootFirst
    MessageBox MB_YESNO|MB_ICONQUESTION "Каб мець магчымасьць абраць беларускую мову ў панэлі кіравання, трэба перазапусціць камп'ютар. Зрабіць гэта зараз?" IDNO NoRebootFirst
      Reboot
NoRebootFirst:

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
  MessageBox MB_YESNO|MB_ICONQUESTION "Каб выдаліць некаторыя файлы, трэба перазапусціць камп'ютар. Зрабіць гэта зараз?" IDNO NoReboot
    Reboot
NoReboot:

SectionEnd
