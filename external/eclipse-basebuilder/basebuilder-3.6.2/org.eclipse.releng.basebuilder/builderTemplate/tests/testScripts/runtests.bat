@echo off

REM default java executable
set vm=java

REM reset list of ant targets in test.xml to execute
set tests=

REM default switch to determine if eclipse should be reinstalled between running of tests
set installmode=clean

REM property file to pass to Ant scripts
set properties=

REM default values for os, ws and arch
set os=win32
set ws=win32
set arch=x86

REM reset ant command line args
set ANT_CMD_LINE_ARGS=

REM ****************************************************************
REM
REM Delete previous Eclipse installation and workspace
REM (This is the Eclipse that controls the test. The test will start another Eclipse.
REM
REM ****************************************************************
if EXIST eclipse rmdir /S /Q eclipse
if EXIST workspace rmdir /s /Q workspace

REM ****************************************************************
REM
REM Install Eclipse and org.eclipse.test plugin
REM
REM ****************************************************************

unzip -qq -o eclipse-SDK*.zip
unzip -qq -o -C VE-junit-tests*.zip */plugins/org.eclipse.test*


:processcmdlineargs

REM ****************************************************************
REM
REM Process command line arguments
REM
REM ****************************************************************

if x%1==x goto setup
if x%1==x-ws set ws=%2 && shift && shift && goto processcmdlineargs
if x%1==x-os set os =%2 && shift && shift && goto processcmdlineargs
if x%1==x-arch set arch=%2 && shift && shift && goto processcmdlineargs
if x%1==x-noclean set installmode=noclean && shift && goto processcmdlineargs
if x%1==x-properties set properties=-propertyfile %2 && shift && shift && goto processcmdlineargs
if x%1==x-vm set vm=%2 && shift && shift && goto processcmdlineargs

set tests=%tests% %1 && shift && goto processcmdlineargs


:setup
REM ****************************************************************
REM
REM	Setup up the test (target) GEF SDK
REM
REM	** if -noclean set, Eclipse will be re-installed only if the 
REM	directory target\eclipse does not exist.  If this directory
REM	exists in a partially installed state, it should be deleted manually
REM	and the script rerun with the same parameter settings. **
REM
REM ****************************************************************

REM command for executing antRunner headless
set antRunner=%vm% -cp eclipse\startup.jar -Dosgi.ws=%ws% -Dosgi.os=%os% -Dosgi.arch=%arch% org.eclipse.core.launcher.Main -application org.eclipse.ant.core.antRunner

if %installmode%==noclean %antRunner% -file test.xml setup -Dws=%ws% -Dos=%os% -Darch=%arch% "-D%installmode%=true" -logger org.apache.tools.ant.DefaultLogger
goto run


:run
REM ***************************************************************************
REM	Run tests by running Ant in Eclipse on the test.xml script
REM ***************************************************************************

%antRunner% -file test.xml %tests% -Dws=%ws% -Dos=%os% -Darch=%arch% %properties%  "-D%installmode%=true" -logger org.apache.tools.ant.DefaultLogger
goto end

:end