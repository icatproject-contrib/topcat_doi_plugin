
(function(){
	'use strict';

    var app = angular.module('topcat');

    app.service('tcDoiMinter', function(tc, helpers, APP_CONFIG){

    	this.create = function(pluginUrl, facility){
    		return new DoiMinter(pluginUrl, facility);
    	};

    	function DoiMinter(pluginUrl, facility){

            this.pluginUrl = function(){
                return pluginUrl;
            };

            this.config = function(){ 
              return APP_CONFIG.doi;
            };

    		this.makePublicDataCollection = helpers.overload({
    			'string, string, array, object, string, string, array, array, object': function(title, description, creators, releaseDate, licenceName, licenceUrl, datasetIds, datafileIds, options){
    				return this.post('makePublicDataCollection', {
    					json: JSON.stringify({
                            icatUrl: facility.config().icatUrl,
                            sessionId: facility.icat().session().sessionId,
                            title: title,
                            description: description,
                            creators: creators,
                            releaseDate: releaseDate.toISOString().replace(/^(\d\d\d\d-\d\d+-\d\d+)T(\d\d:\d\d:\d\d).*$/, '$1 $2'),
                            licenceName: licenceName,
                            licenceUrl: licenceUrl,
                            datasetIds: datasetIds,
                            datafileIds: datafileIds,
                            publisher: this.config().publisher
                        })
    				}, options);
    			},
    			'promise, string, string, array, object, string, string, array, array': function(timeout, title, description, creators, releaseDate, datasetIds, datafileIds){
    				return this.makePublicDataCollection(title, description, creators, releaseDate, licenceName, licenceUrl, datasetIds, datafileIds, {timeout: timeout});
    			},
    			'string, string, array, object, string, string, array, array': function(title, description, creators, releaseDate, licenceName, licenceUrl, datasetIds, datafileIds){
    				return this.makePublicDataCollection(title, description, creators, releaseDate, licenceName, licenceUrl, datasetIds, datafileIds, {});
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

            this.metadata = helpers.overload({
                'number, object': function(id, options){
                    return this.get('metadata/' + id, {}, options);
                },
                'promise, number': function(timeout, id){
                    return this.metadata(id, {timeout: timeout});
                },
                'number': function(id){
                    return this.metadata(id, {});
                }
            });

            this.getUsers = helpers.overload({
                'number, object': function(id, options){
                    return this.get('users/' + id, {}, options);
                },
                'promise, number': function(timeout, id){
                    return this.getUsers(id, {timeout: timeout});
                },
                'number': function(id){
                    return this.getUsers(id, {});
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