var exec = require("cordova/exec");

var StarMgsIO = {
    startDiscovery: function (success, error) {
        exec(success, error, "StarMgsIO", "startDiscovery");
    },
    stopDiscovery: function (success, error) {
        exec(success, error, "StarMgsIO", "stopDiscovery");
    },
    connect: function (id, success, error) {
        exec(success, error, "StarMgsIO", "connect", [id]);
    },
    disconnect: function (success, error) {
        exec(success, error, "StarMgsIO", "disconnect", []);
    },
    isConnected: function (success, error) {
        exec(success, error, "StarMgsIO", "isConnected");
    },
};

module.exports = StarMgsIO;
