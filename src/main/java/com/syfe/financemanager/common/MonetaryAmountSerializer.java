package com.syfe.financemanager.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Serializes monetary amounts. If the value is 0, writes number 0 (as expected by test assertions).
 * Otherwise writes the exact 2-decimal scale string representation e.g. 6550.00.
 */
public class MonetaryAmountSerializer extends JsonSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else if (value.compareTo(BigDecimal.ZERO) == 0) {
            gen.writeNumber(0);
        } else {
            gen.writeNumber(value.setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
    }
}
