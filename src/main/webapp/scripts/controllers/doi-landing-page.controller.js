

(function(){
	'use strict';

    var app = angular.module('topcat');

    app.controller('DoiLandingPageController', function($state, $uibModal, tc){
        var that = this;
        var facilityName = $state.params.facilityName;
        var entityId = parseInt($state.params.entityId);
        var facility = tc.facility(facilityName);

        facility.doiMinter().landingPageInfo(entityId).then(function(landingPageInfo){
        	that.title = landingPageInfo.title;
        });

        this.download = function(){
        	$uibModal.open({
                templateUrl : facility.doiMinter().pluginUrl() + 'views/doi-landing-page-download.html',
                size : 'md',
                controller: 'DoiLandingPageDownloadController as doiLandingPageDownloadController'
            });
        };

    });

})();

