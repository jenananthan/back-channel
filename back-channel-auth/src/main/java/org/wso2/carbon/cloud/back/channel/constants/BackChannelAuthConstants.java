/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.cloud.back.channel.constants;

/**
 * This class holds the contants
 */
public class BackChannelAuthConstants {
    //CUSTOM GRANT HANDLER
    public static final String COMMON_AUTH_ID = "commonAuthId";
    public static final String SAML_SSO_TOKEN_ID = "samlssoTokenId";
    public static final String ISSUER = "issuer";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CUSTOM_GRANT_TYPE = "cloud_auth";
    //HTTTP HEADERS
    public static final String CONTENT_TYPE_HEADER = "Content-type";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String SOAP_ACTION_HEADER = "SOAPAction";
    public static final String SET_COOKIE_HEADER = "Set-Cookie";
    public static final String SAML_SESSION_INDEX = "SessionIndexId";
    //CONTENT TYPES
    public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String TEXT_XML = "text/xml";
    //CONFIGS KEYS
    public static final String OAUTH_TOKEN_EP = "oauth.token.ep";
    public static final String OAUTH_CLIENT_KEY = "oauth.client.key";
    public static final String OAUTH_CLIENT_SECRET = "oauth.client.secret";
    public static final String JWT_AUTHENTICATOR_EP = "jwt.authenticator.ep";
    //OTHER
    public static final String SUPER_TENANT_DOMAIN = "carbon.super";

}
