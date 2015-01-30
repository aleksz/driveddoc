define(['angular', 'id-card'], function(angular) {
'use strict';

var module = angular.module('app.services', []);

// Shared model for current document
module.factory('doc', function ($rootScope) {
    return $rootScope.$new(true);
});

module.factory('idCard', function($log, $rootScope, $q, $timeout) {
	
	var plugin;
	
	return {
		load: function() {
			$log.debug("Loading IdCard plugin");
			loadSigningPlugin("eng");
			plugin =  new IdCardPluginHandler("eng");
			$log.debug("IdCard plugin loaded");
		},
		
		getCertificate: function() {
			
			var _this = this;
			var deferred = $q.defer();

			if (!plugin) {
				try {
					_this.load();
				} catch(ex) {
					deferred.reject(ex);
					return deferred.promise;
				}
			}
			
			plugin.getCertificate(function(cert) {
				deferred.resolve(cert);
			}, function(ex) {
				deferred.reject(ex);
			});
			
			return deferred.promise;
		},
		
		sign: function(certId, payloadHash) {
			var deferred = $q.defer();
			
			plugin.sign(certId, payloadHash, function(signature) {
				deferred.resolve(signature);
			}, function(ex) {
				deferred.reject(ex);
			});
			
			return deferred.promise;
		}
	}
});

module.factory('editor', function (doc, backend, $q, $rootScope, $log, $resource, Container) {
        var editor = null;

        var service = {
            loading: false,
            saving: false,
            rebind: function (element) {
            	$log.info("Rebind editor");
            	editor = element;
            },
            saveToDrive: function(index) {
            	$log.info("Saving " + index + " file to GDrive");
            	return $resource("/api/drivefile").save({ 
            		fileIndex: index, 
            		containerFileId: doc.resource_id 
            		}, function() {
            			$log.info("File saved to GDrive");
            		}).$promise;
            },
            load:function (id, reload) {
                $log.info("Loading resource", id, doc.resource_id);
                
                if (!reload && doc.info && id == doc.resource_id) {
                    return $q.when(doc.info);
                }
                
                this.loading = true;
                
                return Container.get({ 'fileId': id }).$promise.then(angular.bind(this, function (result) {
                        return result;
                    }), function (errorResponse) {

                    	if (errorResponse.status == 415) {
                    		$log.info("Requested resource was not a DDoc, trying to create new DDoc with requested file inside");
                    		return Container.save({ 'fileId': id }).$promise;
                    	}

                    	return errorResponse;
                    	
                    }).then(angular.bind(this, function(result) {
                        this.updateEditor(result);
                        $rootScope.$broadcast('loaded', doc.info);
                        return result;
                    }), function(errorResponse) {
                    	$log.warn("Error loading", errorResponse);
                        $rootScope.$broadcast('error', {
                            message:"An error occured while loading the file"
                        });
                    }).finally(angular.bind(this, function() {
                    	this.loading = false;
                    }));
            },
            updateEditor:function (container) {
                $log.info("Updating editor", container);
                doc.info = container;
                doc.resource_id = container.id;
            },
            state:function () {
                if (this.loading) {
                    return EditorState.LOADING;
                }
                
                return EditorState.CLEAN;
            }
        };
        return service;
    });

module.factory('Container', function ($resource) {
	return $resource('/api/containers/:fileId', { fileId: '@fileId'});
});

module.factory('backend',
    function ($http, $log, $resource) {
	
        var jsonTransform = function (data, headers) {
            return angular.fromJson(data);
        };
        var service = {
            user: function () {
                return $http.get('/api/user', {transformResponse:jsonTransform});
            },
            startSigning: function(fileId, personalId, phoneNumber) {
            	return $http({
            		url: '/api/sign',
            		method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
            		params: { 
            			'file_id': fileId,
            			'personalId': personalId,
            			'phoneNumber': phoneNumber
            		},
            		transformResponse:jsonTransform
            	});
            },
            checkSignatureStatus: function(fileId, sessionId) {
            	return $http({
            		url: '/api/sign',
            		method: 'GET',
                    headers: { 'Content-Type': 'application/json' },
            		params: { 
            			'file_id': fileId,
            			'sessionId': sessionId
            		},
            		transformResponse:jsonTransform
            	});
            },
            prepareSignature: function(fileId, cert) {
            	return $http({
            		url: '/api/signatures',
            		method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
            		data: { 
            			'fileId': fileId,
            			'cert': cert
            		},
            		transformResponse:jsonTransform
            	});
            },
            finalizeSignature: function(fileId, signature) {
            	return $http({
            		url: '/api/sign/id',
            		method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
            		params: { 
            			'file_id': fileId,
            			'signature': signature
            		},
            		transformResponse:jsonTransform
            	});
            },
            getOCSPUploadUrl: function() {
            	return $http({
            		url: '/api/OCSPSignatureContainerUploadURL',
            		method: 'GET',
            		transformResponse:jsonTransform
            	});
            },
            uploadOCSPKey: function(uploadUrl, key) {
            	return $http.post(uploadUrl, key, {
					headers: { 'Content-Type': undefined },
					transformRequest: function(data) { return data; }
        		});
            },
            getOCSPSignatureContainer: function() {
            	return $http.get('/api/OCSPSignatureContainer', {
					transformResponse:jsonTransform
        		});
            }
        };
        return service;
    });
});