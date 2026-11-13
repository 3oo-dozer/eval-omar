package com.eex.domain.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilTest {

    @Test
    void hashIsNotPlainTextAndMatchesOriginalPassword() {
        String hash = PasswordUtil.hashPassword("aliceSecret");

        assertThat(hash).isNotEqualTo("aliceSecret").startsWith("$2");
        assertThat(PasswordUtil.matches("aliceSecret", hash)).isTrue();
        assertThat(PasswordUtil.matches("wrong", hash)).isFalse();
    }
}
