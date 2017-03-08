/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.cloud.identity.oauth2.token.handlers.grant.internal;

import org.wso2.carbon.registry.core.service.TenantRegistryLoader;
import org.wso2.carbon.user.core.service.RealmService;

public class CloudIdentityDataHolder {

    private static CloudIdentityDataHolder instance = new CloudIdentityDataHolder();

    private TenantRegistryLoader tenantRegistryLoader = null;
    private RealmService realmService = null;

    public static CloudIdentityDataHolder getInstance() {
        return instance;
    }

    public void setTenantRegistryLoader(TenantRegistryLoader tenantRegistryLoader) {
        this.tenantRegistryLoader = tenantRegistryLoader;
    }

    public TenantRegistryLoader getTenantRegistryLoader() {
        return this.tenantRegistryLoader;
    }

    public RealmService getRealmService() {
        return this.realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }
}