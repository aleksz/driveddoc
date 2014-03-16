require.config({
	baseUrl: '.',
    paths: {
        'jasmine': 'test/lib/jasmine-2.0.0/jasmine',
        'jasmine-html': 'test/lib/jasmine-2.0.0/jasmine-html',
        'jasmine-console': 'test/lib/jasmine-2.0.0/console',
        'jasmine-boot': 'test/lib/jasmine-2.0.0/boot',
        'angular-mocks': 'test/lib/angular/angular-mocks',
        'spec': 'test/spec',
        'boot': 'test/boot'
    },
    shim: {
    	'angular-mocks': {
    		deps: [ 'angular' ]
    	},
    	'jasmine': {
    		exports: 'jasmine'
    	},
        'jasmine-html': {
            deps: [ 'jasmine' ],
            exports: 'jasmine-html'
        },
        'jasmine-console': {
        	deps: [ 'jasmine' ],
        	exports: 'jasmine-console'
        },
        'jasmine-boot': {
        	deps: [ 'jasmine', 'jasmine-html' ],
        	exports: 'jasmine-boot'
        }
    }
});

//Define all of your specs here. These are RequireJS modules.
var specs = [
  'test/spec/ControllersSpec.js',
  'test/spec/DirectivesSpec.js'
];