'use strict';

var EditorState = {
    LOADING: 0,
    CLEAN: 1
};
//
//google.load('picker', '1');

angular.module('app', ['app.services', 'app.directives', 'app.controllers', 'ngRoute', 'ui.bootstrap', 'ngResource', 'angulartics', 'angulartics.google.analytics']).config(['$routeProvider', '$httpProvider', function ($routeProvider, $httpProvider) {

	$routeProvider.when('/edit/:id', {
		templateUrl: '/public/partials/editor.html',
		controller: 'EditorCtrl'
	});

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

}]);
