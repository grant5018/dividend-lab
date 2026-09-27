package com.example.dividend;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
class EtfTest {
 static BigDecimal n(String s){return new BigDecimal(s);}
 static Calculator.Input fixture(){Calculator.Input i=CalculatorChecks.fixture();i.mode="ETF";i.price=n("1.2");i.buyback=null;i.ttmDividend=n("0.024");i.expectedDistribution=n("0.036");i.sustainableDistribution=n("0.03");i.nav=n("1.2");i.annualFee=n("0.2");for(Calculator.Scenario s:i.scenarios){s.payout=null;s.price=n("1.2");}return i;}
 static Calculator.Holding h(String code,String weight,String forward,String normal){Calculator.Holding h=new Calculator.Holding();h.code=code;h.weight=n(weight);h.forwardYield=forward==null?null:n(forward);h.sustainableYield=normal==null?null:n(normal);return h;}
 static void eq(String expected,BigDecimal actual){assertEquals(0,n(expected).compareTo(actual));}
 @Test void cashAndScenarios(){Calculator.Input i=fixture();i.scenarios.get(0).growth=n("-20");Calculator.Result r=Calculator.calculate(i);eq("2",r.ttm);eq("3",r.forward);eq("2.5",r.sustainable);eq("2.4",r.scenarios.get("悲观"));assertNull(r.shareholder);}
 @Test void unknownIsNotZero(){Calculator.Input i=fixture();i.ttmDividend=null;i.expectedDistribution=null;i.sustainableDistribution=null;Calculator.Result r=Calculator.calculate(i);assertNull(r.ttm);assertNull(r.forward);assertNull(r.sustainable);assertNull(r.scenarios.get("基准"));i.expectedDistribution=BigDecimal.ZERO;eq("0",Calculator.calculate(i).forward);}
 @Test void partialCoverageDoesNotNormalize(){Calculator.Input i=fixture();i.holdings.add(h("A","20","5","4"));Calculator.Result r=Calculator.calculate(i);eq("20",r.coverage);eq("1",r.underlyingForward);eq("0.8",r.underlyingSustainable);assertNull(r.netDividendEstimate);}
 @Test void fullCoverageFeesAndPremium(){Calculator.Input i=fixture();i.nav=n("1");i.holdings.add(h("A","40","5","4"));i.holdings.add(h("B","60","3","2"));Calculator.Result r=Calculator.calculate(i);eq("3.8",r.underlyingForward);eq("2.8",r.underlyingSustainable);eq("3",r.netDividendEstimate);eq("3",r.forward);}
 @Test void unknownHoldingAndNegativeNet(){Calculator.Input i=fixture();i.holdings.add(h("A","100",null,"0"));assertNull(Calculator.calculate(i).underlyingForward);assertNull(Calculator.calculate(i).netDividendEstimate);i.holdings.get(0).forwardYield=n("0");eq("-0.2",Calculator.calculate(i).netDividendEstimate);}
 @Test void invalidInputs(){Calculator.Input i=fixture();i.holdings.add(h("A","60","5","4"));i.holdings.add(h("B","50","3","2"));assertThrows(IllegalArgumentException.class,()->Calculator.calculate(i));i.holdings.get(1).weight=n("40");i.holdings.get(1).code="A";assertThrows(IllegalArgumentException.class,()->Calculator.calculate(i));i.holdings.clear();i.expectedDistribution=n("-1");assertThrows(IllegalArgumentException.class,()->Calculator.calculate(i));i.expectedDistribution=null;i.buyback=n("1");assertThrows(IllegalArgumentException.class,()->Calculator.calculate(i));}
}
