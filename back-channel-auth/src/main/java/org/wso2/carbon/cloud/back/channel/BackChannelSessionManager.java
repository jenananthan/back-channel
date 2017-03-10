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

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jaggeryjs.hostobjects.web.SessionHostObject;
import org.jaggeryjs.scriptengine.exceptions.ScriptException;
import org.wso2.carbon.cloud.back.channel.util.Util;


public class BackChannelSessionManager {

    private volatile Map<String, SessionHostObject> sessionIndexMap =
            new ConcurrentHashMap<String, SessionHostObject>();
    // issuerId, sessionManager.this is to provide sso functionality to multiple jaggery apps.
    private static Map<String, BackChannelSessionManager> sessionManagerMap =
            new ConcurrentHashMap<String, BackChannelSessionManager>();

    private BackChannelSessionManager() {
    }

    public static BackChannelSessionManager getInstance(String issuer) {
        if (sessionManagerMap.get(issuer) == null) {
            synchronized (BackChannelSessionManager.class) {
                if (sessionManagerMap.get(issuer) == null) {
                    BackChannelSessionManager sessionManagerObj = new BackChannelSessionManager();
                    sessionManagerMap.put(issuer, sessionManagerObj);
                }
            }
        }
        return sessionManagerMap.get(issuer);
    }

    public void setSessionAuthenticated(SessionHostObject session, String sessionIndex) throws Exception {
        sessionIndexMap.put(sessionIndex, session);
        clearExpiredSessions();
    }


    public void invalidateSession(String samlLogoutRequest) throws Exception {
        String sessionIndex = Util.getSessionIndex(samlLogoutRequest);
        SessionHostObject session = sessionIndexMap.get(sessionIndex);
        Object[] args = new Object[0];
        SessionHostObject.jsFunction_invalidate(null, session, args, null);
        sessionIndexMap.remove(sessionIndex);
        clearExpiredSessions();
    }


    private void clearExpiredSessions() throws ScriptException {
        Iterator<Map.Entry<String, SessionHostObject>> iterator = sessionIndexMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SessionHostObject> entry = iterator.next();
            SessionHostObject session = entry.getValue();
            long maxInactiveInterval = session.jsGet_maxInactive() * 1000;
            Object[] args = new Object[0];
            long lastAccessTime = SessionHostObject.jsFunction_getLastAccessedTime(null, session, new Object[0], null);
            Date now = new Date();
            if ((now.getTime() - lastAccessTime) > maxInactiveInterval) {
                iterator.remove();
            }
        }
    }

    private void doClusterLogout() {

    }

}
