package com.example.dividend;
import java.math.BigDecimal;
public class CalculatorChecks {
 static BigDecimal n(String s){return new BigDecimal(s);}
 static Calculator.Input fixture(){Calculator.Input i=new Calculator.Input();i.price=n("100");i.shares=n("10000000");i.marketCap=n("1000000000");i.eps=n("10");i.normalizedEps=n("6");i.payout=n("60");i.sustainablePayout=n("50");i.ttmDividend=n("4");i.buyback=n("2");for(String name:new String[]{"悲观","基准","乐观"}){Calculator.Scenario s=new Calculator.Scenario();s.name=name;s.growth=n("0");s.payout=n("60");s.price=n("100");i.scenarios.add(s);}return i;}
 static void eq(String expected,BigDecimal actual){if(n(expected).compareTo(actual)!=0)throw new AssertionError(expected+" != "+actual);}
 static void rejects(Runnable f){try{f.run();}catch(IllegalArgumentException e){return;}throw new AssertionError("Expected validation error");}
 public static void main(String[] args){
 Calculator.Input i=fixture();Calculator.Result r=Calculator.calculate(i);eq("4",r.ttm);eq("6",r.forward);eq("3",r.sustainable);eq("8",r.shareholder);eq("6",r.scenarios.get("基准"));
 i.mode="TOTAL";i.profit=n("100000000");i.normalizedProfit=n("60000000");i.ttmDividend=n("40000000");r=Calculator.calculate(i);eq("4",r.ttm);eq("6",r.forward);eq("3",r.sustainable);
 i.marketCap=null;eq("6",Calculator.calculate(i).forward);i.scenarios.get(0).growth=n("-20");i.scenarios.get(0).price=n("80");eq("6",Calculator.calculate(i).scenarios.get("悲观"));
 i.profit=n("-100");eq("0",Calculator.calculate(i).forward);i.payout=n("101");rejects(()->Calculator.calculate(i));i.payout=n("60");i.price=n("0");rejects(()->Calculator.calculate(i));
 Calculator.Input bad=fixture();bad.marketCap=n("2000000000");rejects(()->Calculator.calculate(bad));bad.marketCap=n("1000000000");bad.scenarios.get(0).name="基准";rejects(()->Calculator.calculate(bad));
 Calculator.Input zero=fixture();zero.buyback=null;zero.payout=n("0");eq("0",Calculator.calculate(zero).shareholder);zero.scenarios.get(0).growth=n("-101");rejects(()->Calculator.calculate(zero));
 System.out.println("PASS: 16 calculation and validation checks");
 }
}
