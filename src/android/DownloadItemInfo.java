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

/**
 * Download item info
 */
class DownloadItemInfo {
    private long id;
    private String uri;
    private int status;
    private int reason;
    private long bytesDownloaded;
    private long bytesTotal;

    public DownloadItemInfo(long id, String uri) {
        this.id = id;
        this.uri = uri;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public void setBytesDownloaded(long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
    }

    public long getBytesTotal() {
        return bytesTotal;
    }

    public void setBytesTotal(long bytesTotal) {
        this.bytesTotal = bytesTotal;
    }

    /**
     * Get download progress percentage
     *
     * @return Percentage
     */
    public int getDownloadProgress() {
        if (this.getBytesTotal() > 0) {
            return (int)Math.ceil((double)this.getBytesDownloaded() / (double)this.getBytesTotal() * 100);
        }

        return 0;
    }
}
