'use strict';

describe("SignatureCtrl", function() {

	var idCard;
	var backend;
	var controller;
	var scope;
	var doc;
	var q;
	var modalInstance;
	var editor;
	
	beforeEach(function() {
		angular.mock.module('app')
		idCard = jasmine.createSpyObj('idCard', [ 'getCertificate', 'sign' ]);
		backend = jasmine.createSpyObj('backend', [ 'prepareSignature', 'finalizeSignature' ]);
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
  
  describe("startIdCardSigning should", function() {
  
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
	
	it("change to id card signing step when signing starts", inject(function() {
		idCard.getCertificate.and.returnValue(mockPromise());
		scope.startIdCardSigning();
		expect(scope.step).toEqual('waitingIdCardAuth');
	}));

	it("finalize signature if user enters correct pin", inject(function() {
		idCard.getCertificate.and.returnValue(mockPromise({ cert: "a", id: "1" }));
		idCard.sign.and.returnValue(mockPromise("hashhh"));
		backend.prepareSignature.and.returnValue(mockPromise({ data: { id: "idd", digest: "abafdgad" }}));
		scope.startIdCardSigning();
		resolveMockPromises();
		expect(backend.finalizeSignature).toHaveBeenCalledWith(422, "idd", "hashhh");
	}));
	
	it("reload editor on successful signing", inject(function() {
		idCard.getCertificate.and.returnValue(mockPromise({ cert: "a", id: "1" }));
		idCard.sign.and.returnValue(mockPromise("hashhh"));
		backend.prepareSignature.and.returnValue(mockPromise({ data: { id: "idd", digest: "abafdgad" }}));
		scope.startIdCardSigning();
		resolveMockPromises();
		expect(editor.load).toHaveBeenCalledWith(422, true);
	}));

	it("close dialog on successful signing", inject(function() {
		idCard.getCertificate.and.returnValue(mockPromise({ cert: "a", id: "1" }));
		idCard.sign.and.returnValue(mockPromise("hashhh"));
		backend.prepareSignature.and.returnValue(mockPromise({ data: { id: "idd", digest: "abafdgad" }}));
		scope.startIdCardSigning();
		resolveMockPromises();
		expect(modalInstance.close).toHaveBeenCalled();
	}));
	
	it("cancel signing dialog if user cancels certificate dialog", inject(function() {
		idCard.getCertificate.and.returnValue(mockFailingPromise(new IdCardException(1, "cancelled")));
		scope.startIdCardSigning();
		resolveMockPromises();
		expect(modalInstance.dismiss).toHaveBeenCalled();
	}));
	
	it("cancel signing dialog if certificate retrieval raises error", inject(function() {
		idCard.getCertificate.and.returnValue(mockFailingPromise(new IdCardException(2, "error")));
		scope.startIdCardSigning();
		resolveMockPromises();
		expect(modalInstance.dismiss).toHaveBeenCalled();
	}));
	
	it("broadcast error message if certificate retrieval raises error", inject(function($rootScope) {
		spyOn($rootScope, "$broadcast");
		idCard.getCertificate.and.returnValue(mockFailingPromise(new IdCardException(2, "errorMsg")));
		scope.startIdCardSigning();
		resolveMockPromises();
		expect($rootScope.$broadcast).toHaveBeenCalledWith('error', { 'message': 'errorMsg'});
	}));
	
	it("cancel signing dialog if user cancels pin dialog", inject(function() {
		idCard.getCertificate.and.returnValue(mockPromise({ cert: "a", id: "1" }));
		backend.prepareSignature.and.returnValue(mockPromise({ data: {}}));
		idCard.sign.and.returnValue(mockFailingPromise(new IdCardException(1, "cancelled")));
		scope.startIdCardSigning();
		resolveMockPromises();
		expect(modalInstance.dismiss).toHaveBeenCalled();
	}));
	
	it("cancel signing dialog if pin retrieval raises error", inject(function() {
		idCard.getCertificate.and.returnValue(mockPromise({ cert: "a", id: "1" }));
		backend.prepareSignature.and.returnValue(mockPromise({ data: {}}));
		idCard.sign.and.returnValue(mockFailingPromise(new IdCardException(2, "errorMsg")));
		scope.startIdCardSigning();
		resolveMockPromises();
		expect(modalInstance.dismiss).toHaveBeenCalled();
	}));
	
	it("broadcast error message if pin retrieval raises error", inject(function($rootScope) {
		spyOn($rootScope, "$broadcast");
		idCard.getCertificate.and.returnValue(mockPromise({ cert: "a", id: "1" }));
		backend.prepareSignature.and.returnValue(mockPromise({ data: {}}));
		idCard.sign.and.returnValue(mockFailingPromise(new IdCardException(2, "errorMsg")));
		scope.startIdCardSigning();
		resolveMockPromises();
		expect($rootScope.$broadcast).toHaveBeenCalledWith('error', { 'message': 'errorMsg'});
	}));
  });
});
