package com.pulsebrief.digest.service;

import com.pulsebrief.digest.api.DigestDetailResponse;
import com.pulsebrief.digest.api.TodayDigestResponse;

public interface DigestService {
    TodayDigestResponse getTodayDigest();

    DigestDetailResponse getDigestDetail(Long id);
}
