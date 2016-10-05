
(function(){
	'use strict';

    var app = angular.module('topcat');

    app.service('tcDoiMinter', function(tc, helpers){

    	this.create = function(pluginUrl, facility){
    		return new DoiMinter(pluginUrl, facility);
    	};

    	function DoiMinter(pluginUrl, facility){

            this.pluginUrl = function(){
                return pluginUrl;
            };

    		this.makePublicDataCollection = helpers.overload({
    			'string, string, array, array, object': function(title, date, datasetIds, datafileIds, options){
    				return this.post('makePublicDataCollection', {
    					icatUrl: facility.config().icatUrl,
    					sessionId: facility.icat().session().sessionId,
                        title: title,
                        date: date,
    					datasetIds: datasetIds.join(','),
    					datafileIds: datafileIds.join(',')
    				}, options);
    			},
    			'promise, string, string, array, array': function(timeout, title, date, datasetIds, datafileIds){
    				return this.makePublicDataCollection(title, date, datasetIds, datafileIds, {timeout: timeout});
    			},
    			'string, string, array, array': function(title, date, datasetIds, datafileIds){
    				return this.makePublicDataCollection(title, date, datasetIds, datafileIds, {});
    			}
    		});

            this.makeEntityPublic = helpers.overload({
                'string, number, object': function(entityType, entityId, options){
                    return this.post('makeEntityPublic', {
                        icatUrl: facility.config().icatUrl,
                        sessionId: facility.icat().session().sessionId,
                        entityType: entityType,
                        entityId: entityId
                    }, options);
                },
                'promise, string, number': function(timeout, entityType, entityId){
                    return this.makeEntityPublic(entityType, entityId, {timeout: timeout});
                },
                'string, number': function(entityType, entityId){
                    return this.makeEntityPublic(entityType, entityId, {});
                }
            });

            this.landingPageInfo = helpers.overload({
                'number, object': function(id, options){
                    return this.get('landingPageInfo/' + id, {}, options);
                },
                'promise, number': function(timeout, id){
                    return this.landingPageInfo(id, {timeout: timeout});
                },
                'number': function(id){
                    return this.landingPageInfo(id, {});
                }
            });

            this.getStatus = helpers.overload({
                'number, object': function(id, options){
                    return this.get('status/' + id, {}, options);
                },
                'promise, number': function(timeout, id){
                    return this.getStatus(id, {timeout: timeout});
                },
                'number': function(id){
                    return this.getStatus(id, {});
                }
            });

            this.prepareData = helpers.overload({
                'number, string, string, object': function(id, fileName, email, options){
                    return this.post('prepareData/' + id, {fileName: fileName, email: email}, options);
                },
                'promise, number, string, string': function(timeout, id,  email){
                    return this.prepareData(id, fileName, email, {timeout: timeout});
                },
                'number, string, string': function(id, fileName, email){
                    return this.prepareData(id, fileName, email, {});
                },
                'number, string, object': function(id, fileName, options){
                    return this.prepareData(id, fileName, "", {timeout: timeout});
                },
                'promise, number, string': function(timeout, id, fileName){
                    return this.prepareData(id, fileName, "", {timeout: timeout});
                },
                'number, string': function(id, fileName){
                    return this.prepareData(id, fileName, "", {});
                }
            });

    		helpers.generateRestMethods(this, tc.config().topcatUrl + "/topcat_doi_plugin/api/");

    	}

    });

})();