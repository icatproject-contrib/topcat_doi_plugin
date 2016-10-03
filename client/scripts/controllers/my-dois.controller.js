

(function(){
	'use strict';

    var app = angular.module('topcat');

    app.controller('MyDoisController', function($state, $q, $scope, $timeout, tc, helpers){
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
        this.facilityName = facilityName;
        var facility = tc.facility(facilityName);
        var icat = facility.icat();
        var gridApi;

        var isScroll = true;
        this.isScroll = isScroll;
        var gridOptions = {
            data: [],
            appScopeProvider: this,
            "columnDefs": [
                {
                    "field": "doi",
                    "title": "DOI"
                },
                {
                    "field": "dataCollectionParameter[entity.type.name == 'title'].stringValue",
                    "title": "Title",
                    "cellTemplate": "<div class='ui-grid-cell-contents'><a ui-sref=\"doi-landing-page({id: row.entity.id})\">{{row.entity.find(&quot;dataCollectionParameter[entity.type.name == 'title'].stringValue&quot;)[0]}}</a></div>"
                },
                {
                    "field": "dataCollectionParameter[entity.type.name == 'releaseDate'].dateTimeValue",
                    "title": "Release Date"
                }
            ]
        };
        helpers.setupIcatGridOptions(gridOptions, "dataCollection");
        this.gridOptions = gridOptions;

        var resultsBuffer = [];
        var isDuplicate = {};

        function getPage(){
            return getResults().then(function(){
                var offset = (page - 1) * pageSize;
                return _.slice(resultsBuffer, offset, offset + pageSize);
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
                "limit ?, ?", (page - 1) * pageSize, pageSize,
                "include dataCollection.parameters.type"
            ]]);
        }

        function updateScroll(resultCount){
            if(isScroll){
                $timeout(function(){
                    var isMore = resultCount == pageSize;
                    if(page == 1) gridApi.infiniteScroll.resetScroll(false, isMore);
                    gridApi.infiniteScroll.dataLoaded(false, isMore);
                });
            }
        }


        gridOptions.onRegisterApi = function(_gridApi) {
            gridApi = _gridApi;

            getPage().then(function(results){
                gridOptions.data = results;
                updateScroll(results.length);
            });

            // gridApi.core.on.filterChanged($scope, function(){
            //     canceler.resolve();
            //     canceler = $q.defer();
            //     page = 1;
            //     gridOptions.data = [];
            //     getPage().then(function(results){
            //         gridOptions.data = results;
            //         updateSelections();
            //         updateScroll(results.length);
            //         updateTotalItems();
            //         saveState();
            //     });
            // });

            if(isScroll){
                //scroll down more data callback (append data)
                gridApi.infiniteScroll.on.needLoadMoreData($scope, function() {
                    page++;
                    getPage().then(function(results){
                        _.each(results, function(result){ gridOptions.data.push(result); });
                        if(results.length == 0) page--;
                        updateScroll(results.length);
                    });
                });

                //scoll up more data at top callback (prepend data)
                gridApi.infiniteScroll.on.needLoadMoreDataTop($scope, function() {
                    page--;
                    getPage().then(function(results){
                        _.each(results.reverse(), function(result){ gridOptions.data.unshift(result); });
                        if(results.length == 0) page++;
                        updateScroll(results.length);
                    });
                });
            } else {
                //pagination callback
                gridApi.pagination.on.paginationChanged($scope, function(_page, _pageSize) {
                    page = _page;
                    pageSize = _pageSize;
                    getPage().then(function(results){
                        gridOptions.data = results;
                    });
                });
            }

        };

    });

})();

