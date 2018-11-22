// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/log;
import ballerina/runtime;
import ballerina/time;
import ballerina/config;
import ballerina/crypto;
import ballerina/internal;
import ballerina/system;

public type ConfigJwtAuthProvider object {

    public InferredJwtAuthProviderConfig configJwtAuthProviderConfig;
    public ConfigAuthStoreProvider configAuthProvider = new;

    public function __init(InferredJwtAuthProviderConfig configJwtAuthProviderConfig) {
        self.configJwtAuthProviderConfig = configJwtAuthProviderConfig;
    }

    public function authenticate(string username, string password) returns boolean {
        boolean isAuthenticated = self.configAuthProvider.authenticate(username, password);
        if (isAuthenticated){
            setAuthToken(username, self.configJwtAuthProviderConfig);
        }
        return isAuthenticated;
    }

    public function getScopes(string username) returns string[] {
        return self.configAuthProvider.getScopes(username);
    }

};
