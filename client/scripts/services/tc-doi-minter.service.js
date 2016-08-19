
(function(){
	'use strict';

    var app = angular.module('topcat');

    app.service('tcDoiMinter', function(tc, helpers){

    	this.create = function(facility){
    		return new DoiMinter(facility);
    	};

    	function DoiMinter(facility){

    		this.makeDataPublic = helpers.overload({
    			'array, array, array, object': function(investigationIds, datasetIds, datafileIds, options){
    				return this.post('makeDataPublic', {
    					icatUrl: facility.config().icatUrl,
    					sessionId: facility.icat().session().sessionId,
    					investigationIds: investigationIds.join(','),
    					datasetIds: datasetIds.join(','),
    					datafileIds: datafileIds.join(',')
    				}, options);
    			},
    			'promise, array, array, array': function(timeout, investigationIds, datasetIds, datafileIds){
    				return this.makeDataPublic(investigationIds, datasetIds, datafileIds, {timeout: timeout});
    			},
    			'array, array, array': function(investigationIds, datasetIds, datafileIds){
    				return this.makeDataPublic(investigationIds, datasetIds, datafileIds, {});
    			}
    		});



    		helpers.generateRestMethods(this, tc.config().topcatUrl + "/topcat_doi_plugin/api/");

    	}

    });

})();