@ echo off

REM script which executes build

REM tag to use when checking out .map file project
set mapVersionTag=HEAD

REM default setting for buildType
set buildType=

REM default setting for buildID
set buildID=

REM default bootclasspath
set bootclasspath=

REM vm used to run the build.  Defaults to java on system path
set vm=java

REM target used if not default (to allow run just a portion of buildAll)
set target=

REM FTP user/password, required for Windows to ftp. Without it, no push.
set ftpUser=
set ftpPassword=

if x%1==x goto usage

:processcmdlineargs

REM ****************************************************************
REM
REM Process command line arguments
REM
REM ****************************************************************
if x%1==x goto run
if x%1==x-mapVersionTag set mapVersionTag=%2 && shift && shift && goto processcmdlineargs
if x%1==x-vm set vm=%2 && shift && shift && goto processcmdlineargs
if x%1==x-bc set bootclasspath=-Dbootclasspath=%2 && shift && shift && goto processcmdlineargs
if x%1==x-target set target=%2 && shift && shift && goto processcmdlineargs
if x%1==x-buildID set buildID=-DbuildId=%2 && shift && shift && goto processcmdlineargs
if x%1==x-ftp set ftpUser=-DftpUser=%2 && set ftpPassword=-DftpPassword=%3 && shift && shift && shift && goto processcmdlineargs
set buildType=%1 && shift && goto processcmdlineargs

:run
if x%buildType%==x goto usage

%vm% -cp ..\org.eclipse.releng.basebuilder\startup.jar org.eclipse.core.launcher.Main -application org.eclipse.ant.core.antRunner -f buildAll.xml %target% %bootclasspath% -DmapVersionTag=%mapVersionTag% -DbuildType=%buildType% %buildID% %ftpUser% %ftpPassword%
goto end

:usage
echo "usage: buildAll [-mapVersionTag HEAD|<branch name>] [-vm <url to java executable to run build>] [-bc <bootclasspath>] [-target <buildall target to execute>] [-buildID <buildID, e.g. 2.1.2>]  [-ftp <userid> <password>] I|M"

:end