

(function(){
	'use strict';

    var app = angular.module('topcat');

    app.controller('MyDoisController', function($state, $q, tc, helpers){
        this.facilities = tc.userFacilities();
        if($state.params.facilityName == ''){
          $state.go('home.my-dois', {facilityName: this.facilities[0].config().name});
          return;
        }

        var page = 1;
        var pageSize = 10;
        var chunk = 1;
        var chunkSize = 100;
        var facilityName = $state.params.facilityName;
        var facility = tc.facility(facilityName);
        var icat = facility.icat();


        this.isScroll = true;
        var gridOptions = _.merge({
            data: [],
            appScopeProvider: this
        }, facility.config().myDois.gridOptions);
        helpers.setupIcatGridOptions(gridOptions, "dataCollection");
        this.gridOptions = gridOptions;

        var resultsBuffer = [];
        var isDuplicate = {};

        function getPage(){
            return getResults().then(function(){
                var out = _.slice(resultsBuffer, (page - 1) * pageSize, pageSize);
                page++;
                return out;
            });
        }   

        function getResults(){
            return getChunks().then(function(isMore){
                if(resultsBuffer.length < page * pageSize && isMore){
                    return getResults();
                } else {
                    return $q.resolve();
                }
            });
        }

        function getChunks(){
            var promises = [];

            var resultCount = 0;

            promises.push(icat.query(buildQuery('dataset', chunk, chunkSize)).then(function(results){
                resultCount += results.length;
                _.each(results, function(result){
                    if(!isDuplicate[result.id]){
                        resultsBuffer.push(result);
                        isDuplicate[result.id] = true;
                    }
                });
            }));

            promises.push(icat.query(buildQuery('datafile', chunk, chunkSize)).then(function(results){
                resultCount += results.length;
                _.each(results, function(result){
                    if(!isDuplicate[result.id]){
                        resultsBuffer.push(result);
                        isDuplicate[result.id] = true;
                    }
                });
            }));

            chunk++;

            return $q.all(promises).then(function(){
                return resultCount > 0;
            });
        }

        function buildQuery(type, page, pageSize){
            var out = [
                "select dataCollection from",
                "DataCollection dataCollection, "
            ];

            if(type == 'dataset'){
                out = _.flatten([out, [
                    "dataCollection.dataCollectionDatasets as dataCollectionDataset, ",
                    "dataCollectionDataset.dataset as dataset, "
                ]]);
            } else {
                out = _.flatten([out, [
                    "dataCollection.dataCollectionDatafiles as dataCollectionDatafile, ",
                    "dataCollectionDatafile.datafile as datafile, ",
                    "datafile.dataset as dataset, "
                ]]);
            }

            return _.flatten([out, [
                "dataset.investigation as investigation, ",
                "investigation.investigationUsers as investigationUser, ",
                "investigationUser.user as user ",
                "where ",
                "dataCollection.doi != null ",
                "and ",
                "user.name = :user ",
                "limit ?, ?", (page - 1) * pageSize, pageSize
            ]]);
        }

        getPage().then(function(results){
            
        });

    });

})();

