/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

/**
 * FilesDownloader
 *
 * @constructor
 */
function FilesDownloader() {}


/**
 * Start a new download for given remote file
 *
 * @param {string} remoteUrl
 * @param {object} destinationFile
 * @param {object} options
 * @param successCallback
 * @param errorCallback
 */
FilesDownloader.prototype.download = function (remoteUrl, destinationFile, options, successCallback, errorCallback) {
    if (!options) {
        options = {};
    }

    options.remoteUrl = remoteUrl;
    options.destinationFileUrl = destinationFile.toURL();

    cordova.exec(successCallback, errorCallback, 'FilesDownloader', 'download', [options]);
};

/**
 * Cancel download for given remote file
 *
 * @param {string} remoteUrl
 * @param successCallback
 * @param errorCallback
 */
FilesDownloader.prototype.cancel = function (remoteUrl, successCallback, errorCallback) {
    var options = {};
    
    options.remoteUrl = remoteUrl;
    
    cordova.exec(successCallback, errorCallback, 'FilesDownloader', 'cancel', [options]);
};

/**
 *
 * @returns {FilesDownloader}
 */
FilesDownloader.install = function () {
  window.FilesDownloader = new FilesDownloader();
  return window.FilesDownloader;
};

cordova.addConstructor(FilesDownloader.install);