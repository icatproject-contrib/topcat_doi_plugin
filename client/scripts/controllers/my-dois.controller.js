

(function(){
	'use strict';

    var app = angular.module('topcat');

    app.controller('MyDoisController', function(tc){
       
        var page = 1;

        function getPage(){

        }

    });

})();

//show any DataCollections with a DOI, with datafiles or datacollections inside of investigations you belong to

//"select dataCollection from DataCollection dataCollection, dataCollection.dataCollectionDatafiles as dataCollectionDatafile, dataCollectionDatafile.datafile as datafile, datafile.dataset as datasetViaDatafile, datasetViaDatafile.investigation as investigationViaDatafile, investigationViaDatafile.investigationUsers as investigationUserViaDatafile, investigationUserViaDatafile.user as userViaDatafile, dataCollection.dataCollectionDatasets as dataCollectionDataset, dataCollectionDataset.dataset as dataset, dataset.investigation as investigationViaDataset, investigationViaDataset.investigationUsers as investigationUserViaDataset, investigationUserViaDataset.user as userViaDataset where dataCollection.doi != null and (userViaDatafile.name = :user or userViaDataset.name = :user)"