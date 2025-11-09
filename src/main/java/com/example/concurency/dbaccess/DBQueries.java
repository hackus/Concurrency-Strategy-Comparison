package com.example.concurency.dbaccess;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Slf4j
public class DBQueries {

    static Optional<UserInfo> insertUser(Long index, String name, HikariDataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO users (id, name) VALUES (?, ?)")) {
            ps.setLong(1, index);
            ps.setString(2, name);
            ps.executeUpdate();
            return Optional.of(new UserInfo(index, name));
        } catch (SQLException e) {
            log.error("Cannot insert user with id:[{}] and name:[{}]", index, name);
        }
        return Optional.empty();
    }

    static Optional<UserInfo> updateUser(long id, String name, HikariDataSource dataSource) {

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET name = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setLong(2, id);
            ps.executeUpdate();
            return Optional.of(new UserInfo(id, name));
        } catch (SQLException e) {
            log.error("Cannot update user with id:[{}] and name:[{}]", id, name);
        }

        return Optional.empty();
    }

    static Optional<UserInfo> selectUser(long id, HikariDataSource dataSource) {

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("Select id, name FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                long userId = rs.getLong("id");
                String userName = rs.getString("name");
                return Optional.of(new UserInfo(userId, userName));
            } else {
                log.error("Selecting user with id:{} not found", id);
            }
        } catch (SQLException e) {
            log.error("Cannot select user with id:[{}], exception: {}", id, e.getMessage());
        }

        return Optional.empty();
    }
}
