'use strict';

(function(){

	var app = angular.module('topcat');

	app.controller('AddCreatorController', function($state, $uibModalInstance, $scope, $q, tc){
		var that = this;
        var facility = tc.facility($state.params.facilityName);

        this.newCreator = "";
        this.candidateCreators = [];
        this.selectedCandidateCreatorPosition = -1;
        var timeout = $q.defer();
        $scope.$on('$destroy', function(){ timeout.resolve(); });


        this.newCreatorKeydown =  function($event){
            if($event.which == 13){
                this.addCreator();
                $event.preventDefault();
            }

            if($event.which == 38){
                this.newCreatorUpKey();
                $event.preventDefault();
            }

            if($event.which == 40){
                this.newCreatorDownKey();
                $event.preventDefault();
            }
        };

        this.newCreatorKeyup = function($event){
            if(!($event.which == 38 || $event.which == 40)){
                this.updateCandidateCreators();
            }
        };

        this.newCreatorUpKey = function(){
            this.selectedCandidateCreatorPosition--;
            if(this.selectedCandidateCreatorPosition < -1) this.selectedCandidateCreatorPosition = -1;
        };

        this.newCreatorDownKey = function(){
            this.selectedCandidateCreatorPosition++;
            if(this.selectedCandidateCreatorPosition > this.candidateCreators.length - 1) this.selectedCandidateCreatorPosition = this.candidateCreators.length - 1;
        };

        this.updateCandidateCreators = function(){
            if(this.newCreator == ""){
                this.candidateCreators = [];
            } else {
                facility.icat().query(timeout.promise, [
                    "select user from User user",
                    "where lower(user.fullName) like concat('%', lower(?), '%')", this.newCreator,
                    "limit 0, 15"
                ]).then(function(users){
                    that.candidateCreators = _.map(users, function(user, position){
                        user.position = position;
                        return user;
                    });
                    that.selectedCandidateCreatorPosition = -1;
                });
            }
        };

        this.addCreator = function(user){
            if(user){
                $scope.makeDataPublicController.creators.push(user.fullName);
            } else if(this.selectedCandidateCreatorPosition > -1){
                $scope.makeDataPublicController.creators.push(this.candidateCreators[this.selectedCandidateCreatorPosition].fullName);
            } else if(this.candidateCreators.length == 1){
                $scope.makeDataPublicController.creators.push(this.candidateCreators[0].fullName);
            }
            $uibModalInstance.dismiss('cancel');
        };

        this.save = function(){
            var userNames = _.map(this.users, function(user){ return user.name; });
            $scope.myMachinesController.machine.share(timeout.promise, userNames).then(function(){
                
            });
        };

    	this.cancel = function() {
            $uibModalInstance.dismiss('cancel');
        };
	});

})();
