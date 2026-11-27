@echo off
setlocal

set "ENOVA_HOME=%~dp0"

"%~dp0app\bin\java.exe" -Duser.language=de ^
    -cp "%~dp0app\app-libs\*" ^
    de.muenchen.enovaeditor.validation.SchemaCheck %*

endlocal & exit /b %ERRORLEVEL%
