

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
        var pageSize = 50;
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
                    "title": "DOI",
                    "cellTemplate": "<div class='ui-grid-cell-contents'><a ui-sref=\"doi-landing-page({facilityName: grid.appScope.facilityName, entityId: row.entity.id})\">{{row.entity.doi}}</a></div>"
                },
                {
                    "field": "dataCollectionParameter[entity.type.name == 'title'].stringValue",
                    "title": "Title",
                    "cellTemplate": "<div class='ui-grid-cell-contents'><a ui-sref=\"doi-landing-page({facilityName: grid.appScope.facilityName, entityId: row.entity.id})\">{{row.entity.find(&quot;dataCollectionParameter[entity.type.name == 'title'].stringValue&quot;)[0]}}</a></div>"
                },
                {
                    "field": "dataCollectionParameter[entity.type.name == 'releaseDate'].dateTimeValue",
                    "title": "Release Date"
                },
                {
                    "field": "createId",
                    "title": "Created By"
                },
                {
                    "field": "createTime",
                    "title": "Created Time"
                }
            ]
        };
        helpers.setupIcatGridOptions(gridOptions, "dataCollection");
        this.gridOptions = gridOptions;


        function getPage(){
            return icat.query([
                'select distinct dataCollection from DataCollection dataCollection',

                ', dataCollection.parameters as parameter',

                'left outer join dataCollection.dataCollectionDatafiles dataCollectionDatafile',
                'left outer join dataCollectionDatafile.datafile datafile',
                'left outer join datafile.dataset dataset1',
                'left outer join dataset1.investigation investigation1',
                'left outer join investigation1.investigationUsers investigationUser1',
                'left outer join investigationUser1.user user1',

                'left outer join dataCollection.dataCollectionDatasets dataCollectionDataset',
                'left outer join dataCollectionDataset.dataset dataset2',
                'left outer join dataset2.investigation investigation2',
                'left outer join investigation2.investigationUsers investigationUser2',
                'left outer join investigationUser2.user user2',

                'where dataCollection.doi != null and',
                '(user1.name = :user or user2.name = :user)',

                //"parameter.type.name = 'title' and parameter.stringValue like concat('%', ?, '%')", 're',

                'limit ?, ?', (page - 1) * pageSize, pageSize,

                'include dataCollection.parameters.type'
            ]);

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

            gridApi.core.on.filterChanged($scope, function(){
                canceler.resolve();
                canceler = $q.defer();
                page = 1;
                gridOptions.data = [];
                getPage().then(function(results){
                    gridOptions.data = results;
                    updateScroll(results.length);
                });
            });

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

