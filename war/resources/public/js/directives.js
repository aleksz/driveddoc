define(['angular', 'spin'], function(angular, Spinner) {
'use strict';

var module = angular.module('app.directives', []);

module.directive('spinner', function($log) {
	return {
		restrict: 'E',
		replace: true,
		template: '<span/>',
		link: function($scope, element, attrs) {
			
			element
				.css("position", "relative")
				.css("display", "inline-block")
				.css("width", "14px")
				.css("height", "14px")
			
			var opts = {
					  lines: 10, // The number of lines to draw
					  length: 5, // The length of each line
					  width: 2, // The line thickness
					  radius: 2, // The radius of the inner circle
					  trail: 30, // Afterglow percentage
					  shadow: false, // Whether to render a shadow
					};

			
			new Spinner(opts).spin(element[0]);
		}
	}
});

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

                	if (attrs["accept"].indexOf(file.type) == -1) {
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
});