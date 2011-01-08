import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.commons.*

class I18nFieldsGrailsPlugin {
    // the plugin version
    def version = "0.4"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def config = ConfigurationHolder.config
    
    // TODO Fill in these fields
    def author = "Jorge Uriarte, Taioli Fabiano"
    def authorEmail = "jorge.uriarte@omelas.net, fbn@maniacmansion.it"
    def title = "i18n Fields"
    def description = '''\\
This plugin provide an easy way of declarativily localize database fields of your content tables.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/i18n-fields"

    def getField = { fieldName ->
        delegate."${fieldName}_${i18nfields.I18nFieldsHelper.getLocale().language}"
    }
    
    def setField = { fieldName, value ->
        delegate."${fieldName}_${i18nfields.I18nFieldsHelper.getLocale().language}" = value
    }
    
    def withLocale = { newLocale, Closure clos ->
        def previous = i18nfields.I18nFieldsHelper.getLocale()
        i18nfields.I18nFieldsHelper.setLocale(newLocale)                    
        clos.run()
        i18nfields.I18nFieldsHelper.setLocale(previous)                    
    }
        
    
    def doWithDynamicMethods = { ctx ->
        // TODO: getLang()
        // TODO: getI18nFieldValue(field)
        // TODO: setI18nFieldValue(field, value)
        MetaClassRegistry registry = GroovySystem.metaClassRegistry
        application.domainClasses.each {domainClass ->
            def i18n_fields = GrailsClassUtils.getStaticPropertyValue(domainClass.clazz, "i18n_fields" )
            def i18n_langs = config.i18nFields.i18n_langs
            if (i18n_fields && i18n_langs) {
                i18n_fields.each() { f ->
                    def getter = GrailsClassUtils.getGetterName(f)
                    def setter = GrailsClassUtils.getSetterName(f)
                    println "Adding ${getter}"
                    domainClass.metaClass."${getter}" = getField.curry(f)
                    domainClass.metaClass."${setter}" = setField.curry(f)
                }
            }
        }        
        application.allArtefacts.each { theClass ->
            theClass.metaClass.withLocale = withLocale
        }
    }
        

}
