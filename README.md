# com.cellules.cordova.audiofrequency

This plugin captures the device's audio input stream and analyses it to return the maximum frequency.

It adds the following `window` event:

* audiofrequency

## Installation

```
cordova plugin add https://github.com/hugodaniel75/cordova-plugin-audiofrequency.git
```

## it is necessary to request permits

```
cordova plugin add cordova-plugin-android-permissions
```

## Supported Platforms

* iOS
* Android

## Example

```javascript

var permissions = cordova.plugins.permissions;

permissions.requestPermission(permissions.RECORD_AUDIO, function( status ){
        if ( status.hasPermission ) {
            window.addEventListener("audiofrequency", onAudiofrequency, false);

            function onAudiofrequency(e) {
                console.log("Frequency: " + e.frequency + " Hz");
            }
        }else {
          console.warn("No :( ");
        }
    });
```
