/*
 *  Copyright 2010 reto.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.apache.clerezza.platform.concepts.core;

import java.security.Permission;
import org.apache.clerezza.permissiondescriptions.PermissionInfo;

/**
 * Permssion to use an application to Manage Concept-Providers. Note that the
 * user additionally need the respecitive rights on the underlying graphs.
 *
 * @author reto
 */
@PermissionInfo(value="Concept Provider Manager Access Permission", description="Grants access " +
    "to the Concept Provider Manager page")
public class ConceptProviderManagerAppPermission extends Permission {

    public ConceptProviderManagerAppPermission() {
        super("Graph Management permission");
    }

    /**
     *
     * @param target ignored
     * @param action ignored
     */
    public ConceptProviderManagerAppPermission(String target, String actions) {
        super("Graph Management permission");
    }

    @Override
    public boolean implies(Permission permission) {
        return equals(permission);
    }

    @Override
    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return 77985;
    }

    @Override
    public String getActions() {
        return "";
    }

}
