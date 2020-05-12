/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerinalang.compiler.parser.test.syntax.expressions;

import org.testng.annotations.Test;

/**
 * Test parsing anonymous function expression.
 * 
 * @since 1.3.0
 */
public class AnonymousFuncExpressionTest extends AbstractExpressionsTest {

    // Valid source tests

//    @Test
    public void testExplicitFuncWithEmptyBlockBody() {
        test("anon-func/anon_func_source_01.bal", "anon-func/anon_func_assert_01.json");
    }
    
//    @Test
    public void testExplicitFuncWithBlockBody() {
        test("anon-func/anon_func_source_02.bal", "anon-func/anon_func_assert_02.json");
    }


    // Recovery test

}
