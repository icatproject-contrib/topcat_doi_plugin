

(function(){
	'use strict';

    var app = angular.module('topcat');

    app.controller('MakeDataPublicController', function($uibModalInstance, $uibModalStack, $timeout, tc, inform){
        
        if(tc.userFacilities().length > 1){
            alert("This feature can't be used with multiple facilities.");
            $timeout(function(){
                $uibModalInstance.dismiss();
            });
            return;
        }

        var that = this;
    	this.state = 'basic_details';
        this.title = "";
    	this.isReleaseDate = false;
    	this.releaseDate = null;
    	this.isReleaseDateOpen = false;
    	this.licence = null;
    	this.hasAcceptedLegal = false;
    	this.dateFormat = 'yyyy-MM-dd';
    	this.licences = [
    		{
    			name: 'mit',
    			title: 'MIT'
    		},
    		{
    			name: 'cc',
    			title: 'Creative Commons'
    		}
    	];
        this.termsAndConditions = "line 1\n line 2\nline 3\nline 4\nline 5\nline 6\nline 7\nline 8\nline 9\n"
    	this.users = _.map(tc.userFacilities(), function(facility){
            var session = facility.icat().session();

            return {
                name: (session.fullName ? session.fullName : session.username) + " (" + facility.config().title + ")",
                facilityName: facility.config().name
            };
        });

        this.facilityName = this.users[0].facilityName;
        this.password = "";

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
    		if(this.state == 'basic_details' && (!this.isReleaseDate || this.releaseDate != null) && this.title != ''){
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
	    	}
    	};

    	this.isConfirmDisabled = function(){
    		return this.password == '';
    	};

    	this.confirm = function(){
            tc.icat(this.facilityName).verifyPassword(this.password).then(function(isValid){
                if(isValid){
                    var facility = tc.userFacilities()[0];
                    var user = facility.user();
                    user.cart().then(function(cart){
                        var datasetIds = [];
                        var datafileIds = [];

                        _.each(cart.cartItems,  function(cartItem){
                            if(cartItem.entityType == 'dataset') datasetIds.push(cartItem.entityId);
                            if(cartItem.entityType == 'datafile') datafileIds.push(cartItem.entityId);
                        });

                        facility.doiMinter().makePublicDataCollection(that.title, that.isReleaseDate ? that.releaseDate : "", datasetIds, datafileIds).then(function(){
                            user.deleteAllCartItems().then(function(){
                                $uibModalStack.dismissAll();
                            });
                        });

                    });
                    
                    //tc.doiMinter().makePublicDataCollection(that.title, that.isReleaseDate ? that.releaseDate : "");
                    //$uibModalStack.dismissAll();
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
