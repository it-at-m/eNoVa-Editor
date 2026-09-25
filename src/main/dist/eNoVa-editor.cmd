@echo off
setlocal

cd /d "%~dp0"

set "ENOVA_HOME=%~dp0"

start "" "%~dp0app\bin\javaw.exe" ^
    -cp "%~dp0app\app-libs\*" ^
    de.muenchen.enovaeditor.Launcher

endlocal
exit