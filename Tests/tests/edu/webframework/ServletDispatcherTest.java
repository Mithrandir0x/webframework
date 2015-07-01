package edu.webframework;

import static org.junit.Assert.*;

import edu.webframework.annotations.HttpMethod;
import edu.webframework.annotations.HttpMethodType;
import edu.webframework.annotations.HttpRequestParameter;
import edu.webframework.annotations.UrlPathController;
import edu.webframework.exceptions.WebControllerDefinitionException;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.util.List;
import java.util.Map;

public class ServletDispatcherTest {

    @Test
    public void testCast() throws Exception {
        System.out.println("---> [START] [ServletDispatcherTest] [testCast] <---");
        MockServletDispatcher s = new MockServletDispatcher();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("string", "hello");
        request.setParameter("integer", "33");
        request.setParameter("float", "3.14");
        request.setParameter("double", "153.14433");

        String strVal = s.cast(request, String.class, new Class[0], "string");
        Integer intVal = s.cast(request, Integer.class, new Class[0], "integer");
        Float floatVal = s.cast(request, Float.class, new Class[0], "float");
        Double doubleVal = s.cast(request, Double.class, new Class[0], "double");

        assertEquals(strVal, "hello");
        assertEquals(intVal, new Integer(33));
        assertEquals(floatVal, new Float(3.14f));
        assertEquals(doubleVal, new Double(153.14433d));
        System.out.println("---> [  END] [ServletDispatcherTest] [testCast] <---");
    }

    @Test
    public void testCastLists() throws Exception {
        System.out.println("---> [START] [ServletDispatcherTest] [testCastLists] <---");
        MockServletDispatcher s = new MockServletDispatcher();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("numbers", new String[]{"1", "2", "3", "4", "5"});

        List<Integer> strVal = s.cast(request, List.class, new Class[] { Integer.class }, "numbers");

        assertNotNull(strVal);
        assertEquals(strVal.size(), 5);
        for ( int i = 0 ; i < 5 ; i++ ) {
            assertEquals(strVal.get(i), new Integer(i + 1));
        }

        Map<String, String> map = s.cast(request, Map.class, new Class[] { String.class, String.class }, "numbers");
        assertNull(map);
        System.out.println("---> [  END] [ServletDispatcherTest] [testCastLists] <---");
    }

    @Test
    public void testCastObject() throws Exception {
        System.out.println("---> [START] [ServletDispatcherTest] [testCastObject] <---");
        MockServletDispatcher s = new MockServletDispatcher();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("testbean.strValue", "hello");
        request.setParameter("testbean.intValue", "33");

        TestBean tb = s.castObject(request, TestBean.class, "testbean");
        assertNotNull(tb);
        assertEquals(tb.getStrValue(), "hello");
        assertEquals(tb.getIntValue(), new Integer(33));
        assertNull(tb.getFloatValue());
        assertNotNull(tb.getMoreBeans());
        assertEquals(tb.getMoreBeans().size(), 0);
        System.out.println("---> [  END] [ServletDispatcherTest] [testCastObject] <---");
    }

    @Test
    public void testGenerateDescriptor() throws Exception {
        System.out.println("---> [START] [ServletDispatcherTest] [testGenerateDescriptor] <---");
        MockServletDispatcher s = new MockServletDispatcher();

        ServletDispatcher.ControllerDescriptor cd = s.generateDescriptor(ControllerA.class);
        assertNotNull(cd);

        ServletDispatcher.HttpMethodActionMap hmam = cd.providers.get(HttpMethodType.GET);
        assertNotNull(hmam);

        ServletDispatcher.HttpRequestArgumentMethodDescriptor hramd = hmam.get("_DEFAULT_");
        assertNotNull(hramd);
        assertEquals(hramd.httpMethod, HttpMethodType.GET);
        assertNotNull(hramd.requestArguments);
        assertEquals(hramd.requestArguments.size(), 2);

        ServletDispatcher.RequestArgument ra = hramd.requestArguments.get(0);
        assertNotNull(ra);
        assertEquals(ra.type, List.class);
        assertNotNull(ra.genericTypes);
        assertEquals(ra.genericTypes.length, 1);
        assertEquals(ra.genericTypes[0], Integer.class);
        System.out.println("---> [  END] [ServletDispatcherTest] [testGenerateDescriptor] <---");
    }

    @Test
    public void testExecuteGetMethodOnTestController() throws Exception {
        System.out.println("---> [START] [ServletDispatcherTest] [testExecuteGetMethodOnTestController] <---");
        MockServletDispatcher s = new MockServletDispatcher();

        s.initializeWebControllers(new Class[]{ControllerA.class});

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("testbean.strValue", "hello");
        request.setParameter("testbean.intValue", "33");
        request.setMethod(HttpMethodType.GET);
        request.setRequestURI("/test/hello/world");

        MockHttpServletResponse response = new MockHttpServletResponse();

        s.doGet(request, response);
        System.out.println("---> [  END] [ServletDispatcherTest] [testExecuteGetMethodOnTestController] <---");
    }

