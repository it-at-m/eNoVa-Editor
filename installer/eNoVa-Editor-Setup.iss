; Inno Setup Script für eNoVa-Editor v2.0
; Entwickelt für die Landeshauptstadt München (it@M) und kommunale Sachbearbeitung

#define MyAppName "eNoVa-Editor"
#define MyAppVersion "2.0"
#define MyAppPublisher "Landeshauptstadt München (it@M)"
#define MyAppURL "https://github.com/it-at-m/eNoVa-Editor"
#define MyAppExeName "eNoVa-Editor.exe"

[Setup]
AppId={{D9B38F88-825B-4C61-B9D8-22F2267B665E}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={localappdata}\Programs\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
OutputDir=..\installer\dist
OutputBaseFilename=eNoVa-Editor-Setup-v2.0
SetupIconFile=app.ico
UninstallDisplayIcon={app}\app.ico
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
ChangesAssociations=yes
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "german"; MessagesFile: "compiler:Languages\German.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"
Name: "contextmenu"; Description: "Rechtsklick-Menü im Windows Explorer hinzufügen (""Mit eNoVa-Editor öffnen"")"; GroupDescription: "Windows-Integration:"

[Files]
Source: "..\target\jpackage-out\eNoVa-Editor\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "app.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; IconFilename: "{app}\app.ico"
Name: "{group}\Musterdaten (Samples)"; Filename: "{app}\samples"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; IconFilename: "{app}\app.ico"; Tasks: desktopicon

[Registry]
; Rechtsklick-Kontextmenü für alle XML-Dateien (Benutzerebene, keine Admin-Rechte erforderlich)
Root: HKCU; Subkey: "Software\Classes\SystemFileAssociations\.xml\shell\eNoVaEditor"; ValueType: string; ValueData: "Mit eNoVa-Editor öffnen"; Flags: uninsdeletekey; Tasks: contextmenu
Root: HKCU; Subkey: "Software\Classes\SystemFileAssociations\.xml\shell\eNoVaEditor"; ValueType: string; ValueName: "Icon"; ValueData: """{app}\app.ico"""; Flags: uninsdeletekey; Tasks: contextmenu
Root: HKCU; Subkey: "Software\Classes\SystemFileAssociations\.xml\shell\eNoVaEditor\command"; ValueType: string; ValueData: """{app}\{#MyAppExeName}"" ""%1"""; Flags: uninsdeletekey; Tasks: contextmenu

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#MyAppName}}"; Flags: nowait postinstall skipifsilent