[Setup]
AppName=TutorHub Secure Exam
AppVersion=1.0.0
DefaultDirName={localappdata}\TutorHubSecureExam
DefaultGroupName=TutorHub Secure Exam
OutputDir=..\dist\installer
OutputBaseFilename=TutorHubSecureExamSetup
Compression=lzma2
SolidCompression=yes
PrivilegesRequired=lowest
DisableProgramGroupPage=yes

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "..\dist\TutorHubSecureExam\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\TutorHub Secure Exam"; Filename: "{app}\run.bat"
Name: "{autodesktop}\TutorHub Secure Exam"; Filename: "{app}\run.bat"; Tasks: desktopicon

[Run]
Filename: "{app}\run.bat"; Description: "{cm:LaunchProgram,TutorHub Secure Exam}"; Flags: shellexec postinstall skipifsilent
