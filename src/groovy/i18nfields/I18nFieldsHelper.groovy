package i18nfields

import groovy.lang.Closure;

class I18nFieldsHelper {
	
	/**
	 * Model to clone when 'transients' must be added to a domain class
	 */
	public static transients_model = ['fieldname'];
		
	static void setLocale(loc) {
		org.springframework.context.i18n.LocaleContextHolder.setLocale(loc)
	}
	static Locale getLocale() {
		return org.springframework.context.i18n.LocaleContextHolder.getLocale()
	}
    
    static def withLocale(def newLocale, Closure clos) {
        def previous = getLocale()
        setLocale(newLocale)
        clos.run()
        setLocale(previous)
    } 
}
