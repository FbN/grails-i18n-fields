package i18nfields.tests

//import i18nfields.I18nTestService;
import i18nfields.tests.TestDomain;
import grails.test.*

class I18nFieldsTests extends GroovyTestCase {
    //def I18nTestService = new I18nTestService()
    
    def I18nTestService
    
    protected void setUp() {
        new TestDomain(name_es:"name es",name_en:"name en",name_pt:"name pt").save(failOnError:true,flush:true)
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testGetter() {
        //Basic test
        TestDomain td= TestDomain.get(1);
        assertNotNull "Should return something", td
        
        //Testing changing the locale
        assertNotNull "Should return something",td.name
        i18nfields.I18nFieldsHelper.setLocale(new Locale("pt", "BR"))
        assertEquals "Should return 'name pt'", "name pt", td.name
        i18nfields.I18nFieldsHelper.setLocale(new Locale("en", "US"))
        assertEquals "Should return 'name en'", "name en", td.name
    }
    
    void testSetter() {
        TestDomain td= TestDomain.get(2)
        assertNotNull "Should return something", td

        i18nfields.I18nFieldsHelper.setLocale(new Locale("pt", "BR"))
        assertEquals "PT name unmodified", "name pt", td.name
        td.name = "NAME PT CHANGED" // Uppercase!
        assertEquals "PT name must changed", "NAME PT CHANGED", td.name
        i18nfields.I18nFieldsHelper.setLocale(new Locale("en", "US"))
        assertEquals "EN name must be unchanged", "name en", td.name
    }
    
    void testWithLocale() {
        TestDomain td= TestDomain.get(3)
        i18nfields.I18nFieldsHelper.setLocale new Locale("en", "US")
         
        // Start withLocale pt_PT block
        i18nfields.I18nFieldsHelper.withLocale(new Locale("pt", "PT")) {
            assertEquals "PT name returned inside withLocale", "name pt", td.name 
        }
        
        // Out of the block is still en_US
        assertEquals "EN name out of the withLocale", "name en", td.name
    }
    
    void testDirectAccess() {
        TestDomain td = TestDomain.get(4)
        assertEquals "PT name directly accessed", "name pt", td.name_pt
        assertEquals "EN name directly accessed", "name en", td.name_en
    }
    
    void testServiceWithLocale() {
        i18nfields.I18nFieldsHelper.setLocale new Locale("en", "US")
        //assertEquals "name is initially in English", "name en", i18nTestService.I18nTestService()
        assertEquals "name is in PT inside the block", "name pt", i18nTestService.serviceInOwnLocale()
        assertEquals "name is finally again in English", "name en", i18nTestService.serviceInDefaultLocale()
    }
    
}
