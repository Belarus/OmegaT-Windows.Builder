Як ствараць пакунак для усталёўкі

1. Трэба мець файлы .mui, запакаваныя ў .zip. Файлы накшталт "windows6.1-KB976932-X64.exe" можна распакаваць праз "windows6.1-KB976932-X64.exe /X:out"

2. .zip будуць распакаваныя праз ExtractMUIs(толькі тыя што ў need-mui.txt) у нейкі каталёг:

d:\my\binaries\windows\ie8x32sp0.zip
  Program Files/Internet Explorer/en-US/hmmapi.dll.mui -> Program Files/Internet Explorer/be-BY/hmmapi.dll_8.0.7600.16385_x32.mui
d:\my\binaries\windows\Windows7_x32_006.zip
  Program Files/Common Files/microsoft shared/ink/en-US/InputPersonalization.exe.mui -> Program Files/Common Files/microsoft shared/ink/be-BY/InputPersonalization.exe_6.1.7600.16385_x32.mui
  Windows/System32/en-US/acppage.dll.mui -> Windows/System32/be-BY/acppage.dll_6.1.7600.16385_x32.mui
  Windows/winsxs/x86_microsoft-windows-a..mecontrol.resources_31bf3856ad364e35_6.1.7600.16385_en-us_dd0e4e2401f276b2/sndvol.exe.mui -> Windows/System32/be-BY/sndvol.exe_6.1.7600.16385_x32.mui
d:\my\binaries\windows\Windows7_x64_sp1.zip
  KB976933-LangsCab0/amd64_microsoft-windows-user32.resources_31bf3856ad364e35_6.1.7601.17514_en-us_9c23fd3941bcc44e/user32.dll.mui -> Windows/System32/be-BY/user32.dll_6.1.7601.17514_x64.mui

ExtractMUIs f:\binaries\Windows\7 c:\temp\win\7
ExtractMUIs f:\binaries\Windows\8 c:\temp\win\8

3. ExtractResources - выняць рэсурсы ў тэкставыя файлы для OmegaT

ExtractResources c:\temp\win\7 ../../Windows.OmegaT/Windows/source/7/
ExtractResources c:\temp\win\8 ../../Windows.OmegaT/Windows/source/8/

4. Пакласьці тэкставыя файлы рэсурсаў у git

5. Скампіляваць рэсурсы праз CompileResources(яно і перакладзе з дапамогай OmegaT), 
   і падрыхтуе файл out\win7bel.nsi для NSIS

6. Скампіляваць пакунак праз NSIS і пакласьці яго на сэрвер праз upload-http.sh
