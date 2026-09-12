@echo off
setlocal
set "DIST_DIR=%~dp0target\distribution\eNoVa-editor"
set "ENOVA_HOME=%DIST_DIR%"
cd /d "%DIST_DIR%"
start "" "%DIST_DIR%\app\bin\javaw.exe" -m de.muenchen.enovaeditor/de.muenchen.enovaeditor.EnovaEditorApplication %*
endlocal
