

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
        var sortColumns = [];


        var isScroll = true;
        this.isScroll = isScroll;
        var gridOptions = {
            data: [],
            appScopeProvider: this,
            "columnDefs": [
                {
                    "field": "doi",
                    "title": "DOI",
                    "cellTemplate": "<div class='ui-grid-cell-contents'><a ui-sref=\"doi-landing-page({facilityName: grid.appScope.facilityName, entityId: row.entity.id})\">{{row.entity.doi}}</a></div>",
                    "orderByField": "dataCollection.doi"
                },
                {
                    "field": "dataCollectionParameter[entity.type.name == 'title'].stringValue",
                    "title": "Title",
                    "cellTemplate": "<div class='ui-grid-cell-contents'><a ui-sref=\"doi-landing-page({facilityName: grid.appScope.facilityName, entityId: row.entity.id})\">{{row.entity.find(&quot;dataCollectionParameter[entity.type.name == 'title'].stringValue&quot;)[0]}}</a></div>",
                    "orderByField": "titleParameter.stringValue"
                
                },
                {
                    "field": "dataCollectionParameter[entity.type.name == 'releaseDate'].dateTimeValue",
                    "title": "Release Date",
                    "orderByField": "releaseDateParameter.dateTimeValue"
                },
                {
                    "field": "createId",
                    "title": "Created By",
                    "orderByField": "dataCollection.createId"
                },
                {
                    "field": "createTime",
                    "title": "Created Time",
                    "orderByField": "dataCollection.createTime"
                }
            ]
        };
        helpers.setupIcatGridOptions(gridOptions, "dataCollection");
        this.gridOptions = gridOptions;
        var canceler = $q.defer();
        $scope.$on('$destroy', function(){
            canceler.resolve();
        });


        function getPage(){
            return icat.query([
                'select distinct dataCollection from DataCollection dataCollection',

                'left outer join dataCollection.parameters as titleParameter',
                'left outer join dataCollection.parameters as releaseDateParameter',

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
                "titleParameter.type.name = 'title' and",
                "releaseDateParameter.type.name = 'releaseDate' and",
                '(user1.name = :user or user2.name = :user or dataCollection.createId = :user) and ',

                function(){
                    var out = [];

                    //doi
                    if(gridOptions.columnDefs[0].filter && gridOptions.columnDefs[0].filter.term){
                        out.push([
                            "UPPER(dataCollection.doi) like concat('%', ?, '%') and", 
                            gridOptions.columnDefs[0].filter.term.toUpperCase(),
                        ]);
                    }

                    //title
                    if(gridOptions.columnDefs[1].filter && gridOptions.columnDefs[1].filter.term){
                        out.push([
                            "UPPER(titleParameter.stringValue) like concat('%', ?, '%') and", 
                            gridOptions.columnDefs[1].filter.term.toUpperCase()
                        ]);
                    }

                    //release date
                    if(gridOptions.columnDefs[2].filters){
                        var from = gridOptions.columnDefs[2].filters[0].term || '';
                        var to = gridOptions.columnDefs[2].filters[1].term || '';
                        if(from != '' || to != ''){
                            from = helpers.completePartialFromDate(from);
                            to = helpers.completePartialToDate(to);
                            out.push([
                                "releaseDateParameter.dateTimeValue between {ts ?} and {ts ?} and",
                                from.safe(),
                                to.safe()
                            ]);
                        }
                    }

                    //created by
                    if(gridOptions.columnDefs[3].filter && gridOptions.columnDefs[3].filter.term){
                        out.push([
                            "UPPER(dataCollection.createId) like concat('%', ?, '%') and", 
                            gridOptions.columnDefs[3].filter.term.toUpperCase()
                        ]);
                    }

                    if(gridOptions.columnDefs[4].filters){
                        var from = gridOptions.columnDefs[4].filters[0].term || '';
                        var to = gridOptions.columnDefs[4].filters[1].term || '';
                        if(from != '' || to != ''){
                            from = helpers.completePartialFromDate(from);
                            to = helpers.completePartialToDate(to);
                            out.push([
                                "dataCollection.createTime between {ts ?} and {ts ?} and",
                                from.safe(),
                                to.safe()
                            ]);
                        }
                    }

                    out.push('1 = 1');

                    var orderBy = _.map(sortColumns, function(sortColumn){
                        return sortColumn.colDef.orderByField + " " + sortColumn.sort.direction;
                    });

                    if(orderBy.length > 0)out.push("order by ?", orderBy.join(', ').safe());

                    return out;
                },

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

            gridApi.core.on.sortChanged($scope, function(grid, _sortColumns){
                sortColumns = _sortColumns;
                page = 1;
                getPage().then(function(results){
                    updateScroll(results.length);
                    gridOptions.data = results;
                });
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

