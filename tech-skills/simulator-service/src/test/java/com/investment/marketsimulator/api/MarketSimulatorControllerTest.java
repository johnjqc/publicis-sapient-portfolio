package com.investment.marketsimulator.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;

import com.investment.marketsimulator.domain.model.Symbol;
import com.investment.marketsimulator.domain.port.in.MarketSimulationUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MarketSimulatorController.class)
class MarketSimulatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketSimulationUseCase simulationUseCase;

    @Test
    void givenTrackedSymbols_whenGetTrackedSymbols_thenReturnsTrackedSymbols() throws Exception {

        given(simulationUseCase.getTrackedSymbols())
                .willReturn(List.of(
                        Symbol.of("AAPL"),
                        Symbol.of("NVDA")));

        mockMvc.perform(get("/api/v1/market-simulator/symbols"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]").value("AAPL"))
                .andExpect(jsonPath("$[1]").value("NVDA"));

        then(simulationUseCase).should().getTrackedSymbols();
    }
}
