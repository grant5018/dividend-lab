package com.example.dividend;
import java.math.*;
import java.util.*;
/** Monetary totals and share counts use yuan and shares; rates use percentage points. */
public final class Calculator {
 public static class Input {
  public String mode="EPS";
  public BigDecimal price, shares, marketCap, profit, eps, payout, normalizedProfit, normalizedEps, sustainablePayout, ttmDividend, buyback;
  public List<Scenario> scenarios=new ArrayList<>();
 }
 public static class Scenario { public String name; public BigDecimal growth, payout, price; }
 public static class Result {
  public BigDecimal ttm, forward, sustainable, shareholder;
  public Map<String,BigDecimal> scenarios=new LinkedHashMap<>();
 }
 private static final BigDecimal HUNDRED=new BigDecimal("100");
 private static void require(boolean ok,String message) { if(!ok) throw new IllegalArgumentException(message); }
 private static BigDecimal positive(BigDecimal n,String name) {require(n!=null && n.signum()>0,name+"必须大于0"); return n;}
 private static BigDecimal nonnegative(BigDecimal n,String name) {require(n!=null && n.signum()>=0,name+"不能为空或小于0");return n;}
 private static BigDecimal rate(BigDecimal n,String name) {nonnegative(n,name);require(n.compareTo(HUNDRED)<=0,name+"应在0～100之间");return n;}
 private static BigDecimal div(BigDecimal a,BigDecimal b) { return a.divide(b,12,RoundingMode.HALF_UP); }
 private static BigDecimal yield(BigDecimal earning,BigDecimal payout,BigDecimal denominator) {return div(earning.max(BigDecimal.ZERO).multiply(payout),denominator).setScale(4,RoundingMode.HALF_UP);}
 public static Result calculate(Input in) {
  require(in!=null,"请输入参数");positive(in.price,"股价");
  require("EPS".equals(in.mode)||"TOTAL".equals(in.mode),"口径必须为EPS或TOTAL");
  if(in.shares!=null) positive(in.shares,"总股本");
  if(in.marketCap!=null) positive(in.marketCap,"市值");
  if(in.shares!=null && in.marketCap!=null) {
   BigDecimal implied=in.shares.multiply(in.price);
   require(div(implied.subtract(in.marketCap).abs(),implied).compareTo(new BigDecimal("0.01"))<=0,"股价×股本与市值偏差超过1%，请统一数据时点");
  }
  rate(in.payout,"预计分红率");rate(in.sustainablePayout,"可持续分红率");
  nonnegative(in.ttmDividend,"TTM现金分红");
  BigDecimal repurchase=in.buyback==null?BigDecimal.ZERO:rate(in.buyback,"回购率");
  boolean total="TOTAL".equals(in.mode);
  BigDecimal denominator=total?(in.marketCap!=null?in.marketCap:positive(in.shares,"总股本或市值").multiply(in.price)):in.price;
  BigDecimal earning=total?in.profit:in.eps, normal=total?in.normalizedProfit:in.normalizedEps;
  require(earning!=null && normal!=null,"请填写当前口径的预计盈利和正常化盈利");
  Result out=new Result();out.ttm=div(in.ttmDividend.multiply(HUNDRED),denominator).setScale(4,RoundingMode.HALF_UP);
  out.forward=yield(earning,in.payout,denominator);out.sustainable=yield(normal,in.sustainablePayout,denominator);
  out.shareholder=out.forward.add(repurchase);
  require(in.scenarios!=null && in.scenarios.size()==3,"必须填写悲观、基准、乐观三个场景");
  Set<String> names=new HashSet<>(Arrays.asList("悲观","基准","乐观"));
  for(Scenario s:in.scenarios) {
   require(s!=null && names.remove(s.name),"场景名称必须为悲观、基准、乐观且不可重复");
   require(s.growth!=null && s.growth.compareTo(HUNDRED.negate())>=0 && s.growth.compareTo(new BigDecimal("1000"))<=0,"盈利增速应在-100～1000之间");
   rate(s.payout,"场景分红率");positive(s.price,"场景价格");
   BigDecimal scenarioDenominator=total?div(denominator.multiply(s.price),in.price):s.price;
   out.scenarios.put(s.name,yield(earning.multiply(BigDecimal.ONE.add(div(s.growth,HUNDRED))),s.payout,scenarioDenominator));
  }
  return out;
 }
}
