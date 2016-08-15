

registerTopcatPlugin(function(pluginUrl){
	return {
		scripts: [
			pluginUrl + 'scripts/controllers/make-data-public.controller.js'
		],

		stylesheets: [
			pluginUrl + 'styles/main.css'
		],

		configSchema: function(){
			//see https://github.com/icatproject/topcat/blob/master/yo/app/scripts/services/object-validator.service.js
		},

		setup: function($uibModal, tc){

			tc.ui().registerCartButton('make-data-public', {insertBefore: 'cancel'}, function(){
				$uibModal.open({
                    templateUrl : pluginUrl + 'views/make-data-public.html',
                    controller: 'MakeDataPublicController as makeDataPublicController',
                    size : 'md'
                })
			});

		}
	};
});

