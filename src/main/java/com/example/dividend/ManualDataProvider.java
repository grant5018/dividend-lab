package com.example.dividend;
import java.util.Optional;
import org.springframework.stereotype.Service;
@Service public class ManualDataProvider implements MarketDataProvider {
 public Optional<Asset> quote(String code){return Optional.empty();}
 public Optional<Calculator.Input> financials(String code){return Optional.empty();}
}
