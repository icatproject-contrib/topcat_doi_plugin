

(function(){
	'use strict';

    var app = angular.module('topcat');

    app.controller('MyDoisController', function($state, tc, helpers){
        this.facilities = tc.userFacilities();
        if($state.params.facilityName == ''){
          $state.go('home.my-dois', {facilityName: this.facilities[0].config().name});
          return;
        }

        var page = 1;
        var pageSize = 10;
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

        function buildQuery(){
            return [
                "select dataCollection from",
                "DataCollection dataCollection, ",

                //all datacollections with a datafile in an investigation user's investigation
                "dataCollection.dataCollectionDatafiles as dataCollectionDatafile, ",
                "dataCollectionDatafile.datafile as datafile, ",
                "datafile.dataset as datasetViaDatafile, ",
                "datasetViaDatafile.investigation as investigationViaDatafile, ",
                "investigationViaDatafile.investigationUsers as investigationUserViaDatafile, ",
                "investigationUserViaDatafile.user as userViaDatafile, ",

                //all datacollections with a dataset in an investigation user's investigation
                "dataCollection.dataCollectionDatasets as dataCollectionDataset, ",
                "dataCollectionDataset.dataset as dataset, ",
                "dataset.investigation as investigationViaDataset, ",
                "investigationViaDataset.investigationUsers as investigationUserViaDataset, ",
                "investigationUserViaDataset.user as userViaDataset ",

                //where a doi has been issued
                //and the current user is the investigation user
                "where ",
                "dataCollection.doi != null ",
                "and ",
                "(userViaDatafile.name = :user or userViaDataset.name = :user) ",

                "limit ?, ?", (page - 1) * pageSize, pageSize
            ]
        }

        function getPage(){
            return icat.query(buildQuery()).then(function(results){
                console.log('results', results);
            });
        }

        getPage();

    });

})();

