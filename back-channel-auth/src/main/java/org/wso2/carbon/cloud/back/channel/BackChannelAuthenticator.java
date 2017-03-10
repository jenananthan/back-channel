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
package org.wso2.carbon.cloud.back.channel;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BufferedHeader;
import org.apache.http.util.EntityUtils;
import org.jaggeryjs.hostobjects.web.RequestHostObject;
import org.jaggeryjs.hostobjects.web.SessionHostObject;
import org.jaggeryjs.scriptengine.exceptions.ScriptException;
import org.json.simple.JSONValue;
import org.mozilla.javascript.NativeObject;
import org.wso2.carbon.cloud.back.channel.constants.BackChannelAuthConstants;
import org.wso2.carbon.cloud.back.channel.config.ConfigReader;
import org.wso2.carbon.cloud.back.channel.dto.AuthenticationInfo;
import org.wso2.carbon.cloud.back.channel.util.Util;
import org.json.simple.JSONObject;
import com.nimbusds.jwt.SignedJWT;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the back channel authentication
 */
public class BackChannelAuthenticator {
    private String commonAuthId;
    private String samlssoTokenId;
    private String issuer;
    private String sessionIndex;
    private ConfigReader configReader;

    public BackChannelAuthenticator(String issuer, String commonAuthId, String samlssoTokenId) throws ScriptException {
        this.issuer = issuer;
        this.configReader = ConfigReader.getInstance();
        this.commonAuthId = commonAuthId;
        this.samlssoTokenId = samlssoTokenId;
    }


    public AuthenticationInfo login() throws IOException, ParseException {
        //invoke custom grant handler
        JSONObject responseJson = invokeTokenEndpoint();
        String jwtToken = null;
        if (responseJson != null) {
            Object token = responseJson.get("id_token");
            if (token != null) {
                jwtToken = token.toString().trim();
            }
        }
        //authenticate by jwt authenticator
        String cookie = null;
        if (jwtToken != null) {
            cookie = jwtAuthentcate(jwtToken);
        }

        String username = extractUser(jwtToken);
        boolean isSuperTenant = isSuperTenant(username);

        if (username != null && cookie != null) {
            return new AuthenticationInfo(username, cookie, isSuperTenant);
        }

        return null;
    }

    private String extractUser(String jwtToken) throws ParseException {
        SignedJWT jwsObject = SignedJWT.parse(jwtToken);
        String userName = jwsObject.getJWTClaimsSet().getSubject();
        return userName;
    }

    private boolean isSuperTenant(String userName) {
        String tenantDomain = MultitenantUtils.getTenantDomain(userName);
        if (BackChannelAuthConstants.SUPER_TENANT_DOMAIN.equals(tenantDomain)) {
            return true;
        }
        return false;
    }

    public void logout() {

    }

    private JSONObject invokeTokenEndpoint() throws IOException {
        //read from configs
        String tokenEP = configReader.getProperty(BackChannelAuthConstants.OAUTH_TOKEN_EP);
        String clientKey = configReader.getProperty(BackChannelAuthConstants.OAUTH_CLIENT_KEY);
        String clientSecret = configReader.getProperty(BackChannelAuthConstants.OAUTH_CLIENT_SECRET);
        //initialize http client
        HttpClient httpClient = Util.getHttpClient(tokenEP);
        HttpPost httpPost = new HttpPost(tokenEP);
        //set body parameters
        List<NameValuePair> params = new ArrayList<NameValuePair>(4);
        params.add(new BasicNameValuePair(BackChannelAuthConstants.GRANT_TYPE,
                                          BackChannelAuthConstants.CUSTOM_GRANT_TYPE));
        params.add(new BasicNameValuePair(BackChannelAuthConstants.COMMON_AUTH_ID, this.commonAuthId));
        params.add(new BasicNameValuePair(BackChannelAuthConstants.SAML_SSO_TOKEN_ID, this.samlssoTokenId));
        params.add(new BasicNameValuePair(BackChannelAuthConstants.ISSUER, this.issuer));
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        //Set headers
        String authValue = "Basic " + Util.encode(clientKey + ":" + clientSecret);
        httpPost.setHeader(BackChannelAuthConstants.CONTENT_TYPE_HEADER,
                           BackChannelAuthConstants.APPLICATION_FORM_URLENCODED);
        httpPost.setHeader(BackChannelAuthConstants.AUTHORIZATION_HEADER, authValue.trim());
        //invoke the endpoint
        HttpResponse response = httpClient.execute(httpPost);
        //get the response
        HttpEntity entity = response.getEntity();
        String responseString = "";
        if (entity != null) {
            responseString = EntityUtils.toString(entity, "UTF-8");
            EntityUtils.consume(entity);
        }
        JSONObject responseJson = (JSONObject) JSONValue.parse(responseString);
        //get the saml session index from header
        Header[] headers = response.getHeaders(BackChannelAuthConstants.SAML_SESSION_INDEX);
        this.sessionIndex = headers[0].getValue();

        return responseJson;
    }

    private String jwtAuthentcate(String jwtToken)
            throws IOException {
        String jwtAuthenticatorEP = configReader.getProperty(BackChannelAuthConstants.JWT_AUTHENTICATOR_EP);
        //initialize http client
        HttpClient httpClient = Util.getHttpClient(jwtAuthenticatorEP);
        HttpPost httpPost = new HttpPost(jwtAuthenticatorEP);
        //set headers
        String authValue = "Bearer " + jwtToken;
        httpPost.setHeader(BackChannelAuthConstants.CONTENT_TYPE_HEADER, BackChannelAuthConstants.TEXT_XML);
        httpPost.setHeader(BackChannelAuthConstants.SOAP_ACTION_HEADER, "rn:getUserInfo");
        httpPost.setHeader(BackChannelAuthConstants.AUTHORIZATION_HEADER, authValue);
        //set payload
        String payload = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body/></Envelope>";
        HttpEntity entity = new ByteArrayEntity(payload.getBytes("UTF-8"));
        httpPost.setEntity(entity);
        //execute
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity responseEntity = response.getEntity();
        Header[] headers = response.getHeaders(BackChannelAuthConstants.SET_COOKIE_HEADER);
        String jsessionCookie = ( headers[0].getValue()).split(";")[0];
        return jsessionCookie;
    }

    private void isAuthenticated() {

    }

    public boolean canAuthenticate() throws ScriptException {
        if (this.commonAuthId != null && this.samlssoTokenId != null) {
            return true;
        }
        return false;
    }

    public String getSessionIndex() {
        return this.sessionIndex;
    }

}
