require(["angular", "app"], function(angular, app) {
	'use strict';
	
	var $html = angular.element(document.getElementsByTagName('html')[0]);

	angular.element().ready(function() {
		angular.resumeBootstrap([app['name']]);
	});
});