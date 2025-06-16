package com.example.demo;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ai.tool.annotation.Tool;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;

import java.io.IOException;
import java.util.*;

@Service
public class KiteTradingService {
    private KiteConnect kiteConnect;
    private String accessToken;

    private static final Logger log = LoggerFactory.getLogger(KiteTradingService.class);

    @Value("${kite.api.key}")
    private String apiKey;

    @Value("${kite.api.secret}")
    private String apiSecret;

    @PostConstruct
    public void init() {
        log.info("Initializing KiteTradingService with API Key: {}", apiKey);
        this.kiteConnect = new KiteConnect(apiKey);
        
        // Set session expiry callback
        kiteConnect.setSessionExpiryHook(() -> {
            log.warn("Kite session expired!");
        });
    }

    private boolean isSessionActive() {
        return kiteConnect != null
            && kiteConnect.getAccessToken() != null
            && !kiteConnect.getAccessToken().isEmpty();
    }

    @Tool(name = "get_login_url", description = "Get Kite login URL for authorization")
    public String getLoginUrl() {
        if (kiteConnect == null) {
            log.error("KiteConnect not initialized. API Key: {}", apiKey);
            return null;
        }
        String url = kiteConnect.getLoginURL();
        log.info("Generated login URL: {}", url);
        return url;
    }

    @Tool(name = "generate_session", description = "Generate Kite session using request token")
    public Map<String, Object> generateSession(String requestToken) {
        try {
            User user = kiteConnect.generateSession(requestToken, apiSecret);
            kiteConnect.setAccessToken(user.accessToken);
            kiteConnect.setPublicToken(user.publicToken);
            this.accessToken = user.accessToken;

            Map<String, Object> response = new HashMap<>();
            response.put("access_token", user.accessToken);
            response.put("public_token", user.publicToken);
            response.put("user_id", user.userId);
            response.put("message", "Session generated successfully");
            
            return response;
        } catch (KiteException | IOException e) {
            log.error("Error generating session", e);
            return Map.of(
                "error", e.getMessage(),
                "status", "failed"
            );
        }
    }

