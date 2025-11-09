package com.example.concurency.dbaccess;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class Utils {
    public static long getRandomUserId(HikariDataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM users ORDER BY RANDOM() LIMIT 1");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Cannot pick a random user: " + e);
//            return getRandomUserId(dataSource);
        }

        return 0;
    }
}
