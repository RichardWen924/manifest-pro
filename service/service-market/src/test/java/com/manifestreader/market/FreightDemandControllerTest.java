package com.manifestreader.market;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.market.controller.FreightDemandController;
import com.manifestreader.market.service.FreightDemandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FreightDemandController.class)
class FreightDemandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FreightDemandService freightDemandService;

    @Test
    void pageEndpointReturnsWrappedResponse() throws Exception {
        when(freightDemandService.page(any())).thenReturn(PageResult.empty(1L, 10L));

        mockMvc.perform(get("/market/demands/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
