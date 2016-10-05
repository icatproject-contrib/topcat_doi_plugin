

(function(){
	'use strict';

    var app = angular.module('topcat');

    app.controller('DoiLandingPageDownloadController', function($state, $uibModalInstance, tc){
        var that = this;

        var facilityName = $state.params.facilityName;
        var entityId = parseInt($state.params.entityId);
        var facility = tc.facility(facilityName);

        var date = new Date();
        var year = date.getFullYear();
        var month = date.getMonth() + 1;
        var day = date.getDate();
        if(day < 10) day = '0' + day;
        var hour = date.getHours();
        if(hour < 10) hour = '0' + hour;
        var minute = date.getMinutes();
        if(minute < 10) minute = '0' + minute;
        var second = date.getSeconds();
        if(second < 10) second = '0' + second;
        this.fileName = facility.config().name + "_" + year + "-" + month + "-" + day + "_" + hour + "-" + minute + "-" + second;

        facility.doiMinter().getStatus(entityId).then(function(status){
        	that.status = status;
        });

        this.next = function(){

        };

        this.cancel = function() {
            $uibModalInstance.dismiss('cancel');
        };

        this.isNextDisabled = function(){
        	return !(this.status !== undefined && this.fileName != '' && (this.status == 'ONLINE' || (this.email && this.email.match(/^[^@\s]+@[^@\s]+$/) && this.fileName)));
        };
        
    });

})();

