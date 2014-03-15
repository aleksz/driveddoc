define(['angular', 'services', 'directives', 'controllers', 'angular-route', 'angular-resource', 'ui-bootstrap', 'ui-bootstrap-tpls', 'angulartics-google-analytics'], function(angular) {

	'use strict';

	return angular.module('app', [
	                       'app.services', 
	                       'app.directives', 
	                       'app.controllers', 
	                       'ngRoute', 
	                       'ui.bootstrap', 
	                       'ngResource', 
	                       'angulartics.google.analytics'])
	                       .config(['$routeProvider', function ($routeProvider) {
	
		$routeProvider.when('/edit/:id', {
			templateUrl: '/public/partials/editor.html',
			controller: 'EditorCtrl'
		});
	
	}]).config(['$httpProvider', function ($httpProvider) {
	
		$httpProvider.responseInterceptors.push(function($q, $rootScope) {
			//TODO: upgrade Angular and use new API
			return function(promise) {
				return promise.then(function(response) {
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
