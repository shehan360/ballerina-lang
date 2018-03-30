/*
 *
 *   Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 * /
 */
package org.ballerinalang.observe.metrics.counter;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.util.metrics.Counter;

import java.util.ArrayList;
import java.util.List;

/**
 * Create and register a counter.
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "metrics",
        functionName = "register",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "Counter",
                structPackage = "ballerina.metrics"),
        args = {@Argument(name = "counter", type = TypeKind.STRUCT, structType = "Counter",
                structPackage = "ballerina.metrics")},
        isPublic = true
)
public class RegisterCounter extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        BStruct counterStruct = (BStruct) context.getRefArgument(0);
        String name = counterStruct.getStringField(0);
        String description = counterStruct.getStringField(1);
        BMap tagsMap = (BMap) counterStruct.getRefField(0);

        if (!tagsMap.isEmpty()) {
            List<String> tags = new ArrayList<>();
            for (Object key : tagsMap.keySet()) {
                tags.add(key.toString());
                tags.add(tagsMap.get(key).stringValue());
            }
            Counter.builder(name).description(description).tags(tags.toArray(new String[tags.size()]))
                    .register();
        } else {
            Counter.builder(name).description(description).register();
        }
    }
}