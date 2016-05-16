/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2009 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import proguard.Configuration;

/**
 * This DataType represents a reference to a ProGuard configuration in Ant.
 *
 * @author Eric Lafortune
 */
public class ConfigurationElement extends DataType
{
    /**
     * Adds the contents of this configuration task to the given configuration.
     * @param configuration the configuration to be extended.
     */
    public void appendTo(Configuration configuration)
    {
        // Get the referenced element.
        if (!isReference())
        {
            throw new BuildException("Nested element <configuration> must have a refid attribute");
        }

        ConfigurationTask configurationTask =
            (ConfigurationTask)getCheckedRef(ConfigurationTask.class,
                                             ConfigurationTask.class.getName());

        // Append the referenced configuration entries to the given configuration.
        configurationTask.appendTo(configuration);
    }
}
