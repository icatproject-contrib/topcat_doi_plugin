

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

        facility.ids().isTwoLevel().then(function(isTwoLevel){
            that.isTwoLevel = isTwoLevel;
        });


        this.next = function(){
        	if(this.email){
        		facility.doiMinter().prepareData(entityId, this.fileName, this.email).then(function(){
        			alert("As soon as the data becomes available you'll emailed a download link.");
        			$uibModalInstance.dismiss();
        		});	
        	} else {
        		facility.doiMinter().prepareData(entityId, this.fileName).then(function(result){
        			$uibModalInstance.dismiss();
        			$(document.body).append($('<iframe>').attr({
                        src: result.downloadUrl
                    }).css({
                        position: 'relative',
                        left: '-1000000px',
                        height: '1px',
                        width: '1px'
                    }));
    			});
        	}
        };

        this.cancel = function() {
            $uibModalInstance.dismiss('cancel');
        };

        this.isNextDisabled = function(){
            if(this.isTwoLevel){
                return this.fileName == '' || !this.email || !this.email.match(/^[^@\s]+@[^@\s]+$/)
            } else {
                return this.fileName == '';
            }
        };
        
    });

})();