    @UrlPathController(path = "/test/{param1}/{param2}")
    static class ControllerA extends WebController {

        @HttpMethod(type = HttpMethodType.GET)
        public void testMethodA(@HttpRequestParameter(name="numbers") List<Integer> numbers, @HttpRequestParameter(name="colors") List<String> colors) throws Exception {
            assertNotNull(numbers);
            assertEquals(numbers.size(), 0);

            assertNotNull(colors);
            assertEquals(colors.size(), 0);
        }

    }

    @Test
    public void testExecuteGetMethodOnTestControllerB() throws Exception {
        System.out.println("---> [START] [ServletDispatcherTest] [testExecuteGetMethodOnTestControllerB] <---");
        MockServletDispatcher s = new MockServletDispatcher();

        s.initializeWebControllers(new Class[] { ControllerB.class });

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("testbean.strValue", "hello");
        request.setParameter("testbean.intValue", "33");
        request.setMethod(HttpMethodType.GET);
        request.setRequestURI("/test/b/");

        MockHttpServletResponse response = new MockHttpServletResponse();

        s.doGet(request, response);
        System.out.println("---> [  END] [ServletDispatcherTest] [testExecuteGetMethodOnTestControllerB] <---");
    }

    @UrlPathController(path = "/test/b/")
    static class ControllerB extends WebController {

        @HttpMethod(type = HttpMethodType.GET)
        public void testMethodA(@HttpRequestParameter(name="testbean") TestBean tb) {
            System.out.println("TestControllerB::testMethodA");
            assertNotNull(tb);
            assertEquals(tb.getStrValue(), "hello");
            assertEquals(tb.getIntValue(), new Integer(33));
            assertNull(tb.getFloatValue());
        }

    }

    @Test
    public void testExceptionHandling() throws Exception {
        System.out.println("---> [START] [ServletDispatcherTest] [testExceptionHandling] <---");
        try {
            MockServletDispatcher s = new MockServletDispatcher();

            s.initializeWebControllers(new Class[]{FalseAssertController.class});

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setMethod(HttpMethodType.GET);
            request.setRequestURI("/test/assertError/");

            MockHttpServletResponse response = new MockHttpServletResponse();

            s.doGet(request, response);
        } catch ( ServletException ex ) {
            System.out.println("Exception caught!");
        }
        System.out.println("---> [  END] [ServletDispatcherTest] [testExceptionHandling] <---");
    }

    @Test
    public void testUnknownUrlHandling() throws Exception {
        System.out.println("---> [START] [ServletDispatcherTest] [testUnknownUrlHandling] <---");
        MockServletDispatcher s = new MockServletDispatcher();

        s.initializeWebControllers(new Class[]{FalseAssertController.class});

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(HttpMethodType.GET);
        request.setRequestURI("/test/unknownurl/hello/world");

        MockHttpServletResponse response = new MockHttpServletResponse();

        s.doGet(request, response);
        System.out.println("---> [  END] [ServletDispatcherTest] [testUnknownUrlHandling] <---");
    }

    @UrlPathController(path = "/test/assertError/")
    static class FalseAssertController extends WebController {

        @HttpMethod(type = HttpMethodType.GET)
        public void assertError() {
            System.out.println("TestExceptionController::assertError");
            throw new RuntimeException("From assertError()");
        }

    }

    @Test
    public void testPrimitiveArgument() throws Exception {
        System.out.println("---> [START] [ServletDispatcherTest] [testPrimitiveArgument] <---");

        MockServletDispatcher s = new MockServletDispatcher();

        try {
            s.generateDescriptor(PrimitiveArgumentController.class);
        } catch ( WebControllerDefinitionException ex ) {
            System.out.println("Exception caught!");
        }

        System.out.println("---> [  END] [ServletDispatcherTest] [testPrimitiveArgument] <---");
    }

    @UrlPathController(path = "/test/PrimitiveArgumentController/")
    static class PrimitiveArgumentController extends WebController {

        @HttpMethod(type = HttpMethodType.GET)
        public void methodWithPrimitiveArguments(@HttpRequestParameter(name="floatVal") Float floatVal, @HttpRequestParameter(name="intVal") int intVal) {
            System.out.println("PrimitiveArgumentController::methodWithPrimitiveArguments");
            assertEquals(intVal, 33);
            assertEquals(floatVal, new Float(3.14));
        }

    }

}