'use strict';

var module = angular.module('app.directives', []);

module.directive('file', function($rootScope, $log) {
	return {
		restrict: 'E',
		replace: true,
		require:'ngModel',
		template: '<input type="file"/>',
		link: function($scope, element, attrs, ngModel) {
			element.bind('change', function(event){
                var files = event.target.files;
                var file = files[0];

                $scope.$apply(function($scope) {
                	ngModel.$setViewValue(element.val());

                	if (file.type != attrs["accept"]) {
                		ngModel.$setValidity('accept', false);
                	} else {
                		ngModel.$setValidity('accept', true);
                		$scope.$parent[attrs["id"]] = file;
                	}
                });
            });
		}
	}
});

module.directive('idCard', function($rootScope, $log) {
	return {
		restrict: 'E',
		replace: true,
		template: '<DIV id="pluginLocation"></DIV>',
		link: function($scope, element, attrs) {
		}
	}
});

module.directive('editor', function(editor) {
    return {
        retrict: 'A',
        link: function (scope, element) {
            editor.rebind(element[0]);
        }
    };
});

module.directive('myalert', function ($rootScope) {
        return {
            restrict:'E',
            replace:true,
            link:function (scope, element) {
            	element.hide();
                $rootScope.$on('error',
                    function (event, data) {
                        scope.message = data.message;
                        element.show();
                    });
            },
            template:'<div class="alert alert-danger">' +
                '  <button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>' +
                '  {{message}}' +
                '</div>'
        }
    })
