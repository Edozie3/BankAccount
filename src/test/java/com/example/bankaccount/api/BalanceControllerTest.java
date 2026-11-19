package com.example.bankaccount.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bankaccount.service.BankAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BalanceController.class)
class BalanceControllerTest {

    private static final String BALANCE_URL = "/api/v1/account/balance";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankAccountService bankAccountService;

    @Test
    void givenBalance_whenGettingBalance_thenReturnsBalanceAndCurrencyAsJson() throws Exception {
        // given
        when(bankAccountService.retrieveBalance()).thenReturn(12345.67);

        // when / then
        mockMvc.perform(get(BALANCE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"balance": 12345.67, "currency": "GBP"}
                        """, JsonCompareMode.STRICT));
    }


    @Test
    void givenPostRequest_whenCallingBalance_thenReturns405Problem() throws Exception {
        // given: the balance endpoint only supports GET

        // when / then
        mockMvc.perform(post(BALANCE_URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
