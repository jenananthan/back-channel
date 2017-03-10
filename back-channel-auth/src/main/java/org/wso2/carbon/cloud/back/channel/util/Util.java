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
package org.wso2.carbon.cloud.back.channel.util;

import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLException;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.util.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Random;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.joda.time.DateTime;
/**
 *
 * This class provides util methods
 */
public class Util {
    private static boolean bootStrapped = false;
    private static Random random = new Random();

    public static XMLObject unmarshall(String saml2SSOString) throws SAMLException {

        //Initializing Open SAML Library
        doBootstrap();
        try {
            //Converting the decoded SAML Response string into DOM object
            ByteArrayInputStream inputStreams = new ByteArrayInputStream(saml2SSOString.getBytes(Charset.forName(
                    "UTF-8")));
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = docBuilder.parse(inputStreams);
            Element element = document.getDocumentElement();

            //Unmarshalling the element
            UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
            Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
            XMLObject xmlObj = unmarshaller.unmarshall(element);
            return xmlObj;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new SAMLException("Error while parsing the decoded SAML token", e);
        } catch (UnmarshallingException e) {
            throw new SAMLException("Error while unmarshalling the decoded SAML token", e);
        }
    }

    public static void doBootstrap() throws SAMLException {
      /* Initializing the OpenSAML library */
        if (!bootStrapped) {
            try {
                DefaultBootstrap.bootstrap();
                bootStrapped = true;
            } catch (ConfigurationException e) {
                throw new SAMLException("Error while bootstrapping OpenSAML library", e);
            }
        }
    }

    public static String decode(String encodedStr) {
        return new String(Base64.decode(encodedStr));
    }

    public static String encode(String plainText) {
        return new String(Base64.encodeBytes(plainText.getBytes(Charset.forName(
                "UTF-8")))).trim();
    }

    public static String getSessionIndex(String samlLogoutReq) throws SAMLException {
        String decoded = decode(samlLogoutReq);
        XMLObject xmlObj = unmarshall(decoded);
        if (xmlObj instanceof LogoutRequest) {
            LogoutRequest logoutRequest = (LogoutRequest) xmlObj;
            String sessionIndex = logoutRequest.getSessionIndexes().get(0).getSessionIndex();
            return sessionIndex;
        } else {
            throw new SAMLException("Provided token in not a SAML logout request token");
        }

    }


    private static HttpClient getHttpClient(int port, String protocol) {
        SchemeRegistry registry = new SchemeRegistry();
        X509HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        socketFactory.setHostnameVerifier(hostnameVerifier);
        if ("https".equals(protocol)) {
            if (port >= 0) {
                registry.register(new Scheme("https", port, socketFactory));
            } else {
                registry.register(new Scheme("https", 443, socketFactory));
            }
        } else if ("http".equals(protocol)) {
            if (port >= 0) {
                registry.register(new Scheme("http", port, PlainSocketFactory.getSocketFactory()));
            } else {
                registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
            }
        }
        HttpParams params = new BasicHttpParams();
        ThreadSafeClientConnManager tcm = new ThreadSafeClientConnManager(registry);
        return new DefaultHttpClient(tcm, params);

    }

    /**
     * Return a http client instance. This http client is configured according to the
     * org.wso2.ignoreHostnameVerification system property.
     *
     * @param url - server endpoint
     * @return HttpClient
     */
    public static HttpClient getHttpClient(String url) throws MalformedURLException {
        URL ulrEndpoint = new URL(url);
        int port = ulrEndpoint.getPort();
        String protocol = ulrEndpoint.getProtocol();
        return getHttpClient(port, protocol);
    }


}
