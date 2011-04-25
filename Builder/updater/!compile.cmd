%WINDIR%\Microsoft.NET\Framework\v3.5\MSBuild.exe /p:Configuration=Release;TargetFrameworkVersion=v3.5;OutDir=.\ chown.csproj
rmdir /S /Q obj

%WINDIR%\Microsoft.NET\Framework\v3.5\MSBuild.exe /p:Configuration=Release;TargetFrameworkVersion=v3.5;OutDir=.\ win7bel-updater.csproj
rmdir /S /Q obj

%WINDIR%\Microsoft.NET\Framework\v3.5\MSBuild.exe /p:Configuration=Release;TargetFrameworkVersion=v3.5;OutDir=.\ win7bel-scheduler.csproj
rmdir /S /Q obj

del *.pdb
pause