'use strict';

function UserCtrl($scope, backend) {
    $scope.user = null;
    $scope.login = function () {
        backend.user().then(function (response) {
            $scope.user = response.data;
        });
    }
    $scope.login();
}

function EditorCtrl($scope, $location, $routeParams, $timeout, editor, doc) {
    console.log($routeParams);
    $scope.editor = editor;
    $scope.doc = doc;
    editor.load($routeParams.id);
}