define(function(require) {
  var SotaDispatcher = require('sota-dispatcher'),
      _ = require('underscore'),
      db = require('../stores/db'),
      checkExists = require('../mixins/check-exists'),
      sendRequest = require('../mixins/send-request');

  var createVehicle = function(payload) {
    var url = '/api/v1/devices';
    const device = {
      deviceName: payload.vehicle.deviceName,
      deviceId: payload.vehicle.deviceName,
      deviceType: 'Vehicle'
    }
    sendRequest.doPost(url, device)
      .success(function(vehicles) {
        SotaDispatcher.dispatch({actionType: 'search-vehicles-by-regex'});
      });
  }

  var Handler = (function() {
      this.dispatchCallback = function(payload) {
        switch(payload.actionType) {
          case 'get-vehicles':
            sendRequest.doGet('/api/v1/devices')
              .success(function(vehicles) {
                db.vehicles.reset(vehicles);
              });
          break;
          case 'create-vehicle':
            createVehicle(payload);
          break;
          case 'search-vehicles-by-regex':
            var query = payload.regex ? '&regex=' + payload.regex : '';

            sendRequest.doGet('/api/v1/devices?namespace=default' + query)
              .success(function(vehicles) {
                db.searchableVehicles.reset(vehicles);
              });
          break;
          case 'fetch-affected-vins':
            var affectedVinsUrl = '/api/v1/resolver/resolve?namespace=default' +
            '&package_name=' + payload.name + '&package_version=' + payload.version;

            sendRequest.doGet(affectedVinsUrl)
              .success(function(vehicles) {
                db.affectedVins.reset(vehicles);
              });
          break;
          case 'get-vehicles-for-package':
            sendRequest.doGet('/api/v1/resolver/devices?packageName=' + payload.name + '&packageVersion=' + payload.version)
              .success(function(vehicles) {
                var list = _.map(vehicles, function(vehicle) {
                  return {id: vehicle};
                });
                db.vehiclesForPackage.reset(list);
              });
          break;
          case 'get-package-queue-for-vin':
            sendRequest.doGet('/api/v1/device_updates/' + payload.id)
              .success(function(pendingUpdates) {
                var pkgs = _.map(pendingUpdates, function(pendingUpdate) {
                  return pendingUpdate.packageId
                });
                db.packageQueueForVin.reset(pkgs);
              });
          break;
          case 'get-package-history-for-vin':
            sendRequest.doGet('/api/v1/history?uuid=' + payload.id)
              .success(function(packages) {
                db.packageHistoryForVin.reset(packages);
              });
          break;
          case 'list-components-on-vin':
            sendRequest.doGet('/api/v1/resolver/devices/' + payload.vin + '/component')
              .success(function(components) {
                db.componentsOnVin.reset(components);
              });
          break;
          case 'add-component-to-vin':
            sendRequest.doPut('/api/v1/devices/' + payload.vin + '/components/' + payload.partNumber)
              .success(function() {
                SotaDispatcher.dispatch({actionType: 'list-components-on-vin', vin: payload.vin});
              });
          break;
          case 'sync-packages-for-vin':
            sendRequest.doPost('/api/v1/device_updates/' + payload.id + '/sync');
          break;
          case 'get-operation-results-for-vin':
            sendRequest.doGet('api/v1/device_updates/' + payload.id + '/results')
              .success(function(operationResults) {
                db.operationResultsForVin.reset(operationResults);
              });
          break;
        }
      };
      SotaDispatcher.register(this.dispatchCallback.bind(this));
  });

  return new Handler();

});