    @Tool(name = "place_order", description = "Place a trading order")
    public Map<String, Object> placeOrder(String tradingSymbol, String transactionType, 
                                        int quantity, double price, String product) {
        if (!isSessionActive()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "failed");
            errorResponse.put("error", "Kite session is not active. Please login first.");
            errorResponse.put("error_type", "AUTHENTICATION_ERROR");
            errorResponse.put("error_code", "SESSION_EXPIRED");
            errorResponse.put("suggestion", "Please use get_login_url and generate_session tools to authenticate first.");
            return errorResponse;
        }
        try {
            OrderParams orderParams = new OrderParams();
            // Set required parameters
            orderParams.tradingsymbol = tradingSymbol;
            orderParams.transactionType = transactionType.equals("BUY") ? 
                Constants.TRANSACTION_TYPE_BUY : Constants.TRANSACTION_TYPE_SELL;
            orderParams.quantity = quantity;
            orderParams.price = price;
            orderParams.product = product != null ? product : Constants.PRODUCT_CNC;
            
            // Set optional parameters with defaults
            orderParams.orderType = Constants.ORDER_TYPE_LIMIT;
            orderParams.exchange = Constants.EXCHANGE_NSE;
            orderParams.validity = Constants.VALIDITY_DAY;
            orderParams.triggerPrice = 0.0;
            orderParams.tag = "MCP";
            orderParams.marketProtection = 0;

            // Place order with regular variety
            Order order = kiteConnect.placeOrder(orderParams, Constants.VARIETY_REGULAR);
            
            Map<String, Object> response = new HashMap<>();
            response.put("order_id", order.orderId);
            response.put("status", "success");
            response.put("message", "Order placed successfully");
            response.put("details", Map.of(
                "trading_symbol", tradingSymbol,
                "transaction_type", transactionType,
                "quantity", quantity,
                "price", price,
                "product", orderParams.product,
                "order_type", orderParams.orderType,
                "validity", orderParams.validity,
                "exchange", orderParams.exchange
            ));
            
            return response;
        } catch (KiteException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "failed");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("error_type", "KITE_API_ERROR");
            errorResponse.put("error_code", e.getClass().getSimpleName());
            errorResponse.put("suggestion", "Please check the order parameters and try again.");
            return errorResponse;
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "failed");
            errorResponse.put("error", "Network error occurred while placing order");
            errorResponse.put("error_type", "NETWORK_ERROR");
            errorResponse.put("error_code", "IO_EXCEPTION");
            errorResponse.put("suggestion", "Please check your internet connection and try again.");
            return errorResponse;
        } catch (JSONException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "failed");
            errorResponse.put("error", "Error processing API response");
            errorResponse.put("error_type", "RESPONSE_ERROR");
            errorResponse.put("error_code", "JSON_EXCEPTION");
            errorResponse.put("suggestion", "Please try again later.");
            return errorResponse;
        }
    }

    @Tool(name = "get_margins", description = "Get account margins")
    public Map<String, Object> getMargins(String segment) {
        if (!isSessionActive()) {
            log.error("Kite session is not active. Please login first.");
            return Map.of("error", "Kite session is not active. Please login first.", "status", "failed");
        }
        try {
            Margin margins = kiteConnect.getMargins(segment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("available_cash", margins.available.cash);
            response.put("utilised_debits", margins.utilised.debits);
            response.put("status", "success");
            
            return response;
        } catch (KiteException | IOException e) {
            log.error("Error getting margins", e);
            return Map.of(
                "error", e.getMessage(),
                "status", "failed"
            );
        }
    }

    @Tool(name = "get_holdings", description = "Get current portfolio holdings")
    public Map<String, Object> getHoldings() {
        if (!isSessionActive()) {
            log.error("Kite session is not active. Please login first.");
            return Map.of("error", "Kite session is not active. Please login first.", "status", "failed");
        }
        try {
            List<Holding> holdings = kiteConnect.getHoldings();
            
            Map<String, Object> response = new HashMap<>();
            response.put("total_holdings", holdings.size());
            
            List<Map<String, Object>> holdingsList = new ArrayList<>();
            for (Holding holding : holdings) {
                Map<String, Object> holdingData = new HashMap<>();
                holdingData.put("trading_symbol", holding.tradingSymbol);
                holdingData.put("day_change", holding.dayChange);
                holdingData.put("day_change_percentage", holding.dayChangePercentage);
                holdingData.put("quantity", holding.quantity);
                holdingData.put("average_price", holding.averagePrice);
                holdingsList.add(holdingData);
            }
            
            response.put("holdings", holdingsList);
            response.put("status", "success");
            
            return response;
        } catch (KiteException | IOException e) {
            log.error("Error getting holdings", e);
            return Map.of(
                "error", e.getMessage(),
                "status", "failed"
            );
        }
    }

    @Tool(name = "get_positions", description = "Get current positions with P&L analysis")
    public Map<String, Object> getPositions() {
        if (!isSessionActive()) {
            log.error("Kite session is not active. Please login first.");
            return Map.of("error", "Kite session is not active. Please login first.", "status", "failed");
        }
        try {
            // Check if required parameters are available
            if (apiKey == null || accessToken == null) {
                log.error("API key or access token is missing");
                return Map.of("error", "Authentication parameters missing", "status", "failed");
            }
            
            // Get positions using KiteConnect
            Map<String, List<Position>> positions = kiteConnect.getPositions();
            
            // Process net positions
            List<Map<String, Object>> netPositions = new ArrayList<>();
            for (Position position : positions.get("net")) {
                if (position.netQuantity != 0) {
                    Map<String, Object> posData = new HashMap<>();
                    posData.put("trading_symbol", position.tradingSymbol);
                    posData.put("quantity", position.netQuantity);
                    posData.put("average_price", position.averagePrice);
                    posData.put("last_price", position.lastPrice);
                    posData.put("pnl", position.pnl);
                    posData.put("unrealised", position.unrealised);
                    posData.put("realised", position.realised);
                    posData.put("product", position.product);
                    posData.put("instrument_token", position.instrumentToken);
                    netPositions.add(posData);
                }
            }
            
            // Process day positions
            List<Map<String, Object>> dayPositions = new ArrayList<>();
            for (Position position : positions.get("day")) {
                if (position.netQuantity != 0) {
                    Map<String, Object> posData = new HashMap<>();
                    posData.put("trading_symbol", position.tradingSymbol);
                    posData.put("quantity", position.netQuantity);
                    posData.put("average_price", position.averagePrice);
                    posData.put("last_price", position.lastPrice);
                    posData.put("pnl", position.pnl);
                    posData.put("product", position.product);
                    dayPositions.add(posData);
                }
            }
            
            // Calculate summary statistics
            double totalPnl = netPositions.stream()
                .mapToDouble(pos -> (Double) pos.get("pnl"))
                .sum();
            
            double totalUnrealised = netPositions.stream()
                .mapToDouble(pos -> (Double) pos.get("unrealised"))
                .sum();
            
            // Prepare result
            Map<String, Object> result = new HashMap<>();
            result.put("net_positions", netPositions);
            result.put("day_positions", dayPositions);
            result.put("total_positions", netPositions.size());
            result.put("total_pnl", totalPnl);
            result.put("total_unrealised_pnl", totalUnrealised);
            result.put("status", "success");
            
            log.info("Successfully retrieved {} net positions and {} day positions", 
                    netPositions.size(), dayPositions.size());
            
            return result;
            
        } catch (KiteException e) {
            log.error("Kite API error while getting positions: {}", e.getMessage(), e);
            return Map.of("error", "Kite API error: " + e.getMessage(), "status", "failed");
        } catch (IOException e) {
            log.error("IO error while getting positions: {}", e.getMessage(), e);
            return Map.of("error", "Network error: " + e.getMessage(), "status", "failed");
        } catch (Exception e) {
            log.error("Unexpected error while getting positions: {}", e.getMessage(), e);
            return Map.of("error", "Unexpected error: " + e.getMessage(), "status", "failed");
        }
    }

    @Tool(name = "get_risk_analysis", description = "Get portfolio risk analysis")
    public Map<String, Object> getRiskAnalysis() {
        if (!isSessionActive()) {
            log.error("Kite session is not active. Please login first.");
            return Map.of("error", "Kite session is not active. Please login first.", "status", "failed");
        }
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Get margins
            Margin margins = kiteConnect.getMargins("equity");
            response.put("available_cash", margins.available.cash);
            response.put("utilised_debits", margins.utilised.debits);
            
            // Get holdings for diversification analysis
            List<Holding> holdings = kiteConnect.getHoldings();
            Map<String, Double> sectorExposure = new HashMap<>();
            double totalValue = 0;
            
            for (Holding holding : holdings) {
                totalValue += holding.averagePrice * holding.quantity;
                // You would need to maintain a mapping of stocks to sectors
                // sectorExposure.merge(holding.sector, holding.averagePrice * holding.quantity, Double::sum);
            }
            
            response.put("total_portfolio_value", totalValue);
            response.put("sector_exposure", sectorExposure);
            
            return response;
        } catch (KiteException | IOException e) {
            log.error("Error getting risk analysis", e);
            return Map.of("error", e.getMessage(), "status", "failed");
        }
    }

    // TODO: Implement historical performance properly
    // @Tool(name = "get_historical_performance", description = "Get historical portfolio performance")
    // public Map<String, Object> getHistoricalPerformance(String fromDate, String toDate) {
    //     if (!isSessionActive()) {
    //         log.error("Kite session is not active. Please login first.");
    //         return Map.of("error", "Kite session is not active. Please login first.", "status", "failed");
    //     }
    //     try {
    //         SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    //         Date from = formatter.parse(fromDate);
    //         Date to = formatter.parse(toDate);
            
    //         Map<String, Object> response = new HashMap<>();
    //         List<Map<String, Object>> dailyPerformance = new ArrayList<>();
            
    //         // Get historical data for each holding
    //         List<Holding> holdings;
    //         try {
    //             holdings = kiteConnect.getHoldings();
    //         } catch (KiteException | IOException e) {
    //             log.error("Error fetching holdings: {}", e.getMessage());
    //             return Map.of(
    //                 "error", "Failed to fetch holdings: " + e.getMessage(),
    //                 "status", "failed"
    //             );
    //         }
            
    //         for (Holding holding : holdings) {
    //             try {
    //                 HistoricalData data = kiteConnect.getHistoricalData(
    //                     from, to, 
    //                     holding.instrumentToken, 
    //                     "day", 
    //                     false, 
    //                     false
    //                 );
                    
    //                 if (data != null && data.dataArrayList != null) {
    //                     for (Object point : data.dataArrayList) {
    //                         Map<String, Object> dayData = new HashMap<>();
    //                         dayData.put("date", point.toString());
    //                         dayData.put("symbol", holding.tradingSymbol);
    //                         dayData.put("instrument_token", holding.instrumentToken);
    //                         dailyPerformance.add(dayData);
    //                     }
    //                 }
    //             } catch (KiteException | IOException e) {
    //                 log.warn("Error fetching historical data for {}: {}", holding.tradingSymbol, e.getMessage());
    //                 // Continue with next holding even if one fails
    //                 continue;
    //             }
    //         }
            
    //         response.put("daily_performance", dailyPerformance);
    //         response.put("status", "success");
    //         return response;
    //     } catch (Exception e) {
    //         log.error("Error getting historical performance", e);
    //         return Map.of(
    //             "error", e.getMessage(),
    //             "status", "failed"
    //         );
    //     }
    // }

    @Tool(name = "get_order_analysis", description = "Get order history analysis")
    public Map<String, Object> getOrderAnalysis() {
        if (!isSessionActive()) {
            log.error("Kite session is not active. Please login first.");
            return Map.of("error", "Kite session is not active. Please login first.", "status", "failed");
        }
        try {
            List<Order> orders = kiteConnect.getOrders();
            
            Map<String, Object> response = new HashMap<>();
            Map<String, Integer> symbolOrderCount = new HashMap<>();
            Map<String, Double> symbolTotalValue = new HashMap<>();
            
            for (Order order : orders) {
                symbolOrderCount.merge(order.tradingSymbol, 1, Integer::sum);
                symbolTotalValue.merge(
                    order.tradingSymbol, 
                    Double.parseDouble(order.quantity) * Double.parseDouble(order.price), 
                    Double::sum
                );
            }
            
            response.put("total_orders", orders.size());
            response.put("symbol_order_count", symbolOrderCount);
            response.put("symbol_total_value", symbolTotalValue);
            
            return response;
        } catch (KiteException | IOException e) {
            log.error("Error getting order analysis", e);
            return Map.of("error", e.getMessage(), "status", "failed");
        }
    }
} 