# Cordova Plugin for Star Micronics Bluetooth Scales

This is a Cordova plugin for interacting with Star Micronics Bluetooth scales. It provides a JavaScript API for starting and stopping device discovery, connecting and disconnecting from devices, and checking the connection status.

## Installation

To install this plugin, use the Cordova CLI:

```sh
cordova plugin add cordova-plugin-starmgsio
```

## Usage

The plugin provides the following methods:

### `startDiscovery(success, error)`
Starts the discovery of Star Micronics Bluetooth scales. The success callback is called with the discovered devices.
Example:
```javascript
StarMgsIO.startDiscovery(function(devices) {
    console.log("Discovered devices: ", devices);
}, function(error) {
    console.error("Error during discovery: ", error);
});
```

### `stopDiscovery(success, error)`
Stops the ongoing discovery process.
Example:
```javascript
StarMgsIO.stopDiscovery(function() {
    console.log("Stopped discovery");
}, function(error) {
    console.error("Error stopping discovery: ", error);
});
```

### `connect(id, success, error)`
Connects to the device with the given `id`. The `success` callback is called with updates about the connection status, disconnection, and weight updates.

Example:
```javascript
StarMgsIO.connect(deviceId, function(update) {
    if(update.update_type === 'connection_update') {
        console.log("Connection update: ", update);
    } else if(update.update_type === 'disconnection_update') {
        console.log("Disconnection update: ", update);
    } else if(update.update_type === 'weight_update') {
        console.log("Weight update: ", update);
    }
}, function(error) {
    console.error("Error connecting to device: ", error);
});
```

### `disconnect(success, error)`
Disconnects from the currently connected device.

Example:
```javascript
StarMgsIO.disconnect(function() {
    console.log("Disconnected from device");
}, function(error) {
    console.error("Error disconnecting from device: ", error);
});
```

### `isConnected(success, error)`
Checks if the device is connected. The success callback is called with a boolean indicating the connection status.

Example:
```javascript
StarMgsIO.isConnected(function(isConnected) {
    console.log("Is connected: ", isConnected);
}, function(error) {
    console.error("Error checking connection status: ", error);
});
```


## Contributing
Contributions are welcome! Please read the contributing guidelines before getting started.

## License
This project is licensed under the Apache License - see the LICENSE file for details.