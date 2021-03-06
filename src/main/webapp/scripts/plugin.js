

registerTopcatPlugin(function(pluginUrl){
	return {
		scripts: [
			pluginUrl + 'scripts/controllers/add-creator.js',
			pluginUrl + 'scripts/controllers/make-data-public.js',
			pluginUrl + 'scripts/controllers/my-dois.js',
			pluginUrl + 'scripts/controllers/doi-landing-page.js',
			pluginUrl + 'scripts/controllers/doi-landing-page-download.js',
			pluginUrl + 'scripts/services/tc-doi-minter.js'
		],

		stylesheets: [
			pluginUrl + 'styles/main.css'
		],

		configSchema: {
			doi: {
				publisher: {_type: 'string'},
				licences: {
					_type: 'array',
					_item: {
						name: {_type: 'string'},
						url: {
							_type: 'string',
							_mandatory: function(o){ return o.name != 'Creative Commons';  }
						},
						terms: {
							_type: "string",
							_mandatory: function(o){ return o.name != 'Creative Commons';  }

						}
					}
				},
				afterUploadMintDoi: {_type: 'boolean', _mandatory: false},
				maxReleaseDays: {_type: 'number', _mandatory: false}
			}
		},

		setup: function($uibModal, $rootScope, $state, tc, tcDoiMinter){

			tc.ui().registerCartButton('make-data-public', {insertBefore: 'cancel'}, function(){
				$uibModal.open({
                    templateUrl : pluginUrl + 'views/make-data-public.html',
                    controller: 'MakeDataPublicController as makeDataPublicController',
                    size : 'md',
	                resolve: {
	                	datafileIds: function(){
	                		return [];
	                	}
	                }
                });
			});

			$rootScope.$on('upload:complete', function(e, datafileIds){
				if(tc.doiMinter($state.params.facilityName).config().afterUploadMintDoi){
		            $uibModal.open({
		                templateUrl : pluginUrl + 'views/make-data-public.html',
		                controller: 'MakeDataPublicController as makeDataPublicController',
		                size : 'md',
		                resolve: {
		                	datafileIds: function(){
		                		return datafileIds;
		                	}
		                }
		            });
		       	}
	        });

			tc.ui().registerMainTab('my-dois', pluginUrl + 'views/my-dois.html', {
				insertAfter: 'my-data',
				controller: 'MyDoisController as myDoisController',
				multiFacility: true
			});

			tc.ui().registerPage('doi-landing-page', pluginUrl + 'views/doi-landing-page.html', {
				url: '/doi-landing-page/:facilityName/DataCollection/:entityId',
				controller: 'DoiLandingPageController as doiLandingPageController'
			});

			_.each(tc.facilities(), function(facility){
				var doiMinter;
				facility.doiMinter = function(){
					if(!doiMinter) doiMinter = tcDoiMinter.create(pluginUrl, facility);
					return doiMinter;
				}
			});

			tc.doiMinter = function(facilityName){
				return tc.facility(facilityName).doiMinter();
			};

		}
	};
});

