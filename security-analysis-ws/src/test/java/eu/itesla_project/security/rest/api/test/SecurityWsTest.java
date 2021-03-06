/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.security.rest.api.test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.powsybl.iidm.network.Network;
import com.powsybl.security.LimitViolationFilter;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.SecurityAnalysisResult;

import eu.itesla_project.security.rest.api.impl.FilePart;
import eu.itesla_project.security.rest.api.impl.SecurityAnalysisServiceImpl;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari at techrain.it>
*/
public class SecurityWsTest {

    private SecurityAnalysisServiceImpl service;

    @Before
    public void setUp() {
        SecurityAnalysisResult result = new SecurityAnalysisResult(
                new LimitViolationsResult(true, Collections.emptyList()), Collections.emptyList());
        service = Mockito.mock(SecurityAnalysisServiceImpl.class);
        when(service.analyze(any(Network.class), any(FilePart.class), any(LimitViolationFilter.class))).thenReturn(result);
        when(service.analyze(any(MultipartFormDataInput.class))).thenCallRealMethod();
    }

    @Test
    public void testSecurityAnalysisJson() {
        MultipartFormDataInput dataInput = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formValues = new HashMap<>();
        formValues.put("format", Collections.singletonList(new InputPartImpl("JSON", MediaType.TEXT_PLAIN_TYPE)));
        formValues.put("limit-types", Collections.singletonList(new InputPartImpl("CURRENT", MediaType.TEXT_PLAIN_TYPE)));

        MultivaluedMap<String, String> headers = new MultivaluedMapImpl<>();
        headers.putSingle("Content-Disposition", "filename=" + "case-file.xiidm");

        formValues.put("case-file",
                Collections.singletonList(new InputPartImpl(
                        new ByteArrayInputStream("Network".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE, headers)));

        MultivaluedMap<String, String> headers2 = new MultivaluedMapImpl<>();
        headers2.putSingle("Content-Disposition", "filename=" + "contingencies-file.csv");
        formValues.put("contingencies-file",
                Collections.singletonList(new InputPartImpl(
                        new ByteArrayInputStream("contingencies".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE, headers2)));

        when(dataInput.getFormDataMap()).thenReturn(formValues);
        Response res = service.analyze(dataInput);
        Assert.assertEquals(200, res.getStatus());

    }

    @Test
    public void testSecurityAnalysisCsv() {
        MultipartFormDataInput dataInput = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formValues = new HashMap<>();
        formValues.put("format", Collections.singletonList(new InputPartImpl("CSV", MediaType.TEXT_PLAIN_TYPE)));
        formValues.put("limit-types", Collections.singletonList(new InputPartImpl("CURRENT", MediaType.TEXT_PLAIN_TYPE)));
        MultivaluedMap<String, String> headers = new MultivaluedMapImpl<>();
        headers.putSingle("Content-Disposition", "filename=" + "case-file.xiidm");

        formValues.put("case-file",
                Collections.singletonList(new InputPartImpl(
                        new ByteArrayInputStream("Network".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE, headers)));

        MultivaluedMap<String, String> headers2 = new MultivaluedMapImpl<>();
        headers2.putSingle("Content-Disposition", "filename=" + "contingencies-file.csv");
        formValues.put("contingencies-file",
                Collections.singletonList(new InputPartImpl(
                        new ByteArrayInputStream("contingencies".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE, headers2)));

        when(dataInput.getFormDataMap()).thenReturn(formValues);
        Response res = service.analyze(dataInput);
        Assert.assertEquals(200, res.getStatus());
    }

    @Test
    public void testWrongFormat() {
        MultipartFormDataInput dataInput = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formValues = new HashMap<>();
        formValues.put("format", Collections.singletonList(new InputPartImpl("ERR", MediaType.TEXT_PLAIN_TYPE)));
        formValues.put("limit-types", Collections.singletonList(new InputPartImpl("CURRENT", MediaType.TEXT_PLAIN_TYPE)));
        MultivaluedMap<String, String> headers = new MultivaluedMapImpl<>();
        headers.putSingle("Content-Disposition", "filename=" + "case-file.xiidm.gz");

        formValues.put("case-file",
                Collections.singletonList(new InputPartImpl(
                        new ByteArrayInputStream("Network".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE, headers)));

        MultivaluedMap<String, String> headers2 = new MultivaluedMapImpl<>();
        headers2.putSingle("Content-Disposition", "filename=" + "contingencies-file.csv");
        formValues.put("contingencies-file",
                Collections.singletonList(new InputPartImpl(
                        new ByteArrayInputStream("contingencies".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE, headers2)));

        when(dataInput.getFormDataMap()).thenReturn(formValues);
        Response res = service.analyze(dataInput);
        Assert.assertEquals(400, res.getStatus());

    }
    
    @Test
    public void testMissingFormat() {
        MultipartFormDataInput dataInput = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formValues = new HashMap<>();
        formValues.put("limit-types", Collections.singletonList(new InputPartImpl("HIGH_VOLTAGE,CURRENT", MediaType.TEXT_PLAIN_TYPE)));
        MultivaluedMap<String, String> headers = new MultivaluedMapImpl<>();
        headers.putSingle("Content-Disposition", "filename=" + "case-file.xiidm.gz");

        formValues.put("case-file",
                Collections.singletonList(new InputPartImpl(
                        new ByteArrayInputStream("Network".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE, headers)));

        MultivaluedMap<String, String> headers2 = new MultivaluedMapImpl<>();
        headers2.putSingle("Content-Disposition", "filename=" + "contingencies-file.csv");
        formValues.put("contingencies-file",
                Collections.singletonList(new InputPartImpl(
                        new ByteArrayInputStream("contingencies".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE, headers2)));

        when(dataInput.getFormDataMap()).thenReturn(formValues);
        Response res = service.analyze(dataInput);
        Assert.assertEquals(400, res.getStatus());

    }

    @Test
    public void testMissingCaseFile() {
        MultipartFormDataInput dataInput = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formValues = new HashMap<>();
        formValues.put("format", Collections.singletonList(new InputPartImpl("JSON", MediaType.TEXT_PLAIN_TYPE)));
        formValues.put("limit-types", Collections.singletonList(new InputPartImpl("CURRENT", MediaType.TEXT_PLAIN_TYPE)));

        MultivaluedMap<String, String> headers2 = new MultivaluedMapImpl<>();
        headers2.putSingle("Content-Disposition", "filename=" + "contingencies-file.csv");
        formValues.put("contingencies-file",
                Collections.singletonList(new InputPartImpl(
                        new ByteArrayInputStream("contingencies".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE, headers2)));

        when(dataInput.getFormDataMap()).thenReturn(formValues);
        Response res = service.analyze(dataInput);
        Assert.assertEquals(400, res.getStatus());

    }
    
    @Test
    public void testWrongLimits() {
        MultipartFormDataInput dataInput = mock(MultipartFormDataInput.class);
        Map<String, List<InputPart>> formValues = new HashMap<>();
        formValues.put("format", Collections.singletonList(new InputPartImpl("JSON", MediaType.TEXT_PLAIN_TYPE)));
        formValues.put("limit-types", Collections.singletonList(new InputPartImpl("ERRR", MediaType.TEXT_PLAIN_TYPE)));

        MultivaluedMap<String, String> headers = new MultivaluedMapImpl<>();
        headers.putSingle("Content-Disposition", "filename=" + "case-file.xiidm.gz");

        formValues.put("case-file",
                Collections.singletonList(new InputPartImpl(
                        new ByteArrayInputStream("Network".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE, headers)));

        MultivaluedMap<String, String> headers2 = new MultivaluedMapImpl<>();
        headers2.putSingle("Content-Disposition", "filename=" + "contingencies-file.csv");
        formValues.put("contingencies-file",
                Collections.singletonList(new InputPartImpl(
                        new ByteArrayInputStream("contingencies".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE, headers2)));

        when(dataInput.getFormDataMap()).thenReturn(formValues);
        Response res = service.analyze(dataInput);
        Assert.assertEquals(400, res.getStatus());

    }
    
}
