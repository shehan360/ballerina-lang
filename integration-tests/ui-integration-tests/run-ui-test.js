/**
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var argv = require('yargs').argv;
// var webdriverio = require('webdriverio');
var shell = require('shelljs');
var spawn = require('child_process').spawn;

if (argv.skipTests === "true") {
    console.log('Skipping UI Tests');
    return;
}else {
    var child = spawn('node', ['start-selenium.js'], {
        stdio: 'ignore',
        detached: true
    }).unref();

    console.log('Running Selenium Tests');
    shell.exec("NODE_ENV=test mocha-webpack --webpack-config webpack.config.js ui-tests/startPageTest.js", function(code) {
        shell.exit(code);
    });
    //var options = {
    //    desiredCapabilities: {
    //        browserName: 'firefox'
    //    }
    //};
    //
    //webdriverio
    //    .remote(options)
    //    .init()
    //    .url('http://localhost:9091/') // navigate to the web page
    //    //.setValue('#orb-search-q', ['surfing'], function () {
    //    //}) // find the element and enter the query
    //    //.submitForm('#orb-search-form') // submit the form
    //    //.title(function (err, res) {
    //    //    console.log('Title was: ' + res.value); // Confirm the page title
    //    //})
    //    .saveScreenshot('./snapshot.png') // Save the screenshot to disk
    //    .end();
}
