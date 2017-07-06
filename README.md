
# Topcat DOI Plugin

Allows your Topcat users to make data public via minting DOIs for any combination of investigations, datasets or datafiles.

## Installation

This follows same standard Icat stack installation procedures. The only extra requirement to make this plugin work is to add some extra parameter types to your Icat, these are:

    {
    	:name => "title",
    	:valueType => "STRING",
    	:units => "title",
    	:applicableToDataCollection => true,
    	:facility => {:id => facility_id}
    }
    
    {
    	:name => "releaseDate",
    	:valueType => "DATE_AND_TIME",
    	:units => "releaseDate",
    	:applicableToDataCollection => true,
    	:facility => {:id => facility_id}
    }

    {
        :name => "mintedBy",
        :valueType => "STRING",
        :units => "mintedBy",
        :applicableToDataCollection => true,
        :facility => {:id => facility_id}
    }

