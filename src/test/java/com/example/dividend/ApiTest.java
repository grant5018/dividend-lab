package com.example.dividend;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tests;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop"})
@ActiveProfiles("h2") @AutoConfigureMockMvc
class ApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
 @Test void formulas(){CalculatorChecks.main(new String[0]);}
 @Test void lifecycle() throws Exception {
  String a=mvc.perform(post("/api/assets").contentType("application/json").content("{\"code\":\"TEST\",\"name\":\"测试标的\",\"price\":100}" )).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  long assetId=mapper.readTree(a).get("id").asLong();
  Api.SaveRequest request=new Api.SaveRequest();request.assetId=assetId;request.title="First";request.input=CalculatorChecks.fixture();
  String m=mvc.perform(post("/api/measurements").contentType("application/json").content(mapper.writeValueAsString(request))).andExpect(status().isOk()).andExpect(jsonPath("$.result.forward").value(6)).andReturn().getResponse().getContentAsString();
  long id=mapper.readTree(m).get("id").asLong();
  mvc.perform(delete("/api/assets/"+assetId)).andExpect(status().isBadRequest());
  request.title="Updated";request.input.eps=CalculatorChecks.n("12");
  mvc.perform(put("/api/measurements/"+id).contentType("application/json").content(mapper.writeValueAsString(request))).andExpect(status().isOk()).andExpect(jsonPath("$.result.forward").value(7.2));
  mvc.perform(get("/api/measurements/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated"));
  mvc.perform(get("/")).andExpect(status().isOk());
  mvc.perform(post("/api/calculate").contentType("application/json").content("{\"price\":0}")).andExpect(status().isBadRequest());
  mvc.perform(delete("/api/measurements/"+id)).andExpect(status().isOk());
  mvc.perform(get("/api/measurements/"+id)).andExpect(status().isNotFound());
  mvc.perform(delete("/api/assets/"+assetId)).andExpect(status().isOk());
 }
}
