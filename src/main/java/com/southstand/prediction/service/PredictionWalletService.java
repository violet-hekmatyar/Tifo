package com.southstand.prediction.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.southstand.prediction.entity.PredictionBeanLog;
import com.southstand.prediction.entity.PredictionWallet;
import com.southstand.prediction.mapper.PredictionBeanLogMapper;
import com.southstand.prediction.mapper.PredictionWalletMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the single source of truth for a user's initial 看台豆 principal.
 *
 * <p>Every account receives {@link #INITIAL_BALANCE} once. The wallet row is
 * unique by user_id, so registration and login can both call this safely.</p>
 */
@Service
public class PredictionWalletService {

    public static final int INITIAL_BALANCE = 1000;

    private final PredictionWalletMapper walletMapper;
    private final PredictionBeanLogMapper beanLogMapper;

    public PredictionWalletService(
            PredictionWalletMapper walletMapper,
            PredictionBeanLogMapper beanLogMapper
    ) {
        this.walletMapper = walletMapper;
        this.beanLogMapper = beanLogMapper;
    }

    /**
     * Returns the wallet for a user, creating it with the initial principal if
     * this account has never received one. Safe to call repeatedly.
     */
    @Transactional(rollbackFor = Exception.class)
    public PredictionWallet ensureWallet(long userId, String remark) {
        PredictionWallet wallet = find(userId);
        if (wallet != null) {
            return wallet;
        }

        wallet = new PredictionWallet();
        wallet.setUserId(userId);
        wallet.setBalance(INITIAL_BALANCE);
        try {
            walletMapper.insert(wallet);
        } catch (DuplicateKeyException concurrentInsert) {
            PredictionWallet existing = find(userId);
            if (existing != null) {
                return existing;
            }
            throw concurrentInsert;
        }

        PredictionBeanLog log = new PredictionBeanLog();
        log.setUserId(userId);
        log.setPredictionId(null);
        log.setChangeAmount(INITIAL_BALANCE);
        log.setBalanceAfter(INITIAL_BALANCE);
        log.setLogType("INITIAL_GRANT");
        log.setRemark(remark);
        beanLogMapper.insert(log);
        return wallet;
    }

    public PredictionWallet find(long userId) {
        return walletMapper.selectOne(Wrappers.<PredictionWallet>lambdaQuery()
                .eq(PredictionWallet::getUserId, userId));
    }
}
