define(['angular', 'services', 'directives', 'controllers', 'angular-route', 
        'angular-resource', 'ui-bootstrap', 'ui-bootstrap-tpls', 
        'angulartics-google-analytics', 'moment'], function(angular) {

	'use strict';

	return angular.module('app', [
	                       'app.services', 
	                       'app.directives', 
	                       'app.controllers', 
	                       'ngRoute', 
	                       'ui.bootstrap', 
	                       'ngResource', 
	                       'angulartics.google.analytics'
	                       ])
	                       .config(['$routeProvider', function ($routeProvider) {
	
		$routeProvider.when('/edit/:id', {
			templateUrl: '/public/partials/editor.html',
			controller: 'EditorCtrl'
		}).when('/admin', {
			templateUrl: '/public/partials/ocspSignatureForm.html',
			controller: 'AdminController'
		});
	
	}]).config(['$httpProvider', function($httpProvider) {
	
		function convertDateStringsToDates(input) {
		    // Ignore things that aren't objects.
		    if (typeof input !== "object") return input;

		    for (var key in input) {
		        if (!input.hasOwnProperty(key)) continue;

		        var value = input[key];

		        if (typeof value === "object") {
		            convertDateStringsToDates(value);
		        } else if (moment(value, "YYYY-MM-DDTHH:mm:ssZ", true).isValid()) {
		        	input[key] = moment(value).toDate();
		        }
		    }
		}
		
		$httpProvider.responseInterceptors.push(function($q, $rootScope) {
			//TODO: upgrade Angular and use new API
			return function(promise) {
				return promise.then(function(response) {
					convertDateStringsToDates(response.data);
					return response;
				}, function(response) {
					
					if (response.status == 400) {
						$rootScope.$broadcast('error', { message: response.data });
					}
					
					return $q.reject(response);
				});
			}
		});
	
	}]).config(['$analyticsProvider', function ($analyticsProvider) {
		$analyticsProvider.virtualPageviews(false);
	}]);
});
