'use strict';

var module = angular.module('app.directives', []);

module.directive('editor', function (editor) {
    return {
        retrict: 'A',
        link: function (scope, element) {
            editor.rebind(element[0]);
        }
    };
});

module.directive('alert',
    function ($rootScope) {
        return {
            restrict:'E',
            replace:true,
            link:function (scope, element) {
            	element.hide();
                $rootScope.$on('error',
                    function (event, data) {
                        scope.message = data.message;
                        element.show();
                    });
            },
            template:'<div class="alert alert-danger">' +
                '  <button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>' +
                '  {{message}}' +
                '</div>'
        }
    })