package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.ReserveAlarmRequest;
import com.tuttime.admin.dto.ReserveAlarmResponse;
import com.tuttime.admin.dto.TokenReserveResponse;
import com.tuttime.admin.dto.TokenReservesAggregatedResponse;
import com.tuttime.admin.entity.ReserveAlarm;
import com.tuttime.admin.entity.TokenReserve;
import com.tuttime.admin.repository.ReserveAlarmRepository;
import com.tuttime.admin.repository.TokenReserveRepository;
import com.tuttime.admin.service.AdminTokenReserveService;
import com.tuttime.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Token Reserve Service Implementation
 * 
 * <p>Manages token reserves observation and alarms for monitoring
 * treasury balances, hot wallet balances, token prices, and supply.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTokenReserveServiceImpl implements AdminTokenReserveService {

    private final TokenReserveRepository tokenReserveRepository;
    private final ReserveAlarmRepository reserveAlarmRepository;

    @Override
    @Transactional(readOnly = true)
    public TokenReservesAggregatedResponse getAggregatedReserves() {
        log.debug("Fetching aggregated token reserves across all networks");
        
        // TODO: Integrate with blockchain contracts to fetch real-time data
        // 1. Connect to smart contracts for each network (Ethereum, BSC, Polygon, etc.)
        //    - Use Web3j or similar library for Ethereum-based chains
        //    - Use appropriate SDK for other chains (Solana, etc.)
        // 2. Fetch treasury balance from contract
        //    - Call contract method: getTreasuryBalance() or read from treasury address
        // 3. Fetch hot wallet balance from contract
        //    - Call contract method: getHotWalletBalance() or read from hot wallet address
        // 4. Fetch token price from DEX or price oracle
        //    - Integrate with Uniswap, PancakeSwap, or Chainlink price feeds
        //    - Or use CoinGecko/CoinMarketCap API as fallback
        // 5. Fetch total supply from contract
        //    - Call ERC20 totalSupply() method
        // 6. Calculate NAV = totalSupply * tokenPrice
        // 7. Aggregate balances across all networks
        //    - Sum treasury balances from all networks
        //    - Sum hot wallet balances from all networks
        //    - Use average or primary network price
        
        List<TokenReserve> allReserves = tokenReserveRepository.findAll();
        
        if (allReserves.isEmpty()) {
            // Return placeholder data if no reserves found
            log.warn("No token reserves found in database. Returning placeholder data.");
            return TokenReservesAggregatedResponse.builder()
                    .treasuryBalance(BigDecimal.ZERO)
                    .hotWalletBalance(BigDecimal.ZERO)
                    .tokenPrice(BigDecimal.ZERO)
                    .totalSupply(BigDecimal.ZERO)
                    .nav(BigDecimal.ZERO)
                    .build();
        }
        
        // Aggregate balances across all networks
        BigDecimal totalTreasury = allReserves.stream()
                .map(TokenReserve::getTreasuryBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalHotWallet = allReserves.stream()
                .map(TokenReserve::getHotWalletBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Use the first reserve's price and supply (or calculate average)
        // TODO: Implement proper aggregation logic (average price, sum supply, etc.)
        TokenReserve firstReserve = allReserves.get(0);
        BigDecimal tokenPrice = firstReserve.getTokenPrice() != null 
                ? firstReserve.getTokenPrice() 
                : BigDecimal.ZERO;
        
        BigDecimal totalSupply = allReserves.stream()
                .map(TokenReserve::getTotalSupply)
                .filter(s -> s != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate NAV = totalSupply * tokenPrice
        BigDecimal nav = totalSupply.multiply(tokenPrice);
        
        return TokenReservesAggregatedResponse.builder()
                .treasuryBalance(totalTreasury)
                .hotWalletBalance(totalHotWallet)
                .tokenPrice(tokenPrice)
                .totalSupply(totalSupply)
                .nav(nav)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenReserveResponse> getAllReserves() {
        log.debug("Fetching all token reserves");
        return tokenReserveRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TokenReserveResponse getReserveByNetwork(String network) {
        log.debug("Fetching token reserve for network: {}", network);
        TokenReserve reserve = tokenReserveRepository.findByNetwork(network)
                .orElseThrow(() -> new BusinessException("Token reserve not found for network: " + network));
        return toResponse(reserve);
    }

    @Override
    @Transactional
    public TokenReserveResponse updateReserve(String network, 
                                             BigDecimal treasuryBalance,
                                             BigDecimal hotWalletBalance,
                                             BigDecimal tokenPrice,
                                             BigDecimal totalSupply,
                                             BigDecimal nav) {
        log.debug("Updating token reserve for network: {}", network);
        
        TokenReserve reserve = tokenReserveRepository.findByNetwork(network)
                .orElse(TokenReserve.builder()
                        .network(network)
                        .treasuryBalance(BigDecimal.ZERO)
                        .hotWalletBalance(BigDecimal.ZERO)
                        .build());
        
        reserve.setTreasuryBalance(treasuryBalance != null ? treasuryBalance : BigDecimal.ZERO);
        reserve.setHotWalletBalance(hotWalletBalance != null ? hotWalletBalance : BigDecimal.ZERO);
        reserve.setTokenPrice(tokenPrice);
        reserve.setTotalSupply(totalSupply);
        reserve.setNav(nav);
        
        TokenReserve saved = tokenReserveRepository.save(reserve);
        log.info("Updated token reserve for network: {}, treasury: {}, hot wallet: {}", 
                network, treasuryBalance, hotWalletBalance);
        
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReserveAlarmResponse> getActiveAlarms() {
        log.debug("Fetching all active alarms");
        return reserveAlarmRepository
                .findByStatusAndIsEnabled(ReserveAlarm.AlarmStatus.ACTIVE, true)
                .stream()
                .map(this::toAlarmResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReserveAlarmResponse> getAlarmsByNetwork(String network) {
        log.debug("Fetching alarms for network: {}", network);
        return reserveAlarmRepository.findActiveAlarmsForNetwork(
                ReserveAlarm.AlarmStatus.ACTIVE, true, network)
                .stream()
                .map(this::toAlarmResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReserveAlarmResponse createAlarm(ReserveAlarmRequest request, Long createdBy) {
        log.debug("Creating alarm: type={}, network={}, threshold={}", 
                request.getAlarmType(), request.getNetwork(), request.getThreshold());
        
        // TODO: Validate alarm configuration
        // - If alarmType is LOW_BALANCE, ensure threshold is reasonable (e.g., > 0)
        // - If alarmType is PRICE_DROP/PRICE_INCREASE, validate threshold format (percentage or absolute)
        // - If network is specified, validate network exists in token_reserves table
        // - Infer metricType from alarmType if not provided:
        //   * LOW_BALANCE -> TREASURY_BALANCE or HOT_WALLET_BALANCE
        //   * PRICE_DROP/PRICE_INCREASE -> PRICE
        //   * SUPPLY_THRESHOLD -> SUPPLY
        //   * NAV_THRESHOLD -> NAV
        
        // Infer metricType from alarmType if not provided
        String metricType = request.getMetricType();
        if (metricType == null || metricType.isEmpty()) {
            metricType = inferMetricTypeFromAlarmType(request.getAlarmType());
        }
        
        ReserveAlarm alarm = ReserveAlarm.builder()
                .network(request.getNetwork())
                .alarmType(ReserveAlarm.AlarmType.valueOf(request.getAlarmType()))
                .status(ReserveAlarm.AlarmStatus.ACTIVE)
                .thresholdValue(request.getThresholdValue())
                .comparisonOperator(request.getComparisonOperator() != null ? request.getComparisonOperator() : "LT")
                .metricType(metricType)
                .description(request.getDescription())
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true)
                .createdBy(createdBy)
                .build();
        
        ReserveAlarm saved = reserveAlarmRepository.save(alarm);
        log.info("Created alarm: id={}, type={}, network={}, threshold={}", 
                saved.getAlarmId(), saved.getAlarmType(), saved.getNetwork(), saved.getThresholdValue());
        
        return toAlarmResponse(saved);
    }
    
    /**
     * Infer metric type from alarm type if not explicitly provided
     * TODO: Implement this method to infer metricType from alarmType
     */
    private String inferMetricTypeFromAlarmType(String alarmType) {
        // Default inference logic
        return switch (alarmType) {
            case "LOW_BALANCE" -> "TREASURY_BALANCE"; // Default to treasury, could be HOT_WALLET_BALANCE
            case "PRICE_DROP", "PRICE_INCREASE", "HIGH_VOLATILITY" -> "PRICE";
            case "SUPPLY_THRESHOLD" -> "SUPPLY";
            case "NAV_THRESHOLD" -> "NAV";
            default -> "BALANCE"; // Fallback
        };
    }

    @Override
    @Transactional
    public ReserveAlarmResponse updateAlarm(Long alarmId, ReserveAlarmRequest request) {
        log.debug("Updating alarm: id={}", alarmId);
        
        ReserveAlarm alarm = reserveAlarmRepository.findById(alarmId)
                .orElseThrow(() -> new BusinessException("Alarm not found: " + alarmId));
        
        alarm.setNetwork(request.getNetwork());
        alarm.setAlarmType(ReserveAlarm.AlarmType.valueOf(request.getAlarmType()));
        alarm.setThresholdValue(request.getThresholdValue());
        alarm.setComparisonOperator(request.getComparisonOperator());
        alarm.setMetricType(request.getMetricType());
        alarm.setDescription(request.getDescription());
        if (request.getIsEnabled() != null) {
            alarm.setIsEnabled(request.getIsEnabled());
        }
        
        // Reset alarm if it was triggered/resolved and is being updated
        if (alarm.getStatus() != ReserveAlarm.AlarmStatus.ACTIVE && alarm.getIsEnabled()) {
            alarm.reset();
        }
        
        ReserveAlarm saved = reserveAlarmRepository.save(alarm);
        log.info("Updated alarm: id={}", saved.getAlarmId());
        
        return toAlarmResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAlarm(Long alarmId) {
        log.debug("Deleting alarm: id={}", alarmId);
        
        if (!reserveAlarmRepository.existsById(alarmId)) {
            throw new BusinessException("Alarm not found: " + alarmId);
        }
        
        reserveAlarmRepository.deleteById(alarmId);
        log.info("Deleted alarm: id={}", alarmId);
    }

    @Override
    @Transactional
    public void checkAndTriggerAlarms() {
        log.debug("Checking and triggering alarms");
        
        // TODO: Integrate alarm checking/triggering with scheduled jobs
        // 1. Create scheduled job class (e.g., ReserveAlarmScheduler) with @Scheduled annotation
        //    - Run every 5 minutes: @Scheduled(fixedRate = 300000)
        //    - Or use cron: @Scheduled(cron = "0 */5 * * * *") (every 5 minutes)
        // 2. In scheduled job, call this method: tokenReserveService.checkAndTriggerAlarms()
        // 3. Integrate with notification service:
        //    - When alarm is triggered, send notification via:
        //      * Email to admins
        //      * Slack webhook
        //      * SMS (for critical alarms)
        //      * Push notification to admin mobile app
        //    - Include alarm details: type, network, current value, threshold, description
        // 4. Handle alarm resolution:
        //    - Check if triggered alarms should be resolved (condition no longer met)
        //    - If condition no longer met, mark alarm as resolved
        //    - Send resolution notification
        // 5. Add alarm history/audit trail:
        //    - Log when alarm was triggered
        //    - Log when alarm was resolved
        //    - Track number of times alarm was triggered
        
        List<ReserveAlarm> activeAlarms = reserveAlarmRepository
                .findByStatusAndIsEnabled(ReserveAlarm.AlarmStatus.ACTIVE, true);
        
        List<TokenReserve> reserves = tokenReserveRepository.findAll();
        
        for (ReserveAlarm alarm : activeAlarms) {
            for (TokenReserve reserve : reserves) {
                // Skip if alarm is network-specific and doesn't match
                if (alarm.getNetwork() != null && !alarm.getNetwork().equals(reserve.getNetwork())) {
                    continue;
                }
                
                BigDecimal currentValue = getCurrentValue(reserve, alarm.getMetricType());
                if (currentValue != null && alarm.shouldTrigger(currentValue)) {
                    // Only trigger if not already triggered
                    if (alarm.getStatus() != ReserveAlarm.AlarmStatus.TRIGGERED) {
                        alarm.trigger();
                        reserveAlarmRepository.save(alarm);
                        log.warn("Alarm triggered: id={}, type={}, network={}, currentValue={}, threshold={}", 
                                alarm.getAlarmId(), alarm.getAlarmType(), reserve.getNetwork(), 
                                currentValue, alarm.getThresholdValue());
                        
                        // TODO: Send notification here
                        // notificationService.sendAlarmNotification(alarm, reserve, currentValue);
                    }
                } else if (alarm.getStatus() == ReserveAlarm.AlarmStatus.TRIGGERED) {
                    // Condition no longer met, resolve the alarm
                    alarm.resolve();
                    reserveAlarmRepository.save(alarm);
                    log.info("Alarm resolved: id={}, type={}, network={}, currentValue={}, threshold={}", 
                            alarm.getAlarmId(), alarm.getAlarmType(), reserve.getNetwork(), 
                            currentValue, alarm.getThresholdValue());
                    
                    // TODO: Send resolution notification here
                    // notificationService.sendAlarmResolutionNotification(alarm, reserve, currentValue);
                }
            }
        }
    }

    /**
     * Get current value for a metric type from a reserve
     */
    private BigDecimal getCurrentValue(TokenReserve reserve, String metricType) {
        return switch (metricType) {
            case "BALANCE" -> reserve.getTotalBalance();
            case "TREASURY_BALANCE" -> reserve.getTreasuryBalance();
            case "HOT_WALLET_BALANCE" -> reserve.getHotWalletBalance();
            case "PRICE" -> reserve.getTokenPrice();
            case "SUPPLY" -> reserve.getTotalSupply();
            case "NAV" -> reserve.getNav();
            default -> null;
        };
    }

    /**
     * Convert entity to response DTO
     */
    private TokenReserveResponse toResponse(TokenReserve reserve) {
        return TokenReserveResponse.builder()
                .reserveId(reserve.getReserveId())
                .network(reserve.getNetwork())
                .treasuryBalance(reserve.getTreasuryBalance())
                .hotWalletBalance(reserve.getHotWalletBalance())
                .totalBalance(reserve.getTotalBalance())
                .tokenPrice(reserve.getTokenPrice())
                .totalSupply(reserve.getTotalSupply())
                .nav(reserve.getNav())
                .lastUpdatedAt(reserve.getLastUpdatedAt())
                .createdAt(reserve.getCreatedAt())
                .updatedAt(reserve.getUpdatedAt())
                .build();
    }

    /**
     * Convert alarm entity to response DTO
     */
    private ReserveAlarmResponse toAlarmResponse(ReserveAlarm alarm) {
        return ReserveAlarmResponse.builder()
                .alarmId(alarm.getAlarmId())
                .network(alarm.getNetwork())
                .alarmType(alarm.getAlarmType().name())
                .status(alarm.getStatus().name())
                .thresholdValue(alarm.getThresholdValue())
                .comparisonOperator(alarm.getComparisonOperator())
                .metricType(alarm.getMetricType())
                .description(alarm.getDescription())
                .triggeredAt(alarm.getTriggeredAt())
                .resolvedAt(alarm.getResolvedAt())
                .isEnabled(alarm.getIsEnabled())
                .createdBy(alarm.getCreatedBy())
                .createdAt(alarm.getCreatedAt())
                .updatedAt(alarm.getUpdatedAt())
                .build();
    }
}

