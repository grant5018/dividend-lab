package com.example.dividend;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api") public class Api {
 private final AssetRepository assets; private final MeasurementRepository measurements; private final ObjectMapper mapper;
 public Api(AssetRepository a,MeasurementRepository m,ObjectMapper o){assets=a;measurements=m;mapper=o;}
 private static void check(boolean b,String m){if(!b)throw new IllegalArgumentException(m);}
 private Asset asset(Long id){return assets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"标的不存在"));}
 private Measurement measurement(Long id){return measurements.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"记录不存在"));}
 @GetMapping("/assets") public List<Asset> assets(){return assets.findAll(org.springframework.data.domain.Sort.by("id").descending());}
 @PostMapping("/assets") public Asset create(@RequestBody Asset a){a.id=null;return saveAsset(a);}
 @PutMapping("/assets/{id}") public Asset update(@PathVariable Long id,@RequestBody Asset a){asset(id);a.id=id;return saveAsset(a);}
 private Asset saveAsset(Asset a){
  check(a.code!=null&&a.code.trim().matches("[A-Za-z0-9._-]{1,32}"),"代码应为1～32位字母、数字、点、下划线或短横线");a.code=a.code.trim().toUpperCase(Locale.ROOT);
  check(a.name!=null&&!a.name.trim().isEmpty()&&a.name.length()<=100,"名称必填且不超过100字");a.name=a.name.trim();
  check(a.industry==null||a.industry.length()<=100,"行业不超过100字");check(a.price!=null&&a.price.signum()>0,"股价必须大于0");
  check(a.shares==null||a.shares.signum()>0,"总股本必须大于0");check(a.marketCap==null||a.marketCap.signum()>0,"总市值必须大于0");
  if(a.shares!=null&&a.marketCap!=null)check(a.price.multiply(a.shares).subtract(a.marketCap).abs().compareTo(a.price.multiply(a.shares).multiply(new java.math.BigDecimal("0.01")))<=0,"股价×股本与市值偏差超过1%");
  check(!assets.existsByCodeAndIdNot(a.code,a.id==null?-1L:a.id),"代码已存在");return assets.save(a);
 }
 @DeleteMapping("/assets/{id}") @Transactional public void deleteAsset(@PathVariable Long id){asset(id);check(!measurements.existsByAssetId(id),"请先删除该标的的历史记录");assets.deleteById(id);}
 @PostMapping("/calculate") public Calculator.Result calculate(@RequestBody Calculator.Input in){return Calculator.calculate(in);}
 public static class SaveRequest { public Long assetId;public String title;public Calculator.Input input; }
 public static class View { public Long id,assetId;public String assetCode,assetName,title,createdAt,updatedAt;public Calculator.Input input;public Calculator.Result result; }
 private View view(Measurement m){try {View v=new View();v.id=m.id;v.assetId=m.assetId;v.assetCode=m.assetCode;v.assetName=m.assetName;v.title=m.title;v.createdAt=m.createdAt;v.updatedAt=m.updatedAt;v.input=mapper.readValue(m.inputJson,Calculator.Input.class);v.result=Calculator.calculate(v.input);return v;}catch(java.io.IOException e){throw new IllegalStateException("保存的数据无法读取",e);}}
 @GetMapping("/measurements") public List<View> list(){List<View> list=new ArrayList<>();for(Measurement m:measurements.findAll(org.springframework.data.domain.Sort.by("id").descending()))list.add(view(m));return list;}
 @GetMapping("/measurements/{id}") public View get(@PathVariable Long id){return view(measurement(id));}
 @PostMapping("/measurements") public View createMeasurement(@RequestBody SaveRequest r){return save(new Measurement(),r);}
 @PutMapping("/measurements/{id}") public View updateMeasurement(@PathVariable Long id,@RequestBody SaveRequest r){return save(measurement(id),r);}
 private View save(Measurement m,SaveRequest r){
  check(r.assetId!=null,"请选择标的");Asset a=asset(r.assetId);check(r.title!=null&&!r.title.trim().isEmpty()&&r.title.length()<=100,"记录标题必填且不超过100字");Calculator.calculate(r.input);
  m.assetId=a.id;m.assetCode=a.code;m.assetName=a.name;m.title=r.title.trim();m.updatedAt=Instant.now().toString();if(m.createdAt==null)m.createdAt=m.updatedAt;
  try{m.inputJson=mapper.writeValueAsString(r.input);}catch(java.io.IOException e){throw new IllegalArgumentException("参数无法保存");}return view(measurements.save(m));
 }
 @DeleteMapping("/measurements/{id}") public void delete(@PathVariable Long id){measurement(id);measurements.deleteById(id);}
 @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) public Map<String,String> invalid(IllegalArgumentException e){return Collections.singletonMap("message",e.getMessage());}
 @ExceptionHandler(HttpMessageNotReadableException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) public Map<String,String> malformed(){return Collections.singletonMap("message","JSON格式、字段名称或数字格式无效");}
 @ExceptionHandler(DataIntegrityViolationException.class) @ResponseStatus(HttpStatus.CONFLICT) public Map<String,String> conflict(){return Collections.singletonMap("message","数据冲突或数值超过数据库精度，请检查代码及数值");}
}
