package com.technicalassessment.btgpactual.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FundTest {

    @Test
    void shouldCreateFundWithBuilder() {
        Fund fund = Fund.builder()
                .fundId("1")
                .name("FPV_BTG_PACTUAL_RECAUDADORA")
                .minimumAmount(75000.0)
                .category("FPV")
                .build();

        assertEquals("1", fund.getFundId());
        assertEquals("FPV_BTG_PACTUAL_RECAUDADORA", fund.getName());
        assertEquals(75000.0, fund.getMinimumAmount());
        assertEquals("FPV", fund.getCategory());
    }

    @Test
    void shouldCreateAllFiveFunds() {
        Fund[] funds = {
                Fund.builder().fundId("1").name("FPV_BTG_PACTUAL_RECAUDADORA").minimumAmount(75000.0).category("FPV").build(),
                Fund.builder().fundId("2").name("FPV_BTG_PACTUAL_ECOPETROL").minimumAmount(125000.0).category("FPV").build(),
                Fund.builder().fundId("3").name("DEUDAPRIVADA").minimumAmount(50000.0).category("FIC").build(),
                Fund.builder().fundId("4").name("FDO-ACCIONES").minimumAmount(250000.0).category("FIC").build(),
                Fund.builder().fundId("5").name("FPV_BTG_PACTUAL_DINAMICA").minimumAmount(100000.0).category("FPV").build()
        };

        assertEquals(5, funds.length);

        // Verify FPV category count
        long fpvCount = java.util.Arrays.stream(funds).filter(f -> "FPV".equals(f.getCategory())).count();
        assertEquals(3, fpvCount);

        // Verify FIC category count
        long ficCount = java.util.Arrays.stream(funds).filter(f -> "FIC".equals(f.getCategory())).count();
        assertEquals(2, ficCount);
    }
}
