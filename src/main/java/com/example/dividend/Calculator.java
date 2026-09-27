package com.example.dividend;
import java.math.*;
import java.util.*;
/** Monetary totals and share counts use yuan and shares; rates use percentage points. */
public final class Calculator {
 public static class Input {
  public String mode="EPS";
  public BigDecimal price, shares, marketCap, profit, eps, payout, normalizedProfit, normalizedEps, sustainablePayout, ttmDividend, buyback;
  public BigDecimal expectedDistribution, sustainableDistribution, nav, annualFee;
  public List<Holding> holdings=new ArrayList<>();
  public List<Scenario> scenarios=new ArrayList<>();
 }
 public static class Holding { public String code; public BigDecimal weight, forwardYield, sustainableYield; }
 public static class Scenario { public String name; public BigDecimal growth, payout, price; }
 public static class Result {
  public BigDecimal ttm, forward, sustainable, shareholder, coverage, underlyingForward, underlyingSustainable, netDividendEstimate;
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
  if("ETF".equals(in.mode)) return etf(in);
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
 private static BigDecimal optionalYield(BigDecimal amount, BigDecimal price) {
  return amount==null?null:yield(nonnegative(amount,"每份现金分红"),HUNDRED,price);
 }
 private static Result etf(Input in) {
  Result out=new Result();
  out.ttm=optionalYield(in.ttmDividend,in.price);
  out.forward=optionalYield(in.expectedDistribution,in.price);
  out.sustainable=optionalYield(in.sustainableDistribution,in.price);
  require(in.buyback==null||in.buyback.signum()==0,"ETF不支持股票回购率，请留空");
  if(in.nav!=null)positive(in.nav,"单位净值");
  if(in.annualFee!=null)rate(in.annualFee,"年费率");
  require(in.holdings!=null&&in.holdings.size()<=500,"持仓列表不能为空值且最多500行");
  BigDecimal coverage=BigDecimal.ZERO, forward=BigDecimal.ZERO, sustainable=BigDecimal.ZERO;
  boolean hasForward=true,hasSustainable=true;Set<String> codes=new HashSet<>();
  for(Holding h:in.holdings){
   require(h!=null&&h.code!=null&&h.code.trim().matches("[A-Za-z0-9._-]{1,32}"),"请填写有效持仓代码");
   require(codes.add(h.code.trim().toUpperCase(Locale.ROOT)),"持仓代码不可重复");
   positive(h.weight,"持仓权重");rate(h.weight,"持仓权重");coverage=coverage.add(h.weight);
   if(h.forwardYield==null)hasForward=false;else forward=forward.add(h.weight.multiply(rate(h.forwardYield,"成分股前瞻股息率")));
   if(h.sustainableYield==null)hasSustainable=false;else sustainable=sustainable.add(h.weight.multiply(rate(h.sustainableYield,"成分股可持续股息率")));
  }
  require(coverage.compareTo(HUNDRED)<=0,"持仓权重合计不能超过100%");out.coverage=coverage;
  if(!in.holdings.isEmpty()){
   if(hasForward)out.underlyingForward=div(forward,HUNDRED).setScale(4,RoundingMode.HALF_UP);
   if(hasSustainable)out.underlyingSustainable=div(sustainable,HUNDRED).setScale(4,RoundingMode.HALF_UP);
  }
  // Weights use fund NAV. No normalization of partial holdings; no inference of fund distributions.
  if(coverage.compareTo(HUNDRED)==0&&out.underlyingForward!=null&&in.nav!=null&&in.annualFee!=null)
   out.netDividendEstimate=out.underlyingForward.subtract(in.annualFee).multiply(div(in.nav,in.price)).setScale(4,RoundingMode.HALF_UP);
  require(in.scenarios!=null&&in.scenarios.size()==3,"必须填写悲观、基准、乐观三个场景");
  Set<String> names=new HashSet<>(Arrays.asList("悲观","基准","乐观"));
  for(Scenario sc:in.scenarios){
   require(sc!=null&&names.remove(sc.name),"场景名称必须为悲观、基准、乐观且不可重复");
   require(sc.growth!=null&&sc.growth.compareTo(HUNDRED.negate())>=0&&sc.growth.compareTo(new BigDecimal("1000"))<=0,"分红增幅应在-100～1000之间");
   positive(sc.price,"场景价格");
   require(sc.payout==null,"ETF场景无需分红率，请留空");
   out.scenarios.put(sc.name,in.expectedDistribution==null?null:optionalYield(in.expectedDistribution.multiply(BigDecimal.ONE.add(div(sc.growth,HUNDRED))),sc.price));
  }
  return out;
 }

}
