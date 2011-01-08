package i18nfields.tests;

import i18nfields.I18nFieldsHelper;

@i18nfields.I18nFields
class TestDomain {

	String name
	String tagline
	
	Long companyId = 0
	
	static i18n_fields = ['name', 'tagline']
	
	static mapping={}
	static constraints = { 
		tagline(nullable:true)    // Includes tagline_es, tagline_en, tagline_pt...
	}
	static transients = [] // Needed if you want the original fields to 'disappear'
		
	// getName() & getTagline() methods will be added but I18nFields
	// Fields name_es, name_en, name_pt, tagline_es, tagline_en, tagline_pt
	// will be added to table.
	// geName_Es

}

