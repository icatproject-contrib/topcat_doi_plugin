
(function(){
	'use strict';

    var app = angular.module('topcat');

    app.service('tcDoiMinter', function(tc, helpers){

    	this.create = function(facility){
    		return new DoiMinter(facility);
    	};

    	function DoiMinter(facility){

    		this.makePublicDataCollection = helpers.overload({
    			'array, array, object': function(datasetIds, datafileIds, options){
    				return this.post('makePublicDataCollection', {
    					icatUrl: facility.config().icatUrl,
    					sessionId: facility.icat().session().sessionId,
    					datasetIds: datasetIds.join(','),
    					datafileIds: datafileIds.join(',')
    				}, options);
    			},
    			'promise, array, array': function(timeout, datasetIds, datafileIds){
    				return this.makePublicDataCollection(datasetIds, datafileIds, {timeout: timeout});
    			},
    			'array, array': function(datasetIds, datafileIds){
    				return this.makePublicDataCollection(datasetIds, datafileIds, {});
    			}
    		});



    		helpers.generateRestMethods(this, tc.config().topcatUrl + "/topcat_doi_plugin/api/");

    	}

    });

})();