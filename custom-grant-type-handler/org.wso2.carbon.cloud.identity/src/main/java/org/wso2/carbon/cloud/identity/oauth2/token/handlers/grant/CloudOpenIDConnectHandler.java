/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.cloud.identity.oauth2.token.handlers.grant;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cloud.identity.oauth2.token.handlers.grant.internal.CloudIdentityDataHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.ResponseHeader;
import org.wso2.carbon.identity.oauth2.model.RequestParameter;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.AbstractAuthorizationGrantHandler;
import org.wso2.carbon.identity.sso.saml.session.SSOSessionPersistenceManager;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;

public class CloudOpenIDConnectHandler extends AbstractAuthorizationGrantHandler {
    private static Log log = LogFactory.getLog(CloudOpenIDConnectHandler.class);
    public static final String COMMON_AUTH_ID_COOKIE = "commonAuthId";
    public static final String SAML_SSO_TOKEN_ID = "samlssoTokenId";
    public static final String ISSUER = "issuer";

    @Override
    public boolean validateGrant(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {
        log.info("*** In CloudOpenIDConnectHandler ***");


        String commonAuthIdCookie = null;
        String samlssoTokenId = null;
        String issuer = null;
        RequestParameter[] parameters = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getRequestParameters();
        for (RequestParameter parameter : parameters) {
            if (COMMON_AUTH_ID_COOKIE.equals(parameter.getKey())) {
                if (parameter.getValue() != null && parameter.getValue().length > 0) {
                    commonAuthIdCookie = parameter.getValue()[0];
                }
            } else if(SAML_SSO_TOKEN_ID.equals(parameter.getKey())) {
                if (parameter.getValue() != null && parameter.getValue().length > 0) {
                    samlssoTokenId = parameter.getValue()[0];
                }
            } else if (ISSUER.equals(parameter.getKey())) {
                if (parameter.getValue() != null && parameter.getValue().length > 0) {
                    issuer = parameter.getValue()[0];
                }
            }

        }


        log.info("*** commonAuthIdCookie: " + commonAuthIdCookie);

        SessionContext session = FrameworkUtils.getSessionContextFromCache(commonAuthIdCookie);
        if (session != null) {
            AuthenticatedUser authUser = session.getAuthenticatedIdPs().get("LOCAL").getUser();
            log.info("Auth user tenant domain: " + authUser.getTenantDomain());
            log.info("Auth username " + authUser.getUserName());

            try {
                TenantManager tenantManager = CloudIdentityDataHolder.getInstance().getRealmService().getTenantManager();
                int tenantId = tenantManager.getTenantId(authUser.getTenantDomain());
                CloudIdentityDataHolder.getInstance().getTenantRegistryLoader().loadTenantRegistry(tenantId);
            } catch (RegistryException e) {
                //TODO
                e.printStackTrace();
            } catch (UserStoreException e) {
                //TODO
                e.printStackTrace();
            }

            //Add the SP as SAML SSO session participant
            String sessionIndexId = null;
            try {
                SAMLSSOServiceProviderDO spDO = getSAMLSSOServiceProviderDO(issuer);
                SSOSessionPersistenceManager sessionPersistenceManager =
                        SSOSessionPersistenceManager.getPersistenceManager();
                if (samlssoTokenId != null && sessionPersistenceManager.isExistingTokenId(samlssoTokenId)) {
                    sessionIndexId = sessionPersistenceManager.getSessionIndexFromTokenId(samlssoTokenId);
                    sessionPersistenceManager.persistSession(sessionIndexId, authUser.getUserName(), spDO,
                                                             null, issuer, spDO.getDefaultAssertionConsumerUrl());
                } else {
                    //TODO
                }
            } catch (IdentityException e) {
                e.printStackTrace();
            }

            //Return session index in response header
            ResponseHeader responseHeader = new ResponseHeader();
            responseHeader.setKey("SessionIndexId");
            responseHeader.setValue(sessionIndexId);
            tokReqMsgCtx.addProperty("RESPONSE_HEADERS", new ResponseHeader[]{responseHeader});

            tokReqMsgCtx.setAuthorizedUser(authUser);
            tokReqMsgCtx.setScope(new String[]{"openid"});
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean authorizeAccessDelegation(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {
        return true;
    }

    @Override
    public boolean validateScope(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {
        return true;
    }

    /**
     * Returns the configured service provider configurations. The
     * configurations are taken from the user registry or from the
     * sso-idp-config.xml configuration file. 
     *
     * @param
     * @return
     * @throws org.wso2.carbon.identity.base.IdentityException
     */
    private SAMLSSOServiceProviderDO getSAMLSSOServiceProviderDO(String issuer)
            throws IdentityException {
        try {
            SSOServiceProviderConfigManager stratosIdpConfigManager = SSOServiceProviderConfigManager
                    .getInstance();
            SAMLSSOServiceProviderDO ssoIdpConfigs = stratosIdpConfigManager
                    .getServiceProvider(issuer);

            if (ssoIdpConfigs == null) {
                IdentityTenantUtil.initializeRegistry(PrivilegedCarbonContext.getThreadLocalCarbonContext()
                                                              .getTenantId(),
                                                      PrivilegedCarbonContext.getThreadLocalCarbonContext()
                                                              .getTenantDomain());
                IdentityPersistenceManager persistenceManager = IdentityPersistenceManager.getPersistanceManager();
                Registry registry = (Registry) PrivilegedCarbonContext.getThreadLocalCarbonContext().getRegistry
                        (RegistryType.SYSTEM_CONFIGURATION);
                ssoIdpConfigs = persistenceManager.getServiceProvider(registry, issuer);
            }
            return ssoIdpConfigs;
        } catch (Exception e) {
            //TODO
            throw IdentityException.error("Error while reading Service Provider configurations", e);
        }
    }


}