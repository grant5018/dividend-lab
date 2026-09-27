# 息望 · 预期股息率测算

Java 8 + Spring Boot 2.7.18 + Maven + 原生 HTML/CSS/JS。默认 MySQL，可切换 H2 文件数据库，无需 Node、前端构建或外部行情密钥。Spring Boot 3 不兼容 Java 8，因此按要求使用 2.7.18。

## 环境与启动

需要 JDK 8、Maven 3.9.x（例如 3.9.9）、MySQL 8.x（或选择 H2）。首次 Maven 构建需要联网下载依赖。

```bash
java -version
mvn -version
```

若尚未安装 Maven，可从 Apache Maven 下载页获取 Maven 3.9.x 二进制包，解压后将其 bin 目录加入 PATH。不要使用要求较新 JDK 的 Maven 4。

### 默认 MySQL

先在本地 MySQL 创建专用库及账号（将示例密码替换为自己的密码）：

```sql
CREATE DATABASE dividend_lab CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'dividend_app'@'localhost' IDENTIFIED BY 'replace-with-your-password';
GRANT ALL PRIVILEGES ON dividend_lab.* TO 'dividend_app'@'localhost';
```

在项目根目录执行，密码只通过环境变量读取，不写入源码：

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=dividend_lab
export DB_USER=dividend_app
read -s DB_PASSWORD
export DB_PASSWORD
mvn clean verify
mvn spring-boot:run
```

运行 `read -s DB_PASSWORD` 后输入密码并回车。若使用已有账号，只需调整环境变量，无需重新建账号。应用自动建表，但不自动建库。未设置 DB_PASSWORD 时默认 MySQL 启动会失败，这是为了避免内置密码。

访问 **http://localhost:8080**。仅监听 127.0.0.1，定位为无登录的本地个人工具。

### H2 零配置启动

```bash
mvn clean verify
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

数据库文件位于启动工作目录的 `data/dividend-lab.mv.db`。在项目根目录启动时即为项目下的 `data/`。关闭服务后备份该目录即可。不要让两个实例同时打开同一数据库文件。

### 打包运行

```bash
mvn clean verify
java -jar target/dividend-lab-1.0.0.jar
# 或使用 H2
java -jar target/dividend-lab-1.0.0.jar --spring.profiles.active=h2
```

可用 `PORT=8081` 更换端口。MySQL 数据文件由本机 MySQL 服务管理，可在 MySQL 执行 `SHOW VARIABLES LIKE 'datadir';` 查看位置。

## 使用流程

1. 新建标的，填写代码、名称、行业、价格，可选股本和市值。
2. 点击标的“测算”，选择 EPS 或净利润口径，录入盈利和分红数据。
3. 完整有效输入后，页面自动调用后端更新结果。
4. 调整悲观、基准、乐观三个场景的盈利增速、分红率、目标价。
5. 保存记录；看板比较每个标的最近更新的记录，历史列表可查看、编辑、删除。

记录保存独立参数快照及标的名称/代码，不会因为后来调整标的价格而自动改变。看板分别显示当前标的价格和快照测算价。编辑历史记录会覆盖原记录；点“新建 / 清空”后保存才创建另一条记录。删除有记录的标的前需先删除对应记录。

## 单位和核心公式

价格、EPS、每股分红均以元计；总利润、市值、总分红以元计；股本以股计。不是“万元”或“亿股”。分红率、回购率、增长率传入百分数数值，例如60表示60%，返回股息率6表示6%。

| 指标 | EPS 口径 | 总额口径 |
|---|---|---|
| TTM 股息率 | 过去12个月每股现金分红 / 股价 | 过去12个月现金分红总额 / 市值 |
| 前瞻股息率 | max(预计 EPS, 0) × 分红率 / 股价 | max(预计净利润, 0) × 分红率 / 市值 |
| 可持续股息率 | max(正常化 EPS, 0) × 可持续分红率 / 股价 | max(正常化净利润, 0) × 可持续分红率 / 市值 |
| 股东回报率（简化） | 前瞻股息率 + 回购率 | 同左 |

