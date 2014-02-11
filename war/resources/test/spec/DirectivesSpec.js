'use strict';

describe('file should', function() {
    var $compile;
    var $rootScope;
    var $scope;
 
	beforeEach(function() {
		angular.mock.module('app')
	});
 
    beforeEach(inject(function(_$compile_, _$rootScope_){
      $compile = _$compile_;
      $rootScope = _$rootScope_;
      $scope = $rootScope.$new();
    }));
    
    it('replace the element with file input', function() {
        var element = $compile("<file ng-model=\"a\"></file>")($scope);
        expect(element.prop("tagName")).toEqual("INPUT");
        expect(element.attr("type")).toEqual("file");
    });
    
    it('be invalid when required but empty', function() {
        var element = $compile("<file ng-model=\"a\" required></file>")($scope);
        $rootScope.$digest();
        expect(element.attr("class")).toContain("ng-invalid");
    });
    
    it('be valid when required and selected', function() {
        var element = $compile("<file ng-model=\"a\" required></file>")($scope);
        $rootScope.$digest();
        //TODO: don't know how to implement it as file input is read-only
//        expect(element.attr("class")).toContain("ng-valid");
    });
});