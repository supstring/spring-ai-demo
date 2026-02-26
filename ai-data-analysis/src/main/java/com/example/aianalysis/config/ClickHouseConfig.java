package com.example.aianalysis.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * ClickHouse 数据源配置
 * 当 app.mcp.enabled=false 时使用 JDBC 直连
 */
@Configuration
@ConditionalOnProperty(name = "app.mcp.enabled", havingValue = "false", matchIfMissing = true)
public class ClickHouseConfig {

    @Value("${clickhouse.url:jdbc:clickhouse://localhost:8123/default}")
    private String url;

    @Value("${clickhouse.username:default}")
    private String username;

    @Value("${clickhouse.password:}")
    private String password;

    @Bean("clickhouseDataSource")
    @Primary
    public DataSource clickhouseDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        ds.setConnectionTestQuery("SELECT 1");
        ds.setMaximumPoolSize(5);
        return ds;
    }
}
