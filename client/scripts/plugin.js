

registerTopcatPlugin(function(pluginUrl){
	return {
		scripts: [
			pluginUrl + 'scripts/controllers/make-data-public.controller.js',
			pluginUrl + 'scripts/controllers/my-dois.controller.js',
			pluginUrl + 'scripts/services/tc-doi-minter.service.js'
		],

		stylesheets: [
			pluginUrl + 'styles/main.css'
		],

		configSchema: {
			facilities: {
				_item: {
					myDois: {
						gridOptions: {
							columnDefs: {
								_type: 'array',
								_item: {
									field: {_type: 'string'},
									title: { _type: 'string', _mandatory: false },
								}
							}
						}
					}
				}
			}
		},

		setup: function($uibModal, tc, tcDoiMinter){

			tc.ui().registerCartButton('make-data-public', {insertBefore: 'cancel'}, function(){
				$uibModal.open({
                    templateUrl : pluginUrl + 'views/make-data-public.html',
                    controller: 'MakeDataPublicController as makeDataPublicController',
                    size : 'md'
                })
			});

			tc.ui().registerMainTab('my-dois', pluginUrl + 'views/my-dois.html', {
				insertAfter: 'my-data',
				controller: 'MyDoisController as myDoisController',
				multiFacility: true
			});

			var doiMinters = {};

			tc.doiMinter = function(facilityName){
				if(!doiMinters[facilityName]) doiMinters[facilityName] = tcDoiMinter.create(tc.facility(facilityName));
				return doiMinters[facilityName];
			};

		}
	};
});

