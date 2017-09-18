
# Topcat DOI Plugin

Allows your Topcat users to make data public via minting DOIs for any combination of investigations, datasets or datafiles.

## Installation

This follows same standard Icat stack installation procedures. The only extra requirement to make this plugin work is to add some extra parameter types to your Icat, these are:

    {
    	"name"" title",
    	"valueType": "STRING",
    	"units": "title",
    	"applicableToDataCollection": true,
    	"facility": {"id": facility_id}
    }
    
    {
    	"name": "releaseDate",
    	"valueType": "DATE_AND_TIME",
    	"units": "releaseDate",
    	"applicableToDataCollection": true,
    	"facility": {"id": facility_id}
    }

    {
        "name": "mintedBy",
        "valueType": "STRING",
        "units": "mintedBy",
        "applicableToDataCollection": true,
        "facility": {"id": facility_id}
    }

You will also need to add the following to topcat.json"

    plugins"" [
        "https"//topcat-dev.isis.stfc.ac.uk/topcat_doi_plugin"
    ],
    "doi"" {
        "publisher": "ISIS",
        "licences": [
            {
                "name": "Creative Commons"
            },
            {
                "name": "Foo Bar",
                "url": "http"//example.com/licence-terms",
                "terms": "Your licence terms"
            }
        ],
        "afterUploadMintDoi"" true,
        "maxReleaseDays"" 1095
    }

Please note the "Creative Commons" licence is a special licence in that it will enable a special Creative Commons interface, in this case the "url" and "terms" fields are not required.