以上比例转换为百分数显示，后端结果保留4位小数，页面显示2位。市值缺省时用股价×股本推算；同时填股本和市值时，二者隐含市值偏差超过1%拒绝计算。EPS 模式无需股本，TOTAL 模式必须有股本或市值。

场景盈利 = 本表预计盈利 × (1 + 场景增速 / 100)。增长率基于本表预计盈利，并非自动从去年利润推算。场景市值 = 当前市值 × 场景价 / 当前价，假设股本不变。场景股息率使用场景盈利、场景分红率、场景价/市值计算。

负盈利允许录入，但按比例推算的分红下限为0；企业实际可能用留存收益分红，需使用者自行判断。TTM 分红、回购率不能为负；分红率/回购率0～100；增长率-100～1000；价格、市值、股本必须大于0。三个情景必须完整且名称唯一。

切换口径时，如果有股本或市值/股价可推算股本，前端会换算盈利、正常化盈利和TTM分红；否则清空这三个字段要求重新录入。

所有指标为税前、名义值，不含个税、手续费、通胀或价格涨跌。回购率需手工提供同一未来12个月区间的预计回购金额/当前市值，不扣除增发稀释，不等于实际收到现金。ETF底层资产股息率不等于ETF实际现金分红率，ETF专用模式支持手工持仓穿透加权，但不自动预测基金分配政策。

## API

JSON 请求和响应，基础路径 `/api`。

| 方法 | 路径 | 功能 |
|---|---|---|
| GET / POST | `/assets` | 列出 / 新建标的 |
| PUT / DELETE | `/assets/{id}` | 编辑 / 删除标的 |
| POST | `/calculate` | 无持久化实时计算 |
| GET / POST | `/measurements` | 列出 / 新建测算快照 |
| GET / PUT / DELETE | `/measurements/{id}` | 查看 / 编辑 / 删除快照 |

标的请求示例：

```json
{"code":"DEMO","name":"示例公司","industry":"示例","price":100,"shares":10000000,"marketCap":1000000000}
```

`POST /api/calculate` 示例（也见 `examples/calculate.json`）：

```json
{
  "mode":"EPS", "price":100, "shares":10000000, "marketCap":1000000000,
  "eps":10, "profit":null, "payout":60,
  "normalizedEps":6, "normalizedProfit":null, "sustainablePayout":50,
  "ttmDividend":4, "buyback":2,
  "scenarios":[
    {"name":"悲观","growth":-20,"payout":50,"price":100},
    {"name":"基准","growth":0,"payout":60,"price":100},
    {"name":"乐观","growth":20,"payout":60,"price":100}
  ]
}
```

```bash
curl -sS http://localhost:8080/api/calculate -H 'Content-Type: application/json' --data-binary @examples/calculate.json
```

结果为 `ttm:4, forward:6, sustainable:3, shareholder:8`，情景分别4%、6%、7.2%。TOTAL模式把 `eps/normalizedEps` 换成 `profit/normalizedProfit`，TTM分红换成总额。

保存请求：`{"assetId":1,"title":"基准测算","input":{...上述计算参数...}}`。响应包含id、标的快照、创建/更新时间、input和result。创建/更新返回200，成功删除返回200空响应，参数错误400，未找到404，唯一约束等冲突409。记录列表按id倒序，看板依据updatedAt选择最近记录。

## 架构与后续数据源

- `Calculator`：纯 Java BigDecimal 计算及规则校验，无 Spring 或数据库依赖。
- `Api`：REST、标的CRUD、测算快照CRUD。
- `AssetRepository` / `MeasurementRepository`：JPA持久化，两种数据库共用模型。
- `MarketDataProvider`：预留 `quote(code)` 和 `financials(code)` 接口。
- `ManualDataProvider`：当前返回空 Optional，由用户录入。后续可实现真实数据源适配器，再增加显式“拉取数据”接口；当前不自动联网覆盖手工数据。
- `static/`：原生前端，自动请求后端计算，避免两套公式不一致。

