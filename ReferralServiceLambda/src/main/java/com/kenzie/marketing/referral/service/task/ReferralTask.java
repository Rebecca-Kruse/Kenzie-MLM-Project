package com.kenzie.marketing.referral.service.task;

import com.kenzie.marketing.referral.model.LeaderboardEntry;
import com.kenzie.marketing.referral.service.ReferralService;
import com.kenzie.marketing.referral.service.dao.ReferralDao;
import com.kenzie.marketing.referral.service.model.ReferralRecord;

import java.util.concurrent.Callable;

public class ReferralTask implements Callable<LeaderboardEntry> {

    private final ReferralRecord record;
    private final ReferralService service;

    public ReferralTask(ReferralRecord record, ReferralDao referralDao){
        this.record = record;
        this.service = new ReferralService(referralDao);
    }

    @Override
    public LeaderboardEntry call() throws Exception {
        return new LeaderboardEntry(service.getDirectReferrals(record.getCustomerId()).size(), record.getCustomerId());
    }
}
