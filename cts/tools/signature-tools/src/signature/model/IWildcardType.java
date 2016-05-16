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

package signature.model;

import java.util.List;

/**
 * {@code IWildcardType} models a wildcard type.
 */
public interface IWildcardType extends ITypeReference {

    /**
     * Returns the upper bounds for this type variable as specified by the
     * extends clause. If no upper bounds are explicitly specified then
     * java.lang.Object is returned as upper bound.
     * 
     * @return the upper bounds for this type variable
     */
    List<ITypeReference> getUpperBounds();

    /**
     * Returns the lower bounds for this type variable as specified by the super
     * clause. If no lower bounds are explicitly specified then null is returned
     * as lower bound.
     * 
     * @return the lower bounds for this type variable
     */
    ITypeReference getLowerBound();

}
