# cordova-plugin-files-downloader

An Android Cordova plugin that allows you to download files using download manager. You can start more than one download and each of them will be presented on the notification bar. You can handle the progress and cancel each download. This plugis handles also download cancellation taken on the notification bar. If you work with `ZIP` archive you can extract it with this plugin also.

## Installation

```bash
cordova plugin add cordova-plugin-files-downloader
```

## Supported Platforms

- Android

## Usage

### download

```js
FilesDownloader.download('https://cordova.apache.org/static/img/cordova_256.png', file, {
    title: 'Downloading...',
    extract: false
}, (result) => {
    // progress
}, (err) => {
    // err
    alert(JSON.stringify(err));
});
```
__Parameters__:

- __remoteUrl__: URL of the file to download

- __destinationFile__: `FileEntry ` object

- __options__: Optional parameters _(Object)_. Valid keys:
  - __title__: The download title in the notification bar
  - __extract__: If true, downloaded `ZIP` archive will be extracted when completed. You receive `finished` status when extraction will be finished.
 
- __successCallback__: A callback with download status and progress. _(Function)_

- __errorCallback__: A callback that executes if an error occurs. _(Function)_

### cancel

```js
FilesDownloader.cancel('https://cordova.apache.org/static/img/cordova_256.png', (result) => {
    // ok
}, (err) => {
    // err
    alert(JSON.stringify(err));
});
```
__Parameters__:

- __remoteUrl__: URL of the file to cancel active downalod

- __successCallback__: A callback with cancellation status. _(Function)_

- __errorCallback__: A callback that executes if an error occurs. _(Function)_