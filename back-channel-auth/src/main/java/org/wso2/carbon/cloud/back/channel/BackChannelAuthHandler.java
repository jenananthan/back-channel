package org.wso2.carbon.cloud.back.channel;

import org.jaggeryjs.hostobjects.web.RequestHostObject;
import org.jaggeryjs.hostobjects.web.SessionHostObject;
import org.jaggeryjs.scriptengine.exceptions.ScriptException;
import org.mozilla.javascript.NativeObject;
import org.wso2.carbon.cloud.back.channel.constants.BackChannelAuthConstants;
import org.wso2.carbon.cloud.back.channel.dto.AuthenticationInfo;


/**
 * This class handles the back channel login and logout
 */
public class BackChannelAuthHandler {

    public static AuthenticationInfo login(RequestHostObject requestHostObject, SessionHostObject session,
                                           String issuer)
            throws Exception {
        String commonAuthId = getCookieValue(requestHostObject, BackChannelAuthConstants.COMMON_AUTH_ID);
        String samlssoTokenId = getCookieValue(requestHostObject, BackChannelAuthConstants.SAML_SSO_TOKEN_ID);
        BackChannelAuthenticator authenticator = new BackChannelAuthenticator(issuer, commonAuthId, samlssoTokenId);
        AuthenticationInfo authenticationInfo = authenticator.login();
        //Manage the session
        String sessionIndex = authenticator.getSessionIndex();
        BackChannelSessionManager sessionManager = BackChannelSessionManager.getInstance(issuer);
        sessionManager.setSessionAuthenticated(session, sessionIndex);
        return authenticationInfo;
    }


    public static void invalidateSession(String issuer, String samlLogoutReq) throws Exception {
        BackChannelSessionManager backChannelSessionManager = BackChannelSessionManager.getInstance(issuer);
        backChannelSessionManager.invalidateSession(samlLogoutReq);
    }

    public static boolean canAuthenticate(RequestHostObject requestHostObject) throws ScriptException {
        String commonAuthId = getCookieValue(requestHostObject, BackChannelAuthConstants.COMMON_AUTH_ID);
        String samlssoTokenId = getCookieValue(requestHostObject, BackChannelAuthConstants.SAML_SSO_TOKEN_ID);
        if (commonAuthId != null && samlssoTokenId != null) {
            return true;
        }
        return false;
    }


    private static String getCookieValue(RequestHostObject requestHostObject, String cookieName)
            throws ScriptException {
        Object[] args = new Object[1];
        args[0] = cookieName;
        NativeObject obj = (NativeObject) (RequestHostObject.jsFunction_getCookie(null, requestHostObject, args, null));
        if(obj == null) {
            return null;
        }
        return (String) obj.get("value");
    }
}
