/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2023 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
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
 * ---license-end
 */

package com.github.xrodney.keycloak.operator.spec;

import javax.validation.constraints.NotNull;

public class KeycloakConnection {
    private String loginRealm;
    private String clientId;
    @NotNull
    private String url;
    private String user;
    private SecretRef passwordSecret;
    private SecretRef clientSecretSecret;
    private String grantType;
    private boolean sslVerify;
    private int connectTimeout;
    private int readTimeout;

    public String getLoginRealm() {
        return loginRealm;
    }

    public void setLoginRealm(String loginRealm) {
        this.loginRealm = loginRealm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public SecretRef getPasswordSecret() {
        return passwordSecret;
    }

    public void setPasswordSecret(SecretRef passwordSecret) {
        this.passwordSecret = passwordSecret;
    }

    public SecretRef getClientSecretSecret() {
        return clientSecretSecret;
    }

    public void setClientSecretSecret(SecretRef clientSecretSecret) {
        this.clientSecretSecret = clientSecretSecret;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public boolean isSslVerify() {
        return sslVerify;
    }

    public void setSslVerify(boolean sslVerify) {
        this.sslVerify = sslVerify;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
