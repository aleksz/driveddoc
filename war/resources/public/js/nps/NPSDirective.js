define(['./module', 'require'], function(nps, require) {
	'use strict';

	nps.directive('nps', function($log) {
		return {
			restrict: 'E',
			replace: true,
			templateUrl: require.toUrl('./nps.html'),
			controller: function($scope) {
			}
		}
	});
});