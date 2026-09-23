package io.github.mabdulloh.booklending.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "app.borrowing")
public class BorrowingRulesConfig {
    private int maxActiveLoansPerMember = 3;
    private int loanDurationDays = 14;
}