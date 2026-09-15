@echo off
setlocal

cd /d "%~dp0"

set "ENOVA_HOME=%~dp0"

start "" "%~dp0app\bin\javaw.exe" ^
    -m de.muenchen.enovaeditor/de.muenchen.enovaeditor.EnovaEditorApplication %*

endlocal
exit