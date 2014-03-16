define(['angular', 'angular-mocks', 'app'], function(angular) {

	describe("SignatureCtrl", function() {
	
		var idCard;
		var backend;
		var controller;
		var scope;
		var doc;
		var q;
		var modalInstance;
		var editor;
		
		function mockPromise(result) {
			var deferred = q.defer();
			deferred.resolve(result)
			return deferred.promise;
		}
		
		function mockFailingPromise(result) {
			var deferred = q.defer();
			deferred.reject(result)
			return deferred.promise;
		}
		
		function resolveMockPromises() {
			scope.$root.$digest();
		}
		
		beforeEach(function() {
			angular.mock.module('app')
			idCard = jasmine.createSpyObj('idCard', [ 'getCertificate', 'sign' ]);
			backend = jasmine.createSpyObj('backend', [ 'prepareSignature', 'finalizeSignature', 'getOCSPUploadUrl', 'uploadOCSPKey' ]);
			modalInstance = jasmine.createSpyObj('modalInstance', [ 'close', 'dismiss' ]);
			editor = jasmine.createSpyObj('editor', [ 'load' ]);
			doc = {	info: { id: 422 }};
		});
		
		beforeEach(inject(function ($controller, $rootScope, $q) {
			q = $q;
			scope = $rootScope.$new();
	        controller = $controller('SignatureCtrl', { 
	        	$scope: scope, 
	        	$modalInstance: modalInstance, 
	        	idCard: idCard,
	        	backend: backend,
	        	editor: editor,
	        	doc: doc
	        	});
	    }));
	  
		describe("storeKey should", function() {
	
			it("notify that storing is in progress", angular.mock.inject(function() {
				backend.getOCSPUploadUrl.and.returnValue(mockPromise({}));
				scope.storeKey();
				expect(scope.storingKey).toEqual(true);
			}));
			
			it("notify that storing is not in progress after successfull upload", angular.mock.inject(function() {
				spyOn(scope, "startIdCardSigning");
				backend.getOCSPUploadUrl.and.returnValue(mockPromise({}));
				backend.uploadOCSPKey.and.returnValue(mockPromise({}));
				scope.storeKey();
				resolveMockPromises();
				expect(scope.storingKey).toEqual(false);
			}));
			
			it("notify that storing is not in progress after upload failure", angular.mock.inject(function() {
				backend.getOCSPUploadUrl.and.returnValue(mockPromise({}));
				backend.uploadOCSPKey.and.returnValue(mockFailingPromise({}));
				scope.storeKey();
				resolveMockPromises();
				expect(scope.storingKey).toEqual(false);
			}));
		});
		
	  describe("startIdCardSigning should", function() {
	  
		it("change to id card signing step when signing starts", angular.mock.inject(function() {
			idCard.getCertificate.and.returnValue(mockPromise());
			scope.startIdCardSigning();
			expect(scope.step).toEqual('waitingIdCardAuth');
		}));
	
		it("finalize signature if user enters correct pin", angular.mock.inject(function() {
			idCard.getCertificate.and.returnValue(mockPromise({ cert: "a", id: "1" }));
			idCard.sign.and.returnValue(mockPromise("hashhh"));
			backend.prepareSignature.and.returnValue(mockPromise({ data: { id: "idd", digest: "abafdgad" }}));
			scope.startIdCardSigning();
			resolveMockPromises();
			expect(backend.finalizeSignature).toHaveBeenCalledWith(422, "idd", "hashhh");
		}));
		
		it("reload editor on successful signing", angular.mock.inject(function() {
			idCard.getCertificate.and.returnValue(mockPromise({ cert: "a", id: "1" }));
			idCard.sign.and.returnValue(mockPromise("hashhh"));
			backend.prepareSignature.and.returnValue(mockPromise({ data: { id: "idd", digest: "abafdgad" }}));
			scope.startIdCardSigning();
			resolveMockPromises();
			expect(editor.load).toHaveBeenCalledWith(422, true);
		}));
	
		it("close dialog on successful signing", angular.mock.inject(function() {
			idCard.getCertificate.and.returnValue(mockPromise({ cert: "a", id: "1" }));
			idCard.sign.and.returnValue(mockPromise("hashhh"));
			backend.prepareSignature.and.returnValue(mockPromise({ data: { id: "idd", digest: "abafdgad" }}));
			scope.startIdCardSigning();
			resolveMockPromises();
			expect(modalInstance.close).toHaveBeenCalled();
		}));
		
		it("cancel signing dialog if user cancels certificate dialog", angular.mock.inject(function() {
			idCard.getCertificate.and.returnValue(mockFailingPromise(new IdCardException(1, "cancelled")));
			scope.startIdCardSigning();
			resolveMockPromises();
			expect(modalInstance.dismiss).toHaveBeenCalled();
		}));
		
		it("cancel signing dialog if certificate retrieval raises error", angular.mock.inject(function() {
			idCard.getCertificate.and.returnValue(mockFailingPromise(new IdCardException(2, "error")));
			scope.startIdCardSigning();
			resolveMockPromises();
			expect(modalInstance.dismiss).toHaveBeenCalled();
		}));
		
		it("broadcast error message if certificate retrieval raises error", angular.mock.inject(function($rootScope) {
			spyOn($rootScope, "$broadcast").and.callThrough();
			idCard.getCertificate.and.returnValue(mockFailingPromise(new IdCardException(2, "errorMsg")));
			scope.startIdCardSigning();
			resolveMockPromises();
			expect($rootScope.$broadcast).toHaveBeenCalledWith('error', { 'message': 'errorMsg'});
		}));
		
		it("cancel signing dialog if user cancels pin dialog", angular.mock.inject(function() {
			idCard.getCertificate.and.returnValue(mockPromise({ cert: "a", id: "1" }));
			backend.prepareSignature.and.returnValue(mockPromise({ data: {}}));
			idCard.sign.and.returnValue(mockFailingPromise(new IdCardException(1, "cancelled")));
			scope.startIdCardSigning();
			resolveMockPromises();
			expect(modalInstance.dismiss).toHaveBeenCalled();
		}));
		
		it("cancel signing dialog if pin retrieval raises error", angular.mock.inject(function() {
			idCard.getCertificate.and.returnValue(mockPromise({ cert: "a", id: "1" }));
			backend.prepareSignature.and.returnValue(mockPromise({ data: {}}));
			idCard.sign.and.returnValue(mockFailingPromise(new IdCardException(2, "errorMsg")));
			scope.startIdCardSigning();
			resolveMockPromises();
			expect(modalInstance.dismiss).toHaveBeenCalled();
		}));
		
		it("broadcast error message if pin retrieval raises error", angular.mock.inject(function($rootScope) {
			spyOn($rootScope, "$broadcast").and.callThrough();
			idCard.getCertificate.and.returnValue(mockPromise({ cert: "a", id: "1" }));
			backend.prepareSignature.and.returnValue(mockPromise({ data: {}}));
			idCard.sign.and.returnValue(mockFailingPromise(new IdCardException(2, "errorMsg")));
			scope.startIdCardSigning();
			resolveMockPromises();
			expect($rootScope.$broadcast).toHaveBeenCalledWith('error', { 'message': 'errorMsg'});
		}));
	  });
	});
});