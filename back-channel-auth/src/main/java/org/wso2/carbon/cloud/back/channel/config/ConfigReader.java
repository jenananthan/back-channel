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
package org.wso2.carbon.cloud.back.channel.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * This class used to read the configurations related to back channel authentication
 */
public class ConfigReader {

    private static ConfigReader configReader = new ConfigReader();
    private static Properties properties;

    static {
        properties = new Properties();
        loadConfig();
    }

    private ConfigReader() {
    }

    public static ConfigReader getInstance() {
        return configReader;
    }

    //read the config from file
    private static void loadConfig() {
        String filePath = CarbonUtils.getCarbonHome() + File.separator + "repository" +
                File.separator + "conf" +File.separator + "cloud" +
                File.separator + "backChannelAuth.properties";
        InputStream input = null;

        try {
            input = new FileInputStream(filePath);
            // load a properties file
            properties.load(input);
        } catch (IOException ex) {
            //TODO
        } finally {
            IOUtils.closeQuietly(input);
        }

    }

    public String getProperty(String key) {
        return properties.get(key).toString().trim();
    }
}
