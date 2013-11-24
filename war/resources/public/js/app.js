'use strict';

var EditorState = {
    LOADING: 0,
    CLEAN: 1
};

google.load('picker', '1');

angular.module('app', ['app.services', 'app.directives', 'ngRoute'])
    .config(['$routeProvider', function ($routeProvider) {
    $routeProvider
        .when('/edit/:id', {
            templateUrl: '/public/partials/editor.html',
            controller: EditorCtrl});
	}]);