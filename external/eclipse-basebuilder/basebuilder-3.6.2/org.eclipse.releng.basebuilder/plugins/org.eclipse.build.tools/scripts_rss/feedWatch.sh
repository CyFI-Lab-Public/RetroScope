#*******************************************************************************
# Copyright (c) 2005, 2006 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     IBM Corporation - initial API and implementation
#*******************************************************************************
#!/bin/sh

export JAVA_HOME=/opt/sun-java2-5.0;
export ANT_HOME=/opt/apache-ant-1.6;
$ANT_HOME/bin/ant -f feedWatch.xml;
