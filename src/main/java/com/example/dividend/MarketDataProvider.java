package com.example.dividend;
import java.util.Optional;
/** Future adapters may fetch quotes and financials. Manual entry is currently authoritative. */
public interface MarketDataProvider {
 Optional<Asset> quote(String code);
 Optional<Calculator.Input> financials(String code);
}
