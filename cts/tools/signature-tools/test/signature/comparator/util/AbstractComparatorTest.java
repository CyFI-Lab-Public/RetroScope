/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package signature.comparator.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;

import signature.compare.ApiComparator;
import signature.compare.IApiComparator;
import signature.compare.model.IApiDelta;
import signature.compare.model.IClassDefinitionDelta;
import signature.compare.model.IPackageDelta;
import signature.converter.util.AbstractConvertTest;
import signature.model.IApi;
public abstract class AbstractComparatorTest extends AbstractConvertTest{

    private IApiComparator comparator;

    @Before
    public void setupComparator() {
        comparator = new ApiComparator();
    }
    
    public IApiDelta compare(IApi from, IApi to){
        return comparator.compare(from, to);
    }

    public IPackageDelta getSinglePackageDelta(IApiDelta apiDelta){
        assertNotNull(apiDelta);
        assertEquals(1, apiDelta.getPackageDeltas().size());
        return apiDelta.getPackageDeltas().iterator().next();
    }
    
    public IClassDefinitionDelta getSingleClassDelta(IApiDelta apiDelta){
        IPackageDelta packageDelta = getSinglePackageDelta(apiDelta);
        assertEquals(1, packageDelta.getClassDeltas().size());
        return packageDelta.getClassDeltas().iterator().next();
    }

}
