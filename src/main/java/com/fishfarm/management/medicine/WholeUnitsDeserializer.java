package com.fishfarm.management.medicine;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
/** Bottles and packets cannot be silently truncated from fractional JSON numbers. */
public class WholeUnitsDeserializer extends JsonDeserializer<Integer> {
 @Override public Integer deserialize(JsonParser parser,DeserializationContext context) throws IOException {
  if(parser.currentToken()!=JsonToken.VALUE_NUMBER_INT)return context.reportInputMismatch(Integer.class,"Enter a whole number of bottles or packets");
  return parser.getIntValue();
 }
}
