

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
			//see https://github.com/icatproject/topcat/blob/master/yo/app/scripts/services/object-validator.service.js
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
				controller: 'MyDoisController as myDoisController'
			});

			var doiMinters = {};

			tc.doiMinter = function(facilityName){
				if(!doiMinters[facilityName]) doiMinters[facilityName] = tcDoiMinter.create(tc.facility(facilityName));
				return doiMinters[facilityName];
			};

		}
	};
});

