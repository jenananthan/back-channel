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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.service.TenantRegistryLoader;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="cloud.identity.service.component" immediate=true
 * @scr.reference name="registry.loader.default"
 * interface="org.wso2.carbon.registry.core.service.TenantRegistryLoader"
 * cardinality="1..1" policy="dynamic"
 * bind="setRegistryLoader" unbind="unsetRegistryLoader"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic"
 * bind="setRealmService" unbind="unsetRealmService"
 */

public class CloudIdentityServiceComponent {
    private static Log log = LogFactory.getLog(CloudIdentityServiceComponent.class);

    protected void setRegistryLoader(TenantRegistryLoader tenantRegistryLoader) {
        log.info("*** In CloudIdentityServiceComponent#setRegistryLoader() ***");
        CloudIdentityDataHolder.getInstance().setTenantRegistryLoader(tenantRegistryLoader);
    }

    protected void unsetRegistryLoader(TenantRegistryLoader tenantRegistryLoader) {
        log.info("*** In CloudIdentityServiceComponent#unsetRegistryLoader() ***");
        CloudIdentityDataHolder.getInstance().setTenantRegistryLoader(null);
    }

    protected void setRealmService(RealmService realmService) {
        log.info("*** In CloudIdentityServiceComponent#setRealmService() ***");
        CloudIdentityDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        log.info("*** In CloudIdentityServiceComponent#unsetRealmService() ***");
        CloudIdentityDataHolder.getInstance().setRealmService(null);

    }

}