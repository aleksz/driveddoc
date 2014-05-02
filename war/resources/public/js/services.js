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
			
			if (!plugin) {
				_this.load();
			}
			
			var deferred = $q.defer();
			
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

module.factory('editor', function (doc, backend, $q, $rootScope, $log, $resource) {
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
                
                return backend.load(id).then(angular.bind(this, function (result) {
                        return result;
                    }), function (errorResponse) {

                    	if (errorResponse.status == 415) {
                    		$log.info("Requested resource was not a DDoc, trying to create new DDoc with requested file inside");
                    		return backend.create(id);
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
//            save:function (newRevision) {
//                $log.info("Saving file", newRevision);
//                if (this.saving || this.loading) {
//                    throw 'Save called from incorrect state';
//                }
//                this.saving = true;
//                var file = this.snapshot();
//
//                // Force revision if first save of the session
//                newRevision = newRevision || doc.timeSinceLastSave() > ONE_HOUR_IN_MS;
//                var promise = backend.save(file, newRevision);
//                promise.then(angular.bind(this,
//                    function (result) {
//                        $log.info("Saved file", result);
//                        this.saving = false;
//                        doc.resource_id = result.data;
//                        doc.lastSave = new Date().getTime();
//                        $rootScope.$broadcast('saved', doc.info);
//                        return doc.info;
//                    }), angular.bind(this,
//                    function (result) {
//                        this.saving = false;
//                        doc.dirty = true;
//                        $rootScope.$broadcast('error', {
//                            action:'save',
//                            message:"An error occured while saving the file"
//                        });
//                        return result;
//                    }));
//                return promise;
//            },
            updateEditor:function (container) {
                $log.info("Updating editor", container);
//                var session = new EditSession(fileInfo.content);
//                session.on('change', function () {
//                    doc.dirty = true;
//                    $rootScope.$apply();
//                });
//                fileInfo.content = null;
//                doc.lastSave = 0;
                doc.info = container;
                doc.resource_id = container.id;
                
//                editor.setSession(session);
//                editor.setReadOnly(!doc.info.editable);
//                editor.focus();
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

module.factory('backend',
    function ($http, $log, $resource) {
	
		var fileResource = $resource('/api/svc');
	
        var jsonTransform = function (data, headers) {
            return angular.fromJson(data);
        };
        var service = {
            user: function () {
                return $http.get('/api/user', {transformResponse:jsonTransform});
            },
            load: function(id) {
            	return fileResource.get({ 'file_id':id }).$promise;
            },
            create: function(originalFile) {
            	return fileResource.save({ 'file_id': originalFile }).$promise;
            }, 
            save:function (fileInfo, newRevision) {
                $log.info('Saving', fileInfo);
                return $http({
                    url:'/api/svc',
                    method:fileInfo.resource_id ? 'PUT' : 'POST',
                    headers:{
                        'Content-Type':'application/json'
                    },
                    params:{
                        'newRevision':newRevision
                    },
                    transformResponse:jsonTransform,
                    data:JSON.stringify(fileInfo)
                });
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