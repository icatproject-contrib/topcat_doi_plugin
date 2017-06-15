

(function(){
	'use strict';

    var app = angular.module('topcat');

    app.controller('MakeDataPublicController', function($uibModalInstance, $uibModalStack, $timeout, $q, tc, inform, datafileIds){
        
        if(tc.userFacilities().length > 1){
            alert("This feature can't be used with multiple facilities.");
            $timeout(function(){
                $uibModalInstance.dismiss();
            });
            return;
        }

        var that = this;
        var facility = tc.userFacilities()[0];
        var icat = facility.icat();
        var user = facility.user();

    	this.state = 'basic_details';
        this.title = "";
    	this.isReleaseDate = false;
        this.isReleaseDateOptions = [
            {
                label: "Now - I want to release the data straight away",
                value: false
            },
            {
                label: "In the future - I want to delay the data release",
                value: true
            }
        ];
    	this.releaseDate = null;
    	this.isReleaseDateOpen = false;
    	this.licence = null;
    	this.hasAcceptedLegal = false;
    	this.dateFormat = 'yyyy-MM-dd';
    	this.licences = facility.doiMinter().config().licences;
        this.termsAndConditions = "line 1\n line 2\nline 3\nline 4\nline 5\nline 6\nline 7\nline 8\nline 9\n"
        this.password = "";
        this.creators = [];
        this.loaded = false;
        this.newCreator = "";
        this.description = "";
        this.isFromCart = false;

        this.creativeCommonsAllowDerivatives = "yes";
        this.creativeCommonsCommercial = "yes";
        this.creativeCommonsLicenceName = "";
        this.creativeCommonsLicenceUrl = "";

        this.updateCreativeCommonsLicence = function(){
            if(this.creativeCommonsCommercial == 'yes'){
                if(this.creativeCommonsAllowDerivatives == 'yes'){
                    this.creativeCommonsLicenceName = "Attribution 4.0 International";
                    this.creativeCommonsLicenceUrl = "http://creativecommons.org/licenses/by/4.0/";
                } else if(this.creativeCommonsAllowDerivatives == 'no'){
                    this.creativeCommonsLicenceName = "Attribution-NoDerivatives 4.0 International";
                    this.creativeCommonsLicenceUrl = "http://creativecommons.org/licenses/by-nd/4.0/";
                } else {
                    this.creativeCommonsLicenceName = "Attribution-ShareAlike 4.0 International";
                    this.creativeCommonsLicenceUrl = "http://creativecommons.org/licenses/by-sa/4.0/";
                }
            } else {
                if(this.creativeCommonsAllowDerivatives == 'yes'){
                    this.creativeCommonsLicenceName = "Attribution-NonCommercial 4.0 International";
                    this.creativeCommonsLicenceUrl = "http://creativecommons.org/licenses/by-nc/4.0/";
                } else if(this.creativeCommonsAllowDerivatives == 'no'){
                    this.creativeCommonsLicenceName = "Attribution-NonCommercial-NoDerivatives 4.0 International";
                    this.creativeCommonsLicenceUrl = "http://creativecommons.org/licenses/by-nc-nd/4.0/";
                } else {
                    this.creativeCommonsLicenceName = "Attribution-NonCommercial-ShareAlike 4.0 International";
                    this.creativeCommonsLicenceUrl = "http://creativecommons.org/licenses/by-nc-sa/4.0/";
                }
            }

            this.hasAcceptedLegal = false;
        }; 

        this.updateCreativeCommonsLicence();

        var datasetIds = [];
        if(datafileIds.length == 0){
            this.isFromCart = true

            user.cart().then(function(cart){
                _.each(cart.cartItems,  function(cartItem){
                    if(cartItem.entityType == 'dataset') datasetIds.push(cartItem.entityId);
                    if(cartItem.entityType == 'datafile') datafileIds.push(cartItem.entityId);
                });
                populateMetadata();
            });
        } else {
            populateMetadata();
        }

        function populateMetadata(){
            var promises = [];
            var investigations = [];

            _.each(_.chunk(datasetIds, 100), function(datasetIds){
                promises.push(icat.query(["select user from User user, user.investigationUsers as investigationUser, investigationUser.investigation as investigation, investigation.datasets as dataset where dataset.id in (?)", datasetIds.join(', ').safe()]).then(function(users){
                    _.each(users, function(user){
                        if(!_.includes(that.creators, user.fullName)){
                            that.creators.push(user.fullName);
                        }
                    });
                }));

                promises.push(icat.query(["select investigation from Investigation investigation, investigation.datasets dataset where dataset.id in (?)", datasetIds.join(', ').safe()]).then(function(currentInvestigations){
                    _.each(currentInvestigations, function(investigation){
                        investigations.push(investigation);
                    });
                }));
            });

            _.each(_.chunk(datafileIds, 100), function(datafileIds){
                promises.push(icat.query(["select user from User user, user.investigationUsers as investigationUser, investigationUser.investigation as investigation, investigation.datasets as dataset, dataset.datafiles as datafile where datafile.id in (?)", datafileIds.join(', ').safe()]).then(function(users){
                    _.each(users, function(user){
                        if(!_.includes(that.creators, user.fullName)){
                            that.creators.push(user.fullName);
                        }
                    });
                }));

                promises.push(icat.query(["select investigation from Investigation investigation, investigation.datasets dataset, dataset.datafiles datafile where datafile.id in (?)", datafileIds.join(', ').safe()]).then(function(currentInvestigations){
                    _.each(currentInvestigations, function(investigation){
                        investigations.push(investigation);
                    });
                }));
            });

            

            $q.all(promises).then(function(){
                if(investigations.length > 0){
                    that.title = investigations[0].title;
                    that.description = investigations[0].summary;
                }
                that.loaded = true;
            });
        }

        this.moveCreatorUp = function(creator){
            var position = _.indexOf(this.creators, creator);
            var newPosition = position - 1;
            if(newPosition >= 0){
                this.creators[position] = this.creators[newPosition];
                this.creators[newPosition] = creator;
            }
        };

        this.moveCreatorDown = function(creator){
            var position = _.indexOf(this.creators, creator);
            var newPosition = position + 1;
            if(newPosition < this.creators.length){
                this.creators[position] = this.creators[newPosition];
                this.creators[newPosition] = creator;
            }
        };

        this.deleteCreator = function(creator){
            this.creators = _.select(this.creators, function(currentCreator){
                return currentCreator != creator;
            });
        };

        this.addCreator = function(){
            if(this.newCreator != "" && !_.includes(this.creators, this.newCreator)){
                this.creators.push(this.newCreator);
                this.newCreator = "";
            }
        };
        
    	this.isPreviousDisabled = function(){
    		return this.state == 'basic_details';
    	};

    	this.previous = function(){
    		if(this.state == 'confirm'){
    			this.state = 'legal';
    			this.password = '';
    		} else if(this.state == 'legal'){
    			this.state = 'basic_details';
    		}
    	};

    	this.isNextDisabled = function(){
    		if(this.state == 'basic_details' && (!this.isReleaseDate || this.releaseDate != null) && this.title != '' && this.description != '' && this.creators.length > 0){
    			return false;
    		}

    		if(this.state == 'legal' && this.licence != null && this.hasAcceptedLegal){
    			return false;
    		}

    		return true;
    	};

    	this.next = function(){
    		this.nextEnabled = false;

    		if(this.state == 'basic_details' && (!this.isReleaseDate || this.releaseDate != null)){
    			this.isReleaseDateOpen = false;
    			this.state = 'legal';
	    	} else if(this.state == 'legal' && this.licence != null && this.hasAcceptedLegal){
	    		this.state = 'confirm';
                this.computedReleaseDate = this.isReleaseDate ? this.releaseDate : new Date();
	    	}
    	};

    	this.isConfirmDisabled = function(){
    		return this.password == '';
    	};

    	this.confirm = function(){
            icat.verifyPassword(this.password).then(function(isValid){
                if(isValid){
                    var licenceName = that.licence.name;
                    var licenceUrl = that.licence.url;
                    if(licenceName = "Creative Commons"){
                        licenceName += " - " + that.creativeCommonsLicenceName;
                        licenceUrl = that.creativeCommonsLicenceUrl;
                    }

                    facility.doiMinter().makePublicDataCollection(that.title, that.description, that.creators, that.computedReleaseDate, licenceName, licenceUrl, datasetIds, datafileIds).then(function(){
                        if(that.isFromCart){
                            user.deleteAllCartItems().then(function(){
                                tc.refresh();
                                $uibModalInstance.dismiss('cancel');
                            });
                        } else {
                            tc.refresh();
                            $uibModalInstance.dismiss('cancel');
                        }
                    }, function(response){
                        inform.add(response.message, {
                            'ttl': 3000,
                            'type': 'danger'
                        });
                    });
                } else {
                    that.password = "";
                    inform.add("Password is invalid - please try again", {
                        'ttl': 1500,
                        'type': 'danger'
                    });
                }
            });
    	};

    	this.cancel = function() {
            $uibModalInstance.dismiss('cancel');
        };

    });

})();
