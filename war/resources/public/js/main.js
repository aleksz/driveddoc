require.config({
	paths: {
        'angular': ['//ajax.googleapis.com/ajax/libs/angularjs/1.2.2/angular.min', 'lib/angular.min'],
        'angular-route': ['//ajax.googleapis.com/ajax/libs/angularjs/1.2.2/angular-route.min', 'lib/angular-route.min'],
        'angular-resource': ['//ajax.googleapis.com/ajax/libs/angularjs/1.2.2/angular-resource.min', 'lib/angular-resource.min'],
        'ui-bootstrap': ['//cdnjs.cloudflare.com/ajax/libs/angular-ui-bootstrap/0.9.0/ui-bootstrap.min', 'lib/ui-bootstrap.min'],
        'ui-bootstrap-tpls': ['//cdnjs.cloudflare.com/ajax/libs/angular-ui-bootstrap/0.9.0/ui-bootstrap-tpls.min', 'lib/ui-bootstrap-tpls.min'],
        'angulartics': ['/public/lib/angulartics/angulartics.min'],
        'angulartics-google-analytics': ['/public/lib/angulartics/angulartics-google-analytics.min']
    },
    shim: {
    	'angular': {
    		exports: 'angular'
    	},
    	'angular-route': {
    		deps: [ 'angular' ]
    	},
    	'angular-resource': {
    		deps: [ 'angular' ]
    	},
    	'angulartics': {
    		deps: [ 'angular' ]
    	},
    	'angulartics-google-analytics': {
    		deps: [ 'angulartics' ]
    	},
    	'ui-bootstrap': {
    		deps: [ 'angular' ]
    	}
    },
    priority: [
   		"angular"
   	]
});

window.name = "NG_DEFER_BOOTSTRAP!";

require(["angular", "app"], function(angular, app) {
	'use strict';
	
	var $html = angular.element(document.getElementsByTagName('html')[0]);

	angular.element().ready(function() {
		angular.resumeBootstrap([app['name']]);
	});
});