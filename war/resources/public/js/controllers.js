define(['angular', 'services'], function(angular) {
'use strict';

var module = angular.module('app.controllers', [])

module.controller('UserCtrl', ['$scope', 'backend', function($scope, backend) {
	$scope.user = null;
	$scope.login = function() {
		backend.user().then(function(response) {
			$scope.user = response.data;
		});
	}
	$scope.login();
} ]);

module.controller('EditorCtrl', ['$scope', '$routeParams', 'editor', 'doc', '$modal', '$log',
                                 function($scope, $routeParams, editor, doc, $modal, $log) {
	$log.info($routeParams);

	$scope.editor = editor;
	$scope.doc = doc;

	editor.load($routeParams.id);
	
	$scope.saveToDrive = function(index) {
		$scope.savingToDrive = $scope.savingToDrive || {};
		$scope.savingToDrive[index] = true;
		
		editor.saveToDrive(index).then(function() {
			$scope.savingToDrive[index] = false;
		});
	}

	$scope.openSignDialog = function() {
		
		var dialog = $modal.open({
			templateUrl : '/public/partials/signDialog.html',
			controller : 'SignatureCtrl',
			backdrop : 'static'
		});
	}
} ]);

module.controller('SignatureCtrl', ['$scope', 'backend', 'doc', '$timeout',
                            		'$log', 'editor', '$rootScope', 'idCard', '$modalInstance', '$analytics',
                            		function($scope, backend, doc, $timeout, $log, editor, 
                            				$rootScope, idCard, $modalInstance, $analytics) {

	$scope.reset = function() {
		$scope.step = 'chooseMethod';
		$scope.signSession = null;
	}

	$scope.reset();

	$scope.cancel = function() {
		$log.info("Cancelling signing");
		$modalInstance.dismiss();
	}

	$scope.close = function() {
		$log.info("Closing signing dialog");
		$modalInstance.close();
	}

	$scope.chooseMobileId = function() {
		$scope.step = 'mobileIdCredentials';
	}

	$scope.chooseIdCard = function() {
		backend.getOCSPSignatureContainer().then(function(response) {
			$log.info(response.data);
			if (response.data) {// TODO fix check
				$scope.startIdCardSigning();
			} else {
				$scope.step = 'ocspCert';
			}
		});
	}

	$scope.startIdCardSigning = function() {

		$scope.step = 'waitingIdCardAuth';
		var chosenIdCardCert;// TODO: don't like it

		idCard.getCertificate().then(function(cert) {
			chosenIdCardCert = cert;
			return backend.prepareSignature(doc.info.id, cert.cert)
		}).then(function(response) {
			return idCard.sign(chosenIdCardCert.id, response.data);
		}).then(function(hash) {
			return backend.finalizeSignature(doc.info.id, hash);
		}).then(function() {
			$scope.close();
			editor.load(doc.info.id, true);
			$analytics.eventTrack('Signed by ID card', {  category: 'sign' })
		}, function(e) {
			if (e instanceof IdCardException) {
				if (e.isError()) {
					$rootScope.$broadcast('error', {
						message : e.message
					});
				}
				$scope.cancel();
			} else {
				throw e;
			}
		});
	}

	$scope.mobileIdForm = {};
	$scope.form = {};
	$scope.signatureContainer = {};

	$scope.storeKey = function() {

		$scope.storingKey = true;
		
		backend.getOCSPUploadUrl().then(function(response) {
			$log.info("Uploading OCSP signature container " + response.data);
			var formData = new FormData();
			formData.append("password", $scope.signatureContainer.pass);
			formData.append("file", $scope.ocspCertFile);
			return backend.uploadOCSPKey(response.data, formData);
		}).then(function() {
			$log.info("OCSP signature container uploaded");
			$scope.startIdCardSigning();
		}).finally(function() {
			$scope.storingKey = false;
		});
	}

	$scope.startSigning = function() {

		$scope.step = 'mobileIdPoll';

		backend.startSigning(doc.info.id, $scope.mobileIdForm.personalId,
				$scope.mobileIdForm.phoneNumber).then(function(response) {

			if ($scope.cancelled) {
				return;
			}

			$scope.signSession = response.data;

			if (isFailedSignSession($scope.signSession)) {
				$rootScope.$broadcast('error', {
					message : 'Wrong signer info'
				})
				$scope.waitingForPin = false;
			} else {
				checkSignatureStatus();
			}
		});
	}

	function isFailedSignSession(signSession) {
		return signSession.sessionId == null
	}

	function checkSignatureStatus() {
		backend.checkSignatureStatus(doc.info.id, $scope.signSession.sessionId)
				.then(function(response) {
					$log.info("Signature status " + response.data);
					if (response.data == 'OUTSTANDING_TRANSACTION') {
						if (!$scope.cancelled) {
							$timeout(checkSignatureStatus, 5000);
						}
					} else if (response.data == 'SIGNATURE') {
						$scope.close();
						editor.load(doc.info.id, true);
					} else if (response.data == 'EXPIRED_TRANSACTION') {
						$scope.close();
						$rootScope.$broadcast('error', {
							message : 'Signing expired'
						})
					} else if (response.data == 'USER_CANCEL') {
						$scope.close();
						$rootScope.$broadcast('error', {
							message : 'Signing cancelled'
						})
					} else {
						$scope.close();
						$rootScope.$broadcast('error', {
							message : 'Signing failed'
						})
					}
				});
	}
} ]);
});