package i18nfields

import i18nfields.tests.TestDomain

class I18nTestService {

    static transactional = true    
    
    def serviceInOwnLocale(){
        TestDomain td= TestDomain.get(5)
        def out = ''
        withLocale(new Locale("pt", "BR")) {
            out = td.name
        }
        out
    }
    
    def serviceInDefaultLocale(){
        TestDomain td= TestDomain.get(5)
        def out = ''
        withLocale(new Locale("en", "US")) {
            out = td.name
        }
        out
    }
}
