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

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utils
 */
final class Utils {
    public static final String STATUS_NEW = "new";
    public static final String STATUS_DOWNLOADING = "downloading";
    public static final String STATUS_PAUSED = "paused";
    public static final String STATUS_FINALIZING = "finalizing";
    public static final String STATUS_FINISHED = "finished";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_EXTRACTING = "extracting";

    /**
     * Get new download item
     *
     * @param remoteUrl Remote URL
     * @param destinationFileUrl Destination file location
     * @param callback Cordova callback
     * @return DownloadItem
     */
    static DownloadItem getDownloadItem(String remoteUrl, String destinationFileUrl, CallbackContext callback) {
        return new DownloadItem(
                remoteUrl,
                destinationFileUrl,
                callback
        );
    }

    /**
     * Get status for given item
     *
     * @param status int Download status
     * @return String
     */
    public static String getStatus(int status) {
        switch (status) {
            case DownloadManager.STATUS_SUCCESSFUL:
                return STATUS_FINISHED;
            case DownloadManager.STATUS_FAILED:
                return STATUS_FAILED;
            case DownloadManager.STATUS_PAUSED:
                return STATUS_PAUSED;
            case DownloadManager.STATUS_PENDING:
                return STATUS_NEW;
            case DownloadManager.STATUS_RUNNING:
                return STATUS_DOWNLOADING;
            default:
                break;
        }

        return STATUS_NEW;
    }

    /**
     * Get JSON result for given item
     *
     * @param item Download item object
     * @param status Set status
     * @param progress Progress percentage
     * @return JSONObject
     */
    static JSONObject getResultJSON(DownloadItem item, String status, int progress) throws JSONException {
        JSONObject jsonProgress = new JSONObject();
        JSONObject obj = new JSONObject();
        obj.put("url", item.getRemoteUrl());
        obj.put("id", item.getId());
        obj.put("progress", progress);
        obj.put("status", status);

        return obj;
    }

    /**
     * Get JSON result for given item
     *
     * @param item Download item object
     * @param status Set status
     * @return JSONObject
     */
    static JSONObject getResultJSON(DownloadItem item, String status) throws JSONException {
        return getResultJSON(item, status, 0);
    }

    /**
     * Get JSON result for given error
     *
     * @param message Error description
     * @param code Error code
     * @param details Detailed error message
     * @return JSONObject
     */
    static JSONObject getErrorJSON(String message, int code, String details) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("message", message);
        obj.put("code", code);
        obj.put("details", details);

        return obj;
    }

    public interface ExtractZipProgress
    {
        void Progress(int percentage);
    }

    /**
     * Extract given zip to provided path
     *
     * @param destinationPath Destination path (working directory)
     * @param zipName Archive file name
     * @return True if passed
     * @throws IOException IO Error
     */
    static boolean extractZip(String destinationPath, String zipName, ExtractZipProgress progress) throws IOException {
        InputStream is;
        ZipInputStream zis;
        try {
            long total = new File(destinationPath + "/" + zipName).length();
            long processed = 0;

            String filename;
            is = new FileInputStream(destinationPath + "/" + zipName);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                if (progress != null) {
                    processed += ze.getCompressedSize();
                    int percentage = (int)Math.ceil((double)processed / (double)total * 100);
                    progress.Progress(percentage);
                }

                filename = ze.getName();

                if (ze.isDirectory()) {
                    File fmd = new File(destinationPath + "/" + filename);
                    fmd.mkdirs();
                    continue;
                }

                FileOutputStream fout = new FileOutputStream(destinationPath + "/" + filename);

                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }

                fout.close();
                zis.closeEntry();
            }

            zis.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Copy given file
     *
     * @param src Source file
     * @param dst Destination file
     * @throws IOException IO Error
     */
    public static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
}