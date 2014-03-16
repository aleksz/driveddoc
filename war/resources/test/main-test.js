require.config({
    paths: {
        'jasmine': 'lib/jasmine-2.0.0/jasmine',
        'jasmine-html': 'lib/jasmine-2.0.0/jasmine-html',
        'jasmine-console': 'lib/jasmine-2.0.0/console',
        'jasmine-boot': 'lib/jasmine-2.0.0/boot',
        'angular-mocks': 'lib/angular/angular-mocks',
        'spec': 'spec'
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
            exports: 'jasmine'
        },
        'jasmine-console': {
        	deps: [ 'jasmine' ],
        	exports: 'jasmine'
        },
        'jasmine-boot': {
        	deps: [ 'jasmine', 'jasmine-html' ],
        	exports: 'jasmine'
        }
    }
});

//Define all of your specs here. These are RequireJS modules.
var specs = [
  'spec/ControllersSpec',
  'spec/DirectivesSpec'
];