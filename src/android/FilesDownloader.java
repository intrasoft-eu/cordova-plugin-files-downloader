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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;

import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;

/**
 * Main Plugin class.
 * Manages many downloads with one interface.
 */
public class FilesDownloader extends CordovaPlugin {
    private static final String ACTION_DOWNLOAD = "download";
    private static final String ACTION_CANCEL = "cancel";
    private static final long UPDATE_INTERVAL = 1000;

    private Activity cordovaActivity;
    private DownloadManager downloadManager;
    private BroadcastReceiver downloadReceiver = null;
    private HashMap<String, DownloadItem> items = new HashMap<String, DownloadItem>();

    @Override
    protected void pluginInitialize() {
        cordovaActivity = this.cordova.getActivity();
        downloadManager = (DownloadManager) cordovaActivity.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        try {
            if (ACTION_DOWNLOAD.equals(action)) {
                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            download(args, callbackContext);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
                        }
                    }
                });

                return true;
            }

            if (ACTION_CANCEL.equals(action)) {
                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            cancel(args, callbackContext);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
                        }
                    }
                });

                return true;
            }

            return false;
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            callbackContext.error(Utils.getErrorJSON("Could not execute given action.", 0, e.getMessage()));
        }
        return true;
    }

    /**
     * Register new URL in download manager
     *
     * @param args Arguments
     * @param callbackContext Callback context
     * @throws JSONException JSON error
     */
    private void download(JSONArray args, CallbackContext callbackContext) throws JSONException {
        try {
            JSONObject arg_object = args.getJSONObject(0);
            final String remoteUrl = arg_object.getString("remoteUrl");
            String destinationFileUrl = arg_object.getString("destinationFileUrl");
            String title = arg_object.has("title") ? arg_object.getString("title") : "";
            boolean extract = arg_object.has("extract") && arg_object.getBoolean("extract");

            if (items.containsKey(remoteUrl)) {
                DownloadItem item = items.get(remoteUrl);
                item.stopMonitoring();
                items.remove(remoteUrl);
            }

            DownloadItem item = Utils.getDownloadItem(remoteUrl, destinationFileUrl, callbackContext);
            item.setTitle(title);
            item.setExtract(extract);

            DownloadItemInfo info = this.findDownloadInfoByUri(item.getRemoteUrl());

            if (null == info) {
                File temporaryFile = new File(Uri.parse(item.getTemporaryFileUrl()).getPath());
                if (temporaryFile.exists()) {
                    if (!temporaryFile.delete()) {
                        throw new DownloadException(100, "Could not delete existing temporary file.");
                    }
                }

                DownloadManager.Request request = item.getNewRequest();
                item.setId(this.downloadManager.enqueue(request));
                info = this.findDownloadInfoByUri(item.getRemoteUrl());
            } else {
                item.setId(info.getId());
            }

            items.put(item.getRemoteUrl(), item);

            item.startMonitoring(new TimerTask() {
                @Override
                public void run() {
                    DownloadItemInfo downloadInfo = findDownloadInfoById(item.getId());
                    if (null != downloadInfo) {
                        String status = Utils.getStatus(downloadInfo.getStatus());
                        if (Utils.STATUS_FINISHED.equals(status) && item.isExtract()) {
                            status = Utils.STATUS_EXTRACTING;
                        }

                        item.sendResult(status, downloadInfo.getDownloadProgress());
                    } else {
                        checkDownloadItem(item, DownloadManager.STATUS_FAILED);
                    }
                }
            }, UPDATE_INTERVAL);

            if (info != null) {
                checkDownloadItem(item, info.getStatus());
            }

            checkDownloadReceiver();
        } catch (DownloadException e) {
            System.err.println("Exception: " + e.getMessage());
            callbackContext.error(Utils.getErrorJSON("Could not start download for given URL.", e.getCode(), e.getMessage()));
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            callbackContext.error(Utils.getErrorJSON("Could not start download for given URL.", 0, e.getMessage()));
        }
    }

    /**
     * Cancel download
     *
     * @param args Arguments
     * @param callbackContext Callback context
     * @throws JSONException JSON error
     */
    private void cancel(JSONArray args, CallbackContext callbackContext) throws JSONException {
        try {
            JSONObject arg_object = args.getJSONObject(0);
            final String remoteUrl = arg_object.getString("remoteUrl");

            DownloadItemInfo info = this.findDownloadInfoByUri(remoteUrl);
            if (null == info) {
                throw new DownloadException(104, "Given URL is not registered in DownloadManager.");
            }

            DownloadItem item = this.getDownloadItemById(info.getId());
            if (null == item) {
                throw new DownloadException(104, "There is no active download for given URL.");
            }

            item.sendResult(Utils.STATUS_CANCELLED);
            this.flushDownload(item);
        } catch (DownloadException e) {
            System.err.println("Exception: " + e.getMessage());
            callbackContext.error(Utils.getErrorJSON("Could not start download for given URL.", e.getCode(), e.getMessage()));
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            callbackContext.error(Utils.getErrorJSON("Could not start download for given URL.", 0, e.getMessage()));
        }
    }

    /**
     * Find download item by id
     *
     * @param id Download ID
     * @return Download item
     */
    private DownloadItem getDownloadItemById(long id) {
        for (DownloadItem item : items.values()) {
            if (id == item.getId()) {
                return item;
            }
        }

        return null;
    }

    /**
     * Get download info object for given cursor
     *
     * @param cursor Cursor with download result
     * @return DownloadItemInfo
     */
    private DownloadItemInfo getDownloadInfo(Cursor cursor) {
        int colId = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
        int colUri = cursor.getColumnIndex(DownloadManager.COLUMN_URI);
        int colStatus = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
        int colReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
        int colBytesDownloaded = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
        int colBytesTotal = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

        DownloadItemInfo info = new DownloadItemInfo(cursor.getLong(colId), cursor.getString(colUri));
        info.setStatus(cursor.getInt(colStatus));
        info.setReason(cursor.getInt(colReason));
        info.setBytesDownloaded(cursor.getLong(colBytesDownloaded));
        info.setBytesTotal(cursor.getLong(colBytesTotal));

        return info;
    }

    /**
     * Find download info for given URI
     *
     * @param uri Download URI
     * @return DownloadItemInfo
     */
    private DownloadItemInfo findDownloadInfoByUri(String uri) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterByStatus(
                    DownloadManager.STATUS_PENDING |
                    DownloadManager.STATUS_RUNNING |
                    DownloadManager.STATUS_PAUSED |
                    DownloadManager.STATUS_SUCCESSFUL
        );

        try (Cursor cursor = this.downloadManager.query(query)) {
            int colUri = cursor.getColumnIndex(DownloadManager.COLUMN_URI);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                if (uri.equals(cursor.getString(colUri))) {
                    return this.getDownloadInfo(cursor);
                }
            }
        }

        return null;
    }

    /**
     * Find download info for given ID
     *
     * @param id Download ID
     * @return DownloadItemInfo
     */
    private DownloadItemInfo findDownloadInfoById(long id) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);

        try (Cursor cursor = this.downloadManager.query(query)) {
            if (cursor.moveToFirst()) {
                return this.getDownloadInfo(cursor);
            }
        }

        return null;
    }

    /**
     * Remove and stop monitoring given download item
     *
     * @param downloadItem Download item to flush
     */
    private void flushDownload(DownloadItem downloadItem) {
        downloadItem.stopMonitoring();
        downloadManager.remove(downloadItem.getId());
        items.remove(downloadItem.getRemoteUrl());
        checkDownloadReceiver();
    }

    /**
     * Check if given download has been finished
     *
     * @param downloadItem Download item
     * @param status Download status
     */
    private void checkDownloadItem(DownloadItem downloadItem, int status) {
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            try {
                downloadItem.sendResult(Utils.STATUS_FINALIZING);

                File temporaryFile = new File(Uri.parse(downloadItem.getTemporaryFileUrl()).getPath());
                File destinationFile = new File(Uri.parse(downloadItem.getDestinationFileUrl()).getPath());

                if (destinationFile.exists()) {
                    if (!destinationFile.delete()) {
                        throw new DownloadException(101, "Could not remove destination file.");
                    }
                }

                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try {
                            try {
                                Utils.copyFile(temporaryFile, destinationFile);
                            } catch (IOException e) {
                                throw new DownloadException(102, "Could not save downloaded file.");
                            }

                            temporaryFile.delete();

                            if (downloadItem.isExtract()) {
                                downloadItem.sendResult(Utils.STATUS_EXTRACTING);
                                boolean res = Utils.extractZip(
                                        destinationFile.getParent(),
                                        destinationFile.getName(),
                                        percentage -> {
                                            downloadItem.sendResult(Utils.STATUS_EXTRACTING, percentage);
                                        }
                                );

                                if (!res) {
                                    throw new DownloadException(103, "Could not extract downloaded file.");
                                }

                                destinationFile.delete();

                                downloadItem.sendResult(Utils.STATUS_FINISHED);
                            } else {
                                downloadItem.sendResult(Utils.STATUS_FINISHED);
                            }
                        } catch (DownloadException e) {
                            System.err.println("Exception: " + e.getMessage());
                            downloadItem.sendResult(Utils.STATUS_FAILED);
                            downloadItem.sendError("This download could not be processed.", e.getCode(), e);
                        } catch (Exception e) {
                            System.err.println("Exception: " + e.getMessage());
                            downloadItem.sendResult(Utils.STATUS_FAILED);
                            downloadItem.sendError("This download could not be processed.", 0, e);
                        } finally {
                            flushDownload(downloadItem);
                        }
                    }
                });
            } catch (DownloadException e) {
                System.err.println("Exception: " + e.getMessage());
                downloadItem.sendResult(Utils.STATUS_FAILED);
                downloadItem.sendError("This download could not be processed.", e.getCode(), e);
            } catch (Exception e) {
                System.err.println("Exception: " + e.getMessage());
                downloadItem.sendResult(Utils.STATUS_FAILED);
                downloadItem.sendError("This download could not be processed. ", 0, e);
            } finally {
                this.flushDownload(downloadItem);
            }
        } else if (status == DownloadManager.STATUS_FAILED) {
            this.flushDownload(downloadItem);
            downloadItem.sendResult(Utils.STATUS_CANCELLED);
        }
    }

    /**
     * Get new broadcast receiver
     *
     * @return BroadcastReceiver
     */
    private BroadcastReceiver getNewDownloadReceiver() {
        return new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (downloadId == -1) return;

                DownloadItem downloadItem = getDownloadItemById(downloadId);
                if (null == downloadItem) return;

                DownloadItemInfo info = findDownloadInfoById(downloadId);
                int status = null != info ? info.getStatus() : DownloadManager.STATUS_FAILED;

                checkDownloadItem(downloadItem, status);
            }
        };
    }

    /**
     * Register / unregister broadcast receiver
     */
    private void checkDownloadReceiver() {
        if (!this.items.isEmpty()) {
            if (null == this.downloadReceiver) {
                this.downloadReceiver = this.getNewDownloadReceiver();
            }

            this.cordovaActivity.registerReceiver(this.downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } else {
            if (null != this.downloadReceiver) {
                try {
                    this.cordovaActivity.unregisterReceiver(this.downloadReceiver);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
        }
    }
}