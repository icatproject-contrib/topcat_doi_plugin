

(function(){
	'use strict';

    var app = angular.module('topcat');

    app.controller('DoiLandingPageController', function($state, tc){
        var that = this;
        var id = parseInt($state.params.id);

        tc.doiMinter().landingPageInfo(id).then(function(landingPageInfo){
        	that.title = landingPageInfo.title;
        });
    });

})();

