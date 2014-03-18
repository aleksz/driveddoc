require.config({
	baseUrl: '/',
	paths: {
        'angular': ['//ajax.googleapis.com/ajax/libs/angularjs/1.2.2/angular.min', 'public/lib/angular.min'],
        'angular-route': ['//ajax.googleapis.com/ajax/libs/angularjs/1.2.2/angular-route.min', 'public/lib/angular-route.min'],
        'angular-resource': ['//ajax.googleapis.com/ajax/libs/angularjs/1.2.2/angular-resource.min', 'public/lib/angular-resource.min'],
        'ui-bootstrap': ['//cdnjs.cloudflare.com/ajax/libs/angular-ui-bootstrap/0.9.0/ui-bootstrap.min', 'public/lib/ui-bootstrap.min'],
        'ui-bootstrap-tpls': ['//cdnjs.cloudflare.com/ajax/libs/angular-ui-bootstrap/0.9.0/ui-bootstrap-tpls.min', 'public/lib/ui-bootstrap-tpls.min'],
        'angulartics': 'public/lib/angulartics/angulartics',
        'angulartics-google-analytics': 'public/lib/angulartics/angular-google-analytics',
        'app': 'public/js/app',
        'controllers': 'public/js/controllers',
        'services': 'public/js/services',
        'directives': 'public/js/directives',
        'id-card': 'public/lib/idCard',
        'boot': 'public/js/boot'
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
    	'ui-bootstrap': {
    		deps: [ 'angular' ]
    	},
    	'ui-bootstrap-tpls': {
    		deps: [ 'ui-bootstrap' ]
    	}
    },
    priority: [
   		"angular"
   	]
});

window.name = "NG_DEFER_BOOTSTRAP!";

require(['boot'], function() {
	console.log('boot loaded')
});