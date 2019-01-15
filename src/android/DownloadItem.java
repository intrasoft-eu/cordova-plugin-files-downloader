/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package eu.intrasoft.cordova.filesdownloader;

import android.app.DownloadManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Download item
 */
class DownloadItem {
    private long id;
    private String remoteUrl;
    private String destinationFileUrl;
    private String title;
    private boolean extract;
    private CallbackContext callback;
    private Timer timer;

    DownloadItem(String remoteUrl, String destinationFileUrl, CallbackContext callback) {
        this.remoteUrl = remoteUrl;
        this.destinationFileUrl = destinationFileUrl;
        this.callback = callback;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getDestinationFileUrl() {
        return destinationFileUrl;
    }

    /**
     * Get temporary file url (in the downloads folder)
     *
     * @return String
     * @throws IOException IO Error
     */
    public String getTemporaryFileUrl() throws IOException {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        String filename = this.getDestinationFileUrl().substring(this.getDestinationFileUrl().lastIndexOf("/") + 1);
        File outputFile = new File(downloadDir, filename.concat(".download"));// File.createTempFile(filename, "download", outputDir);

        return Uri.fromFile(outputFile).toString();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isExtract() {
        return extract;
    }

    public void setExtract(boolean extract) {
        this.extract = extract;
    }

    public CallbackContext setCallback(CallbackContext callback) {
        return this.callback = callback;
    }

    public CallbackContext getCallback() {
        return callback;
    }

    /**
     * Get new request for this item
     *
     * @return DownloadManager.Request
     */
    public DownloadManager.Request getNewRequest() throws IOException {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(this.getRemoteUrl()));
        request.setTitle(this.getTitle());
        request.setVisibleInDownloadsUi(false);
        request.setDestinationUri(Uri.parse(this.getTemporaryFileUrl()));

        return request;
    }

    public void stopMonitoring() {
        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }
    }

    public void startMonitoring(TimerTask timerTask, long interval) {
        final long id = this.getId();

        this.timer = new Timer();
        timer.schedule(timerTask, 0, interval);
    }

    /**
     * Send result to UI
     *
     * @param status Current status
     * @param progress Current progress
     */
    public void sendResult(String status, int progress) {
        try {
            JSONObject info = Utils.getResultJSON(this, status, progress);
            PluginResult progressUpdate = new PluginResult(PluginResult.Status.OK, info);
            progressUpdate.setKeepCallback(true);
            this.getCallback().sendPluginResult(progressUpdate);
        } catch (JSONException e) {
            e.printStackTrace();
            this.getCallback().sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
        }
    }

    /**
     * Send result to UI
     *
     * @param status Current status
     */
    public void sendResult(String status) {
        this.sendResult(status, 0);
    }

    /**
     * Send error to UI
     *
     * @param message Message for UI
     * @param code Internal error code
     * @param error Inner exception
     */
    public void sendError(String message, int code, Exception error) {
        try {
            this.getCallback().error(Utils.getErrorJSON(message, code, error.getMessage()));
        } catch (JSONException e) {
            e.printStackTrace();
            this.getCallback().sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
        }
    }

    /**
     * Send error to UI
     *
     * @param message Message for UI
     * @param error Inner exception
     */
    public void sendError(String message, Exception error) {
        sendError(message, 0, error);
    }
}