本地开发采用Hibernate ddl-auto=update。切换数据库只改profile和连接环境变量，但已有数据不会自动迁移；应先导出/迁移数据。若向生产演进，需增加认证、访问控制、数据库迁移版本管理，并规划升级受支持的JDK与Spring Boot版本。

## 测试与本次验证状态

`mvn clean verify` 运行纯公式检查以及H2内存数据库的MockMvc集成测试（创建标的、保存/读取/编辑/删除记录、校验错误、引用保护）。测试固定使用H2，不访问个人MySQL数据库。

本次环境已实际通过：
- 使用本机Java 8的javac编译独立计算引擎和检查程序，并执行计算/校验检查。
- 使用Node检查前端JavaScript语法。

**尚未验证完整Maven构建、Spring Boot启动、真实HTTP API及浏览器页面。** 本机缺少Maven，下载尝试因环境DNS/提权权限策略被拦截；不能将已写入的集成测试视为已通过。

无Maven时可复现纯计算检查：

```bash
mkdir -p target/core-checks
javac -encoding UTF-8 -d target/core-checks src/main/java/com/example/dividend/Calculator.java src/test/java/com/example/dividend/CalculatorChecks.java
java -cp target/core-checks com.example.dividend.CalculatorChecks
```


## ETF 专用测算（新增）

在计算口径选择「ETF · 现金分红 / 底层持仓」。例如159222，先新建标的，再输入价格和以下参数。无需EPS、净利润或100%分红率。数据均需手工核实，不内置159222实时行情或预测。

- 过去12个月实际每份现金分红、未来12个月预计每份现金分红、可持续每份现金分红假设：单位元/份；允许空值，返回null、页面显示“—”，代表未知。明确没有分红才填0。每10份分红需除以10转换。
- 三个基金现金分红率 = 对应每份现金分红 ÷ 买入价格 × 100%。已录入的基金现金分红不重复扣费。
- ETF情景 = 预计每份现金分红 × (1 + 分红增幅/100) ÷ 场景价格 × 100%，不输入场景分红率。
- ETF不计算股票回购或股东回报率；shareholder返回null。
- 底层持仓按基金净资产占比输入weight（百分数），同时输入各股票的前瞻和可持续股息率（百分数）。底层贡献 = Σ(weight × yield / 100)，输出百分数。
- 权重必须大于0，总和不得超过100%，代码不可重复。支持不完整持仓，但绝不自动归一化：20%权重、5%股息率仅贡献1个百分点，不能当成基金股息率5%。任一持仓该类股息率缺失，则该类汇总返回null。
- 只有覆盖率恰好100%、前瞻股息率齐全、填写单位净值nav和年费率annualFee时，才计算扣费后股息贡献相对于买入价 = (底层前瞻贡献 − 年费率) × 单位净值 / 买入价格。可能为负，仅为简化贡献估算，不是基金现金分红或基金总收益，也未计税费、交易成本和未来调仓。
- annualFee是管理费、托管费等年费率合计，输入0.2表示0.2%。所有权重、净值与持仓股息率应尽可能取同一时点。
- ETF参数和持仓随历史记录保存；旧EPS/TOTAL记录继续按旧口径读取。不自动将之前借用EPS录入的ETF记录转换，需新建ETF记录重新输入。

API仍使用POST /api/calculate和原有测算记录接口，mode新增ETF。新增字段为expectedDistribution、sustainableDistribution、nav、annualFee、holdings；持仓字段code、weight、forwardYield、sustainableYield。ETF场景payout须省略或null。响应新增coverage、underlyingForward、underlyingSustainable、netDividendEstimate。参见examples/etf.json（纯假设演示，非159222实际数据）。

ETF改动验证：Maven离线test-compile成功；使用本地依赖直接运行6项ETF测试和股票计算检查，以及Spring TestContext/MockMvc的股票、ETF创建/读取/更新/删除持久化测试。前端通过JavaScript语法检查。标准mvn test所需Surefire插件尚未缓存，离线执行失败；未完成真实浏览器与MySQL联调。
